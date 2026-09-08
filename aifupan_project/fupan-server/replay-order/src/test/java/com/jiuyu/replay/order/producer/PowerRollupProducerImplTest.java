package com.jiuyu.replay.order.producer;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.entity.PowerRollupWatermarkEntity;
import com.jiuyu.replay.order.entity.TenantPowerRollupEntity;
import com.jiuyu.replay.order.entity.UserPowerRollupEntity;
import com.jiuyu.replay.order.producer.impl.PowerRollupProducerImpl;
import com.jiuyu.replay.order.repository.service.PowerRollupWatermarkService;
import com.jiuyu.replay.order.repository.service.TenantPowerRollupService;
import com.jiuyu.replay.order.repository.service.UserPowerRollupService;
import com.jiuyu.replay.order.vo.PowerAggregateVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link PowerRollupProducerImpl#refreshPowerRollup} 增量聚合单测
 *
 * <p>覆盖 AC-4（租户归属）与 AC-5（增量幂等 / 恰好一次 / 累加语义 / 水位线推进）。</p>
 *
 * <p>两张 rollup 表用 <b>有状态的 Mockito stub</b> 模拟（{@code userStore} / {@code tenantStore}），
 * 因此「连续跑两次值不翻倍」是对真实累加结果的断言，而非仅仅 verify 调用次数。</p>
 *
 * <p><b>不在本单测范围</b>：净额口径 {@code SUM(CASE WHEN signs=0 THEN quantity ELSE -quantity END)}
 * 与 {@code id > 水位线} 的范围扫写在 XML（{@code UserPowerRollupDao.xml}）里，
 * 由 {@link #mapperXml_incrementalAggregate_isNetAmountAndPkRangeScan()} 做 SQL 文本断言 +
 * 联调验证，Java 侧只能验证内存归并与落表语义。</p>
 *
 * @author test-engineer
 * @date 2026-07-24
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PowerRollupProducerImplTest {

    private static final String AI_TOKEN_CODE = "aiTokenNum";

    @Mock
    private UserPowerRollupService userPowerRollupService;

    @Mock
    private TenantPowerRollupService tenantPowerRollupService;

    @Mock
    private PowerRollupWatermarkService powerRollupWatermarkService;

    @Captor
    private ArgumentCaptor<List<PowerAggregateVo>> deltaListCaptor;

    @Captor
    private ArgumentCaptor<Collection<UserPowerRollupEntity>> userSaveCaptor;

    private PowerRollupProducerImpl producer;

    /**
     * 用户维度落表内存镜像：userId → user_power_consume
     */
    private final Map<String, Long> userStore = new LinkedHashMap<>();

    /**
     * 租户维度落表内存镜像：tenantId → tenant_power_consume
     */
    private final Map<Long, Long> tenantStore = new LinkedHashMap<>();

    /**
     * 生产代码用 LambdaQueryWrapper.select(Entity::getXxx) 读现存行，需要 MyBatis-Plus 的
     * TableInfo lambda 缓存；单测无 Spring 上下文，故在此手动初始化两个实体的表信息。
     */
    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserPowerRollupEntity.class);
        TableInfoHelper.initTableInfo(assistant, TenantPowerRollupEntity.class);
    }

    @BeforeEach
    void setUp() {
        producer = new PowerRollupProducerImpl(userPowerRollupService, tenantPowerRollupService,
                powerRollupWatermarkService);
        userStore.clear();
        tenantStore.clear();
        wireStatefulStores();
    }

    // ---------- 有状态 stub：把两张 rollup 表模拟成内存 Map ----------

    /**
     * 让 list / accumulate / saveBatch 三个方法读写内存镜像，从而可断言"累加而非覆盖""不翻倍"。
     */
    @SuppressWarnings("unchecked")
    private void wireStatefulStores() {
        // 用户表：现存行读取（生产代码只用返回值构建 existIds，故返回全量镜像即可）
        when(userPowerRollupService.list(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            List<UserPowerRollupEntity> list = new ArrayList<>();
            // key = "userId_tenantId"，还原出 userId + tenantId，供生产代码按复合键匹配 existKeys
            for (String key : userStore.keySet()) {
                String[] parts = key.split("_");
                UserPowerRollupEntity entity = new UserPowerRollupEntity();
                entity.setUserId(Long.valueOf(parts[0]));
                entity.setTenantId(Long.valueOf(parts[1]));
                list.add(entity);
            }
            return list;
        });
        when(userPowerRollupService.accumulateUserPowerConsume(anyList(), any(Date.class))).thenAnswer(inv -> {
            List<PowerAggregateVo> deltaList = inv.getArgument(0);
            for (PowerAggregateVo vo : deltaList) {
                userStore.merge(vo.getUserId() + "_" + vo.getTenantId(), vo.getPowerConsume(), Long::sum);
            }
            return deltaList.size();
        });
        when(userPowerRollupService.saveBatch(any(Collection.class))).thenAnswer(inv -> {
            Collection<UserPowerRollupEntity> saveList = inv.getArgument(0);
            for (UserPowerRollupEntity entity : saveList) {
                userStore.put(entity.getUserId() + "_" + entity.getTenantId(), entity.getUserPowerConsume());
            }
            return true;
        });

        // 租户表
        when(tenantPowerRollupService.list(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            List<TenantPowerRollupEntity> list = new ArrayList<>();
            for (Long tenantId : tenantStore.keySet()) {
                TenantPowerRollupEntity entity = new TenantPowerRollupEntity();
                entity.setTenantId(tenantId);
                list.add(entity);
            }
            return list;
        });
        when(tenantPowerRollupService.accumulateTenantPowerConsume(anyList(), any(Date.class))).thenAnswer(inv -> {
            List<PowerAggregateVo> deltaList = inv.getArgument(0);
            for (PowerAggregateVo vo : deltaList) {
                tenantStore.merge(vo.getTenantId(), vo.getPowerConsume(), Long::sum);
            }
            return deltaList.size();
        });
        when(tenantPowerRollupService.saveBatch(any(Collection.class))).thenAnswer(inv -> {
            Collection<TenantPowerRollupEntity> saveList = inv.getArgument(0);
            for (TenantPowerRollupEntity entity : saveList) {
                tenantStore.put(entity.getTenantId(), entity.getTenantPowerConsume());
            }
            return true;
        });
    }

    // ---------- 辅助方法 ----------

    /**
     * 构造一条增量聚合行。
     */
    private PowerAggregateVo row(Long userId, Long tenantId, Long powerConsume, Long maxDetailId) {
        PowerAggregateVo vo = new PowerAggregateVo();
        vo.setUserId(userId);
        vo.setTenantId(tenantId);
        vo.setPowerConsume(powerConsume);
        vo.setMaxDetailId(maxDetailId);
        return vo;
    }

    /**
     * 构造水位线行。
     */
    private PowerRollupWatermarkEntity watermark(Long lastDetailId) {
        PowerRollupWatermarkEntity entity = new PowerRollupWatermarkEntity();
        entity.setId(1L);
        entity.setBizCode(AI_TOKEN_CODE);
        entity.setLastDetailId(lastDetailId);
        entity.setIsDeleted(0);
        return entity;
    }

    // ========== AC-5 场景1：连续两次执行，第二次无新增行 → 零写入、值不翻倍 ==========

    /**
     * AC-5：第一次跑落表，第二次因 id > 新水位线无新增明细 → 不产生任何写入，两表值不翻倍（恰好一次）。
     */
    @Test
    @DisplayName("AC-5 连续两次执行：第二次无新增明细 → 零写入且两表值不翻倍")
    void refreshPowerRollup_runTwice_secondRunNoNewDetail_noWriteAndNoDoubling() {
        // Given：水位线 100，第一轮扫出 2 行（新上界 200），第二轮该区间已被消费
        PowerRollupWatermarkEntity wm = watermark(100L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 100L))
                .thenReturn(Arrays.asList(row(1001L, 1001L, 3000L, 180L), row(1002L, 1001L, 2000L, 200L)));
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 200L))
                .thenReturn(Collections.emptyList());

        // When：第一次执行
        producer.refreshPowerRollup();

        // Then：首轮落表正确，水位线推进到 200
        assertEquals(3000L, userStore.get("1001_1001"));
        assertEquals(2000L, userStore.get("1002_1001"));
        assertEquals(5000L, tenantStore.get(1001L));
        assertEquals(200L, wm.getLastDetailId());
        verify(powerRollupWatermarkService, times(1)).updateById(wm);

        // When：第二次执行（同一水位线之后已无新增明细）
        producer.refreshPowerRollup();

        // Then：值完全不变（不翻倍）
        assertEquals(3000L, userStore.get("1001_1001"));
        assertEquals(2000L, userStore.get("1002_1001"));
        assertEquals(5000L, tenantStore.get(1001L));
        // 且第二次没有产生任何写入：save / accumulate / 水位线推进的调用次数都停留在第一轮
        verify(userPowerRollupService, times(1)).saveBatch(any(Collection.class));
        verify(tenantPowerRollupService, times(1)).saveBatch(any(Collection.class));
        verify(userPowerRollupService, never()).accumulateUserPowerConsume(anyList(), any(Date.class));
        verify(tenantPowerRollupService, never()).accumulateTenantPowerConsume(anyList(), any(Date.class));
        verify(powerRollupWatermarkService, times(1)).updateById(any(PowerRollupWatermarkEntity.class));
        assertEquals(200L, wm.getLastDetailId());
    }

    // ========== AC-5 场景2：事务失败 → 水位线不推进；重跑同区间结果等同只跑一次 ==========

    /**
     * AC-5：落表阶段抛异常 → 异常上抛（交给 @Transactional 回滚），水位线不推进。
     */
    @Test
    @DisplayName("AC-5 落表抛异常：异常上抛且水位线不推进")
    void refreshPowerRollup_persistFails_watermarkNotAdvanced() {
        // Given
        PowerRollupWatermarkEntity wm = watermark(100L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 100L))
                .thenReturn(Collections.singletonList(row(1001L, 1001L, 3000L, 200L)));
        when(userPowerRollupService.saveBatch(any(Collection.class)))
                .thenThrow(new IllegalStateException("模拟落表失败"));

        // When / Then：异常上抛，Spring 事务据此回滚
        assertThrows(IllegalStateException.class, () -> producer.refreshPowerRollup());

        // 水位线既未被推进也未落库
        assertEquals(100L, wm.getLastDetailId());
        verify(powerRollupWatermarkService, never()).updateById(any(PowerRollupWatermarkEntity.class));
    }

    /**
     * AC-5：失败回滚后按同一水位线重跑同一区间，结果与"只成功跑一次"完全一致（恰好一次）。
     *
     * <p>回滚由 {@code @Transactional} 保证，单测中以清空内存镜像来模拟回滚后的状态。</p>
     */
    @Test
    @DisplayName("AC-5 失败回滚后重跑同一区间：结果与只跑一次一致（恰好一次）")
    void refreshPowerRollup_retryAfterRollback_resultEqualsSingleSuccessfulRun() {
        // Given：先记录"只成功跑一次"的基准结果
        PowerRollupWatermarkEntity baselineWm = watermark(100L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(baselineWm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 100L))
                .thenReturn(Arrays.asList(row(1001L, 1001L, 3000L, 180L), row(1002L, 1001L, 2000L, 200L)));
        producer.refreshPowerRollup();
        Map<String, Long> expectedUser = new LinkedHashMap<>(userStore);
        Map<Long, Long> expectedTenant = new LinkedHashMap<>(tenantStore);
        long expectedWatermark = baselineWm.getLastDetailId();

        // Given：重置为"第一轮失败并已回滚"的状态——两表空、水位线仍是 100
        userStore.clear();
        tenantStore.clear();
        PowerRollupWatermarkEntity retryWm = watermark(100L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(retryWm);

        // When：重跑同一区间
        producer.refreshPowerRollup();

        // Then：结果与只成功跑一次完全一致
        assertEquals(expectedUser, userStore);
        assertEquals(expectedTenant, tenantStore);
        assertEquals(expectedWatermark, retryWm.getLastDetailId());
    }

    /**
     * AC-5：累加与水位线推进必须同事务——断言方法上的 {@code @Transactional(rollbackFor = Exception.class)}。
     * 这是"恰好一次"的唯一真实保障，缺失则回滚语义失效。
     */
    @Test
    @DisplayName("AC-5 refreshPowerRollup 标注 @Transactional(rollbackFor = Exception.class)")
    void refreshPowerRollup_isAnnotatedTransactionalWithRollbackForException() throws NoSuchMethodException {
        Method method = PowerRollupProducerImpl.class.getMethod("refreshPowerRollup");
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional, "refreshPowerRollup 必须标注 @Transactional");
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class),
                "@Transactional 必须声明 rollbackFor = Exception.class");
    }

    // ========== AC-5 场景3：水位线行缺失 → 抛业务异常（任务对外呈现为失败）==========

    /**
     * AC-5：水位线行不存在 → 抛 {@link BusinessException} 终止，不得退化为全量跑、不得写表。
     *
     * <p>必须抛异常而非静默 return：静默 return 会让任务壳照常返回 R.ok，
     * XXL-Job Admin 显示「执行成功」，运维收不到告警，汇总长期不产出也无人发现。</p>
     */
    @Test
    @DisplayName("AC-5 水位线行缺失：抛 BusinessException 且不查聚合、不写表")
    void refreshPowerRollup_watermarkRowMissing_abortsWithoutAnyQueryOrWrite() {
        // Given
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(null);

        // When / Then：异常上抛，任务壳据此标记 XXL-Job 失败
        assertThrows(BusinessException.class, () -> producer.refreshPowerRollup());

        // 连聚合 SQL 都不发（禁止退化为全量跑），两表零写入
        verify(userPowerRollupService, never()).aggregateIncrementalPowerConsume(any(), any());
        verifyNoInteractions(tenantPowerRollupService);
        verify(userPowerRollupService, never()).saveBatch(any(Collection.class));
        verify(powerRollupWatermarkService, never()).updateById(any(PowerRollupWatermarkEntity.class));
        assertTrue(userStore.isEmpty());
        assertTrue(tenantStore.isEmpty());
    }

    /**
     * AC-5：水位线行存在但 lastDetailId 为 null（脏数据）→ 同样抛业务异常，不得当成 0 全量跑。
     */
    @Test
    @DisplayName("AC-5 水位线 lastDetailId 为 null：抛 BusinessException 且不退化为全量跑")
    void refreshPowerRollup_watermarkLastDetailIdNull_abortsWithoutFullScan() {
        // Given
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(watermark(null));

        // When / Then
        assertThrows(BusinessException.class, () -> producer.refreshPowerRollup());

        verify(userPowerRollupService, never()).aggregateIncrementalPowerConsume(any(), any());
        verify(userPowerRollupService, never()).saveBatch(any(Collection.class));
        verify(powerRollupWatermarkService, never()).updateById(any(PowerRollupWatermarkEntity.class));
        assertTrue(userStore.isEmpty());
        assertTrue(tenantStore.isEmpty());
    }

    // ========== AC-5 场景4：本轮无新增明细 → 直接返回 ==========

    /**
     * AC-5：聚合结果为空 → 不推进水位线、不写表。
     */
    @Test
    @DisplayName("AC-5 本轮无新增明细：不推进水位线、不写表")
    void refreshPowerRollup_noIncrementalRows_returnsWithoutWriteOrWatermarkAdvance() {
        // Given
        PowerRollupWatermarkEntity wm = watermark(500L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 500L))
                .thenReturn(Collections.emptyList());

        // When
        producer.refreshPowerRollup();

        // Then
        assertEquals(500L, wm.getLastDetailId());
        verify(powerRollupWatermarkService, never()).updateById(any(PowerRollupWatermarkEntity.class));
        verify(userPowerRollupService, never()).saveBatch(any(Collection.class));
        verify(userPowerRollupService, never()).accumulateUserPowerConsume(anyList(), any(Date.class));
        verifyNoInteractions(tenantPowerRollupService);
    }

    // ========== AC-5 场景5：累加语义（已存在 += delta；不存在 save + Snowflake + 初值=delta）==========

    /**
     * AC-5：已存在行走 accumulate（+= delta，不是覆盖），不存在行走 saveBatch。
     */
    @Test
    @DisplayName("AC-5 累加语义：已存在行 += delta 而非覆盖；新行 save 初值 = delta")
    void refreshPowerRollup_existingRowAccumulatesDelta_newRowSavesWithDeltaAsInitialValue() {
        // Given：1001 已有 3000，1003 全新
        userStore.put("1001_1001", 3000L);
        tenantStore.put(1001L, 3000L);
        PowerRollupWatermarkEntity wm = watermark(200L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 200L))
                .thenReturn(Arrays.asList(row(1001L, 1001L, 700L, 260L), row(1003L, 1003L, 900L, 280L)));

        // When
        producer.refreshPowerRollup();

        // Then：1001 = 3000 + 700（累加），1003 = 900（初值 = delta）
        assertEquals(3700L, userStore.get("1001_1001"));
        assertEquals(900L, userStore.get("1003_1003"));
        assertEquals(3700L, tenantStore.get(1001L));
        assertEquals(900L, tenantStore.get(1003L));

        // 传给 UPDATE 的是增量 delta 而不是累计总额
        verify(userPowerRollupService).accumulateUserPowerConsume(deltaListCaptor.capture(), any(Date.class));
        assertEquals(1, deltaListCaptor.getValue().size());
        assertEquals(1001L, deltaListCaptor.getValue().get(0).getUserId());
        assertEquals(700L, deltaListCaptor.getValue().get(0).getPowerConsume());

        // 新行带 Snowflake id + create_date / update_date + is_deleted=0
        verify(userPowerRollupService).saveBatch(userSaveCaptor.capture());
        assertEquals(1, userSaveCaptor.getValue().size());
        UserPowerRollupEntity saved = userSaveCaptor.getValue().iterator().next();
        assertEquals(1003L, saved.getUserId());
        assertEquals(900L, saved.getUserPowerConsume());
        assertNotNull(saved.getId(), "新行必须带 Snowflake 主键");
        assertTrue(saved.getId() > 0L, "Snowflake id 必须为正数");
        assertEquals(0, saved.getIsDeleted());
        assertNotNull(saved.getCreateDate());
        assertNotNull(saved.getUpdateDate());
        assertEquals(1003L, saved.getTenantId());
    }

    // ========== AC-1 场景6/7：净额口径 ==========

    /**
     * AC-1：同一用户既有扣减又有失败回退时，聚合行给出的是净额，Java 侧原样累加不再二次加工。
     *
     * <p>净额由 XML 的 {@code SUM(CASE WHEN signs=0 THEN quantity ELSE -quantity END)} 计算；
     * 本用例验证多行同用户（parent_user_id 变化导致分组分裂）在内存归并时是<b>代数和</b>，不是只取正值。</p>
     */
    @Test
    @DisplayName("AC-4 复合键：同用户在不同租户的消费各自落一行，不跨租户合并")
    void refreshPowerRollup_sameUserDifferentTenants_keptSeparatePerTenant() {
        // Given：同一用户 2001 两条聚合行——租户 2001 下 +5000、租户 3000 下 -1200
        PowerRollupWatermarkEntity wm = watermark(0L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 0L))
                .thenReturn(Arrays.asList(row(2001L, 2001L, 5000L, 50L), row(2001L, 3000L, -1200L, 60L)));

        // When
        producer.refreshPowerRollup();

        // Then：按 (user_id, tenant_id) 各记各的，不合并成一条
        assertEquals(5000L, userStore.get("2001_2001"));
        assertEquals(-1200L, userStore.get("2001_3000"));
    }

    /**
     * AC-1 极端场景：回退大于扣减 → 净额为负。
     *
     * <p><b>实际行为记录</b>：生产代码不做下限保护，负值会被原样写入 rollup 表，
     * 最终经 {@code pageListNew} 展示为负数（详见测试报告 [Issues Found]）。本用例只断言"不抛异常"。</p>
     */
    @Test
    @DisplayName("AC-1 回退大于扣减：净额为负，不抛异常（负值原样落表）")
    void refreshPowerRollup_refundExceedsConsume_negativeNetDoesNotThrow() {
        // Given
        PowerRollupWatermarkEntity wm = watermark(0L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 0L))
                .thenReturn(Collections.singletonList(row(2002L, 2002L, -800L, 70L)));

        // When：不抛异常
        producer.refreshPowerRollup();

        // Then：负值原样落表（当前实现无下限钳制）
        assertEquals(-800L, userStore.get("2002_2002"));
        assertEquals(-800L, tenantStore.get(2002L));
    }

    // ========== AC-4 场景8/9：租户归属 ==========

    /**
     * AC-4：子账号消耗归入其主账号的租户汇总；主账号自身消耗归入自身；
     * userPowerConsume 严格取用户自身聚合，不并入父账号。
     */
    @Test
    @DisplayName("AC-4 租户归属：子账号消耗并入主账号租户汇总，用户维度不并入父账号")
    void refreshPowerRollup_subAccountConsume_rollsUpToOwnerTenantButNotToParentUserRow() {
        // Given：主账号 5000 消耗 1000；子账号 5001/5002 各消耗 300/200，owner 均为 5000
        //        另有一个无关主账号 6000 消耗 700，用于验证租户不串味
        PowerRollupWatermarkEntity wm = watermark(0L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 0L))
                .thenReturn(Arrays.asList(
                        row(5000L, 5000L, 1000L, 10L),
                        row(5001L, 5000L, 300L, 20L),
                        row(5002L, 5000L, 200L, 30L),
                        row(6000L, 6000L, 700L, 40L)));

        // When
        producer.refreshPowerRollup();

        // Then：租户 5000 = 1000 + 300 + 200
        assertEquals(1500L, tenantStore.get(5000L));
        assertEquals(700L, tenantStore.get(6000L));

        // 用户维度各记各的：主账号仍是自身的 1000，未把子账号并进来
        assertEquals(1000L, userStore.get("5000_5000"));
        assertEquals(300L, userStore.get("5001_5000"));
        assertEquals(200L, userStore.get("5002_5000"));

        // 用户行的 tenant_id 落的是主账号，便于按租户排查
        verify(userPowerRollupService).saveBatch(userSaveCaptor.capture());
        Map<Long, Long> ownerOf = new LinkedHashMap<>();
        for (UserPowerRollupEntity entity : userSaveCaptor.getValue()) {
            ownerOf.put(entity.getUserId(), entity.getTenantId());
        }
        assertEquals(5000L, ownerOf.get(5000L));
        assertEquals(5000L, ownerOf.get(5001L));
        assertEquals(5000L, ownerOf.get(5002L));
        assertEquals(6000L, ownerOf.get(6000L));
    }

    /**
     * AC-5 补充：水位线推进到本轮扫描到的全局 MAX(id)，而不是任意一行的 maxDetailId。
     */
    @Test
    @DisplayName("AC-5 水位线推进到本轮全局 MAX(detail id)")
    void refreshPowerRollup_advancesWatermarkToGlobalMaxDetailId() {
        // Given：三行的 maxDetailId 乱序
        PowerRollupWatermarkEntity wm = watermark(100L);
        when(powerRollupWatermarkService.getByBizCode(AI_TOKEN_CODE)).thenReturn(wm);
        when(userPowerRollupService.aggregateIncrementalPowerConsume(AI_TOKEN_CODE, 100L))
                .thenReturn(Arrays.asList(
                        row(1L, 1L, 10L, 305L),
                        row(2L, 2L, 20L, 999L),
                        row(3L, 3L, 30L, 412L)));

        // When
        producer.refreshPowerRollup();

        // Then
        assertEquals(999L, wm.getLastDetailId());
        assertNotNull(wm.getUpdateDate());
        verify(powerRollupWatermarkService).updateById(wm);
    }

    // ========== 增量聚合 SQL 文本断言（净额 + PK 范围扫 + 不 JOIN）==========

    /**
     * AC-1 / AC-5：增量聚合 SQL 必须是净额口径 + id 范围扫，且不得对 signs 过滤、不得 JOIN。
     * SQL 写在 XML 中，Java 单测无法执行，故以文本断言守住口径红线。
     */
    @Test
    @DisplayName("AC-1/AC-5 增量聚合 SQL：净额 CASE WHEN + id 范围扫 + create_date 安全滞后 + 无 signs 过滤 + 无 JOIN")
    void mapperXml_incrementalAggregate_isNetAmountAndPkRangeScan() throws Exception {
        String xml = readClasspath("/mapper/order/UserPowerRollupDao.xml");
        // 只取 <select> 块，避开注释里出现的 GROUP BY / signs 等字样
        String selectBlock = xml.substring(xml.indexOf("<select id=\"aggregateIncrementalPowerConsume\""),
                xml.indexOf("</select>"));
        String compact = selectBlock.replaceAll("\\s+", " ");

        // 净额口径：signs=0 加、其余减
        assertTrue(compact.contains("SUM(CASE WHEN upd.signs = 0 THEN upd.quantity ELSE -upd.quantity END)"),
                "增量聚合必须使用净额 SUM(CASE WHEN signs=0 THEN quantity ELSE -quantity END)");
        // 增量：id > 水位线（PK 范围扫）
        assertTrue(compact.contains("upd.id &gt; #{lastDetailId}"), "必须按 id > 水位线做增量 PK 范围扫");
        // 只按 commodity_type_code 过滤，不得再加 signs 过滤（否则把已回退的失败消耗算进去）
        assertTrue(compact.contains("upd.commodity_type_code = #{commodityTypeCode}"));
        String whereClause = compact.substring(compact.indexOf("WHERE"), compact.indexOf("GROUP BY"));
        assertFalse(whereClause.contains("signs"),
                "WHERE 中不得出现 signs 过滤条件（否则把已回退的失败消耗算成消耗，虚高）");
        // 安全滞后：Snowflake id 在对象构造时分配，id 序 != 提交序；
        // 只处理已静置 >= 1 小时的行，避免「id 小于水位线但当时尚未提交」的行被永久漏算
        assertTrue(whereClause.contains("upd.create_date &lt; DATE_SUB(NOW(), INTERVAL 1 HOUR)"),
                "增量聚合 WHERE 必须带 create_date 安全滞后过滤（1 小时），否则未提交事务的行会被永久漏算");
        // 不再 JOIN tb_tenant：租户直接取明细行 upd.tenant_id（写入时已固化），避免一主账号多租户 fan-out
        assertFalse(compact.toUpperCase().contains(" JOIN "), "增量聚合不得 JOIN 任何表，tenant_id 直接取 upd.tenant_id");
        assertTrue(compact.contains("upd.tenant_id"), "增量聚合必须直接取明细行 upd.tenant_id");
        // 口径修正：只算用户创建的真实消费，排除系统清零/reset（asset_creation_type=1）
        assertTrue(compact.contains("IFNULL(upd.asset_creation_type, 0) = 0"),
                "增量聚合必须过滤 asset_creation_type=0，排除系统清零/reset 污染");
        // 禁止 ${} 拼接
        assertFalse(xml.contains("${"), "禁止使用 ${} 拼 SQL");
    }

    /**
     * AC-5：两条 UPDATE 必须是 += delta 的累加语义，而不是覆盖赋值。
     */
    @Test
    @DisplayName("AC-5 双表 UPDATE 为 += delta 累加语义，非覆盖")
    void mapperXml_accumulateUpdates_areIncrementNotOverwrite() throws Exception {
        String userXml = readClasspath("/mapper/order/UserPowerRollupDao.xml").replaceAll("\\s+", " ");
        String tenantXml = readClasspath("/mapper/order/TenantPowerRollupDao.xml").replaceAll("\\s+", " ");

        assertTrue(userXml.contains("SET user_power_consume = user_power_consume +"),
                "用户表必须走 user_power_consume = user_power_consume + delta");
        assertTrue(tenantXml.contains("SET tenant_power_consume = tenant_power_consume +"),
                "租户表必须走 tenant_power_consume = tenant_power_consume + delta");
        // 租户 XML 不得再对明细表跑第二条聚合
        assertFalse(tenantXml.contains("tb_user_property_details"),
                "租户 XML 不得包含对明细表的第二条聚合 SQL");
        assertFalse(tenantXml.contains("${"), "禁止使用 ${} 拼 SQL");
    }

    /**
     * 读取 classpath 下的 mapper XML 文本。
     */
    private String readClasspath(String path) throws Exception {
        try (InputStream in = PowerRollupProducerImplTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "找不到 classpath 资源：" + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
