package com.atakmap.android.takml_android.net;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.atakmap.android.takml_android.Constants;
import com.atakmap.android.takml_android.util.TakServerUtils;
import com.atakmap.comms.NetConnectString;
import com.bbn.tak.comms.ApiClient;
import com.bbn.tak_sync_file_manager.FileDownload;
import com.bbn.tak_sync_file_manager.TakFileManagerClient;
import com.bbn.tak_sync_file_manager.model.FileInfo;
import com.bbn.tak_sync_file_manager.model.IndexFile;
import com.bbn.tak_sync_file_manager.model.IndexRow;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TakFsManager {
    private static final String TAG = TakFsManager.class.getName();
    private static TakFsManager takFsManager;
    private TakFileManagerClient takFileManagerClient;

    private static final int CONCURRENT_SEGMENTED_FILES_DOWNLOAD_LIMIT = 10;
    private static final int TIMEOUT_FILE_DOWNLOAD_SECONDS = 120; // 2 minutes
    private File segmentsDir, takmlDownloadDir;

    public static final String TAK_FS_TAKML_DOWNLOAD_DIR = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "takml_takfs_download";
    public static final String TAK_FS_TAKML_SEGMENTS_STORAGE_DIR = Environment
            .getExternalStorageDirectory() + File.separator + "atak" + File.separator
            + "takml_takfs_segments";

    public static TakFsManager getInstance(){
        if(takFsManager == null){
            takFsManager = new TakFsManager();
        }
        return takFsManager;
    }

    private TakFsManager(){
    }

    public void initialize(ApiClient apiClient){
        segmentsDir = new File(TAK_FS_TAKML_SEGMENTS_STORAGE_DIR);
        if(!segmentsDir.exists()){
            if(segmentsDir.mkdirs()){
                Log.d(TAG, "Created takml takfs storage directory");
            }else{
                Log.e(TAG, "Could not create takml takfs storage directory");
            }
        }
        takmlDownloadDir = new File(TAK_FS_TAKML_DOWNLOAD_DIR);
        if(!takmlDownloadDir.exists()){
            if(takmlDownloadDir.mkdirs()){
                Log.d(TAG, "Created takml takfs download storage directory");
            }else{
                Log.e(TAG, "Could not create takfs download storage directory");
            }
        }

        if(takFileManagerClient == null) {
            takFileManagerClient = new TakFileManagerClient(apiClient, "takml_takfs_sync_mission");
        }
    }

    public Set<IndexRow> getModels(){
        IndexFile indexFile = takFileManagerClient.downloadIndexFile();
        if(indexFile != null && indexFile.getCategoryToRows() != null){
            return indexFile.getCategoryToRows().get("Models");
        }
        return new HashSet<>();
    }

    public void downloadModel(IndexRow indexRow, DownloadTakmlModel callback){
        AsyncTask.execute((Runnable) () -> {
            boolean isSegmentedFile = indexRow.getOptionalSegments() != null
                    && !indexRow.getOptionalSegments().isEmpty();

            CountDownLatch countDownLatch = new CountDownLatch(1);
            if (isSegmentedFile) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setOptionalSegments(indexRow.getOptionalSegments());
                fileInfo.setAdditionalMetadata(indexRow.getAdditionalMetadata());
                fileInfo.setFileName(indexRow.getName());
                fileInfo.setHash(indexRow.getHash());

                takFileManagerClient.downloadSegmentedFile(takmlDownloadDir, segmentsDir, fileInfo,
                        (boolean success, String modelHash, String message, File file) -> {
                            if (success) {
                                callback.modelDownloaded(file);
                            } else {
                                Log.e(TAG, "Could not download segmented file with hash " + indexRow.getHash());
                            }
                            countDownLatch.countDown();
                        },
                        (modelHash, totalSegments, fileDownloadFailed, fetchedSegments, message) -> Log.d(TAG,
                                "Downloading large segmented model: " + fetchedSegments + "/" + totalSegments),
                        CONCURRENT_SEGMENTED_FILES_DOWNLOAD_LIMIT);
            } else {
                takFileManagerClient.downloadFile(indexRow.getHash(), (b, inputStream) -> {
                    if (b) {
                        File file = new File(takmlDownloadDir.getPath() + File.separator + indexRow.getName() + ".zip");
                        try(FileOutputStream outStream = new FileOutputStream(file)) {
                            IOUtils.copy(inputStream, outStream);
                        } catch (IOException e) {
                            Log.e(TAG, "IOException downloading takml model", e);
                        }
                        callback.modelDownloaded(file);
                    } else {
                        Log.e(TAG, "Could not download file with hash " + indexRow.getHash());
                    }
                    countDownLatch.countDown();
                });
            }

            try {
                if (!countDownLatch.await(TIMEOUT_FILE_DOWNLOAD_SECONDS, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Timed out waiting for operation");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Timed out waiting for operation");
            }
        });

    }
}
