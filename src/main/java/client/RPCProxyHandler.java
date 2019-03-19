package main.java.client;



import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import main.java.annotation.RPCURL;
import main.java.balancing.RoundRobin;
import main.java.fail.FailBack;
import main.java.zkServer.ZkServer;

public class RPCProxyHandler<T> implements InvocationHandler {
    public static Map requestLockMap=new ConcurrentHashMap<String,RPCRequest>();;//全局map 每个请求对应的锁 用于同步等待每个异步的RPC请求
    private static AtomicLong requestTimes=new AtomicLong(0);//记录调用的次数 也作为ID标志
	
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
            	 String str[] = ipAndHost.split(":");
            	 request.setClassName(className);
            	 request.setMethodName(url.methodName());
            	 request.setParameters(args);
            	 requestLockMap.put(request.getRequestID(),request);
            	 RPCRequestNet.connect(str[0], Integer.parseInt(str[1])).send(request);
            	 requestLockMap.remove(request.getRequestID());
            	 return request.getResult();
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
}
