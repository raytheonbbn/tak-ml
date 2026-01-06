import static org.junit.Assert.fail;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.atakmap.android.takml.mx_framework.onnx_plugin.OnnxProcessingParams;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.atakmap.android.takml.mx_framework.onnx_plugin.OnnxPlugin;
import com.atakmap.android.takml_android.ModelTypeConstants;
import com.atakmap.android.takml_android.TakmlModel;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;
import com.atakmap.android.takml_android.takml_result.Recognition;
import com.atakmap.android.takml_android.takml_result.TakmlResult;

@RunWith(AndroidJUnit4.class)
public class OnnxTests {
    private final Context testContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    private File readAssetToFile(String assetFileName){
        InputStream inputStream = null;
        try {
            inputStream = testContext.getAssets().open(assetFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        File tempFile = new File(testContext.getCacheDir(), assetFileName);

        // Write the asset's contents to the temporary file
        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return tempFile;
    }

    @Test
    public void testOnnxImageClassification() throws TakmlInitializationException, IOException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<String> labels = Files.readAllLines(readAssetToFile("labels.txt").toPath());

        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("Example Onnx",
                readAssetToFile("mobilenetv2-7.onnx"),
                ".onnx",
                ModelTypeConstants.IMAGE_CLASSIFICATION)
                .setLabels(labels)
                .build();

        OnnxPlugin onnxMxPlugin = new OnnxPlugin();
        onnxMxPlugin.instantiate(takmlModel);

        File file =  readAssetToFile("img.png");
        byte[] fileContent = Files.readAllBytes(file.toPath());
        onnxMxPlugin.execute(fileContent, (takmlResults, success, modelType) -> {
            for(TakmlResult takmlResult : takmlResults) {
                Recognition recognition = (Recognition) takmlResult;
                System.out.println("Model output: " + recognition.getLabel() + " " + recognition.getConfidence());
            }
            countDownLatch.countDown();
        });

        try {
            if(!countDownLatch.await(10, TimeUnit.SECONDS)){
                Assert.fail("Did not finish inference");
            }
        } catch (InterruptedException e){
            Assert.fail("InterruptedException running inference" + e.getMessage());
        }
    }
}