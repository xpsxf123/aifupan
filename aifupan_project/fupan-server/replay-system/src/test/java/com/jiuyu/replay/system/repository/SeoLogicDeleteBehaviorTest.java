package com.jiuyu.replay.system.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁住 MyBatis-Plus 全局逻辑删除对更新语句的影响。
 *
 * <p><b>这组测试存在的原因</b>：项目在 application-*.yml 里配了全局
 * {@code mybatis-plus.global-config.db-config.logic-delete-field: isDeleted}。
 * 该配置会让 MP 把所有实体的 isDeleted 字段识别为逻辑删除字段，进而
 * <b>把它从 updateById 生成的 SET 子句中剔除</b>——实体上手动 setIsDeleted(1)
 * 会被框架静默丢弃，UPDATE 语句里根本没有这一列。
 *
 * <p>该缺陷在本需求里造成过实际后果：删除分类/标签时名称与 slug 被改成了墓碑值，
 * 但 is_deleted 仍是 0，记录继续出现在列表与官网，而文章标签关联已被物理清掉。
 * 它没有被任何测试发现，是靠 code review 实测 MP 源码才暴露的。
 *
 * <p>验证的是 SQL <b>生成</b>阶段的行为——这正是缺陷所在的层。真实执行只是把生成好的
 * 语句发给数据库，不会再改动 SET 子句。需要真实数据库才能验证的部分见类末注释。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("MyBatis-Plus 全局逻辑删除对更新语句的影响")
class SeoLogicDeleteBehaviorTest {

    /** 与 application-*.yml 中 mybatis-plus.global-config.db-config 保持一致 */
    private static TableInfo tableInfoWithGlobalLogicDelete(Class<?> entityClass) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("isDeleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);

        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        return TableInfoHelper.initTableInfo(assistant, entityClass);
    }

    @Nested
    @DisplayName("全局配置的实际效果")
    class GlobalConfigEffect {

        @Test
        @DisplayName("三张 SEO 表都被全局配置识别为带逻辑删除——即使实体没有 @TableLogic")
        void tableInfo_withGlobalLogicDeleteField_marksEntitiesAsLogicDelete() {
            for (Class<?> entityClass : new Class<?>[]{
                    SeoTagEntity.class, SeoCategoryEntity.class, SeoArticleEntity.class}) {
                TableInfo tableInfo = tableInfoWithGlobalLogicDelete(entityClass);

                assertTrue(tableInfo.isWithLogicDelete(),
                        entityClass.getSimpleName() + " 未被识别为逻辑删除实体，"
                                + "说明全局配置的前提变了，本组测试的结论需重新评估");
            }
        }

        @Test
        @DisplayName("updateById 的 SET 子句里没有 is_deleted —— 手动 setIsDeleted 会被静默丢弃")
        void updateByIdSqlSet_withLogicDeleteField_excludesIsDeletedColumn() {
            TableInfo tableInfo = tableInfoWithGlobalLogicDelete(SeoTagEntity.class);

            // ignoreLogicDelFiled=true 正是 UpdateById.injectMappedStatement 使用的参数
            String sqlSet = tableInfo.getAllSqlSet(true, null);

            assertFalse(sqlSet.contains("is_deleted"),
                    "若这条断言失败，说明 MP 版本或全局配置变了、updateById 已能写入 is_deleted，"
                            + "那么 Service 里改用 LambdaUpdateWrapper 的迂回就可以简化。实际 SET：" + sqlSet);
            // 其余字段仍在，确认不是整个 SET 都空了导致的假通过
            assertTrue(sqlSet.contains("name="), "实际 SET：" + sqlSet);
            assertTrue(sqlSet.contains("slug="), "实际 SET：" + sqlSet);
        }

        @Test
        @DisplayName("查询会自动追加 is_deleted 过滤——代码里再写一次属重复条件")
        void logicDeleteSql_withLogicDeleteField_appendedToWhere() {
            TableInfo tableInfo = tableInfoWithGlobalLogicDelete(SeoTagEntity.class);

            String logicDeleteSql = tableInfo.getLogicDeleteSql(true, true);

            assertTrue(logicDeleteSql.contains("is_deleted"),
                    "实际：" + logicDeleteSql);
        }
    }

    @Nested
    @DisplayName("删除实现必须绕开该限制")
    class DeleteImplementationWorkaround {

        @Test
        @DisplayName("LambdaUpdateWrapper.set 能写入 is_deleted —— 这是删除实现所依赖的前提")
        void lambdaUpdateWrapperSqlSet_setIsDeleted_containsColumn() {
            // LambdaUpdateWrapper 依赖 TableInfo 的 lambda 缓存，须先初始化实体元信息。
            // 这里同样带上全局逻辑删除配置，确保与生产环境条件一致
            tableInfoWithGlobalLogicDelete(SeoTagEntity.class);

            LambdaUpdateWrapper<SeoTagEntity> wrapper = new LambdaUpdateWrapper<SeoTagEntity>()
                    .set(SeoTagEntity::getTagName, "zhibofupan__del_1001")
                    .set(SeoTagEntity::getSlug, "zhibofupan__del_1001")
                    .set(SeoTagEntity::getIsDeleted, SeoConstant.DELETED)
                    .set(SeoTagEntity::getUpdateDate, LocalDateTime.now())
                    .eq(SeoTagEntity::getId, 1001L);

            String sqlSet = wrapper.getSqlSet();

            assertTrue(sqlSet.contains("is_deleted"),
                    "ew.sqlSet 也被过滤了逻辑删除字段，删除将完全失效。实际 SET：" + sqlSet);
            assertTrue(sqlSet.contains("name") && sqlSet.contains("slug"),
                    "墓碑值改写缺失，删除后无法重建同名同 slug 记录。实际 SET：" + sqlSet);
        }

        @Test
        @DisplayName("分类删除同样依赖 LambdaUpdateWrapper 写入 is_deleted")
        void lambdaUpdateWrapperSqlSet_forCategory_containsIsDeleted() {
            tableInfoWithGlobalLogicDelete(SeoCategoryEntity.class);

            LambdaUpdateWrapper<SeoCategoryEntity> wrapper = new LambdaUpdateWrapper<SeoCategoryEntity>()
                    .set(SeoCategoryEntity::getCategoryName, "zhiboyunying__del_2002")
                    .set(SeoCategoryEntity::getSlug, "zhibo-yunying__del_2002")
                    .set(SeoCategoryEntity::getIsDeleted, SeoConstant.DELETED)
                    .eq(SeoCategoryEntity::getId, 2002L);

            assertTrue(wrapper.getSqlSet().contains("is_deleted"), "实际 SET：" + wrapper.getSqlSet());
        }
    }

    /*
     * 以下场景本层无法覆盖，需真实数据库的集成测试（当前项目无 H2/Testcontainers 基础设施）：
     *
     * 1. 删除后用原生 SQL 查该行，确认 is_deleted 真的落为 1、name/slug 真的是墓碑值；
     * 2. uk_seo_tag_name / uk_seo_tag_slug 唯一索引参与下，「删除后立刻重建同名同 slug」成立；
     * 3. 并发下 DuplicateKeyException 的捕获与重查分支；
     * 4. @Transactional(rollbackFor = Exception.class) 的真实回滚边界；
     * 5. LambdaQueryWrapper 渲染出的 SQL 能在 MySQL 上跑通（列名映射、last("limit 1") 拼接位置）。
     */
}
