package com.bbn.takml_server.lib;

public class TakmlInitializationException extends Exception {
    private static final long serialVersionUID = -5997393899085631866L;

    public TakmlInitializationException() {
    }

    public TakmlInitializationException(String message) {
        super(message);
    }

    public TakmlInitializationException(Throwable cause) {
        super(cause);
    }

    public TakmlInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TakmlInitializationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
