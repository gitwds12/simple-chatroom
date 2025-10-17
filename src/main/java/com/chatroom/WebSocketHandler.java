package com.chatroom;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        ChatRoomManager.addChannel(ctx.channel());
        logger.info("新连接加入: {}", channelId);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        String channelId = ctx.channel().id().asShortText();
        String username = ChatRoomManager.getUsername(ctx.channel());
        ChatRoomManager.removeChannel(ctx.channel());
        
        if (username != null) {
            broadcastSystemMessage(username + " 离开了聊天室");
            logger.info("用户离开: {} ({})", username, channelId);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, request);
        }
    }

    private void handleTextMessage(ChannelHandlerContext ctx, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "join":
                    handleJoin(ctx, json.get("username").getAsString());
                    break;
                case "message":
                    handleMessage(ctx, json.get("content").getAsString());
                    break;
                default:
                    logger.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            logger.error("处理消息失败", e);
        }
    }

    private void handleJoin(ChannelHandlerContext ctx, String username) {
        ChatRoomManager.setUsername(ctx.channel(), username);
        broadcastSystemMessage(username + " 加入了聊天室");
        sendUserCount();
        logger.info("用户加入: {}", username);
    }

    private void handleMessage(ChannelHandlerContext ctx, String content) {
        String username = ChatRoomManager.getUsername(ctx.channel());
        if (username == null) {
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("type", "message");
        response.addProperty("username", username);
        response.addProperty("content", content);
        response.addProperty("time", dateFormat.format(new Date()));

        ChatRoomManager.broadcast(new TextWebSocketFrame(response.toString()));
        logger.info("[{}]: {}", username, content);
    }

    private void broadcastSystemMessage(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "system");
        response.addProperty("content", message);
        response.addProperty("time", dateFormat.format(new Date()));

        ChatRoomManager.broadcast(new TextWebSocketFrame(response.toString()));
        sendUserCount();
    }

    private void sendUserCount() {
        JsonObject response = new JsonObject();
        response.addProperty("type", "userCount");
        response.addProperty("count", ChatRoomManager.getOnlineCount());

        ChatRoomManager.broadcast(new TextWebSocketFrame(response.toString()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("连接异常", cause);
        ctx.close();
    }
}