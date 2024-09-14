package com.lihao.webSocket.netty;

import com.lihao.entity.dto.TokenUserInfoDto;
import com.lihao.redis.RedisComponent;
import com.lihao.utils.StringTools;
import com.lihao.webSocket.ChannelContextUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ChannelHandler.Sharable
public class HandlerWebSocket extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Resource
    private RedisComponent redisComponent;
    @Resource
    private ChannelContextUtils channelContextUtils;
    private static final Logger logger  = LoggerFactory.getLogger(HandlerWebSocket.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("有新的连接加入......");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelContextUtils.removeContext(ctx.channel());
        logger.info("有新的连接断开......");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame textWebSocketFrame) throws Exception {
        Channel channel = ctx.channel();
        Attribute<String> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        String userId = attribute.get();
        /*logger.info("收到消息userId{}的消息：{}",userId,textWebSocketFrame.text());*/
        redisComponent.saveHeartBeat(userId);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof WebSocketServerProtocolHandler.HandshakeComplete){
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete) evt;
            String url = complete.requestUri();
            String token = getToken(url);
            if(token == null){
                ctx.channel().close();
                return;
            }
            TokenUserInfoDto tokenUserInfoDto = redisComponent.getTokenUserInfoDto(token);
            if(tokenUserInfoDto == null){
                ctx.channel().close();
                return;
            }
            channelContextUtils.addContext(tokenUserInfoDto.getUserId(),ctx.channel());
        }
    }
    private String getToken(String url){
        if(StringTools.isEmpty(url) || url.indexOf("?") == -1){
            return null;
        }
        String[] queryParams = url.split("\\?");
        if(queryParams.length != 2){
            return null;
        }
        String[] params = queryParams[1].split("=");
        if(params.length != 2){
            return null;
        }
        return params[1];
    }
}
