package main.java.Decode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import main.java.client.RPCRequest;

import org.msgpack.MessagePack;

public class ClientMsgPackEncode extends MessageToByteEncoder<Object>{

	@Override
	protected void encode(ChannelHandlerContext arg0, Object msg, ByteBuf out)
			throws Exception {
			RPCRequest request = (RPCRequest) msg;
			MessagePack msgPack = new MessagePack();
			//msgPack.register(RPCRequest.class);
			byte[] raw = null;
			raw = msgPack.write(request);
			out.writeBytes(raw);	
	}
}
