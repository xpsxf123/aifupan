package com.jiuyu.replay.video.project.producer;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.bo.email.ReportFailureEmailBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailAccountEntity;
import com.jiuyu.replay.video.project.service.VideoHotSearchEmailAccountService;
import com.jiuyu.replay.video.project.service.IpCityService;
import com.jiuyu.replay.video.project.util.EmailAccountTypeUtil;
import com.jiuyu.replay.video.project.vo.email.HotSearchEmailAccountVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 热搜邮箱账号池管理 Producer
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoHotSearchEmailAccountProducer {

    private final VideoHotSearchEmailAccountService videoHotSearchEmailAccountService;
    private final IpCityService ipCityService;

    /**
     * 获取可用邮箱账号
     *
     * @param clientIp 客户端IP
     * @return 可用邮箱账号
     */
    public R<HotSearchEmailAccountVo> getAvailableAccount(String clientIp) {
        try {
            String cityByIp = ipCityService.getCityByIp(clientIp);
            VideoHotSearchEmailAccountEntity availableAccount = videoHotSearchEmailAccountService.getAvailableAccount(clientIp, cityByIp);
            if (availableAccount == null) {
                return R.error("暂无可用账号，请稍后重试!");
            }
            HotSearchEmailAccountVo build = HotSearchEmailAccountVo.builder()
                    .accountId(availableAccount.getId())
                    .email(availableAccount.getEmail())
                    .emailPassword(availableAccount.getEmailPassword())
                    .accountType(availableAccount.getAccountType())
                    .accountTypeName(EmailAccountTypeUtil.getAccountTypeName(availableAccount.getAccountType()))
                    .accountCity(availableAccount.getCity())
                    .clientCity(cityByIp)
                    .isSameCity(StrUtil.isNotBlank(cityByIp) && StrUtil.isNotBlank(availableAccount.getCity()) && cityByIp.equals(availableAccount.getCity()))
                    .useTimeoutMinutes(availableAccount.getUseTimeoutMinutes())
                    .build();
            return R.ok(build);
        } catch (Exception e) {
            log.error("获取可用邮箱账号失败, clientIp: {}", clientIp, e);
            return R.error("获取邮箱账号失败: " + e.getMessage());
        }
    }

    /**
     * 上报不可用邮箱账号
     *
     * @param bo 上报参数
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> reportFailureAccount(ReportFailureEmailBo bo) {
        try {
            videoHotSearchEmailAccountService.reportUnavailableAccount(bo);
            return R.ok(true);
        } catch (Exception e) {
            log.error("上报失败账号异常, bo: {}", bo, e);
            return R.error("上报失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入邮箱账号
     *
     * @param file Excel文件
     * @return 导入结果
     */
    public R<Map<String, Object>> batchImportAccounts(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return R.error("文件不能为空");
            }

            // 检查文件类型
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return R.error("文件格式错误，仅支持Excel文件（.xlsx或.xls）");
            }

            Map<String, Object> result = videoHotSearchEmailAccountService.importAccounts(file);
            return R.ok(result);
        } catch (Exception e) {
            log.error("批量导入邮箱账号失败", e);
            return R.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * redis数据结构续期
     */
    public void renewRedisExpiration() {
        videoHotSearchEmailAccountService.renewRedisExpiration();
    }

    /**
     * 同步redis数据到数据库持久化
     */
    public void syncRedisToDatabase() {
        videoHotSearchEmailAccountService.syncRedisToDatabase();
    }

    /**
     * 释放超时账号
     */
    public void releaseTimeoutAccounts() {
        videoHotSearchEmailAccountService.autoReleaseTimeoutAccounts();
    }

    /**
     * 自动修复账号
     */
    public void autoRepairAccounts() {
        videoHotSearchEmailAccountService.autoRepairAccounts();
    }

    /**
     * 邮箱池告警
     */
    public void poolCapacityAlert() {
        videoHotSearchEmailAccountService.poolCapacityAlert();
    }

    /**
     * 全量重新加载数据库数据到 Redis 定时任务
     */
    public void reloadAllAccountsFromDatabase() {
        videoHotSearchEmailAccountService.reloadAllAccountsFromDatabase();
    }
}

