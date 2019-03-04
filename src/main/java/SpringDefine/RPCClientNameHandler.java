package main.java.SpringDefine;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;


public class RPCClientNameHandler extends NamespaceHandlerSupport{

	@Override
	public void init() {
		registerBeanDefinitionParser("rpcClient", new ClientBeanLoad());
	}

}
