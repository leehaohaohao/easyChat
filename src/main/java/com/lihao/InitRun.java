package com.lihao;

import com.lihao.redis.RedisUtils;
import com.lihao.webSocket.netty.NettyWebSocketStarter;
import io.lettuce.core.RedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;

@Component("initRun")
public class InitRun implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);
    @Resource
    private DataSource dataSource;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private NettyWebSocketStarter nettyWebSocketStarter;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            dataSource.getConnection();
            redisUtils.get("test");
            new Thread(nettyWebSocketStarter).start();
            logger.info("服务启动成功，可以开始愉快的开发了！");
        }catch (SQLException e){
            logger.error("数据库配置错误，请检查数据库配置！");
        }catch(RedisConnectionFailureException e){
            logger.error("Redis配置错误，请检查Redis配置！");
        }catch (Exception e){
            logger.error("服务启动失败！",e);
        }
    }
}
