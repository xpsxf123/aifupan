package com.jiuyu.replay.api.mybatisplus;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * mybatis-plus自动生成代码
 *
 * @author RayChou
 * @since 2025-05-29
 */
public class CodeGenerator {

    public static void main(String[] args) {
        // 数据库配置
        String url = "jdbc:mysql://ifupan-test-mysql.rwlb.rds.aliyuncs.com:3306/replay-dev?useSSL=false&serverTimezone=UTC";
        String username = "ifupan_root";
        String password = "TGFa3s%%zP(ijk-n";

        // 项目路径
        String projectPath = System.getProperty("user.dir") + "\\replay-api";

        // 代码生成
        FastAutoGenerator.create(url, username, password)
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("RayChou")        // 作者
                            .outputDir(projectPath + "/src/test/java") // 输出目录
                            .dateType(DateType.TIME_PACK) // 使用java.time包下的时间类
                            .commentDate("yyyy-MM-dd")   // 注释日期格式
                            .disableOpenDir();  // 禁止打开输出目录
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent("com.jiuyu.replay")    // 父包名
                            .moduleName("video")        // 模块名（可选）
                            .entity("entity")           // 实体包名
                            .service("service")          // service包名
                            .serviceImpl("service.impl") // service实现包名
                            .mapper("dao")           // mapper包名
                            .xml("mapper.xml")           // xml文件位置
                            .controller("controller")    // controller包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/test/java/com/jiuyu/replay/common/mapperXml")); // XML路径
                })
                // 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude("tb_video_hot_search_video") // 要生成的表名
                            .addTablePrefix("tb_")  // 表前缀过滤

                            // Entity策略

                            .entityBuilder().enableFileOverride().enableLombok()               // 启用Lombok
                            .enableChainModel()           // 链式模型
                            .enableTableFieldAnnotation()  // 字段注解
                            .versionColumnName("version") // 乐观锁字段名
                            .logicDeleteColumnName("is_deleted")// 逻辑删除字段名
                            .formatFileName("%sEntity")

                            // Mapper策略
                            .mapperBuilder().enableFileOverride().enableMapperAnnotation()     // 启用@Mapper
                            .enableBaseResultMap()        // 生成resultMap
                            .enableBaseColumnList()       // 生成columnList
                            .formatMapperFileName("%sDao")

                            // Service策略
                            .serviceBuilder().enableFileOverride().formatServiceFileName("%sService") // 服务接口命名
                            .formatServiceImplFileName("%sServiceImpl") // 服务实现命名

                            // Controller策略
                            .controllerBuilder().enableFileOverride().enableRestStyle();           // 启用REST风格
                })
                // 模板引擎
                .templateEngine(new FreemarkerTemplateEngine()).execute();
    }
}