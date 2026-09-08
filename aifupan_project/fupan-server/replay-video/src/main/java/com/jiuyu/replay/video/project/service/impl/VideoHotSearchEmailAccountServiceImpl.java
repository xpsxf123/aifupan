package com.jiuyu.replay.video.project.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.constant.BusinessCachePrefix;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.video.project.bo.email.HotSearchEmailImportBo;
import com.jiuyu.replay.video.project.bo.email.ReportFailureEmailBo;
import com.jiuyu.replay.video.project.dao.VideoHotSearchEmailAccountDao;
import com.jiuyu.replay.video.project.dao.VideoHotSearchEmailFailureLogDao;
import com.jiuyu.replay.video.project.dao.VideoHotSearchEmailUsageLogDao;
import com.jiuyu.replay.video.project.dto.HotSearchAccountRedisDTO;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailAccountEntity;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailFailureLogEntity;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailUsageLogEntity;
import com.jiuyu.replay.video.project.service.IpCityService;
import com.jiuyu.replay.video.project.service.VideoHotSearchEmailAccountService;
import com.jiuyu.replay.video.project.util.AESUtil;
import com.jiuyu.replay.video.project.util.DingTalkRobotUtil;
import com.jiuyu.replay.video.project.util.EmailAccountTypeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 热搜邮箱账号池管理服务 V2
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 基于 Redis 的全新架构实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoHotSearchEmailAccountServiceImpl extends ServiceImpl<VideoHotSearchEmailAccountDao, VideoHotSearchEmailAccountEntity> implements VideoHotSearchEmailAccountService {

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoHotSearchEmailAccountDao emailAccountDao;
    private final VideoHotSearchEmailUsageLogDao usageLogDao;
    private final VideoHotSearchEmailFailureLogDao failureLogDao;
    private final IpCityService ipCityService;

    @Value("${hot-search.dingtalk.webhook-url:}")
    private String dingTalkWebhookUrl;

    @Value("${hot-search.alert.threshold:10}")
    private int alertThreshold;

    @Value("${hot-search.account.day-max-request-count:50}")
    private int dayMaxRequestCount;

    /**
     * IP 锁定策略
     * 1. 锁住当天
     * 2. 锁住10分钟
     */
    @Value("${hot-search.account.ip-lock-strategy:1}")
    private int ipLockStrategy;

    @Value("${spring.profiles.active:unknown}")
    private String activeProfile;

    // 常量
    private static final int DEFAULT_MAX_CONCURRENT_USERS = 5;
    private static final int DEFAULT_USE_TIMEOUT_MINUTES = 60;
    private static final int IP_MAPPING_TTL_HOURS = 1;
    private static final int REDIS_TTL_DAYS = 7;

    // Lua 脚本
    private DefaultRedisScript<String> allocateAccountScript;
    private DefaultRedisScript<String> releaseAccountScript;

    /**
     * 初始化 Lua 脚本
     */
    @PostConstruct
    public void init() {

        // 加载合并版分配账号脚本
        allocateAccountScript = new DefaultRedisScript<>();
        allocateAccountScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/allocate_account.lua")));
        allocateAccountScript.setResultType(String.class);

        // 加载释放账号脚本
        releaseAccountScript = new DefaultRedisScript<>();
        releaseAccountScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/release_account.lua")));
        releaseAccountScript.setResultType(String.class);

        log.info("Lua 脚本加载完成");
    }

    /**
     * 获取可用邮箱账号（使用统一版Lua脚本）
     *
     * @param clientIp   客户端 IP
     * @param clientCity 客户端城市
     *
     * @return 邮箱账号实体
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public VideoHotSearchEmailAccountEntity getAvailableAccount(String clientIp, String clientCity) {
        log.info("【账号分配开始】clientIp={}, clientCity={}, thread={}", clientIp, clientCity, Thread.currentThread().getName());
        // 如果触发单日最高异常次数，则返回错误
        if (stringRedisTemplate.hasKey(BusinessCachePrefix.HOT_SEARCH_EMAIL_DAY_LIMIT.prefix + clientIp)) {
            return null;
        }
        // 使用统一版Lua脚本处理所有场景
        log.info("【使用统一脚本】clientIp={}, clientCity={}", clientIp, clientCity);
        VideoHotSearchEmailAccountEntity account = allocateFromUnifiedPool(clientIp, clientCity);
        if (account != null) {
            log.info("【统一脚本分配成功】clientIp={}, clientCity={}, accountId={}", clientIp, clientCity, account.getId());
            return account;
        }

        log.warn("【Lua脚本分配失败】clientIp={}, clientCity={}", clientIp, clientCity);

        // 兜底逻辑：从数据库获取使用最少的账号
        log.warn("【启用兜底逻辑】Redis 池中没有可用账号，从数据库获取：clientIp={}, clientCity={}", clientIp, clientCity);
        VideoHotSearchEmailAccountEntity fallbackAccount = allocateFromDatabaseFallback(clientIp, clientCity);
        if (fallbackAccount != null) {
            log.warn("【兜底逻辑成功】clientIp={}, clientCity={}, accountId={}", clientIp, clientCity, fallbackAccount.getId());
            return fallbackAccount;
        }

        log.error("【账号分配失败】所有渠道均无可用邮箱账号：clientIp={}, clientCity={}", clientIp, clientCity);
        return null;
    }

    /**
     * 使用统一版Lua脚本分配账号（处理所有场景）
     */
    private VideoHotSearchEmailAccountEntity allocateFromUnifiedPool(String clientIp, String clientCity) {
        // 根据是否有客户端城市决定同城池池key
        String cityPoolKey = StrUtil.isNotBlank(clientCity)
            ? BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix + clientCity
            : "";
        String globalPoolKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        String ipMappingKeyPrefix = BusinessCachePrefix.HOT_SEARCH_EMAIL_IP_MAPPING.prefix;

        log.info("【统一脚本执行前】clientIp={}, cityPoolKey={}, globalPoolKey={}, hasCity={}",
            clientIp, cityPoolKey, globalPoolKey, StrUtil.isNotBlank(clientCity));

        // 执行统一版Lua脚本
        String result = stringRedisTemplate.execute(
            allocateAccountScript,
            Arrays.asList(cityPoolKey, globalPoolKey, accountsHashKey, ipMappingKeyPrefix),
            String.valueOf(System.currentTimeMillis()),
            clientIp,
            StrUtil.isNotBlank(clientCity) ? clientCity : "",
            String.valueOf(REDIS_TTL_DAYS * 24 * 60 * 60) // Redis TTL（秒）
        );

        log.info("【统一脚本执行后】clientIp={}, result={}", clientIp, result);

        if (StrUtil.isBlank(result)) {
            log.info("【统一脚本返回空】clientIp={}, cityPoolKey={}", clientIp, cityPoolKey);
            return null;
        }

        return handleAllocationResult(result, clientIp, clientCity);
    }

    /**
     * 上报不可用账号
     *
     * @param bo 上报信息
     *
     * @description 将账号标记为不可用，从 Redis 移除，记录失败日志
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reportUnavailableAccount(ReportFailureEmailBo bo) {
        Long accountId = bo.getAccountId();
        log.info("上报不可用账号：accountId={}, reason={}", accountId, bo.getFailureReason());

        VideoHotSearchEmailAccountEntity account = emailAccountDao.selectById(accountId);
        if (account == null) {
            log.warn("账号 {} 不存在", accountId);
            return;
        }

        // 调用统一的不可用账号处理逻辑
        handleUnavailableAccount(account, bo.getClientIp(), bo.getFailureReason());

        // 记录失败日志
        VideoHotSearchEmailFailureLogEntity failureLog = VideoHotSearchEmailFailureLogEntity.builder()
            .id(SnowflakeManager.nextValue())
            .emailAccountId(accountId)
            .email(account.getEmail())
            .clientIp(bo.getClientIp())
            .clientCity(StrUtil.isNotBlank(bo.getClientIp()) ? ipCityService.getCityByIp(bo.getClientIp()) : null)
            .failureType(bo.getFailureType())
            .failureReason(bo.getFailureReason())
            .errorCode(bo.getErrorCode())
            .errorMessage(bo.getErrorMessage())
            .createdDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .isDeleted((byte) 0)
            .build();
        failureLogDao.insert(failureLog);

        // 更新账号状态
        account.setLastFailureTime(LocalDateTime.now());
        account.setCurrentUserCount(0);
        account.setLastUseClientIp("");
        account.setCity("");
        account.setUpdateDate(LocalDateTime.now());
        if (bo.getFailureType() != null && bo.getFailureType() == 2) {
            account.setAccountStatus(3);
            log.info("账号 {} 因账号被封永久禁用，状态设置为3", accountId);
        } else {
            account.setAccountStatus(0);
            log.info("账号 {} 标记为不可用，状态设置为0", accountId);
        }
        emailAccountDao.updateById(account);

        LocalDateTime now = LocalDateTime.now();
        Long errorCount = ChainWrappers.lambdaQueryChain(failureLogDao)
            .eq(VideoHotSearchEmailFailureLogEntity::getClientIp, bo.getClientIp())
            .gt(VideoHotSearchEmailFailureLogEntity::getCreatedDate, now.withHour(0).withMinute(0).withSecond(0))
            .count();

        log.info("账号 {} 已标记为不可用并从 Redis 移除，失败日志已记录。 本日累计上传异常数：{}", accountId, errorCount);
        if (errorCount != null && errorCount >= dayMaxRequestCount) {
            log.warn("【异常数达到阈值】本日累计上传异常数：{}，阈值为：{}，开始发送钉钉通知", errorCount, dayMaxRequestCount);
            if (Objects.equals(activeProfile, "prod")) {
                DingTalkRobotUtil.sendMarkdownMessage(dingTalkWebhookUrl, "邮箱账号-客户端IP异常-" + activeProfile, "本日上报异常数量以达到阈值：" + dayMaxRequestCount + ", IP: " + bo.getClientIp());
            }

            long timeout =  Duration.between(now,now.withHour(23).withMinute(59).withSecond(59)).getSeconds();
            if (ipLockStrategy == 2) {
                timeout = 600;
            }
            stringRedisTemplate.opsForValue().set(BusinessCachePrefix.HOT_SEARCH_EMAIL_DAY_LIMIT.prefix + bo.getClientIp(), LocalDateTimeUtil.formatNormal(now), timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 释放超时账号
     *
     * @description 查询超过 1 小时未释放的账号，执行释放操作
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void releaseTimeoutAccounts() {
        log.info("开始释放超时账号");

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusHours(IP_MAPPING_TTL_HOURS);
        LambdaQueryWrapper<VideoHotSearchEmailUsageLogEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(VideoHotSearchEmailUsageLogEntity::getUseTime, timeoutThreshold)
            .isNull(VideoHotSearchEmailUsageLogEntity::getReleaseTime);

        List<VideoHotSearchEmailUsageLogEntity> timeoutLogs = usageLogDao.selectList(queryWrapper);
        if (timeoutLogs.isEmpty()) {
            log.info("没有超时记录");
            return;
        }
        Set<Long> accountIds = timeoutLogs.stream().map(VideoHotSearchEmailUsageLogEntity::getEmailAccountId).collect(Collectors.toSet());
        List<VideoHotSearchEmailAccountEntity> videoHotSearchEmailAccountEntityList = emailAccountDao.selectBatchIds(accountIds);
        Map<Long, VideoHotSearchEmailAccountEntity> accountEntityMap = videoHotSearchEmailAccountEntityList.stream()
            .collect(Collectors.toMap(VideoHotSearchEmailAccountEntity::getId, Function.identity(), (existing, replacement) -> existing));
        log.info("查询到 {} 条超时记录", timeoutLogs.size());

        int successCount = 0;
        for (VideoHotSearchEmailUsageLogEntity usageLog : timeoutLogs) {
            try {
                // 如果账号状态不是可用或使用中不需要更新redis超时释放逻辑
                VideoHotSearchEmailAccountEntity videoHotSearchEmailAccountEntity = accountEntityMap.get(usageLog.getEmailAccountId());
                if (videoHotSearchEmailAccountEntity.getAccountStatus() == 1 || videoHotSearchEmailAccountEntity.getAccountStatus() == 2) {
                    // 执行redis释放账号逻辑
                    releaseAccount(usageLog.getEmailAccountId(), videoHotSearchEmailAccountEntity.getMaxConcurrentUsers(), usageLog.getClientIp());
                }
                usageLog.setReleaseTime(LocalDateTime.now());
                usageLogDao.updateById(usageLog);

                successCount++;
            } catch (Exception e) {
                log.error("释放账号失败：accountId={}", usageLog.getEmailAccountId(), e);
            }
        }

        log.info("释放超时账号完成：总数={}, 成功={}", timeoutLogs.size(), successCount);
    }


    /**
     * 自动释放超时账号
     *  通过分批查询的方式查找并释放超时使用的账号，更新账号状态和使用记录
     * @apiNote 切记不要加事务, 本场景无需事务 否则就是超大数据集的事务
     */
    public void autoReleaseTimeoutAccounts() {
        // 创建分批查询对象，每次查询最多500条超时使用记录
        BatchQuery<Long, VideoHotSearchEmailUsageLogEntity> batchQuery = new BatchQuery<>((limit, idx) -> usageLogDao.loadTimeoutUse(idx, limit), VideoHotSearchEmailUsageLogEntity::getId)
            .batch(500);

        // 创建查询账号信息的函数，带缓存功能
        Function<List<Long>, Map<Long, VideoHotSearchEmailAccountEntity>> queryAccountFunction = FunctionUtil.cacheFunction(accountIds -> {
            log.info("查询超时账号条数： {}", accountIds.size());
            List<VideoHotSearchEmailAccountEntity> accountEntityList = emailAccountDao.selectBatchIds(accountIds);
            if (EmptyUtil.isEmpty(accountEntityList)) {
                return Map.of();
            }
            return accountEntityList.stream().collect(Collectors.toMap(VideoHotSearchEmailAccountEntity::getId, Function.identity(), (existing, replacement) -> existing));
        });

        // 初始化成功处理计数器
        AtomicInteger successCount = new AtomicInteger();

        // 处理每一批次的超时记录
        batchQuery.consumer((batch,timeoutLogs) -> {
            log.info("开始处理第{}批超时使用记录： {} 条", batch, timeoutLogs.size());
            // 批量释放使用记录，不管账户状态
            List<Long> useIds = timeoutLogs.stream().map(VideoHotSearchEmailUsageLogEntity::getId).toList();
            // 不存在抓异常，唯一失败的可能为数据库不可用，那么就中断本次任务
            usageLogDao.batchRelease(useIds);
            log.info("第{}批释放超时使用完成：总数={}", batch, timeoutLogs.size());
            // 乐观式增加成功计数
            successCount.addAndGet(timeoutLogs.size());

            // 提取账号ID列表
            List<Long> accountIds = timeoutLogs.stream().map(VideoHotSearchEmailUsageLogEntity::getEmailAccountId).distinct().toList();

            // 查询相关账号信息
            Map<Long, VideoHotSearchEmailAccountEntity> accountEntityMap = queryAccountFunction.apply(accountIds);
            if (EmptyUtil.isEmpty(accountEntityMap)) {
                log.info("第{}批没有超时账号信息.... 跳过本次",  batch);
                return;
            }

            // 准备需要更新的账号列表
            List<VideoHotSearchEmailAccountEntity> updateAccounts = new ArrayList<>(accountIds.size());

            // 处理每一条超时记录
            timeoutLogs.forEach(usageLog -> {
                VideoHotSearchEmailAccountEntity videoHotSearchEmailAccountEntity = accountEntityMap.get(usageLog.getEmailAccountId());

                // 只处理状态为可用(1)或使用中(2)的账号
                if (videoHotSearchEmailAccountEntity.getAccountStatus() == 1 || videoHotSearchEmailAccountEntity.getAccountStatus() == 2) {
                    // 执行redis释放账号逻辑
                    try {
                        releaseAccount(usageLog.getEmailAccountId(), videoHotSearchEmailAccountEntity.getMaxConcurrentUsers(), usageLog.getClientIp());

                        // 更新当前用户计数
                        Integer currentUserCount = videoHotSearchEmailAccountEntity.getCurrentUserCount();
                        if (currentUserCount > 0) {
                            videoHotSearchEmailAccountEntity.setCurrentUserCount(currentUserCount - 1);
                        }

                        // 如果没有用户使用该账号，重置账号状态
                        if (videoHotSearchEmailAccountEntity.getCurrentUserCount() <= 0) {
                            videoHotSearchEmailAccountEntity.setAccountStatus(1);
                            videoHotSearchEmailAccountEntity.setLastUseClientIp("");
                            videoHotSearchEmailAccountEntity.setCity("");
                            videoHotSearchEmailAccountEntity.setUpdateDate(LocalDateTime.now());
                        }

                        // 构造需要更新的账号实体
                        VideoHotSearchEmailAccountEntity accountEntity = new VideoHotSearchEmailAccountEntity();
                        accountEntity.setId(videoHotSearchEmailAccountEntity.getId());
                        accountEntity.setAccountStatus(videoHotSearchEmailAccountEntity.getAccountStatus());
                        accountEntity.setUpdateDate(videoHotSearchEmailAccountEntity.getUpdateDate());
                        accountEntity.setCurrentUserCount(videoHotSearchEmailAccountEntity.getCurrentUserCount());
                        accountEntity.setLastUseClientIp(videoHotSearchEmailAccountEntity.getLastUseClientIp());
                        accountEntity.setCity(videoHotSearchEmailAccountEntity.getCity());
                        updateAccounts.add(accountEntity);

                    } catch (Exception e) {
                        log.error("释放账号失败：accountId={}, 占用记录ID={}", usageLog.getEmailAccountId(), usageLog.getId(), e);
                        successCount.decrementAndGet();
                    }
                }
            });

            // 批量更新数据库中的账号信息
            if (EmptyUtil.isNotEmpty(updateAccounts)) {
                try {
                    super.updateBatchById(updateAccounts);
                } catch (Exception e) {
                    // 此次无法更新成功也无所谓，让定时任务从redis刷到数据库兜底
                    log.error("批量更新数据库账号失败：accounts={}", updateAccounts.stream().map(VideoHotSearchEmailAccountEntity::getId).toList(), e);
                }

            }
        });

        // 执行批量查询和处理，限制最大处理梳理为1万条
        long totalCount = batchQuery.run(0L, 10000);
        if (totalCount > 0) {
            log.info("释放超时账号完成：总数={}, 成功={}", totalCount, successCount.get());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importAccounts(MultipartFile file) {
        log.info("开始导入邮箱账号，文件名: {}", file.getOriginalFilename());

        List<HotSearchEmailImportBo> importList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        int totalCount = 0;
        int successCount = 0;

        try {
            EasyExcel.read(file.getInputStream(), HotSearchEmailImportBo.class, new PageReadListener<HotSearchEmailImportBo>(importList::addAll)).sheet().doRead();

            totalCount = importList.size();
            log.info("解析到 {} 条数据", totalCount);

            if (importList.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("totalCount", 0);
                result.put("successCount", 0);
                result.put("failCount", 0);
                result.put("errorMessages", errorMessages);
                return result;
            }

            List<String> emailList = importList.stream()
                .map(HotSearchEmailImportBo::getEmail)
                .filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());

            LambdaQueryWrapper<VideoHotSearchEmailAccountEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(VideoHotSearchEmailAccountEntity::getEmail, emailList);
            List<VideoHotSearchEmailAccountEntity> existAccounts = emailAccountDao.selectList(queryWrapper);

            Set<String> existEmailSet = existAccounts.stream()
                .map(VideoHotSearchEmailAccountEntity::getEmail)
                .collect(Collectors.toSet());

            List<VideoHotSearchEmailAccountEntity> validAccounts = new ArrayList<>();
            Set<String> processedEmails = new HashSet<>();

            for (int i = 0; i < importList.size(); i++) {
                HotSearchEmailImportBo importBo = importList.get(i);
                int rowNum = i + 2;

                try {
                    if (StrUtil.isBlank(importBo.getEmail())) {
                        errorMessages.add("第" + rowNum + "行：邮箱账号不能为空");
                        continue;
                    }

                    if (StrUtil.isBlank(importBo.getEmailPassword())) {
                        errorMessages.add("第" + rowNum + "行：邮箱密码不能为空");
                        continue;
                    }

                    if (existEmailSet.contains(importBo.getEmail())) {
                        errorMessages.add("第" + rowNum + "行：邮箱账号 " + importBo.getEmail() + " 已存在");
                        continue;
                    }

                    if (processedEmails.contains(importBo.getEmail())) {
                        errorMessages.add("第" + rowNum + "行：邮箱账号 " + importBo.getEmail() + " 在文件中重复");
                        continue;
                    }

                    Integer accountType = EmailAccountTypeUtil.parseAccountType(importBo.getAccountType());
                    if (accountType == null) {
                        errorMessages.add("第" + rowNum + "行：账号类型格式错误，支持：Gmail、Outlook、QQ邮箱、163邮箱、其他");
                        continue;
                    }

                    VideoHotSearchEmailAccountEntity account = VideoHotSearchEmailAccountEntity.builder()
                        .id(SnowflakeManager.nextValue())
                        .email(importBo.getEmail())
                        .emailPassword(AESUtil.encrypt(importBo.getEmailPassword(), importBo.getEmail()))
                        .accountType(accountType)
                        .city(StrUtil.isNotBlank(importBo.getCity()) ? importBo.getCity() : "")
                        .accountStatus(1)
                        .currentUserCount(0)
                        .maxConcurrentUsers(importBo.getMaxConcurrentUsers() != null && importBo.getMaxConcurrentUsers() > 0
                            ? importBo.getMaxConcurrentUsers() : DEFAULT_MAX_CONCURRENT_USERS)
                        .useTimeoutMinutes(importBo.getUseTimeoutMinutes() != null && importBo.getUseTimeoutMinutes() > 0
                            ? importBo.getUseTimeoutMinutes() : DEFAULT_USE_TIMEOUT_MINUTES)
                        .remark(importBo.getRemark())
                        .createdDate(LocalDateTime.now())
                        .updateDate(LocalDateTime.now())
                        .isDeleted((byte) 0)
                        .build();

                    validAccounts.add(account);
                    processedEmails.add(importBo.getEmail());
                    successCount++;

                } catch (Exception e) {
                    log.error("第{}行导入失败", rowNum, e);
                    errorMessages.add("第" + rowNum + "行：导入失败 - " + e.getMessage());
                }
            }

            if (!validAccounts.isEmpty()) {
                this.saveBatch(validAccounts);
                log.info("批量插入 {} 条账号到数据库", validAccounts.size());

                addAccountsToRedisBatch(validAccounts);
                log.info("批量加载 {} 条账号到 Redis", validAccounts.size());
            }

            log.info("导入完成：总数={}, 成功={}, 失败={}", totalCount, successCount, totalCount - successCount);

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalCount);
            result.put("successCount", successCount);
            result.put("failCount", totalCount - successCount);
            result.put("errorMessages", errorMessages);

            return result;

        } catch (Exception e) {
            log.error("导入邮箱账号失败", e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    @Override
    public void renewRedisExpiration() {
        log.info("开始 Redis 续期");

        String globalKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;

        stringRedisTemplate.expire(globalKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
        stringRedisTemplate.expire(accountsHashKey, REDIS_TTL_DAYS, TimeUnit.DAYS);

        String cityPoolPattern = BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix + "*";
        org.springframework.data.redis.core.ScanOptions scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
            .match(cityPoolPattern)
            .count(5000)
            .build();
        try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String cityKey = cursor.next();
                stringRedisTemplate.expire(cityKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("Redis 续期城市池失败", e);
        }

        log.info("Redis 续期完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRedisToDatabase() {
        log.info("开始同步 Redis 到数据库");

        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        Map<Object, Object> redisAccountsMap = stringRedisTemplate.opsForHash().entries(accountsHashKey);

        if (redisAccountsMap.isEmpty()) {
            log.info("Redis 中没有账号数据");
            return;
        }

        LambdaQueryWrapper<VideoHotSearchEmailAccountEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(VideoHotSearchEmailAccountEntity::getAccountStatus, 1, 2);

        List<VideoHotSearchEmailAccountEntity> allAccounts = emailAccountDao.selectList(queryWrapper);
        int totalCount = allAccounts.size();
        int batchSize = 1000;
        int syncCount = 0;

        for (int i = 0; i < totalCount; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalCount);
            List<VideoHotSearchEmailAccountEntity> batchAccounts = allAccounts.subList(i, endIndex);

            List<VideoHotSearchEmailAccountEntity> updateList = new ArrayList<>();

            for (VideoHotSearchEmailAccountEntity account : batchAccounts) {
                try {
                    Object redisData = redisAccountsMap.get(account.getId().toString());
                    if (redisData != null) {
                        HotSearchAccountRedisDTO redisDTO = JSONUtil.toBean(redisData.toString(), HotSearchAccountRedisDTO.class);

                        int currentUserCount = redisDTO.getAvailableCount() != null
                            ? account.getMaxConcurrentUsers() - redisDTO.getAvailableCount()
                            : 0;

                        account.setCurrentUserCount(currentUserCount);

                        if (StrUtil.isNotBlank(redisDTO.getCity())) {
                            account.setCity(redisDTO.getCity());
                        }

                        if (redisDTO.getLastUseTime() != null) {
                            account.setLastUseTime(LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(redisDTO.getLastUseTime()),
                                ZoneId.systemDefault()));
                        }
                        if (StrUtil.isNotBlank(redisDTO.getLastIp())) {
                            account.setLastUseClientIp(redisDTO.getLastIp());
                        }

                        account.setUpdateDate(LocalDateTime.now());
                        updateList.add(account);
                    }
                } catch (Exception e) {
                    log.error("同步账号失败：accountId={}", account.getId(), e);
                }
            }

            if (!updateList.isEmpty()) {
                this.updateBatchById(updateList);
                syncCount += updateList.size();
                log.info("批量同步进度：{}/{}, 本批次同步数量={}", endIndex, totalCount, updateList.size());
            }
        }

        log.info("同步 Redis 到数据库完成：总数={}, 同步数量={}", totalCount, syncCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoRepairAccounts() {
        log.info("开始自动修复账号");

        LocalDateTime repairThreshold = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<VideoHotSearchEmailAccountEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoHotSearchEmailAccountEntity::getAccountStatus, 0)
            .lt(VideoHotSearchEmailAccountEntity::getLastUseTime, repairThreshold);

        List<VideoHotSearchEmailAccountEntity> unavailableAccounts = emailAccountDao.selectList(queryWrapper);

        if (unavailableAccounts.isEmpty()) {
            log.info("没有需要修复的账号");
            return;
        }

        int repairedCount = 0;
        List<String> repairedEmails = new ArrayList<>();

        for (VideoHotSearchEmailAccountEntity account : unavailableAccounts) {
            try {
                account.setAccountStatus(1);
                account.setCurrentUserCount(0);
                account.setUpdateDate(LocalDateTime.now());
                repairedCount++;
                repairedEmails.add(account.getEmail());
            } catch (Exception e) {
                log.error("修复账号失败：accountId={}", account.getId(), e);
            }
        }

        // 批量调用数据库和Redis
        updateBatchById(unavailableAccounts);
        addAccountsToRedisBatch(unavailableAccounts);

        log.info("自动修复账号完成：修复数量={}", repairedCount);

        if (repairedCount > 0 && StrUtil.isNotBlank(dingTalkWebhookUrl)) {
            if (Objects.equals(activeProfile, "prod")) {
                String message = DingTalkRobotUtil.buildAccountRepairMessage(repairedCount, repairedEmails, activeProfile);
                DingTalkRobotUtil.sendMarkdownMessage(dingTalkWebhookUrl, "邮箱账号自动修复通知", message);
            }
        }
    }

    @Override
    public void poolCapacityAlert() {
        log.info("开始检查账号池容量");

        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        Map<Object, Object> allAccounts = stringRedisTemplate.opsForHash().entries(accountsHashKey);

        if (allAccounts.isEmpty()) {
            log.warn("Redis 中没有账号数据");
            return;
        }

        int totalCount = allAccounts.size();
        int availableCount = 0;

        for (Map.Entry<Object, Object> entry : allAccounts.entrySet()) {
            try {
                HotSearchAccountRedisDTO redisDTO = JSONUtil.toBean(entry.getValue().toString(), HotSearchAccountRedisDTO.class);
                if (redisDTO.getAvailableCount() != null && redisDTO.getAvailableCount() > 0) {
                    availableCount++;
                }
            } catch (Exception e) {
                log.error("解析账号数据失败：{}", entry.getKey(), e);
            }
        }

        log.info("账号池容量检查完成：总数={}, 可用={}, 阈值={}", totalCount, availableCount, alertThreshold);

        if (availableCount < alertThreshold && StrUtil.isNotBlank(dingTalkWebhookUrl)) {
            if (Objects.equals(activeProfile, "prod")) {
                String message = DingTalkRobotUtil.buildAccountPoolAlertMessage(availableCount, totalCount, alertThreshold, activeProfile);
                DingTalkRobotUtil.sendMarkdownMessage(dingTalkWebhookUrl, "邮箱账号池容量告警", message);
                log.info("已发送账号池容量告警通知");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reloadAllAccountsFromDatabase() {
        log.info("开始全量重新加载数据库数据到 Redis");

        String globalPoolKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        String cityPoolPrefix = BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix;
        String ipMappingPrefix = BusinessCachePrefix.HOT_SEARCH_EMAIL_IP_MAPPING.prefix;

        List<String> cityKeys = new ArrayList<>();
        org.springframework.data.redis.core.ScanOptions cityScanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
            .match(cityPoolPrefix + "*")
            .count(5000)
            .build();
        try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(cityScanOptions)) {
            while (cursor.hasNext()) {
                cityKeys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("扫描城市池 Redis 数据失败", e);
        }
        if (!cityKeys.isEmpty()) {
            stringRedisTemplate.delete(cityKeys);
            log.info("删除城市池 Redis 数据：{} 个", cityKeys.size());
        }

        List<String> ipKeys = new ArrayList<>();
        org.springframework.data.redis.core.ScanOptions ipScanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
            .match(ipMappingPrefix + "*")
            .count(5000)
            .build();
        try (org.springframework.data.redis.core.Cursor<String> cursor = stringRedisTemplate.scan(ipScanOptions)) {
            while (cursor.hasNext()) {
                ipKeys.add(cursor.next());
            }
        } catch (Exception e) {
            log.error("扫描 IP 映射 Redis 数据失败", e);
        }
        if (!ipKeys.isEmpty()) {
            stringRedisTemplate.delete(ipKeys);
            log.info("删除 IP 映射 Redis 数据：{} 个", ipKeys.size());
        }

        stringRedisTemplate.delete(globalPoolKey);
        stringRedisTemplate.delete(accountsHashKey);
        log.info("删除全局池和账号 Hash Redis 数据");

        LambdaQueryWrapper<VideoHotSearchEmailAccountEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(VideoHotSearchEmailAccountEntity::getAccountStatus, 1);

        List<VideoHotSearchEmailAccountEntity> allAccounts = emailAccountDao.selectList(queryWrapper);

        if (allAccounts.isEmpty()) {
            log.warn("数据库中没有可用账号");
            return;
        }

        try {
            addAccountsToRedisBatch(allAccounts);
            log.info("全量重新加载完成：总数={}", allAccounts.size());
        } catch (Exception e) {
            log.error("批量加载账号到 Redis 失败", e);
        }
    }

    /**
     * 处理分配结果
     */
    private VideoHotSearchEmailAccountEntity handleAllocationResult(String result, String clientIp, String clientCity) {
        try {
            log.info("【处理分配结果开始】clientIp={}, result={}", clientIp, result);

            HotSearchAccountRedisDTO redisDTO = JSONUtil.toBean(result, HotSearchAccountRedisDTO.class);
            Long accountId = Long.parseLong(redisDTO.getAccountId());

            log.info("【解析Redis数据】clientIp={}, accountId={}, availableCount={}, fromCache={}, currentPool={}",
                clientIp, accountId, redisDTO.getAvailableCount(), redisDTO.getFromCache(), redisDTO.getCurrentPool());

            // 从数据库查询完整账号信息
            VideoHotSearchEmailAccountEntity account = emailAccountDao.selectById(accountId);
            if (account == null) {
                log.warn("【数据库账号不存在】clientIp={}, accountId={}", clientIp, accountId);
                return null;
            }

            log.info("【数据库账号信息】clientIp={}, accountId={}, status={}, currentUserCount={}, maxConcurrentUsers={}",
                clientIp, accountId, account.getAccountStatus(), account.getCurrentUserCount(), account.getMaxConcurrentUsers());

            // 检查账号状态是否可用
            if (!isAccountAvailable(account)) {
                log.warn("【账号状态不可用】clientIp={}, accountId={}, status={}", clientIp, accountId, account.getAccountStatus());
                // 账号状态异常，执行清理逻辑
                handleUnavailableAccount(account, clientIp, "账号状态异常，状态码: " + account.getAccountStatus());
                return null;
            }

            if (!redisDTO.getFromCache()) {
                // 记录使用日志
                log.info("【记录使用日志】clientIp={}, accountId={}", clientIp, accountId);
                logUsage(accountId, clientIp, clientCity);
                // 更新数据库的数据
                account.setCurrentUserCount(account.getCurrentUserCount() + 1);
                account.setLastUseTime(LocalDateTime.now());
                account.setLastUseClientIp(clientIp);
                account.setUpdateDate(LocalDateTime.now());
                account.setCity(clientCity);
                emailAccountDao.updateById(account);
                log.info("【更新数据库数据】clientIp={}, accountId={}, city={} currentUserCount={}, lastUseTime={}", account.getLastUseClientIp(), clientCity, accountId, account.getCurrentUserCount(), account.getLastUseTime());
            } else {
                log.info("【IP映射缓存命中】clientIp={}, accountId={}, 不记录使用日志", clientIp, accountId);
            }

            log.info("【处理分配结果成功】clientIp={}, accountId={}, city={}, fromCache={}",
                clientIp, accountId, redisDTO.getCity(), redisDTO.getFromCache());
            return account;
        } catch (Exception e) {
            log.error("【处理分配结果异常】clientIp={}, result={}", clientIp, result, e);
            return null;
        }
    }

    /**
     * 记录使用日志
     */
    private void logUsage(Long accountId, String clientIp, String clientCity) {
        try {
            VideoHotSearchEmailUsageLogEntity usageLog = new VideoHotSearchEmailUsageLogEntity();
            usageLog.setId(SnowflakeManager.nextValue());
            usageLog.setEmailAccountId(accountId);
            usageLog.setClientIp(clientIp);
            usageLog.setClientCity(clientCity);
            usageLog.setUseTime(LocalDateTime.now());
            usageLog.setCreatedDate(LocalDateTime.now());
            usageLog.setUpdateDate(LocalDateTime.now());
            usageLog.setIsDeleted((byte) 0);
            usageLogDao.insert(usageLog);
        } catch (Exception e) {
            log.error("记录使用日志失败：accountId={}, clientIp={}", accountId, clientIp, e);
        }
    }

    /**
     * 检查账号是否可用
     */
    private boolean isAccountAvailable(VideoHotSearchEmailAccountEntity account) {
        if (account == null) {
            return false;
        }
        // 状态为可用(1)或使用中(2)
        return account.getAccountStatus() == 1 || account.getAccountStatus() == 2;
    }

    /**
     * 处理不可用账号（统一清理逻辑）
     *
     * @param account       账号实体
     * @param clientIp      客户端 IP
     * @param failureReason 失败原因
     */
    private void handleUnavailableAccount(VideoHotSearchEmailAccountEntity account, String clientIp, String failureReason) {
        Long accountId = account.getId();
        log.warn("处理不可用账号：accountId={}, reason={}", accountId, failureReason);

        // 从 Redis Hash 中删除账号详情
        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        stringRedisTemplate.opsForHash().delete(accountsHashKey, accountId.toString());

        // 从全局池中移除
        String globalKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
        stringRedisTemplate.opsForZSet().remove(globalKey, accountId.toString());

        // 从同城池中移除
        if (StrUtil.isNotBlank(account.getCity())) {
            String cityKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix + account.getCity();
            stringRedisTemplate.opsForZSet().remove(cityKey, accountId.toString());
        }
        // 清除IP映射关系
        if (StrUtil.isNotBlank(account.getLastUseClientIp()) || StrUtil.isNotBlank(clientIp)) {
            String ipMappingKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_IP_MAPPING.prefix + account.getLastUseClientIp();
            stringRedisTemplate.delete(List.of(ipMappingKey, BusinessCachePrefix.HOT_SEARCH_EMAIL_IP_MAPPING.prefix + clientIp));
        }

        log.info("账号 {} 已从 Redis 移除（Hash + 全局池 + 同城[{}]池 + IP[{},{}]映射）", accountId, account.getCity(), account.getLastUseClientIp(), clientIp);
    }

    /**
     * 释放单个账号
     */
    private void releaseAccount(Long accountId, Integer maxConcurrentUsers, String clientIp) {
        int maxConcurrentUsersAvg = maxConcurrentUsers != null ? maxConcurrentUsers : DEFAULT_MAX_CONCURRENT_USERS;
        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        String globalPoolKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
        String cityPoolPrefix = BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix;

        String result = stringRedisTemplate.execute(
            releaseAccountScript,
            Arrays.asList(accountsHashKey, globalPoolKey, cityPoolPrefix),
            accountId.toString(),
            String.valueOf(System.currentTimeMillis()),
            String.valueOf(maxConcurrentUsersAvg),
            String.valueOf(REDIS_TTL_DAYS * 24 * 60 * 60)
        );

        if ("OK".equals(result)) {
            log.debug("账号 {} 释放成功", accountId);
            // 清除IP映射关系
            if (StrUtil.isNotBlank(clientIp)) {
                stringRedisTemplate.delete(BusinessCachePrefix.HOT_SEARCH_EMAIL_IP_MAPPING.prefix + clientIp);
            }
        } else {
            log.warn("账号 {} 释放失败", accountId);
        }
    }

    /**
     * 兜底逻辑：从数据库获取使用最少的账号
     *
     * @param clientIp   客户端 IP
     * @param clientCity 客户端城市
     *
     * @return 邮箱账号实体
     *
     * @description 当 Redis 池中无可用账号时，从数据库获取 current_user_count 最小、last_use_time 最早的账号
     */
    private VideoHotSearchEmailAccountEntity allocateFromDatabaseFallback(String clientIp, String clientCity) {
        try {
            LambdaQueryWrapper<VideoHotSearchEmailAccountEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(VideoHotSearchEmailAccountEntity::getAccountStatus, 1)
                .orderByAsc(VideoHotSearchEmailAccountEntity::getCurrentUserCount)
                .orderByAsc(VideoHotSearchEmailAccountEntity::getLastUseTime)
                .last("LIMIT 1");

            VideoHotSearchEmailAccountEntity account = emailAccountDao.selectOne(queryWrapper);
            if (account == null) {
                log.error("数据库中也没有可用账号");
                return null;
            }
            if (account.getCurrentUserCount() >= account.getMaxConcurrentUsers()) {
                log.error("账号 {} 已达到最大并发用户数，无法分配", account.getId());
                return null;
            }
            account.setCurrentUserCount(account.getCurrentUserCount() + 1);
            account.setLastUseTime(LocalDateTime.now());
            emailAccountDao.updateById(account);

            VideoHotSearchEmailUsageLogEntity usageLog = new VideoHotSearchEmailUsageLogEntity();
            usageLog.setId(SnowflakeManager.nextValue());
            usageLog.setEmailAccountId(account.getId());
            usageLog.setClientIp(clientIp);
            usageLog.setClientCity(clientCity);
            usageLog.setUseTime(LocalDateTime.now());
            usageLog.setReleaseTime(LocalDateTime.now());
            usageLog.setCreatedDate(LocalDateTime.now());
            usageLog.setUpdateDate(LocalDateTime.now());
            usageLog.setIsDeleted((byte) 0);
            usageLogDao.insert(usageLog);

            if (StrUtil.isNotBlank(dingTalkWebhookUrl)) {
                if (Objects.equals(activeProfile, "prod")) {
                    String alertMessage = DingTalkRobotUtil.buildAccountFallbackAlertMessage(
                        clientIp,
                        clientCity,
                        account.getId(),
                        account.getEmail(),
                        account.getCurrentUserCount(),
                        activeProfile
                    );
                    DingTalkRobotUtil.sendMarkdownMessage(dingTalkWebhookUrl, "热搜邮箱账号池告警", alertMessage);
                }
            }

            log.warn("兜底逻辑分配账号成功：accountId={}, email={}, currentUserCount={}",
                account.getId(), account.getEmail(), account.getCurrentUserCount());

            return account;
        } catch (Exception e) {
            log.error("兜底逻辑分配账号失败", e);
            return null;
        }
    }

    /**
     * 批量将账号加载到 Redis
     *
     * @param accounts 账号实体列表
     *
     * @description 批量将账号信息加载到 Redis Hash 和对应的池（同城池或全局池）
     */
    private void addAccountsToRedisBatch(List<VideoHotSearchEmailAccountEntity> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }

        String accountsHashKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_ACCOUNTS.prefix;
        Map<String, String> hashData = new HashMap<>();
        Map<String, Set<ZSetOperations.TypedTuple<String>>> poolData = new HashMap<>();

        for (VideoHotSearchEmailAccountEntity account : accounts) {
            int currentUserCount = account.getCurrentUserCount() != null ? account.getCurrentUserCount() : 0;
            int maxConcurrentUsers = account.getMaxConcurrentUsers() != null ? account.getMaxConcurrentUsers() : DEFAULT_MAX_CONCURRENT_USERS;

            if (currentUserCount >= maxConcurrentUsers) {
                continue;
            }

            int availableCount = maxConcurrentUsers - currentUserCount;
            long lastUseTimeMillis = account.getLastUseTime() != null
                ? account.getLastUseTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

            String poolKey;
            if (StrUtil.isNotBlank(account.getCity())) {
                poolKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_CITY_POOL.prefix + account.getCity();
            } else {
                poolKey = BusinessCachePrefix.HOT_SEARCH_EMAIL_GLOBAL_POOL.prefix;
            }

            HotSearchAccountRedisDTO redisDTO = HotSearchAccountRedisDTO.builder()
                .availableCount(availableCount)
                .city(account.getCity())
                .lastUseTime(lastUseTimeMillis)
                .lastIp(account.getLastUseClientIp())
                .currentPool(poolKey)
                .build();

            hashData.put(account.getId().toString(), JSONUtil.toJsonStr(redisDTO));

            poolData.computeIfAbsent(poolKey, k -> new HashSet<>())
                .add(ZSetOperations.TypedTuple.of(
                    account.getId().toString(),
                    (double) lastUseTimeMillis
                ));
        }

        if (!hashData.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(accountsHashKey, hashData);
            stringRedisTemplate.expire(accountsHashKey, REDIS_TTL_DAYS, TimeUnit.DAYS);
        }

        for (Map.Entry<String, Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>>> entry : poolData.entrySet()) {
            stringRedisTemplate.opsForZSet().add(entry.getKey(), entry.getValue());
            stringRedisTemplate.expire(entry.getKey(), REDIS_TTL_DAYS, TimeUnit.DAYS);
        }
    }

}
