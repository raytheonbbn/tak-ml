package com.atakmap.android.takml_android.remote_inference;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.TakmlExecutor;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.emm.TestTakml;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.net.SelectedTAKServer;
import com.atakmap.android.takml_android.takml_result.Regression;
import com.atakmap.android.takml_android.takml_result.TakmlResult;
import com.atakmap.android.takml_android.tensor_processor.InferInput;

import com.atakmap.android.takml_android.tensor_processor.InferOutput;
import com.atakmap.android.takml_android.tensor_processor.KserveTensorConverterUtil;
import com.atakmap.android.takml_android.tensor_processor.TensorProcessor;
import com.atakmap.android.takml_android.util.TakServerInfo;
import com.bbn.takml_server.client.models.InferenceRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RemoteInferenceTests {
    private static final String TAG = RemoteInferenceTests.class.getName();

    private Context context;
    private final KServeAPIServer kServeAPIServer = new KServeAPIServer();
    @Before
    public void before() throws IOException {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        kServeAPIServer.start();
    }

    @Test
    public void testRemoteModelExecution() throws TakmlInitializationException, InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TestTakml takml = new TestTakml(context);

        InferInput inferInput = new InferInput();
        inferInput.setData(new Float[]{1.0f});
        inferInput.setShape(new long[]{1});

        InferenceRequest inferenceRequest = new InferenceRequest();
        inferenceRequest.setInputs(KserveTensorConverterUtil.convertInferInputs(Collections.singletonList(inferInput)));

        TakmlModel takmlModel = new TakmlModel.TakmlRemoteModelBuilder("test", ModelTypeConstants.GENERIC_RECOGNITION, new TensorProcessor() {
            @Override
            public List<InferInput> processInputTensor(List<byte[]> input) {
                InferInput inferInput = new InferInput();
                float value = Float.parseFloat(new String(input.get(0)));
                inferInput.setData(new Float[]{value});
                inferInput.setShape(new long[]{1});
                return Collections.singletonList(inferInput);
            }

            @Override
            public List<List<? extends TakmlResult>> processOutputTensor(List<InferOutput> outputs) {
                return Collections.singletonList(Collections.singletonList(new Regression(outputs.iterator().next().getData()[0])));
            }
        }, "http://localhost:8342", "test").build();
        takml.addTakmlModel(takmlModel);
        TakmlExecutor remoteTakmlExecutor = takml.createExecutor(takmlModel);
        final List<TakmlResult> takmlResultsTest = new ArrayList<>();
        remoteTakmlExecutor.executePrediction("1.0".getBytes(StandardCharsets.UTF_8), (takmlResults, success, modelName, modelType) -> {
            synchronized (takmlResultsTest){
                takmlResultsTest.addAll(takmlResults.iterator().next());
            }
            countDownLatch.countDown();
        });

        if(!countDownLatch.await(10, TimeUnit.SECONDS)){
            Assert.fail("Timed out waiting for request");
        }

        Assert.assertFalse(takmlResultsTest.isEmpty());
        Regression takmlResult = (Regression) takmlResultsTest.iterator().next();

        Assert.assertEquals(takmlResult.getPredictionResult(), 2.0, 0.0001);
    }

    @After
    public void after(){
        kServeAPIServer.stop();
    }
}
