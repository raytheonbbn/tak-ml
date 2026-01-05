package com.bbn.takml_server.feedback;

// add relevant output error for different expected model types
public enum OutputErrorType {
    FALSE_POSITIVE,
    FALSE_NEGATIVE,
    INCORRECT_LABEL,
    MISSING_LABEL,
    OTHER
}
