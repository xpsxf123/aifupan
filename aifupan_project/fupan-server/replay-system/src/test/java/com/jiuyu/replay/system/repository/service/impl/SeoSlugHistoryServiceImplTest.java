package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoSlugHistoryEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * slug 历史记录的行为校验。
 *
 * <p>这组测试锁的是「301 到底跳到哪里」这件事——它没有任何运行时报错可依赖：
 * 记错了只会让读者被导向错误页面，或者让本该 301 的旧地址直接 404，
 * 而累积的排名与外链就此丢失，没人会收到告警。
 *
 * @author claude
 * @date 2026-08-18
 */
@DisplayName("SeoSlugHistoryServiceImpl")
class SeoSlugHistoryServiceImplTest {

    private SeoSlugHistoryServiceImpl service;

    /** 注册实体到 MP 的 lambda 列名缓存，否则 LambdaQueryWrapper 构造即抛异常 */
    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoSlugHistoryServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoSlugHistoryEntity.class);
    }

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new SeoSlugHistoryServiceImpl());
    }

    @Nested
    @DisplayName("记录变更")
    class Record {

        @Test
        @DisplayName("旧 slug 为空时不记录——新建的实体没有旧值可记")
        void skipsBlankOldSlug() {
            service.record(SeoConstant.SLUG_ENTITY_ARTICLE, 1L, null);
            service.record(SeoConstant.SLUG_ENTITY_ARTICLE, 1L, "   ");
            verify(service, never()).save(any());
        }

        @Test
        @DisplayName("entityId 为空时不记录——记了也解析不出跳转目标")
        void skipsNullEntityId() {
            service.record(SeoConstant.SLUG_ENTITY_ARTICLE, null, "old-slug");
            verify(service, never()).save(any());
        }

        @Test
        @DisplayName("首次记录：插入新行，只存旧值")
        void insertsWhenAbsent() {
            doReturn(null).when(service).getOne(any(LambdaQueryWrapper.class));
            doReturn(true).when(service).save(any());

            service.record(SeoConstant.SLUG_ENTITY_ARTICLE, 1001L, "old-slug");

            ArgumentCaptor<SeoSlugHistoryEntity> captor =
                    ArgumentCaptor.forClass(SeoSlugHistoryEntity.class);
            verify(service).save(captor.capture());
            SeoSlugHistoryEntity saved = captor.getValue();

            assertEquals(SeoConstant.SLUG_ENTITY_ARTICLE, saved.getEntityType());
            assertEquals(1001L, saved.getEntityId());
            assertEquals("old-slug", saved.getOldSlug());
            assertNotNull(saved.getId(), "主键是 IdType.INPUT，必须由代码显式生成雪花 ID");
            assertNotNull(saved.getCreateDate(), "create_date 非空字段，不填会在插入时报错");
        }

        @Test
        @DisplayName("旧 slug 已存在时更新归属，而不是插入重复行")
        void updatesWhenPresent() {
            SeoSlugHistoryEntity existing = new SeoSlugHistoryEntity();
            existing.setId(999L);
            existing.setEntityId(1001L);
            doReturn(existing).when(service).getOne(any(LambdaQueryWrapper.class));
            doReturn(true).when(service).update(any(LambdaUpdateWrapper.class));

            // 场景：A 用过 old-slug 改走，后来 B 也用了 old-slug 又改走。
            // 唯一键 uk_type_old_slug 决定了只能有一行，归属应更新为最新的 B
            service.record(SeoConstant.SLUG_ENTITY_ARTICLE, 2002L, "old-slug");

            verify(service, never()).save(any());
            verify(service).update(any(LambdaUpdateWrapper.class));
        }
    }

    @Nested
    @DisplayName("解析旧 slug")
    class Resolve {

        @Test
        @DisplayName("无记录时返回 null，由调用方决定 404")
        void returnsNullWhenAbsent() {
            doReturn(null).when(service).getOne(any(LambdaQueryWrapper.class));
            assertNull(service.resolveEntityId(SeoConstant.SLUG_ENTITY_ARTICLE, "never-existed"));
        }

        @Test
        @DisplayName("空 slug 直接返回 null，不打数据库")
        void returnsNullForBlank() {
            assertNull(service.resolveEntityId(SeoConstant.SLUG_ENTITY_ARTICLE, null));
            assertNull(service.resolveEntityId(SeoConstant.SLUG_ENTITY_ARTICLE, "  "));
            verify(service, never()).getOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("命中时返回实体 ID——注意这不代表实体仍存在，调用方须再查一次")
        void returnsEntityId() {
            SeoSlugHistoryEntity row = new SeoSlugHistoryEntity();
            row.setEntityId(1001L);
            doReturn(row).when(service).getOne(any(LambdaQueryWrapper.class));

            assertEquals(1001L, service.resolveEntityId(SeoConstant.SLUG_ENTITY_ARTICLE, "old-slug"));
        }
    }
}
