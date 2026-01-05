package com.bbn.takml_server.model_management.ui;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

public class AddTakmlModelUIRequest {
    private MultipartFile model;
    private String requesterCallsign;
    private boolean runOnTakmlServer;
    private List<String> supportedDevices = new ArrayList<>();

    public AddTakmlModelUIRequest() {
    }

    public AddTakmlModelUIRequest(MultipartFile model, String requesterCallsign, boolean runOnTakmlServer) {
        this.model = model;
        this.requesterCallsign = requesterCallsign;
        this.runOnTakmlServer = runOnTakmlServer;
    }

    public MultipartFile getModel() {
        return model;
    }

    public void setModel(MultipartFile model) {
        this.model = model;
    }

    public String getRequesterCallsign() {
        return requesterCallsign;
    }

    public void setRequesterCallsign(String requesterCallsign) {
        this.requesterCallsign = requesterCallsign;
    }

    public boolean getRunOnTakmlServer() {
        return runOnTakmlServer;
    }

    public void setRunOnTakmlServer(boolean runOnTakmlServer) {
        this.runOnTakmlServer = runOnTakmlServer;
    }

    public List<String> getSupportedDevices() {
        return supportedDevices;
    }

    public void setSupportedDevices(List<String> supportedDevices) {
        this.supportedDevices = supportedDevices;
    }
}
