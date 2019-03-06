# RPC框架
自己设计一个的轻量级的RPC框架

## 1.将\target 下的service.jar引入自己的项目中
如果是maven 可以将其放入自己的maven仓库或者本地之间引入
```
<dependency>
	    <groupId>service</groupId>
	    <artifactId>service</artifactId>
	    <version>1.0</version>
	    <scope>system</scope>
	    <systemPath>D:\service.jar</systemPath>
</dependency>
```
## 2.引入jar包
本jar需要用到netty zookeeper jackson Spring等可以从该项项目的pom.xml中copy

## 3.编写一个sping-rpc.xml 文件
注意头文件需要引入 <br>
xmlns:rpcClient="http://www.springframework.org/schema/rpcClient" <br>
http://www.springframework.org/schema/rpcClient<br>
http://www.springframework.org/schema/rpcClient/spring-rpcClient.xsd<br>
这个是自己定义的标签
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xmlns:p="http://www.springframework.org/schema/p" xmlns:cache="http://www.springframework.org/schema/cache"
	xmlns:rpcClient="http://www.springframework.org/schema/rpcClient"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
			http://www.springframework.org/schema/beans/spring-beans-4.1.xsd
			http://www.springframework.org/schema/context
			http://www.springframework.org/schema/context/spring-context-4.1.xsd
			http://www.springframework.org/schema/rpcClient
			http://www.springframework.org/schema/rpcClient/spring-rpcClient.xsd
			http://www.springframework.org/schema/cache
     		http://www.springframework.org/schema/cache/spring-cache.xsd">	
    <!-- 调用接口注册bean -->
	<rpcClient:rpcClient package="main.java.work.service"></rpcClient:rpcClient>
	<bean id="zkServer" class="main.java.zkServer.ZkServer" init-method="start" > 
		<property name="ZookeeperIpHost" value="172.16.12.34:2181"></property>
		<!-- 服务注册扫描包 -->
		<property name="baseackage" value="main.java.work.service"></property>
	</bean>
	<!-- 服务段 -->
	<bean id="RPC" class="main.java.core.RPC" init-method="Initialized" >
		<property name="port" value="8885"></property>
	</bean>
</beans>
```
## 4.服务端
在serviceImpl中写上@RPCServer 认为该类是一个服务
```
@Service
@RPCServer
public class serverWorld2 {
	public String message(String world){
		return "Hello world";
	} 
}
```
## 5.客户端<br>
写一个service写上@RPCClient 认为其实一个消费接口 主要不要再改类上面添加@Service等注解 因为有自己写的方法注入bean
```
@RPCClient
public interface clientWorld {
	@RPCURL(className="serverWorld2",methodName="message") 
	public List message(String world);
}
```
最后就像调用实现类一样在controller中调用<br>
```
@Controller
@RequestMapping("/clientWorld")
public class clientWorldController {
	
	@Autowired
	private clientWorld clientWorld;
	
	@RequestMapping("sendMessage")
	@ResponseBody
	public List sendMessage(String message){
		return clientWorld.message(message);
	}

}
```
## 6.启动zookeeper服务 在配置文件中配好ip和port

## 7.启动项目即可

# 未完成
## 1.zookeeper的集群
## 2.负载均衡 目前只有轮询
