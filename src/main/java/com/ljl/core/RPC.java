package com.ljl.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljl.client.RPCRequest;
import com.ljl.server.RPCResponse;
import com.ljl.server.RPCResponseNet;

import java.io.IOException;

public class RPC {

    private static ObjectMapper objectMapper=new ObjectMapper();
    private static int port;
    
    public int getPort() {
    	return port;
    }
    
    public void setPort(int port) {
    	this.port = port;
    }
    public void Initialized() {
    	//初始化方法 不开线程会和tomcat 冲突
    	new Thread() {
    		public void run() {
    			RPC.start();
    		}
    	}.start();//开启线程
    }

    /**
     * 实现端启动RPC服务
     */
    public static void start(){
        RPCResponseNet.connect(port);
    }
    

    public static String requestEncode(RPCRequest request) throws JsonProcessingException {
    	//System.getProperty("line.separator") == 换行符 主要目的是为了和netty的LineBasedFrameDecoder连用 防止粘包和拆包
        return objectMapper.writeValueAsString(request)+System.getProperty("line.separator");
    }

    public static RPCRequest requestDeocde(String json) throws IOException {
        return objectMapper.readValue(json,RPCRequest.class);
    }

    public static String responseEncode(RPCResponse response) throws JsonProcessingException {
    	//System.getProperty("line.separator") == 换行符 主要目的是为了和netty的LineBasedFrameDecoder连用 防止粘包和拆包
        return objectMapper.writeValueAsString(response)+System.getProperty("line.separator");
    }

    public static Object responseDecode(String json) throws IOException {
        return objectMapper.readValue(json,RPCResponse.class);
    }

}
