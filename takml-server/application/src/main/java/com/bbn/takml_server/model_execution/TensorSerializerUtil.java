package com.bbn.takml_server.model_execution;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TensorSerializerUtil {
    public static float[] convertToFloatArr(List<BigDecimal> tensor){
        float[] ret = new float[tensor.size()];
        for(int i=0; i<tensor.size(); i++){
            ret[i] = tensor.get(i).floatValue();
        }
        return ret;
    }

    public static List<BigDecimal> convertToBigDecimal(float[] tensor){
        List<BigDecimal> ret = new ArrayList<>();
        for(float val : tensor){
            ret.add(new BigDecimal(val));
        }
        return ret;
    }

    public static long[] convertShapeToLong(List<Integer> shape){
        long[] ret = new long[shape.size()];
        for(int i=0; i<shape.size(); i++){
            ret[i] = shape.get(i);
        }
        return ret;
    }
}
