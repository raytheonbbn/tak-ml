package com.bbn.takml_server.takml_model;

import java.util.List;

public class ModelTypeConstants {
    public static String GENERIC_RECOGNITION = "GENERIC_RECOGNITION";
    public static String LINEAR_REGRESSION = "LINEAR_REGRESSION";
    public static String IMAGE_CLASSIFICATION = "IMAGE_CLASSIFICATION";
    public static String OBJECT_DETECTION = "OBJECT_DETECTION";

    public static List<String> getTypes(){
        return List.of(GENERIC_RECOGNITION, LINEAR_REGRESSION, IMAGE_CLASSIFICATION, OBJECT_DETECTION);
    }
}
