package com.atakmap.android.takml_android.remote_inference;

import android.util.Log;

import com.bbn.takml_server.client.models.InferInput;
import com.bbn.takml_server.client.models.InferOutput;
import com.bbn.takml_server.client.models.InferenceRequest;
import com.bbn.takml_server.client.models.InferenceResponse;
import com.bbn.takml_server.client.models.TakmlModelInfo;
import com.google.gson.Gson;

import org.json.JSONException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class KServeAPIServer extends NanoHTTPD {
    private static final String TAG = KServeAPIServer.class.getName();

    public KServeAPIServer() {
        super(8342); // Start server on port 8342
    }

    public void start() throws IOException {
        start(SOCKET_READ_TIMEOUT, false);
        System.out.println("Server running on http://localhost:8342");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> headers = session.getHeaders();
        Map<String, String> params = session.getParms();

        // Read request body
        String requestBody = parseBody(session);

        Log.d(TAG, "serve: " + requestBody);

        // Route handling based on OpenAPI paths
        if(uri.contains("/mx/v2/models/")){
            if (method == Method.POST) {
                try {
                    return handleExecuteModel(requestBody);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if(uri.contains("/mx/models")){
            try {
                return handleGetModels();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }else{
             return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\": \"Not found\"}");
        }

        return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "application/json", "{\"error\": \"Method not allowed\"}");
    }

    /**
     * @return List of TakmlModels wrapped in Response
     * </pre>
     *
     * @throws JSONException
     */
    private Response handleGetModels() throws JSONException {
        List<TakmlModelInfo> takmlModelsToReturn = new ArrayList<>();
        TakmlModelInfo takmlModel = new TakmlModelInfo();
        takmlModel.setName("Test Model 1");
        takmlModelsToReturn.add(takmlModel);
        TakmlModelInfo takmlModel2 = new TakmlModelInfo();
        takmlModel2.setName("Test Model 2");
        takmlModelsToReturn.add(takmlModel2);

        return newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(takmlModelsToReturn));
    }

    /**
     * <pre>
     * Handles execution of a TAK ML Model, takes in an input and performs a very simple
     * increment (e.g. input 1 -> output 2).
     *
     * @param body - Expected of type InferInput
     * @return InferenceResponse wrapped in Response
     * </pre>
     *
     * @throws JSONException
     */
    private Response handleExecuteModel(String body) throws JSONException {
        InferenceRequest inferenceRequest = new Gson().fromJson(body, InferenceRequest.class);
        InferInput inferInput = inferenceRequest.getInputs().iterator().next();
        BigDecimal inputData = inferInput.getData().iterator().next();

        InferOutput inferOutput = new InferOutput();
        inferOutput.setData(Collections.singletonList(inputData.add(BigDecimal.valueOf(1.0))));

        InferenceResponse inferenceResponse = new InferenceResponse();
        inferenceResponse.setId("id");
        inferenceResponse.setModelName(inferInput.getName());
        inferenceResponse.setModelVersion("1.0");
        inferenceResponse.setOutputs(Collections.singletonList(inferOutput));

        return newFixedLengthResponse(Response.Status.OK, "application/json", new Gson().toJson(inferenceResponse));
    }

    // Utility method to parse the request body
    private String parseBody(IHTTPSession session) {
        final HashMap<String, String> map = new HashMap<String, String>();

        try {
            session.parseBody(map);
            return map.get("postData");
        } catch (Exception e) {
            return "{}"; // Return an empty JSON object if parsing fails
        }
    }

    public void stop(){
        super.stop();
    }
}