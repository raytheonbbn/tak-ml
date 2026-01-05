package com.atakmap.android.takml_android.tensor_processor;

import com.atakmap.android.takml_android.takml_result.TakmlResult;

import java.util.List;

public interface TensorProcessor {
    List<InferInput> processInputTensor(List<byte[]> input);
    List<List<? extends TakmlResult>> processOutputTensor(List<InferOutput> outputs);
}
