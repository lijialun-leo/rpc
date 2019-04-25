package main.java.zkServer;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.InetAddress;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import main.java.annotation.RPCServer;
import main.java.core.RPC;
import main.java.server.InvokeServiceUtil;

import org.apache.zookeeper.KeeperException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ZkServer implements ApplicationContextAware{
	public static ConcurrentHashMap<String, Map<String,Map<String,String>>> serviceMap = new ConcurrentHashMap<String,Map<String,Map<String,String>>>();
	public static Map<String, String> map = new HashMap<String, String>();
	public static ApplicationContext serverContext;
	public static String ZookeeperIpHost;
	public static String baseackage;
	public static ZookeeperBase zk;
	
	public void start(){
		RPC rpc = serverContext.getBean(RPC.class);
		try {
			//连接zk
			zk = new ZookeeperBase(ZookeeperIpHost);
			//创建节点 获取当前项目路径
			Enumeration<URL> urls =Thread.currentThread().getContextClassLoader().getResources(baseackage.replace(".", "/"));
			while (urls.hasMoreElements()){
                URL url =  urls.nextElement();
                if(null != url){
                    String  protocol = url.getProtocol();
                    if(protocol.equals("file")){
                        String packagePath = url.getPath().replaceAll("%20"," ");//去空格
                       System.out.println("server"+packagePath);
                       File file = new File(packagePath);
                       //遍历目录将服务放入map中
                       func(file,baseackage);
                    }
                }
            }
			if(!zk.nodeExists("/RPCSERVER")){
				zk.createNode("/RPCSERVER", "ROOT");
			}
			Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	        while (it.hasNext()) {
	             Map.Entry<String, String> entry = it.next();
	             if(!zk.nodeExists("/RPCSERVER/"+entry.getKey())){
	 				zk.createNode("/RPCSERVER/"+entry.getKey(), entry.getValue());
	 			 }
	             InetAddress address = InetAddress.getLocalHost();//获取的是本地的IP地址 //PC-20140317PXKX/192.168.0.121
	             String hostAddress = address.getHostAddress();//192.168.0.121     
	             if(!zk.nodeExists("/RPCSERVER/"+entry.getKey()+"/"+hostAddress+":"+rpc.getPort())){
	            	 //使用临时节点当zk断开连接的时候会自动消失
	            	 zk.createNodeForTemporary("/RPCSERVER/"+entry.getKey()+"/"+hostAddress+":"+rpc.getPort(),"true");
	            	 InvokeServiceUtil.map.put(entry.getValue(), 0);
	            	 System.out.println("/RPCSERVER/"+entry.getKey()+"/"+hostAddress+":"+rpc.getPort()+" 创建");
	             }
	        }
	        startTask();
	        //zk.closeConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private static void func(File file,String packageName){
		File[] fs = file.listFiles();
		if(fs.length != 0){
			for(File f:fs){
				if(f.isDirectory())	//若是目录，则递归打印该目录下的文件
				{
					String packageName2 = packageName + "."+f.getName();
					func(f,packageName2);
				}
				if(f.isFile()){		//若是文件，直接打印
					String str = f.getName().split("\\.")[0];
					String className = packageName +"."+str;
					className = className.substring(0, className.length());
					try {
						Class implClass = Class.forName(className);
						Annotation annotation = implClass.getAnnotation(RPCServer.class);
						if(annotation != null){
							String simpleName = str;
							map.put(simpleName, className);
						}
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
    
    public void deleteNode(){
    	try {
			ZookeeperBase zk = new ZookeeperBase(ZookeeperIpHost);
			Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	        while (it.hasNext()) {
	             Map.Entry<String, String> entry = it.next();
	             zk.rmr("/RPCSERVER/"+entry.getKey());
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    //运行过程中获取IOC容器
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    	serverContext=applicationContext;
    }

    public String getZookeeperIpHost() {
		return ZookeeperIpHost;
	}
	
	public void setZookeeperIpHost(String zookeeperIpHost) {
		ZookeeperIpHost = zookeeperIpHost;
	}
	
	public String getBaseackage() {
		return baseackage;
	}
	
	public void setBaseackage(String baseackage) {
		this.baseackage = baseackage;
	}
	
	//定时任务
	public void startTask(){
		System.out.println("*****************************************************");
		//创建计时器
		Timer timer = new Timer("开始执行任务-------------------");
		//创建计时器任务(TimerTaskBean:是自定义的类，继承了TimerTask抽象类)
		TimerTask task = new TimerTaskBean();
		//调用计时器的schedule方法（时间表），此处的60000代表：在当前时间的60000毫秒之后，此线程会被唤醒
		timer.schedule(task, 5, 60000);
		System.out.println("定时任务已启动，于5秒后执行");
		System.out.println("*****************************************************");
	}
	
	class TimerTaskBean extends TimerTask {
		@Override
		public void run() { 
			Map serverMap = InvokeServiceUtil.map;
	        Iterator<Map.Entry<String, Integer>> it = serverMap.entrySet().iterator();
	        while (it.hasNext()) {
	             Map.Entry<String, Integer> entry = it.next();
	             entry.setValue(0);
	        }
		}
	}
    
}
