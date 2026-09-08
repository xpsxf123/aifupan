package com.jiuyu.replay.power.producer;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.entity.UserRemarkEntity;
import com.jiuyu.replay.power.producer.impl.UserRemarkProducerImpl;
import com.jiuyu.replay.power.repository.service.UserRemarkService;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.vo.LatestRemarkVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserRemarkProducerImpl#batchLatestByUserIds} 批量最新跟进单测
 *
 * <p>覆盖 AC-2（N+1 红线：1 次 remark 批量 IN + 1 次 createIds 批量 listByIds）与
 * AC-3（无跟进 / 跟进人解析不到的兜底）。</p>
 *
 * @author test-engineer
 * @date 2026-07-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserRemarkProducerImplTest {

    @Mock
    private UserRemarkService userRemarkService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserRemarkProducerImpl producer;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<UserRemarkEntity>> wrapperCaptor;

    /**
     * LambdaQueryWrapper 需要 MyBatis-Plus 的 TableInfo lambda 缓存，单测无 Spring 上下文，手动初始化。
     */
    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserRemarkEntity.class);
    }

    // ---------- 辅助方法 ----------

    /**
     * 构造一条跟进记录。
     */
    private UserRemarkEntity remark(Long id, Long userId, String content, Date createDate, Long createId) {
        UserRemarkEntity entity = new UserRemarkEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setRemark(content);
        entity.setCreateDate(createDate);
        entity.setCreateId(createId);
        entity.setIsDeleted(0);
        return entity;
    }

    /**
     * 构造一个跟进人用户。
     */
    private UserEntity user(Long id, String nickName) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setNickName(nickName);
        return entity;
    }

    // ========== AC-3 场景11：空入参 → 空 Map 且不发查询 ==========

    /**
     * AC-3：userIds 为 null → 返回空 Map，不发任何查询。
     */
    @Test
    @DisplayName("AC-3 入参为 null：返回空 Map 且不发查询")
    void batchLatestByUserIds_nullInput_returnsEmptyMapWithoutQuery() {
        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(userRemarkService);
        verifyNoInteractions(userService);
    }

    /**
     * AC-3：userIds 为空集合 → 返回空 Map，不发任何查询。
     */
    @Test
    @DisplayName("AC-3 入参为空集合：返回空 Map 且不发查询")
    void batchLatestByUserIds_emptyInput_returnsEmptyMapWithoutQuery() {
        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRemarkService);
        verifyNoInteractions(userService);
    }

    /**
     * AC-3：入参全为 null 元素 → 去重过滤后为空，返回空 Map 且不发查询。
     */
    @Test
    @DisplayName("AC-3 入参全为 null 元素：过滤后返回空 Map 且不发查询")
    void batchLatestByUserIds_allNullElements_returnsEmptyMapWithoutQuery() {
        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Arrays.asList(null, null));

        assertTrue(result.isEmpty());
        verifyNoInteractions(userRemarkService);
    }

    // ========== AC-3 场景10：无跟进记录 ==========

    /**
     * AC-3：用户无任何跟进记录 → 返回空 Map（上层据此把 latestRemark 置 null），不解析跟进人，不抛异常。
     */
    @Test
    @DisplayName("AC-3 用户无跟进记录：返回空 Map 且不解析跟进人")
    void batchLatestByUserIds_noRemarkRows_returnsEmptyMapAndSkipsNickNameLookup() {
        when(userRemarkService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Arrays.asList(1001L, 1002L));

        assertTrue(result.isEmpty());
        assertNull(result.get(1001L));
        verify(userService, never()).listByIds(anyCollection());
    }

    // ========== AC-1 场景12：同用户多条 → 取首条（create_date DESC, id DESC） ==========

    /**
     * AC-1：同一用户多条跟进 → 取查询结果首条；tie-break 用 create_date 相同、id 不同的数据验证。
     *
     * <p>排序在 SQL 侧（ORDER BY create_date DESC, id DESC），本用例同时断言
     * wrapper 确实带了这两级排序，且 Java 侧「首条即最新」的去重语义正确。</p>
     */
    @Test
    @DisplayName("AC-1 同用户多条跟进：按 create_date DESC, id DESC 取首条（含 id tie-break）")
    void batchLatestByUserIds_multipleRemarksForOneUser_takesFirstWithIdTieBreak() {
        // Given：三条同 create_date、id 不同，DB 已按 id DESC 返回
        Date sameMoment = new Date(1_760_000_000_000L);
        when(userRemarkService.list(wrapperCaptor.capture()))
                .thenReturn(Arrays.asList(
                        remark(9003L, 1001L, "最新一条", sameMoment, 7001L),
                        remark(9002L, 1001L, "较早一条", sameMoment, 7001L),
                        remark(9001L, 1001L, "最早一条", sameMoment, 7001L)));
        when(userService.listByIds(anyCollection()))
                .thenReturn(Collections.singletonList(user(7001L, "张三")));

        // When
        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Collections.singletonList(1001L));

        // Then：取 id 最大的那条
        assertEquals(1, result.size());
        assertEquals("最新一条", result.get(1001L).getRemark());
        assertEquals(sameMoment, result.get(1001L).getRemarkTime());
        assertEquals("张三", result.get(1001L).getCreateName());

        // 排序口径由 SQL 保证：create_date DESC 主序 + id DESC tie-break
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("create_date DESC"), "必须按 create_date DESC 排序，实际：" + sqlSegment);
        assertTrue(sqlSegment.contains("id DESC"), "必须以 id DESC 做 tie-break，实际：" + sqlSegment);
        // 仅按 user_id + is_deleted 过滤（tb_user_remark 物理无 tenant_id，见 openspec E-2）
        assertTrue(sqlSegment.contains("is_deleted"), "必须过滤 is_deleted，实际：" + sqlSegment);
    }

    /**
     * AC-1：多用户各取各自最新一条，互不串味。
     */
    @Test
    @DisplayName("AC-1 多用户批量：每人各取自己的最新一条")
    void batchLatestByUserIds_multipleUsers_eachGetsOwnLatest() {
        Date newer = new Date(1_760_000_100_000L);
        Date older = new Date(1_759_000_000_000L);
        when(userRemarkService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(
                        remark(9010L, 1001L, "A用户最新", newer, 7001L),
                        remark(9009L, 1002L, "B用户最新", newer, 7002L),
                        remark(9008L, 1001L, "A用户旧的", older, 7001L),
                        remark(9007L, 1002L, "B用户旧的", older, 7002L)));
        when(userService.listByIds(anyCollection()))
                .thenReturn(Arrays.asList(user(7001L, "张三"), user(7002L, "李四")));

        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Arrays.asList(1001L, 1002L));

        assertEquals(2, result.size());
        assertEquals("A用户最新", result.get(1001L).getRemark());
        assertEquals("张三", result.get(1001L).getCreateName());
        assertEquals("B用户最新", result.get(1002L).getRemark());
        assertEquals("李四", result.get(1002L).getCreateName());
    }

    // ========== AC-3 场景13：跟进人 create_id 解析不到 ==========

    /**
     * AC-3：跟进人已被删除 / 查不到 → createName 为 null，其余字段照常返回，不抛异常。
     */
    @Test
    @DisplayName("AC-3 跟进人解析不到：createName 为 null，其余字段照常")
    void batchLatestByUserIds_createIdUnresolvable_createNameIsNullButOthersIntact() {
        Date moment = new Date(1_760_000_000_000L);
        when(userRemarkService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(remark(9101L, 1001L, "内容仍在", moment, 8888L)));
        // 跟进人已删除，listByIds 查不到
        when(userService.listByIds(anyCollection())).thenReturn(Collections.emptyList());

        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Collections.singletonList(1001L));

        LatestRemarkVo vo = result.get(1001L);
        assertNotNull(vo);
        assertEquals("内容仍在", vo.getRemark());
        assertEquals(moment, vo.getRemarkTime());
        assertNull(vo.getCreateName());
    }

    /**
     * AC-3：跟进记录的 create_id 为 null → 不发姓名解析查询，createName 为 null。
     */
    @Test
    @DisplayName("AC-3 create_id 为 null：不发姓名解析查询，createName 为 null")
    void batchLatestByUserIds_nullCreateId_skipsNickNameLookup() {
        when(userRemarkService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(remark(9102L, 1001L, "无跟进人", new Date(), null)));

        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Collections.singletonList(1001L));

        assertNotNull(result.get(1001L));
        assertNull(result.get(1001L).getCreateName());
        verify(userService, never()).listByIds(anyCollection());
    }

    // ========== AC-2 场景14：N+1 红线 ==========

    /**
     * AC-2：N 个用户 → remark 批量 IN 恰 1 次、listByIds 恰 1 次，for-loop 内不得有任何查询。
     */
    @Test
    @DisplayName("AC-2 N+1 红线：list 与 listByIds 各只调用 1 次")
    void batchLatestByUserIds_nUsers_issuesExactlyOneRemarkQueryAndOneNickNameQuery() {
        // Given：20 个用户、20 条跟进、5 个不同跟进人
        int n = 20;
        Long[] userIds = new Long[n];
        UserRemarkEntity[] remarks = new UserRemarkEntity[n];
        for (int i = 0; i < n; i++) {
            userIds[i] = 1000L + i;
            remarks[i] = remark(9200L + i, userIds[i], "跟进" + i, new Date(1_760_000_000_000L + i), 7000L + (i % 5));
        }
        when(userRemarkService.list(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(remarks));
        when(userService.listByIds(anyCollection())).thenReturn(Arrays.asList(
                user(7000L, "跟进人0"), user(7001L, "跟进人1"), user(7002L, "跟进人2"),
                user(7003L, "跟进人3"), user(7004L, "跟进人4")));

        // When
        Map<Long, LatestRemarkVo> result = producer.batchLatestByUserIds(Arrays.asList(userIds));

        // Then：装配 20 行，但查询次数固定为 1 + 1
        assertEquals(n, result.size());
        verify(userRemarkService, times(1)).list(any(LambdaQueryWrapper.class));
        verify(userService, times(1)).listByIds(anyCollection());
    }

    /**
     * AC-2：入参含重复 userId → 去重后仍只发 1 次批量查询。
     */
    @Test
    @DisplayName("AC-2 入参含重复 userId：去重后仍只发 1 次批量查询")
    void batchLatestByUserIds_duplicateUserIds_deduplicatesBeforeQuery() {
        when(userRemarkService.list(wrapperCaptor.capture()))
                .thenReturn(Collections.singletonList(remark(9301L, 1001L, "内容", new Date(), 7001L)));
        when(userService.listByIds(anyCollection()))
                .thenReturn(Collections.singletonList(user(7001L, "张三")));

        producer.batchLatestByUserIds(Arrays.asList(1001L, 1001L, 1001L, 1002L));

        verify(userRemarkService, times(1)).list(any(LambdaQueryWrapper.class));

        // IN 列表已去重为 2 个：4 个入参只生成 2 个占位符
        LambdaQueryWrapper<UserRemarkEntity> wrapper = wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment();
        Matcher matcher = Pattern.compile("user_id IN \\((#\\{[^}]+\\})(,#\\{[^}]+\\})*\\)").matcher(sqlSegment);
        assertTrue(matcher.find(), "必须走 user_id IN 批量查询，实际：" + sqlSegment);
        String inClause = matcher.group();
        assertEquals(2, inClause.split(",").length, "去重后 IN 应只剩 2 个占位符，实际：" + inClause);

        // 去重后的实参就是 1001 / 1002（getSqlSegment 后 paramNameValuePairs 才可读）
        Collection<Object> paramValues = wrapper.getParamNameValuePairs().values();
        assertTrue(paramValues.contains(1001L), "实参应含 1001，实际：" + paramValues);
        assertTrue(paramValues.contains(1002L), "实参应含 1002，实际：" + paramValues);
    }
}
