package com.bbn.tak.takml.sensor.server.settings;

public class Settings {
	private Database database;
	private Server server;
	private FrostServer frostServer;
	
	public FrostServer getFrostServer() {
		return frostServer;
	}
	public void setFrostServer(FrostServer frostServer) {
		this.frostServer = frostServer;
	}
	public Database getDatabase() {
		return database;
	}
	public void setDatabase(Database database) {
		this.database = database;
	}
	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}
	
	
}
