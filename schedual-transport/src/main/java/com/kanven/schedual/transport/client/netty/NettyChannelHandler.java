package com.kanven.schedual.transport.client.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kanven.schedual.network.protoc.MessageTypeProto.MessageType;
import com.kanven.schedual.network.protoc.RequestProto.Request;
import com.kanven.schedual.network.protoc.ResponseProto.Response;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

class NettyChannelHandler extends ChannelInboundHandlerAdapter {

	private static final Logger log = LoggerFactory.getLogger(NettyChannelHandler.class);

	private NettyBootstrap bootstrap;

	public NettyChannelHandler(NettyBootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Response response = (Response) msg;
		MessageType type = response.getType();
		if (type == MessageType.PONG) {
			if (log.isDebugEnabled()) {
				log.debug("收到pong消息...");
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("response:" + response);
			}
			String requestId = response.getRequestId();
			NettyResponse res = bootstrap.unregist(requestId);
			if (res == null) {
				log.error(requestId + "请求没有对应响应对象！");
				return;
			}
			res.callback(response);
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
		if (event instanceof IdleStateEvent) {
			IdleStateEvent state = (IdleStateEvent) event;
			if (state.state().equals(IdleState.ALL_IDLE)) {
				if (log.isDebugEnabled()) {
					log.debug("发起心跳检查...");
				}
				Request.Builder rb = Request.newBuilder();
				rb.setType(MessageType.PING);
				ctx.writeAndFlush(rb.build());
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("handler处理出现异常！", cause);
		ctx.close();
	}

}
