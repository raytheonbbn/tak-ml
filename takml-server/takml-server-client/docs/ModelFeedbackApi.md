# ModelFeedbackApi

All URIs are relative to *http://localhost:8234*

Method | HTTP request | Description
------------- | ------------- | -------------
[**addModelFeedback**](ModelFeedbackApi.md#addModelFeedback) | **POST** /model_feedback/add_feedback | Add feedback to model
[**getFeedbackForModel**](ModelFeedbackApi.md#getFeedbackForModel) | **GET** /model_feedback/get_feedback | Get all feedback for a model

<a name="addModelFeedback"></a>
# **addModelFeedback**
> Object addModelFeedback(modelName, modelVersion, callsign, inputText, inputFile, output, isCorrect, outputErrorType, evaluationConfidence, evaluationRating, comment, validInput)

Add feedback to model

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelFeedbackApi;


ModelFeedbackApi apiInstance = new ModelFeedbackApi();
String modelName = "modelName_example"; // String | 
Double modelVersion = 3.4D; // Double | 
String callsign = "callsign_example"; // String | 
String inputText = "inputText_example"; // String | 
File inputFile = new File("inputFile_example"); // File | 
String output = "output_example"; // String | 
Boolean isCorrect = true; // Boolean | 
String outputErrorType = "outputErrorType_example"; // String | 
Integer evaluationConfidence = 56; // Integer | 
Integer evaluationRating = 56; // Integer | 
String comment = "comment_example"; // String | 
Boolean validInput = true; // Boolean | 
try {
    Object result = apiInstance.addModelFeedback(modelName, modelVersion, callsign, inputText, inputFile, output, isCorrect, outputErrorType, evaluationConfidence, evaluationRating, comment, validInput);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelFeedbackApi#addModelFeedback");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  | [optional]
 **modelVersion** | **Double**|  | [optional]
 **callsign** | **String**|  | [optional]
 **inputText** | **String**|  | [optional]
 **inputFile** | **File**|  | [optional]
 **output** | **String**|  | [optional]
 **isCorrect** | **Boolean**|  | [optional]
 **outputErrorType** | **String**|  | [optional] [enum: FALSE_POSITIVE, FALSE_NEGATIVE, INCORRECT_LABEL, MISSING_LABEL, OTHER]
 **evaluationConfidence** | **Integer**|  | [optional] [enum: 5, 1]
 **evaluationRating** | **Integer**|  | [optional] [enum: 5, 1]
 **comment** | **String**|  | [optional]
 **validInput** | **Boolean**|  | [optional]

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: */*

<a name="getFeedbackForModel"></a>
# **getFeedbackForModel**
> List&lt;FeedbackResponse&gt; getFeedbackForModel(modelName, modelVersion)

Get all feedback for a model

### Example
```java
// Import classes:
//import com.bbn.takml_server.ApiException;
//import com.bbn.takml_server.client.ModelFeedbackApi;


ModelFeedbackApi apiInstance = new ModelFeedbackApi();
String modelName = "modelName_example"; // String | 
Double modelVersion = 3.4D; // Double | 
try {
    List<FeedbackResponse> result = apiInstance.getFeedbackForModel(modelName, modelVersion);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling ModelFeedbackApi#getFeedbackForModel");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **modelName** | **String**|  |
 **modelVersion** | **Double**|  | [optional]

### Return type

[**List&lt;FeedbackResponse&gt;**](FeedbackResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

