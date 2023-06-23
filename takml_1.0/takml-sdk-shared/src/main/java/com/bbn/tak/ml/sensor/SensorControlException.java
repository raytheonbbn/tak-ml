package com.bbn.tak.ml.sensor;

public class SensorControlException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private String message;
	private Exception cause;

	public SensorControlException(String message) {
		this.message = message;
	}
	
	public SensorControlException(Exception cause) {
		this.cause = cause;
	}
	
	public SensorControlException(String message, Exception cause) {
		this.cause = cause;
		this.message = message;
	}
	
	public SensorControlException() {
		
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Exception getCause() {
		return cause;
	}
	public void setCause(Exception cause) {
		this.cause = cause;
	}
}
