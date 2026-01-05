# ModelMetricsApi

All URIs are relative to *http://localhost:8234*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addModelMetrics**](ModelMetricsApi.md#addModelMetrics) | **POST** /metrics/add_model_metrics | Add Model Inference Metrics
[**deleteModelMetrics**](ModelMetricsApi.md#deleteModelMetrics) | **DELETE** /metrics/delete_model_metrics | 
[**getAllModelMetrics**](ModelMetricsApi.md#getAllModelMetrics) | **GET** /metrics/get_all_model_metrics | Get All Model Metrics
[**getModelInferenceCounts**](ModelMetricsApi.md#getModelInferenceCounts) | **GET** /metrics/get_model_inference_count | Get Model Metrics
[**getModelMetrics**](ModelMetricsApi.md#getModelMetrics) | **GET** /metrics/get_model_metrics | Get Model Metrics

<a name="addModelMetrics"></a>
# **addModelMetrics**
> Object addModelMetrics(body)

Add Model Inference Metrics

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelMetricsApi;


ModelMetricsApi apiInstance = new ModelMetricsApi();
AddModelMetricsRequest body = new AddModelMetricsRequest(); // AddModelMetricsRequest | 
try {
    Object result = apiInstance.addModelMetrics(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelMetricsApi#addModelMetrics");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**AddModelMetricsRequest**](AddModelMetricsRequest.md)|  |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

<a name="deleteModelMetrics"></a>
# **deleteModelMetrics**
> Object deleteModelMetrics(modelName, modelVersion)



### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelMetricsApi;


ModelMetricsApi apiInstance = new ModelMetricsApi();
String modelName = "modelName_example"; // String | 
String modelVersion = "modelVersion_example"; // String | 
try {
    Object result = apiInstance.deleteModelMetrics(modelName, modelVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelMetricsApi#deleteModelMetrics");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  |
 **modelVersion** | **String**|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="getAllModelMetrics"></a>
# **getAllModelMetrics**
> List&lt;ModelMetrics&gt; getAllModelMetrics()

Get All Model Metrics

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelMetricsApi;


ModelMetricsApi apiInstance = new ModelMetricsApi();
try {
    List<ModelMetrics> result = apiInstance.getAllModelMetrics();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelMetricsApi#getAllModelMetrics");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;ModelMetrics&gt;**](ModelMetrics.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="getModelInferenceCounts"></a>
# **getModelInferenceCounts**
> Object getModelInferenceCounts(modelName, modelVersion)

Get Model Metrics

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelMetricsApi;


ModelMetricsApi apiInstance = new ModelMetricsApi();
String modelName = "modelName_example"; // String | 
String modelVersion = "modelVersion_example"; // String | 
try {
    Object result = apiInstance.getModelInferenceCounts(modelName, modelVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelMetricsApi#getModelInferenceCounts");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  |
 **modelVersion** | **String**|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="getModelMetrics"></a>
# **getModelMetrics**
> Object getModelMetrics(modelName, modelVersion)

Get Model Metrics

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelMetricsApi;


ModelMetricsApi apiInstance = new ModelMetricsApi();
String modelName = "modelName_example"; // String | 
String modelVersion = "modelVersion_example"; // String | 
try {
    Object result = apiInstance.getModelMetrics(modelName, modelVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelMetricsApi#getModelMetrics");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  |
 **modelVersion** | **String**|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

