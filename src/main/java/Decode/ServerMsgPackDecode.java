package main.java.Decode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import main.java.client.RPCRequest;

import org.msgpack.MessagePack;

public class ServerMsgPackDecode extends MessageToMessageDecoder<ByteBuf>{

	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf msg,
			List<Object> out) throws Exception {
		// TODO Auto-generated method stub
		final byte[] array;
        final int length = msg.readableBytes();
        array = new byte[length];
        msg.getBytes(msg.readerIndex(), array, 0, length);
        MessagePack msgPack = new MessagePack();
        out.add(msgPack.read(array,RPCRequest.class));
	}

}
