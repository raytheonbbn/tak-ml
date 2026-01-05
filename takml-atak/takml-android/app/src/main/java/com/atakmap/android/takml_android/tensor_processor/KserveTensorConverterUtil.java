package com.atakmap.android.takml_android.tensor_processor;

import com.bbn.takml_server.client.models.InferInput;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class KserveTensorConverterUtil {
    public static List<InferInput> convertInferInputs(List<com.atakmap.android.takml_android.tensor_processor.InferInput> inferInputs){
        List<InferInput> ret = new ArrayList<>();
        for(com.atakmap.android.takml_android.tensor_processor.InferInput input : inferInputs){
            InferInput conv = new InferInput();
            /// NOTE: this assumes the input data is always a Float[], which is what the KServe API expects
            /// when running locally, the mx plugins can support other types (e.g. byte[])
            conv.setData(convertToBigDecimalList((Float[]) input.getData()));
            conv.setShape(convertToBigDecimalList(input.getShape()));
            conv.setName(input.getName());
            conv.setDatatype(input.getDatatype());
            ret.add(conv);
        }
        return ret;
    }

    public static List<InferOutput> convertInferOutputs(List<com.bbn.takml_server.client.models.InferOutput> outputs) {
        List<InferOutput> ret = new ArrayList<>();
        for (com.bbn.takml_server.client.models.InferOutput output : outputs) {
            InferOutput conv = new InferOutput();
            conv.setData(convertToFloatArr(output.getData()));
            conv.setShape(convertToLongArr(output.getShape()));
            ret.add(conv);
        }
        return ret;
    }

    public static List<BigDecimal> convertToBigDecimalList(Float[] array) {
        List<BigDecimal> ret = new ArrayList<>(array.length);
        for (float value : array) {
            ret.add(BigDecimal.valueOf(value));
        }
        return ret;
    }

    public static List<Integer> convertToBigDecimalList(long[] array) {
        List<Integer> ret = new ArrayList<>(array.length);
        for (long value : array) {
            ret.add((int) value);
        }
        return ret;
    }

    public static float[] convertToFloatArr(List<BigDecimal> tensor){
        float[] ret = new float[tensor.size()];
        for(int i=0; i<tensor.size(); i++){
            ret[i] = tensor.get(i).floatValue();
        }
        return ret;
    }

    public static long[] convertToLongArr(List<Integer> tensor){
        long[] ret = new long[tensor.size()];
        for(int i=0; i<tensor.size(); i++){
            ret[i] = tensor.get(i).longValue();
        }
        return ret;
    }
}
