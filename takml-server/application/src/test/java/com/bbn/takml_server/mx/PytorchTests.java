package com.bbn.takml_server.mx;

import com.bbn.takml_server.lib.TakmlInitializationException;
import com.bbn.takml_server.model_execution.TensorSerializerUtil;
import com.bbn.takml_server.model_execution.api.model.InferInput;
import com.bbn.takml_server.model_execution.api.model.InferOutput;
import com.bbn.takml_server.model_execution.api.model.ModelTensor;
import com.bbn.takml_server.model_execution.mx_plugins.OnnxMxPlugin;
import com.bbn.takml_server.model_execution.mx_plugins.PytorchMxPlugin;
import com.bbn.takml_server.model_execution.takml_result.Recognition;
import com.bbn.takml_server.takml_model.ModelTypeConstants;
import com.bbn.takml_server.takml_model.TakmlModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Fail.fail;

public class PytorchTests {
    private static final Logger logger = LogManager.getLogger(PytorchTests.class);

    @Test
    public void testPytorchImageClassification() throws TakmlInitializationException, IOException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<String> labels = List.of("cat", "dog");
        TakmlModel takmlModel = new TakmlModel.TakmlModelBuilder("Dogs and Cats Pytorch",
                new File("src/test/resources/example_models/dogs_cats_model/dogs_cats_model.torchscript"),
                ".pt",
                ModelTypeConstants.IMAGE_CLASSIFICATION)
                .setLabels(labels)
                .build();

        PytorchMxPlugin pytorchMxPlugin = new PytorchMxPlugin();
        pytorchMxPlugin.instantiate(takmlModel);

        File file = new File("src/test/resources/cat.jpeg");
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // Load the input data as a Bitmap
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(fileContent));
        } catch (Exception e) {
            logger.error("Error occurred while trying to decode image", e);
            return;
        }

        List<BigDecimal> input = ImagePreprocessor.createInputTensor(image, null).getLeft();
        pytorchMxPlugin.execute(new InferInput(UUID.randomUUID().toString(), List.of(1, 3, 224, 224), ModelTypeConstants.IMAGE_CLASSIFICATION, input),
                (takmlResults, success, modelType) -> {
            if(takmlResults != null) {
                InferOutput inferOutput = takmlResults.iterator().next();
                List<BigDecimal> tensorOutput = inferOutput.getData();
                float[] tensorOutputFloat = TensorSerializerUtil.convertToFloatArr(tensorOutput);
                List<Recognition> recognitions = PostProcessorUtil.getProbabilities(tensorOutputFloat, true, labels);
                for(Recognition recognition : recognitions) {
                    logger.info("Model output: {} {}", recognition.getLabel(), recognition.getConfidence());
                }
            }
            countDownLatch.countDown();
        });

        try {
            if(!countDownLatch.await(10, TimeUnit.SECONDS)){
                fail("Did not finish inference");
            }
        } catch (InterruptedException e){
            fail("InterruptedException running inference", e);
        }
    }
}
