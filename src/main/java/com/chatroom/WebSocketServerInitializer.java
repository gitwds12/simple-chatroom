package com.chatroom;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP 编解码器
        pipeline.addLast(new HttpServerCodec());
        // 聚合 HTTP 消息
        pipeline.addLast(new HttpObjectAggregator(65536));
        // 支持大文件传输
        pipeline.addLast(new ChunkedWriteHandler());
        // 处理静态资源
        pipeline.addLast(new HttpStaticFileHandler());
        // WebSocket 协议处理
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        // 自定义业务处理器
        pipeline.addLast(new WebSocketHandler());
    }
}