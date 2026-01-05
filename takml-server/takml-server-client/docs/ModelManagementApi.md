# ModelManagementApi

All URIs are relative to *http://localhost:8234*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addModel**](ModelManagementApi.md#addModel) | **POST** /model_management/add_model_wrapper | Add a new TAKML model, returns id for TAK ML Model
[**downloadModel**](ModelManagementApi.md#downloadModel) | **GET** /model_management/get_model/{modelHash} | Download a model binary by model hash
[**editModel**](ModelManagementApi.md#editModel) | **POST** /model_management/edit_model_wrapper/{modelHash} | Edit or replace an existing TAKML model by model hash
[**getModelMetadata**](ModelManagementApi.md#getModelMetadata) | **GET** /model_management/get_model_metadata/{modelHash} | Get metadata for a specific model by model hash
[**getModels**](ModelManagementApi.md#getModels) | **GET** /model_management/get_models | Get metadata for all models
[**removeModel**](ModelManagementApi.md#removeModel) | **DELETE** /model_management/remove_model/{hash} | Remove a model by hash
[**searchModels**](ModelManagementApi.md#searchModels) | **GET** /model_management/search | Search Models

<a name="addModel"></a>
# **addModel**
> Object addModel(takmlModelWrapper, requesterCallsign, runOnServer, supportedDevices)

Add a new TAKML model, returns id for TAK ML Model

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
File takmlModelWrapper = new File("takmlModelWrapper_example"); // File | 
String requesterCallsign = "requesterCallsign_example"; // String | 
Boolean runOnServer = true; // Boolean | 
List<String> supportedDevices = Arrays.asList("supportedDevices_example"); // List<String> | 
try {
    Object result = apiInstance.addModel(takmlModelWrapper, requesterCallsign, runOnServer, supportedDevices);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#addModel");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **takmlModelWrapper** | **File**|  | [optional]
 **requesterCallsign** | **String**|  | [optional]
 **runOnServer** | **Boolean**|  | [optional]
 **supportedDevices** | [**List&lt;String&gt;**](String.md)|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: */*

<a name="downloadModel"></a>
# **downloadModel**
> byte[] downloadModel(modelHash)

Download a model binary by model hash

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
String modelHash = "modelHash_example"; // String | 
try {
    byte[] result = apiInstance.downloadModel(modelHash);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#downloadModel");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelHash** | **String**|  |

### Return type

**byte[]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="editModel"></a>
# **editModel**
> Object editModel(modelHash, takmlModelWrapper, requesterCallsign, runOnServer, supportedDevices)

Edit or replace an existing TAKML model by model hash

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
String modelHash = "modelHash_example"; // String | 
File takmlModelWrapper = new File("takmlModelWrapper_example"); // File | 
String requesterCallsign = "requesterCallsign_example"; // String | 
Boolean runOnServer = true; // Boolean | 
List<String> supportedDevices = Arrays.asList("supportedDevices_example"); // List<String> | 
try {
    Object result = apiInstance.editModel(modelHash, takmlModelWrapper, requesterCallsign, runOnServer, supportedDevices);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#editModel");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelHash** | **String**|  |
 **takmlModelWrapper** | **File**|  | [optional]
 **requesterCallsign** | **String**|  | [optional]
 **runOnServer** | **Boolean**|  | [optional]
 **supportedDevices** | [**List&lt;String&gt;**](String.md)|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: */*

<a name="getModelMetadata"></a>
# **getModelMetadata**
> IndexRow getModelMetadata(modelHash)

Get metadata for a specific model by model hash

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
String modelHash = "modelHash_example"; // String | 
try {
    IndexRow result = apiInstance.getModelMetadata(modelHash);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#getModelMetadata");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelHash** | **String**|  |

### Return type

[**IndexRow**](IndexRow.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="getModels"></a>
# **getModels**
> List&lt;IndexRow&gt; getModels()

Get metadata for all models

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
try {
    List<IndexRow> result = apiInstance.getModels();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#getModels");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;IndexRow&gt;**](IndexRow.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="removeModel"></a>
# **removeModel**
> Object removeModel(hash)

Remove a model by hash

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
String hash = "hash_example"; // String | 
try {
    Object result = apiInstance.removeModel(hash);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#removeModel");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **hash** | **String**|  |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

<a name="searchModels"></a>
# **searchModels**
> List&lt;IndexRow&gt; searchModels(modelName, modelType)

Search Models

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelManagementApi;


ModelManagementApi apiInstance = new ModelManagementApi();
String modelName = "modelName_example"; // String | 
String modelType = "modelType_example"; // String | 
try {
    List<IndexRow> result = apiInstance.searchModels(modelName, modelType);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelManagementApi#searchModels");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  | [optional]
 **modelType** | **String**|  | [optional]

### Return type

[**List&lt;IndexRow&gt;**](IndexRow.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

