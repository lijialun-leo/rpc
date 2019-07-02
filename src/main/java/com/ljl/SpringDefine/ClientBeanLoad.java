package com.ljl.SpringDefine;

import com.ljl.ProxyFactory.MethodProxyFactory;
import com.ljl.annotation.RPCClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientBeanLoad implements BeanDefinitionParser{
	//
	public static Map<String, String> map = new HashMap<String, String>();
	
	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		String packageName = element.getAttribute("package");
		Enumeration<URL> urls;
		try {
			//获取实际调用项目路径
			urls = Thread.currentThread().getContextClassLoader().getResources(packageName.replace(".", "/"));
			while (urls.hasMoreElements()){
				URL url =  urls.nextElement();
				if(null != url){
					String  protocol = url.getProtocol();
					if(protocol.equals("file")){
						String packagePath = url.getPath().replaceAll("%20"," ");//去空格
						File file = new File(packagePath);
						//遍历目录获取RPCClient注解的class
						func(file,packageName);
					}
				}
			}
			//加载bean
			Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
	        while (it.hasNext()) {
	             Map.Entry<String, String> entry = it.next();
	             try {
					BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
					GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
					//设置属性 即所对应的消费接口
			        definition.getPropertyValues().add("interfaceClass", Class.forName(entry.getValue()));
			        //设置Calss 即代理工厂
			        definition.setBeanClass(MethodProxyFactory.class);
			        //按照查找Bean的Class的类型
			        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
			        BeanDefinitionRegistry bean = parserContext.getRegistry();
			        bean.registerBeanDefinition(entry.getKey(), definition);
			        System.out.println(entry.getKey()+": 被加载");
	             } catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
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
						Annotation annotation = implClass.getAnnotation(RPCClient.class);
						if(annotation != null){
							String simpleName = str;
							map.put(simpleName, className);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
