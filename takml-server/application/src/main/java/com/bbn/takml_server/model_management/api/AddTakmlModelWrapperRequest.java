package com.bbn.takml_server.model_management.api;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class AddTakmlModelWrapperRequest {
    private MultipartFile takmlModelWrapper;
    private String requesterCallsign;
    private boolean runOnServer;
    private List<String> supportedDevices = new ArrayList<>();

    public AddTakmlModelWrapperRequest(){
    }

    public AddTakmlModelWrapperRequest(MultipartFile takmlModelWrapper, String requesterCallsign, boolean runOnServer) {
        this.takmlModelWrapper = takmlModelWrapper;
        this.requesterCallsign = requesterCallsign;
        this.runOnServer = runOnServer;
    }

    public MultipartFile getTakmlModelWrapper() {
        return takmlModelWrapper;
    }

    public void setTakmlModelWrapper(MultipartFile takmlModelWrapper) {
        this.takmlModelWrapper = takmlModelWrapper;
    }

    public String getRequesterCallsign() {
        return requesterCallsign;
    }

    public void setRequesterCallsign(String requesterCallsign) {
        this.requesterCallsign = requesterCallsign;
    }

    public boolean getRunOnServer() {
        return runOnServer;
    }

    public void setRunOnServer(boolean runOnServer) {
        this.runOnServer = runOnServer;
    }

    public boolean isRunOnServer() {
        return runOnServer;
    }

    public List<String> getSupportedDevices() {
        return supportedDevices;
    }

    public void setSupportedDevices(List<String> supportedDevices) {
        this.supportedDevices = supportedDevices;
    }
}
