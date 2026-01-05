package com.atakmap.android.takml_android.metrics;

import android.os.Environment;
import android.util.Log;

import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.configuration.MetricsConfiguration;
import com.atakmap.android.takml_android.net.TakmlServerClient;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.bbn.takml_server.client.models.AddModelMetricsRequest;
import com.bbn.takml_server.client.models.InferenceMetric;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricsManager {
    private static final String TAG = MetricsManager.class.getName();

    public static final String TAKML_METRICS_DIR = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "takml" + File.separator + "metrics";
    public static final String TAKML_METRICS_PENDING = TAKML_METRICS_DIR + File.separator
            + "pending_metrics.json";

    private final Takml takml;
    private volatile boolean running = false;
    private final MetricsConfiguration metricsConfiguration;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final Gson gson = new Gson();

    private final ConcurrentMap<String, List<InferenceMetric>> pendingMetrics = new ConcurrentHashMap<>();

    public MetricsManager(Takml takml){
        this.takml = takml;
        this.metricsConfiguration = takml.getTakmlServerConfiguration().getMetricsConfiguration();
    }

    public void start(){
        if (running) {
            return;
        }
        File metricsDir = new File(TAKML_METRICS_DIR);
        if (!metricsDir.exists()){
            metricsDir.mkdirs();
        }

        if (metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.OFFLINE_PUSH_ON_RECONNECT) {
            // read any pending metrics
            File pendingMetricsFile = new File(TAKML_METRICS_PENDING);
            if (pendingMetricsFile.exists()) {
                Type pendingMetricsType = new TypeToken<ConcurrentMap<String, List<InferenceMetric>>>() {
                }.getType();
                try (FileReader fileReader = new FileReader(pendingMetricsFile)) {
                    ConcurrentMap<String, List<InferenceMetric>> pendingOnDisk =
                            gson.fromJson(fileReader, pendingMetricsType);
                    if (pendingOnDisk != null) {
                        pendingMetrics.putAll(pendingOnDisk);
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "FileNotFoundException reading file " + pendingMetricsFile.getPath(), e);
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception reading file " + pendingMetricsFile.getPath(), e);
                }
            }
        }

        running = true;
        executorService.scheduleAtFixedRate(() -> {
            for (String modelNameVersion : pendingMetrics.keySet()) {
                // Atomically grab and remove the current list for this key
                List<InferenceMetric> inferenceMetrics = pendingMetrics.remove(modelNameVersion);
                if (inferenceMetrics == null || inferenceMetrics.isEmpty()) {
                    continue;
                }

                writeMetricsToDisk(modelNameVersion, inferenceMetrics);

                if (metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.ONLINE
                        || metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.OFFLINE_PUSH_ON_RECONNECT) {
                    submitModelInferenceMetrics(modelNameVersion, inferenceMetrics);
                }
            }
        }, metricsConfiguration.getPublishRateSeconds(),
        metricsConfiguration.getPublishRateSeconds(),
        TimeUnit.SECONDS);

    }

    public void consumeMetrics(TakmlModel takmlModel, long startTime, List<List<? extends TakmlResult>> takmlResultsList){
        if(metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.DISABLED){
            return;
        }

        List<InferenceMetric> inferenceMetrics = new ArrayList<>();
        for (List<? extends TakmlResult> takmlResults : takmlResultsList) {
            if (takmlResults.isEmpty()) {
                continue;
            }

            InferenceMetric inferenceMetric = new InferenceMetric();
            inferenceMetric.setStartMillis(startTime);
            inferenceMetric.setDurationMillis(System.currentTimeMillis() - startTime);

            TakmlResult takmlResult = takmlResults.iterator().next();
            if (takmlResult instanceof Recognition) {
                Recognition recognition = (Recognition) takmlResult;
                inferenceMetric.setConfidence(recognition.getConfidence());
            } else {
                /** not yet supported; must call submitModelInferenceMetrics manually */
            }

            inferenceMetrics.add(inferenceMetric);
        }
        AddModelMetricsRequest addModelMetricsRequest = new AddModelMetricsRequest();
        addModelMetricsRequest.setRequestId(UUID.randomUUID().toString());
        addModelMetricsRequest.setDeviceMetadata(DeviceInfoUtil.getInfo());
        addModelMetricsRequest.setModelName(takmlModel.getName());
        addModelMetricsRequest.setModelVersion(takmlModel.getVersionNumber());
        addModelMetricsRequest.setInferenceMetrics(inferenceMetrics);

        pendingMetrics.computeIfAbsent(takmlModel.getName() + "-" +
                takmlModel.getVersionNumber(), k -> Collections.synchronizedList(new ArrayList<>()))
                .addAll(addModelMetricsRequest.getInferenceMetrics());
    }


    /**
     * Write metrics to disk. This is written to:
     * {@link #TAKML_METRICS_DIR}
     */
    private void submitModelInferenceMetrics(String modelNameVersion, List<InferenceMetric> inferenceMetrics) {
        int idx = modelNameVersion.lastIndexOf("-");
        String name = modelNameVersion.substring(0, idx);
        String version = modelNameVersion.substring(idx + 1);

        AddModelMetricsRequest addModelMetricsRequest = new AddModelMetricsRequest();
        addModelMetricsRequest.setModelName(name);
        addModelMetricsRequest.setModelVersion(Double.valueOf(version));
        addModelMetricsRequest.setDeviceMetadata(DeviceInfoUtil.getInfo());
        addModelMetricsRequest.setRequestId(UUID.randomUUID().toString());
        addModelMetricsRequest.setInferenceMetrics(inferenceMetrics);

        TakmlServerClient takmlServerClient = takml.getTakmlServerClient();
        takmlServerClient.submitMetricsAsync(addModelMetricsRequest, (success, response) -> {
            Log.d(TAG, "metricsSubmitted: " + success);
            if(!success && metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.OFFLINE_PUSH_ON_RECONNECT){
                // Requeue for retry later
                pendingMetrics.computeIfAbsent(modelNameVersion, k -> new ArrayList<>()).addAll(inferenceMetrics);
            }
        });
    }

    /**
     * Submit Model Inference Metrics, representing a Model name/version with a collection of
     * recorded inference metrics
     */
    private void writeMetricsToDisk(String modelNameVersion, List<InferenceMetric> inferenceMetrics) {
        File file = new File(TAKML_METRICS_DIR, modelNameVersion.replaceAll("\\s+", "_") + ".json");
        Type inferenceMetricListType = new TypeToken<List<InferenceMetric>>(){}.getType();
        List<InferenceMetric> allInferenceMetrics = new ArrayList<>();
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                List<InferenceMetric> existing =
                        gson.fromJson(fileReader, inferenceMetricListType);
                if (existing != null) {
                    allInferenceMetrics.addAll(existing);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException reading file " + file.getPath(), e);
            } catch (IOException e) {
                Log.e(TAG, "IO Exception reading file " + file.getPath(), e);
            }
        } else{
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "IOException writing metrics to disk", e);
            }
        }

        allInferenceMetrics.addAll(inferenceMetrics);

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(allInferenceMetrics, writer);
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Could not create file " + file.getPath(), e);
        }
    }

    public void shutdown(){
        running = false;
        executorService.shutdown();
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks forcefully
                executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    Log.e(TAG, "Pool did not terminate");
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (metricsConfiguration.getOperationMode() == MetricsConfiguration.Mode.OFFLINE_PUSH_ON_RECONNECT) {
            // Write any pending metrics to disk
            try (Writer writer = new FileWriter(TAKML_METRICS_PENDING)) {
                gson.toJson(pendingMetrics, writer);
                writer.flush();
            } catch (IOException e) {
                Log.e(TAG, "Could not create file " + TAKML_METRICS_PENDING, e);
            }
        }
    }
}
