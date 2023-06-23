package com.bbn.tak.takml.sensor.server.settings;

public class Server {
	private Integer securePort;
	private Integer insecurePort;
	private String keystorePath;
	private String keystorePass;
	private String truststorePath;
	private String truststorePass;
	
	public Integer getSecurePort() {
		return securePort;
	}
	public void setSecurePort(Integer securePort) {
		this.securePort = securePort;
	}
	public String getKeystorePath() {
		return keystorePath;
	}
	public void setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
	}
	public String getKeystorePass() {
		return keystorePass;
	}
	public void setKeystorePass(String keystorePass) {
		this.keystorePass = keystorePass;
	}
	public String getTruststorePath() {
		return truststorePath;
	}
	public void setTruststorePath(String truststorePath) {
		this.truststorePath = truststorePath;
	}
	public String getTruststorePass() {
		return truststorePass;
	}
	public void setTruststorePass(String truststorePass) {
		this.truststorePass = truststorePass;
	}
	public Integer getInsecurePort() {
		return insecurePort;
	}
	public void setInsecurePort(Integer insecurePort) {
		this.insecurePort = insecurePort;
	}
	
	
}
