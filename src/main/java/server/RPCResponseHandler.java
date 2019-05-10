package main.java.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import main.java.client.RPCRequest;

public class RPCResponseHandler extends ChannelInboundHandlerAdapter{
	
	static ExecutorService executorService = Executors.newFixedThreadPool(10);
	//final EventExecutorGroup businessGroup = new DefaultEventExecutorGroup(10);
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws IOException {
    	executorService.execute(new Runnable() {
			@Override
			public void run() {
				System.out.println("服务端接收到请求");
		    	RPCRequest request = (RPCRequest) msg;
		    	//业务处理 假设每次处理需要1S
		    	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		        String result=InvokeServiceUtil.invoke(request);
		        RPCResponse response=new RPCResponse();
		        response.setRequestID(request.getRequestID());
		        response.setResult(result);
		        ctx.writeAndFlush(response);
				
			}
		});
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //flush方法再全部写到通道中
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
