package com.atakmap.android.takml.takml;

import com.bbn.takml_sdk_android.mx_framework.Recognition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassificationEntity {
    private static ClassificationEntity instance;

    private byte[] image;
    private ArrayList<Recognition> classificationResults;
    public static ClassificationEntity getInstance(){
        if(null == instance){
            instance = new ClassificationEntity();
        }
        return instance;
    }

    private ClassificationEntity(){

    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public ArrayList<Recognition> getClassificationResults() {
        return classificationResults;
    }

    public void setClassificationResults(ArrayList<Recognition> classificationResults) {
        this.classificationResults = classificationResults;
    }
}
