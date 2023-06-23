package com.bbn.tak.takml.sensor.server.settings;

public class Database {
	private String url = "jdbc:postgresql://localhost:5432/sensorthings"; // DEFAULT
	private String user = "sensorthings"; // DEFAULT
	private String password = "ChangeMe"; // DEFAULT
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
}
