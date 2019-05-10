package main.java.client;



import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import main.java.annotation.RPCURL;
import main.java.balancing.RoundRobin;
import main.java.fail.FailBack;
import main.java.zkServer.ZkServer;

public class RPCProxyHandler<T> implements InvocationHandler {
    public static Map requestLockMap=new ConcurrentHashMap<String,RPCRequest>();;//全局map 每个请求对应的锁 用于同步等待每个异步的RPC请求
    private AtomicLong requestTimes=new AtomicLong(0);//记录调用的次数 也作为ID标志
	
    /*public RPCProxyHandler(){
    	System.out.println("*****************************************************");
		//创建计时器
		Timer timer = new Timer("开始执行任务-------------------");
		//创建计时器任务(TimerTaskBean:是自定义的类，继承了TimerTask抽象类)
		TimerTask task = new TimerTaskBean();
		//调用计时器的schedule方法（时间表），此处的60000代表：在当前时间的60000毫秒之后，此线程会被唤醒
		timer.schedule(task, 5, 60000);
		System.out.println("定时任务已启动，于5秒后执行");
		System.out.println("*****************************************************");
    }*/
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] obj) throws Throwable {
        RPCRequest request=new RPCRequest();
        request.setRequestID(buildRequestID(method.getName()));
        //获取调用方法的ClassName和MethodName
        RPCURL url = method.getAnnotation(RPCURL.class);
        Map serverMap = ZkServer.serviceMap.get(url.className());
        Iterator<Map.Entry<String, Map<String,String>>> it = serverMap.entrySet().iterator();
        while (it.hasNext()) {
             Map.Entry<String, Map<String,String>> entry = it.next();
             String className = entry.getKey();
             Map<String,String> serverList = entry.getValue();
             Iterator<Map.Entry<String,String>> serverListIt = serverList.entrySet().iterator();
             List<String> list = new ArrayList<String>();
             while (serverListIt.hasNext()) {
            	 Map.Entry<String,String> ipEntry = serverListIt.next();
            	 list.add(ipEntry.getKey());
             }
             String ipAndHost = RoundRobin.getServer(list);//后期需要添加负载均衡策略(已有轮询)
             String isTrue = serverList.get(ipAndHost);
             if(isTrue.equals("true")){
	            	 String [] strList = new String[obj.length];
		            	 for (int i = 0; i < obj.length; i++) {
		            		 strList[i] = obj[i].toString();
	            	 }
            		 String str[] = ipAndHost.split(":");
            		 request.setClassName(className);
            		 request.setMethodName(url.methodName());
            		 request.setParameters(strList);
            		 requestLockMap.put(request.getRequestID(),request);
            		 RPCRequestNet.getRPCRequestNet().connect(str[0], Integer.parseInt(str[1]),request);
            		 requestLockMap.remove(request.getRequestID());
            		 //System.out.println("调用次数"+requestTimes.intValue());
            		 Type returnType =  method.getGenericReturnType();
            		 Class typeClass = Class.forName(returnType.toString().split(" ")[1].trim());
            		 Gson gson = new Gson();
            		 return gson.fromJson(request.getResult(), typeClass);
            		 /*if(!returnType.toString().split(" ")[1].trim().startsWith("java.lang.")){
            			 ObjectMapper objectMapper = new ObjectMapper();
            			 objectMapper.readValue(request.getResult(), typeClass);
            			 return objectMapper.readValue(request.getResult(), typeClass);
             		}else{
             			Constructor constructor = typeClass.getConstructor(new Class[]{String.class});
             			return constructor.newInstance(request.getResult());
             		}*/
             }else{
            	 FailBack failBack = (FailBack) ZkServer.serverContext.getBean(url.failClassName());
            	 return failBack.failBack();
             }
        }
        return "找不到服务";
    }
    
    //生成请求的唯一ID
    private String buildRequestID(String methodName){
        StringBuilder sb=new StringBuilder();
        sb.append(requestTimes.incrementAndGet());
        sb.append(System.currentTimeMillis());
        sb.append(methodName);
        Random random = new Random();
        sb.append(random.nextInt(1000));
        return sb.toString();
    }

    /*class TimerTaskBean extends TimerTask {
		*//**
		 * 要执行的定时任务代码块
		 *//*
		@Override
		public void run() { 
			requestTimes.set(new Long(0));
		}
	}*/

}
