package com.jiuyu.replay.power.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
public class MybatisConfig {

    /**
     * 配置mybatis分页插件
     * @return
     */
    @Bean
    public MybatisPlusInterceptor paginationInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置请求的页面大于最大页后的操作，true：跳回到首页，false：继续请求 // 默认false
//        paginationInnerInterceptor.setOverflow(false);
        //设置最大的单页数据数量，单页分页条数限制(默认无限制）
//        paginationInnerInterceptor.setMaxLimit(500L);

        mybatisPlusInterceptor.addInnerInterceptor(paginationInnerInterceptor);

        return mybatisPlusInterceptor;
    }

}
