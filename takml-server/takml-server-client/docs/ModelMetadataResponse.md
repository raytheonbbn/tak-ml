# ModelMetadataResponse

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**_links** | [**Links**](Links.md) |  |  [optional]
**name** | **String** | Name of the model | 
**versions** | **List&lt;String&gt;** | Available versions of the model |  [optional]
**platform** | **String** | Platform/framework of the model (e.g., \&quot;TensorFlow\&quot;, \&quot;PyTorch\&quot;) | 
**inputs** | [**List&lt;ModelTensor&gt;**](ModelTensor.md) | Input tensors for the model | 
**outputs** | [**List&lt;ModelTensor&gt;**](ModelTensor.md) | Output tensors for the model | 
