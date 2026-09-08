package com.jiuyu.replay.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.jiuyu.replay"})
@EnableCaching
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@MapperScan({"com.jiuyu.replay.power.repository.dao", "com.jiuyu.replay.power.mapper", "com.jiuyu.replay.words.repository.dao", "com.jiuyu.replay.third.repository.dao", "com.jiuyu.replay.system.repository.dao", "com.jiuyu.replay.order.repository.dao", "com.jiuyu.replay.common.repository.dao", "com.jiuyu.replay.agent.repository.dao", "com.jiuyu.replay.ai.repository.dao", "com.jiuyu.replay.activity.repository.dao", "com.jiuyu.replay.reward.repository.dao", "com.jiuyu.replay.video.project.dao"})
@EnableMongoRepositories(basePackages = {"com.jiuyu.replay.ai.repository.mongo", "com.jiuyu.replay.words.repository.mongo", "com.jiuyu.replay.video.project.repository"})
public class ReplayApiApplication {

    public static void main(String[] args) {

        SpringApplication.run(ReplayApiApplication.class, args);
    }

}
