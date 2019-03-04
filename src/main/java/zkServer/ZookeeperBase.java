package main.java.zkServer;
 
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
 
public class ZookeeperBase implements Watcher {
 
	private static final int SESSION_TIME_OUT = 2000;
	private static final String basePath = "/request";
	private CountDownLatch countDownLatch = new CountDownLatch(1);
	private ZooKeeper zookeeper = null;
	
	@Override
	public void process(WatchedEvent event) {
		if(event.getState()==KeeperState.SyncConnected){
			System.out.println("连接成功");
			countDownLatch.countDown();
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
		Stat stat = this.zookeeper.exists(path, false);
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
			return this.zookeeper.getChildren(path, false);
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
}
