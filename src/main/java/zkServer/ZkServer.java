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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import main.java.annotation.RPCServer;
import main.java.core.RPC;

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
	            	 System.out.println("/RPCSERVER/"+entry.getKey()+"/"+hostAddress+":"+rpc.getPort()+" 创建");
	             }
	        }
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
    
}
