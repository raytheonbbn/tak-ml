# AddFeedbackRequest

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**modelName** | **String** |  | 
**modelVersion** | **Double** |  | 
**callsign** | **String** |  | 
**inputText** | **String** |  |  [optional]
**inputFile** | [**File**](File.md) |  |  [optional]
**output** | **String** |  | 
**isCorrect** | **Boolean** |  | 
**outputErrorType** | [**OutputErrorTypeEnum**](#OutputErrorTypeEnum) |  |  [optional]
**evaluationConfidence** | **Integer** |  |  [optional]
**evaluationRating** | **Integer** |  |  [optional]
**comment** | **String** |  |  [optional]
**validInput** | **Boolean** |  |  [optional]

<a name="OutputErrorTypeEnum"></a>
## Enum: OutputErrorTypeEnum
Name | Value
---- | -----
FALSE_POSITIVE | &quot;FALSE_POSITIVE&quot;
FALSE_NEGATIVE | &quot;FALSE_NEGATIVE&quot;
INCORRECT_LABEL | &quot;INCORRECT_LABEL&quot;
MISSING_LABEL | &quot;MISSING_LABEL&quot;
OTHER | &quot;OTHER&quot;
