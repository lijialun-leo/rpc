package main.java.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import main.java.core.RPC;

import com.fasterxml.jackson.core.JsonProcessingException;

public class RPCRequestNet {

    private RPCRequestNet(String host,int port) {
        //netty线程组
        EventLoopGroup group=new NioEventLoopGroup();
        //启动辅助类 用于配置各种参数
        Bootstrap b=new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY,true)//禁止使用Nagle算法 作用小数据即时传输
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new LineBasedFrameDecoder(8192));//以换行符分包
                        socketChannel.pipeline().addLast(new StringDecoder());//将接收到的对象转为字符串
                        socketChannel.pipeline().addLast(new RPCRequestHandler());//添加相应回调处理和编解码器
                    }
                });
        try {
            ChannelFuture f=b.connect(host,port).sync();
            f.addListener(new ChannelFutureListener() {
                @Override
                //监听事件
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                	
                }
            });

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static RPCRequestNet connect(String host,int port){
    	RPCRequestNet instance=new RPCRequestNet(host,port);
        return instance;
    }

    //向实现端发送请求
    public void send(RPCRequest request){
        String requestJson= null;
        try {
            requestJson = RPC.requestEncode(request);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        ByteBuf requestBuf= Unpooled.copiedBuffer(requestJson.getBytes());
        //发送请求给服务段
        RPCRequestHandler.channelCtx.writeAndFlush(requestBuf);
        System.out.println("调用"+request.getRequestID()+"已发送");
        synchronized (request) {
        	//因为异步 所以不阻塞的话 该线程获取不到返回值
            //放弃对象锁 并阻塞等待notify
            try {
				request.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }


}
