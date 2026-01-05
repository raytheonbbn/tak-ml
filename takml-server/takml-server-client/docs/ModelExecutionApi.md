# ModelExecutionApi

All URIs are relative to *http://localhost:8234*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getModelVersionMetadata**](ModelExecutionApi.md#getModelVersionMetadata) | **GET** /v2/models/{MODEL_NAME}/versions/{MODEL_VERSION} | Model Version Metadata
[**getModelVersionReady**](ModelExecutionApi.md#getModelVersionReady) | **GET** /v2/models/{MODEL_NAME}/versions/{MODEL_VERSION}/ready | Model Version Ready
[**getServerMetadata**](ModelExecutionApi.md#getServerMetadata) | **GET** /v2/ | Server Metadata
[**getV2HealthLive**](ModelExecutionApi.md#getV2HealthLive) | **GET** /v2/health/live | Server Live
[**getV2HealthReady**](ModelExecutionApi.md#getV2HealthReady) | **GET** /v2/health/ready | Server Ready
[**postModelVersionInfer**](ModelExecutionApi.md#postModelVersionInfer) | **POST** /v2/models/{MODEL_NAME}/versions/{MODEL_VERSION}/infer | Inference

<a name="getModelVersionMetadata"></a>
# **getModelVersionMetadata**
> ModelMetadataResponse getModelVersionMetadata(MODEL_NAME, MODEL_VERSION)

Model Version Metadata

Retrieves metadata for a specific version of a model. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
String MODEL_NAME = "MODEL_NAME_example"; // String | Name of the model
String MODEL_VERSION = "MODEL_VERSION_example"; // String | Version of the model
try {
    ModelMetadataResponse result = apiInstance.getModelVersionMetadata(MODEL_NAME, MODEL_VERSION);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#getModelVersionMetadata");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **MODEL_NAME** | **String**| Name of the model |
 **MODEL_VERSION** | **String**| Version of the model |

### Return type

[**ModelMetadataResponse**](ModelMetadataResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getModelVersionReady"></a>
# **getModelVersionReady**
> getModelVersionReady(MODEL_NAME, MODEL_VERSION)

Model Version Ready

The \&quot;model version ready\&quot; API indicates if a specific version of a model is ready for inference. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
String MODEL_NAME = "MODEL_NAME_example"; // String | Name of the model
String MODEL_VERSION = "MODEL_VERSION_example"; // String | Version of the model
try {
    apiInstance.getModelVersionReady(MODEL_NAME, MODEL_VERSION);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#getModelVersionReady");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **MODEL_NAME** | **String**| Name of the model |
 **MODEL_VERSION** | **String**| Version of the model |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="getServerMetadata"></a>
# **getServerMetadata**
> ServerMetadataResponse getServerMetadata()

Server Metadata

Retrieves metadata about the inference server. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
try {
    ServerMetadataResponse result = apiInstance.getServerMetadata();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#getServerMetadata");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**ServerMetadataResponse**](ServerMetadataResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

<a name="getV2HealthLive"></a>
# **getV2HealthLive**
> getV2HealthLive()

Server Live

The \&quot;server live\&quot; API indicates if the inference server is running and able to handle requests. This can be used for Kubernetes liveness probes. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
try {
    apiInstance.getV2HealthLive();
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#getV2HealthLive");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="getV2HealthReady"></a>
# **getV2HealthReady**
> getV2HealthReady()

Server Ready

The \&quot;server ready\&quot; API indicates if the inference server is ready to handle inference requests. This can be used for Kubernetes readiness probes. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
try {
    apiInstance.getV2HealthReady();
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#getV2HealthReady");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined

<a name="postModelVersionInfer"></a>
# **postModelVersionInfer**
> InferenceResponse postModelVersionInfer(body, MODEL_NAME, MODEL_VERSION)

Inference

Performs inference using a specific version of a model. 

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelExecutionApi;


ModelExecutionApi apiInstance = new ModelExecutionApi();
InferenceRequest body = new InferenceRequest(); // InferenceRequest | 
String MODEL_NAME = "MODEL_NAME_example"; // String | Name of the model
String MODEL_VERSION = "MODEL_VERSION_example"; // String | Version of the model
try {
    InferenceResponse result = apiInstance.postModelVersionInfer(body, MODEL_NAME, MODEL_VERSION);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelExecutionApi#postModelVersionInfer");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**InferenceRequest**](InferenceRequest.md)|  |
 **MODEL_NAME** | **String**| Name of the model |
 **MODEL_VERSION** | **String**| Version of the model |

### Return type

[**InferenceResponse**](InferenceResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

