package com.bbn.takml_server.mx;


import com.bbn.takml_server.takml_model.ProcessingParams;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImagePreprocessor {
    private static final int DEFAULT_DIM_BATCH_SIZE = 1;
    private static final int DEFAULT_DIM_PIXEL_SIZE = 3;
    private static final int DEFAULT_IMAGE_SIZE_X = 224;
    private static final int DEFAULT_IMAGE_SIZE_Y = 224;

    /**
     * Creates an input tensor
     * @param image
     * @param processingParams
     * @return
     */
    public static Pair<List<BigDecimal>, BufferedImage> createInputTensor(BufferedImage image, ProcessingParams processingParams) {
        float[][][][] tensorData;
        float[] mean;
        float[] standardDeviation;

        int imageSizeX = DEFAULT_IMAGE_SIZE_X, imageSizeY = DEFAULT_IMAGE_SIZE_Y;

        if(processingParams != null){
            imageSizeX = processingParams.getDimPixelWidth();
            imageSizeY = processingParams.getDimPixelHeight();

            long[] shape = processingParams.getModelShape();
            tensorData = new float[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            mean = processingParams.getMean();
            standardDeviation = processingParams.getStd();
        }else{
            tensorData = new float[DEFAULT_DIM_BATCH_SIZE][DEFAULT_DIM_PIXEL_SIZE][DEFAULT_IMAGE_SIZE_X][DEFAULT_IMAGE_SIZE_Y];
            mean = new float[] { 0.485f, 0.456f, 0.406f };
            standardDeviation = new float[] { 0.229f, 0.224f, 0.225f };
        }

        // crop image
        int width = image.getWidth();
        int height = image.getHeight();
        int startX = 0;
        int startY = 0;
        if (width > height) {
            startX = (width - height) / 2;
            width = height;
        } else {
            startY = (height - width) / 2;
            height = width;
        }
        image = image.getSubimage(startX, startY, width, height);

        // Resize image
        var resizedImage = image.getScaledInstance(imageSizeX, imageSizeY, Image.SCALE_SMOOTH);

        // Process image
        BufferedImage scaledImage = new BufferedImage(imageSizeX, imageSizeY, BufferedImage.TYPE_INT_ARGB);

        scaledImage.getGraphics().drawImage(resizedImage, 0, 0, null);

        for (var y = 0; y < scaledImage.getHeight(); y++) {
            for (var x = 0; x < scaledImage.getWidth(); x++) {
                int color = scaledImage.getRGB(x,y);
                // Extract RGB values
                int blue = color & 0xff;
                int green = (color & 0xff00) >> 8;
                int red = (color & 0xff0000) >> 16;

                if(processingParams != null) {
                    // Possibly normalize and assign to tensor
                    tensorData[0][0][y][x] = (red / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[0]) / standardDeviation[0];
                    tensorData[0][1][y][x]= (green / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[1]) / standardDeviation[1];
                    tensorData[0][2][y][x] = (blue / (processingParams.isNormalizePixelValues() ? 255f : 1f) - mean[2]) / standardDeviation[2];
                }else{
                    // Normalize and assign to tensor
                    tensorData[0][0][y][x] = (red / 255f - mean[0]) / standardDeviation[0];
                    tensorData[0][1][y][x]= (green / 255f - mean[1]) / standardDeviation[1];
                    tensorData[0][2][y][x] = (blue / 255f - mean[2]) / standardDeviation[2];
                }
            }
        }

        return Pair.of(flatten(tensorData), scaledImage);
    }

    public static List<BigDecimal> flatten(float[][][][] array4D) {
        List<BigDecimal> flattened = new ArrayList<>();
        for (float[][][] array3D : array4D) {
            for (float[][] array2D : array3D) {
                for (float[] array1D : array2D) {
                    for (float num : array1D) {
                        flattened.add(new BigDecimal(num));
                    }
                }
            }
        }
        return flattened;
    }
}