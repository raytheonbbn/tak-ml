# FeedbackResponse

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **Long** |  |  [optional]
**modelName** | **String** |  |  [optional]
**modelVersion** | **Double** |  |  [optional]
**callsign** | **String** |  |  [optional]
**inputType** | [**InputTypeEnum**](#InputTypeEnum) |  |  [optional]
**input** | **String** |  |  [optional]
**output** | **String** |  |  [optional]
**isCorrect** | **Boolean** |  |  [optional]
**outputErrorType** | [**OutputErrorTypeEnum**](#OutputErrorTypeEnum) |  |  [optional]
**evaluationConfidence** | **Integer** |  |  [optional]
**evaluationRating** | **Integer** |  |  [optional]
**comment** | **String** |  |  [optional]
**createdAt** | [**OffsetDateTime**](OffsetDateTime.md) |  |  [optional]

<a name="InputTypeEnum"></a>
## Enum: InputTypeEnum
Name | Value
---- | -----
TEXT | &quot;TEXT&quot;
IMAGE | &quot;IMAGE&quot;
AUDIO | &quot;AUDIO&quot;
OTHER | &quot;OTHER&quot;

<a name="OutputErrorTypeEnum"></a>
## Enum: OutputErrorTypeEnum
Name | Value
---- | -----
FALSE_POSITIVE | &quot;FALSE_POSITIVE&quot;
FALSE_NEGATIVE | &quot;FALSE_NEGATIVE&quot;
INCORRECT_LABEL | &quot;INCORRECT_LABEL&quot;
MISSING_LABEL | &quot;MISSING_LABEL&quot;
OTHER | &quot;OTHER&quot;
