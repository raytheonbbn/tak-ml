package com.atakmap.android.takml.mx_framework.onnx_plugin;

import android.graphics.Bitmap;
import java.nio.FloatBuffer;

public class ImageProcessor {
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int IMAGE_SIZE_X = 224;
    private static final int IMAGE_SIZE_Y = 224;

    private static Bitmap cropAndResizeImage(Bitmap image, int imageSizeX, int imageSizeY) {
        // Crop image to square
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

        Bitmap croppedImage = Bitmap.createBitmap(image, startX, startY, width, height);

        // Resize image

        // Return the processed image
        return Bitmap.createScaledBitmap(croppedImage, imageSizeX, imageSizeY, true);
    }

    public static float[][][][] preProcess(Bitmap bitmap, OnnxProcessingParams onnxProcessingParams) {
        float[][][][] tensorData;
        float[] mean;
        float[] standardDeviation;

        int imageSizeX = IMAGE_SIZE_X;
        int imageSizeY = IMAGE_SIZE_Y;
        if(onnxProcessingParams != null){
            imageSizeX = onnxProcessingParams.getDimPixelWidth();
            imageSizeY = onnxProcessingParams.getDimPixHeight();

            long[] shape = onnxProcessingParams.getModelShape();
            tensorData = new float[(int) shape[0]][(int) shape[1]][(int) shape[2]][(int) shape[3]];
            mean = onnxProcessingParams.getMean();
            standardDeviation = onnxProcessingParams.getStd();
        }else{
            tensorData = new float[DIM_BATCH_SIZE][DIM_PIXEL_SIZE][IMAGE_SIZE_X][IMAGE_SIZE_Y];
            mean = new float[] { 0.485f, 0.456f, 0.406f };
            standardDeviation = new float[] { 0.229f, 0.224f, 0.225f };
        }

        bitmap = cropAndResizeImage(bitmap,imageSizeX, imageSizeY);

        for (int y = 0; y < imageSizeY; y++) {
            for (int x = 0; x < imageSizeX; x++) {
                int color = bitmap.getPixel(x, y);

                // Extract RGB values
                int blue = color & 0xff;
                int green = (color & 0xff00) >> 8;
                int red = (color & 0xff0000) >> 16;

                if(onnxProcessingParams != null) {
                    // Normalize and assign to tensor
                    tensorData[0][y][x][0] = (red - mean[0]) / standardDeviation[0];
                    tensorData[0][y][x][1] = (green - mean[1]) / standardDeviation[1];
                    tensorData[0][y][x][2] = (blue - mean[2]) / standardDeviation[2];
                }else{
                    // Normalize and assign to tensor
                    tensorData[0][0][y][x] = (red / 255f - mean[0]) / standardDeviation[0];
                    tensorData[0][1][y][x]= (green / 255f - mean[1]) / standardDeviation[1];
                    tensorData[0][2][y][x] = (blue / 255f - mean[2]) / standardDeviation[2];
                }
            }
        }

        return tensorData;
    }
}