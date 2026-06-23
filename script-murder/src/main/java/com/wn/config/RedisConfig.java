package com.wn.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import redis.clients.jedis.JedisPooled;

import java.text.SimpleDateFormat;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public JedisPooled jedisPooled(){
        return new JedisPooled(host,port,null,password);
    }
    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        //1.构建实例
        RedisTemplate<String,Object> redisTemplate = new RedisTemplate<>();
        //2.注册工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //3.配置string类型的序列化方案
        redisTemplate.setKeySerializer(RedisSerializer.string());
        //3.1 对象也会参与序列化 GenericJackson2JsonRedisSerializer ---> 存入redis中，会带上Class信息。{class:"com.woniuxy.entity.User"}
        //上面这个就可以直接返回你的对象出来。（自动反序列化）
//        new Jackson2JsonRedisSerializer<>(); --->{json对象}--->ObjectMapper JSON解析对象,返回的对象是 LinkedHashMap
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss"));

//        serializer.setObjectMapper(objectMapper);

        redisTemplate.setValueSerializer(serializer);
        //3.2 hash也需要配置
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }




}
