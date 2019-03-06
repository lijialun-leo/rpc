package main.java.zkServer;
 
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
 
public class ZookeeperBase implements Watcher {
	
	private static final int SESSION_TIME_OUT = 2000;
	private static final String basePath = "/request";
	private CountDownLatch countDownLatch = new CountDownLatch(1);
	private ZooKeeper zookeeper = null;
	
	@Override
	public void process(WatchedEvent event) {
		// 事件类型
        EventType eventType = event.getType();
        // 受影响的path
        String path = event.getPath();
		if(EventType.None == eventType){
			System.out.println("连接成功");
			//初始化服务列表 
			try {
				intiServerMap("/RPCSERVER");
			} catch (KeeperException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			countDownLatch.countDown();
		}/*else if(EventType.NodeCreated == eventType){//监听到传教节点
			String paths[] = path.split("/");
			Map<String,List<Map<String,String>>> childMap = new HashMap<String, List<Map<String,String>>>();
			List<Map<String,String>> childList = new ArrayList<Map<String,String>>();
			if(paths.length == 5){
				childMap = ZkServer.serviceMap.get(paths[3]);
				try {
					childList = childMap.get(new String(getData("/"+paths[2]+"/"+paths[3])));
					Map<String,String> map = new HashMap<String, String>();
					map.put(paths[4], new String(getData("/"+paths[2]+"/"+paths[3]+"/"+paths[4])));
					childList.add(map);
					System.out.println("服务上线"+path);
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				try {
					childMap.put(new String(getData("/"+paths[2]+"/"+paths[3])), childList);
					ZkServer.serviceMap.put(paths[3], childMap);
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}else if(EventType.NodeDeleted == eventType){
			String paths[] = path.split("/");
			Map<String,List<Map<String,String>>> childMap = ZkServer.serviceMap.get(paths[3]);
			try {
				List<Map<String,String>> childList = childMap.get(new String(getData("/"+paths[2]+"/"+paths[3])));
				Iterator<Map<String,String>> iterator = childList.iterator();
				while (iterator.hasNext()) {
					Map<String,String> map = (Map<String,String>) iterator.next();
					if(map.containsKey(paths[4])){
						childList.remove(map);
						System.out.println("服务下线"+path);
					}
				}
			} catch (KeeperException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}*/else if(EventType.NodeChildrenChanged == eventType){//某节点的其子节点有变化
			String paths[] = path.split("/");
			ConcurrentHashMap<String,CopyOnWriteArrayList<String>> Map = ZkServer.serviceMap.get(paths[3]);
			try {
				//目前列表中数据
				CopyOnWriteArrayList<String> childList = Map.get(new String(getData("/"+paths[2]+"/"+paths[3])));
				//节点数据
				List<String> childrens = getChilds("/"+paths[2]+"/"+paths[3]);
				//上线处理
				if(childrens.size() !=0){
					for (String child : childrens) {
						if(!childList.contains(child)){
							childList.add(child);
							System.out.println("服务上线"+path+"/"+child);
						}
						
					}
				}
				//下线处理
				if(childList.size() != 0){
					for (String str : childList) {
						if(childrens.size() !=0){
							for (String child : childrens) {
								if(!child.equals(str)){
									childList.remove(str);
									System.out.println("服务下线"+path+"/"+child);
								}
							}
						}else{
							childList.remove(str);
							System.out.println("服务下线"+path);
						}
					}
				}
			} catch (KeeperException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
 
	public ZookeeperBase(String host) throws IOException, InterruptedException{
		this.zookeeper = new ZooKeeper(host, SESSION_TIME_OUT, this);
		countDownLatch.await();
	}
	
	
	//==================== 工具函数  ==========================
	public String pathChange(String path){
		if(path.startsWith(ZookeeperBase.basePath)){
			return path;
		}else{
			return ZookeeperBase.basePath + path;
		}			
	}
	
	//===================== 节点操作函数 ==========================
	//node是否存在
	public Boolean nodeExists(String path) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		Stat stat = this.zookeeper.exists(path, true);
		return stat == null ? false : true;
	}
	//创建临时node
	public Boolean createNodeForTemporary(String path, String data) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(!this.nodeExists(path)) {
			String listPath[] = path.split("/");
			String prePath = "";
			for(int i=1; i<listPath.length-1; i++){
				prePath = prePath + "/" + listPath[i];
				if(!this.nodeExists(prePath)){
					this.zookeeper.create(prePath, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);	
				}
			}
			this.zookeeper.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			return true;
		}else{
			return false;
		}
	}
	
	//创建node
	public Boolean createNode(String path, String data) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(!this.nodeExists(path)) {
			String listPath[] = path.split("/");
			String prePath = "";
			for(int i=1; i<listPath.length-1; i++){
				prePath = prePath + "/" + listPath[i];
				if(!this.nodeExists(prePath)){
					this.zookeeper.create(prePath, "".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);	
				}
			}
			this.zookeeper.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			return true;
		}else{
			return false;
		}
	}
	//获取node内容
	public String getData(String path) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(this.nodeExists(path)) {
			 return new String(this.zookeeper.getData(path, false, null));
		}else{
			return null;
		}
	}
	//设置node内容
	public Boolean setData(String path, String data) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(this.nodeExists(path)){		
			this.zookeeper.setData(path, data.getBytes(), -1);	
			return true;
		}else{
			return false;
		}
	}
	//删除node
	public Boolean delNode(String path) throws InterruptedException, KeeperException{
		path = this.pathChange(path);
		if(this.nodeExists(path)){
			this.zookeeper.delete(path, -1);
			return true;
		}else{
			return false;
		}
	}
	//获取子节点
	public List<String> getChilds(String path) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(this.nodeExists(path)){
			return this.zookeeper.getChildren(path, true);
		}else{
			return null;
		}
	}
	//获取子节点数量
	public Integer getChildsNum(String path) throws KeeperException, InterruptedException{
		path = this.pathChange(path);
		if(this.getChilds(path) == null){
			return null;
		}else{
			return this.getChilds(path).size();
		}
	}
	//关闭连接
	public void closeConnection() throws InterruptedException{
		if(this.zookeeper != null){
			zookeeper.close();
		}
	}
	//删除节点
	public void rmr(String path) throws Exception {
        //获取路径下的节点
		path = this.pathChange(path);
        List<String> children = this.zookeeper.getChildren(path, false);
        for (String pathCd : children) {
            //获取父节点下面的子节点路径
            String newPath = "";
            //递归调用,判断是否是根节点
            if (path.equals("/")) {
                newPath = "/" + pathCd;
            } else {
                newPath = path + "/" + pathCd;
            }
            rmr(newPath);
        }
        //删除节点,并过滤zookeeper节点和 /节点
        if (path != null && !path.trim().startsWith("/zookeeper") && !path.trim().equals("/")) {
        	this.zookeeper.delete(path, -1);
            //打印删除的节点路径
            System.out.println("被删除的节点为：" + path);
        }
    }
	
	//初始化服务列表
	public  void intiServerMap(String path) throws KeeperException, InterruptedException{
        List<String> children = getChilds(path);
        if(children.size() !=0){
        	for (String pathCd : children) {
        		ConcurrentHashMap<String,CopyOnWriteArrayList<String>> parentMap = new ConcurrentHashMap<String, CopyOnWriteArrayList<String>>();
        		CopyOnWriteArrayList<String> childList = new CopyOnWriteArrayList<String>();
        		List<String> childrens = getChilds(path+"/"+pathCd);
        		String parentContext =  new String(getData(path+"/"+pathCd));
        		if(childrens.size() !=0){
        			for (String child : childrens) {
        				childList.add(child);
        			}
        		}
        		parentMap.put(parentContext, childList);
        		ZkServer.serviceMap.put(pathCd, parentMap);
        	}
        }
        
        ObjectMapper objectMapper=new ObjectMapper();
        try {
			System.out.println(objectMapper.writeValueAsString(ZkServer.serviceMap));
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
