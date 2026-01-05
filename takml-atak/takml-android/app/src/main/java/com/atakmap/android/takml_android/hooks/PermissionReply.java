package com.atakmap.android.takml_android.hooks;

public class PermissionReply {
    private final boolean permitted;
    private final String modelSelected;
    private final String requestId;

    public PermissionReply(boolean permitted, String requestId) {
        this.permitted = permitted;
        this.requestId = requestId;
        modelSelected = null;
    }

    public PermissionReply(boolean permitted, String requestId, String modelSelected) {
        this.permitted = permitted;
        this.requestId = requestId;
        this.modelSelected = modelSelected;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isPermitted() {
        return permitted;
    }

    public String getModelSelected() {
        return modelSelected;
    }
}
