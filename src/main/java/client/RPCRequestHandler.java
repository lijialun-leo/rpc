package main.java.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import main.java.core.RPC;
import main.java.server.RPCResponse;

public class RPCRequestHandler extends ChannelInboundHandlerAdapter  {

    public static ChannelHandlerContext channelCtx;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	 super.channelActive(ctx);
    }
     
    @Override
    //异步调用读取管道数据
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RPCResponse response= (RPCResponse) msg;
        //System.out.println("获取到服务器返回值"+responseJson);
        synchronized (RPCProxyHandler.requestLockMap.get(response.getRequestID())) {
            //当客户段获取到返回值的时候唤醒在该对象锁上wait的线程
            RPCRequest request= (RPCRequest) RPCProxyHandler.requestLockMap.get(response.getRequestID());
            //System.out.println("2 : "+response.getResult());
            request.setResult(response.getResult());
            request.notifyAll();
        }
        
    }
    
}
