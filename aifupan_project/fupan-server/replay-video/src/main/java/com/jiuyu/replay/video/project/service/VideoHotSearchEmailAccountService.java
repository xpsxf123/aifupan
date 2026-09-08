package com.jiuyu.replay.video.project.service;

import com.jiuyu.replay.video.project.bo.email.ReportFailureEmailBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailAccountEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 热搜邮箱账号池管理 Service 接口
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 热搜邮箱账号池管理服务接口，定义账号分配、释放、导入、重新加载、上报不可用等功能
 */
public interface VideoHotSearchEmailAccountService {

    /**
     * 获取可用邮箱账号
     *
     * @param clientIp 客户端 IP
     * @param clientCity 客户端城市
     * @return 邮箱账号实体
     */
    VideoHotSearchEmailAccountEntity getAvailableAccount(String clientIp, String clientCity);

    /**
     * 上报不可用账号
     *
     * @param bo 不可用信息
     */
    void reportUnavailableAccount(ReportFailureEmailBo bo);

    /**
     * 释放超时账号
     */
    void autoReleaseTimeoutAccounts();

    /**
     * 批量导入邮箱账号（Excel）
     *
     * @param file Excel文件
     * @return 导入结果
     */
    Map<String, Object> importAccounts(MultipartFile file);

    /**
     * Redis 续期
     */
    void renewRedisExpiration();

    /**
     * 同步 Redis 到数据库
     */
    void syncRedisToDatabase();

    /**
     * 自动修复账号
     */
    void autoRepairAccounts();

    /**
     * 池容量告警
     */
    void poolCapacityAlert();

    /**
     * 全量重新加载数据库数据到 Redis
     */
    void reloadAllAccountsFromDatabase();
}

