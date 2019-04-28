package main.java.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;

import main.java.client.RPCRequest;
import main.java.core.RPC;

public class RPCResponseHandler extends ChannelInboundHandlerAdapter{

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        String requestJson= (String) msg;
        RPCRequest request= RPC.requestDeocde(requestJson);
        Object result=InvokeServiceUtil.invoke(request);
        RPCResponse response=new RPCResponse();
        response.setRequestID(request.getRequestID());
        response.setResult(result);
        String respStr=RPC.responseEncode(response);
        ByteBuf responseBuf= Unpooled.copiedBuffer(respStr.getBytes());
        //写入缓存数组
        //System.out.println("response : " + respStr);
        ctx.writeAndFlush(responseBuf);
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
