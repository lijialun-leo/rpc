package com.ljl.client;

import org.msgpack.annotation.Message;

@Message
public class RPCRequest {

	private String requestID;
	private String className;
	private String methodName;
	private String[] parameters;
	private String result;
	
	public String getRequestID() {
		return requestID;
	}
	public void setRequestID(String requestID) {
		this.requestID = requestID;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public String[] getParameters() {
		return parameters;
	}
	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
	
}
