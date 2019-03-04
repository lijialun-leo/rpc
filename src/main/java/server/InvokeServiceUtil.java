package main.java.server;


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.stereotype.Service;

import main.java.annotation.RPCServer;
import main.java.client.RPCRequest;
import main.java.util.StringUtil;
import main.java.zkServer.ZkServer;

public class InvokeServiceUtil {

    /**
     * 反射调用相应实现类并结果
     * @param request
     * @return
     */
    public static Object invoke(RPCRequest request){
        Object result=null;
        try {
            Class implClass=Class.forName(request.getClassName());
            Object[] parameters=request.getParameters();
            int parameterNums=request.getParameters().length;
            Class[] parameterTypes=new Class[parameterNums];
            for (int i = 0; i <parameterNums ; i++) {
                parameterTypes[i]=parameters[i].getClass();
            }
            Method method=implClass.getDeclaredMethod(request.getMethodName(),parameterTypes);
            //获取到注册的服务bean
            Object implObj=ZkServer.serverContext.getBean(StringUtil.toLowerCaseFirstOne(implClass.getSimpleName()));
            result=method.invoke(implObj,parameters);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }

}
