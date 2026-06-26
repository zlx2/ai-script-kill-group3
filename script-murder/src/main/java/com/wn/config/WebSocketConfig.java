/**
 * @Author: 鱼
 * @Description: WebSocket的配置
 * @DateTime: 2026/6/25 10:25
 * @Component:
 **/
package com.wn.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置类
 *
 * 【作用】
 * 注册 WebSocket 端点，配置允许的域名。
 *
 * 【前端连接地址】
 * 大厅：ws://localhost:8080/ws/game?userId=1001&roomId=lobby
 * 房间：ws://localhost:8080/ws/game?userId=1001&roomId=房间UUID
 *
 * 【参数说明】
 * - userId：用户ID
 * - roomId：房间ID，传 "lobby" 表示在大厅
 */
@Configuration
@EnableWebSocket          // 开启 WebSocket 支持
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler WebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器
        // 第一个参数：处理器对象
        // 第二个参数：WebSocket 连接地址
        registry.addHandler(WebSocketHandler, "/ws/game")
                // 允许跨域（开发环境用 *，生产环境要改成具体域名）
                .setAllowedOrigins("*");
    }

    /**
     * 创建 WebSocket 容器工厂
     * 【作用】
     * 配置 WebSocket 容器，设置最大消息缓冲区大小。
     * 【参数说明】
     * - maxTextMessageBufferSize：最大文本消息缓冲区大小，单位字节
     * - maxBinaryMessageBufferSize：最大二进制消息缓冲区大小，单位字节
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(128 * 1024);
        container.setMaxBinaryMessageBufferSize(128 * 1024);
        return container;
    }
}