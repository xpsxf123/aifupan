package com.jiuyu.replay.video.project.controller;

import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.video.project.bo.email.ReportFailureEmailBo;
import com.jiuyu.replay.video.project.producer.VideoHotSearchEmailAccountProducer;
import com.jiuyu.replay.video.project.vo.email.HotSearchEmailAccountVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 热搜邮箱账号管理 Controller
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 热搜邮箱账号池管理接口，提供账号获取、上报、批量导入、释放超时、重新加载等功能
 */
@Slf4j
@RestController
@RequestMapping("/replay/video/hotSearch/email")
@RequiredArgsConstructor
@Validated
@Tag(name = "V2.5.3短视频/热搜邮箱账号管理", description = "热搜邮箱账号池管理接口")
public class VideoHotSearchEmailAccountController {

    private final VideoHotSearchEmailAccountProducer emailAccountProducer;

    /**
     * 获取可用的邮箱账号
     *
     * @param request HTTP请求
     * @return 邮箱账号信息
     */
    @GetMapping("/getAccount")
    @Operation(summary = "客户端-获取可用的邮箱账号", description = "智能分配邮箱账号：同城优先 + 使用时间最早")
    public R<HotSearchEmailAccountVo> getAvailableAccount(HttpServletRequest request) {
        // 获取客户端IP TODO 方便客户端测试入参客户端IP
        String clientIp = StrUtil.isNotBlank(request.getParameter("clientIp")) ? request.getParameter("clientIp") : getClientIp(request);
        log.info("客户端IP: {} 请求获取邮箱账号", clientIp);
        // 调用Producer获取可用账号
        return emailAccountProducer.getAvailableAccount(clientIp);
    }

    /**
     * 上报不可用的邮箱账号
     *
     * @param bo      上报信息
     * @param request HTTP请求
     * @return 操作结果
     */
    @PostMapping("/reportFailure")
    @Operation(summary = "客户端-上报不可用的邮箱账号", description = "客户端上报邮箱账号不可用，包含失败原因")
    public R<Boolean> reportFailureAccount(@Validated @RequestBody ReportFailureEmailBo bo,
                                           HttpServletRequest request) {
        // 获取客户端IP TODO 方便客户端测试入参客户端IP
        String clientIp = StrUtil.isNotBlank(bo.getClientIp()) ? bo.getClientIp() : getClientIp(request);
        bo.setClientIp(clientIp);

        log.info("客户端IP: {} 上报账号 {} 不可用", clientIp, bo.getEmail());

        // 调用Producer上报失败账号
        return emailAccountProducer.reportFailureAccount(bo);
    }

    /**
     * 批量导入邮箱账号（Excel上传）
     *
     * @param file Excel文件
     * @return 导入结果
     */
    @PostMapping("/batchImport")
    @Operation(summary = "API-批量导入邮箱账号", description = "通过Excel批量上传邮箱账号，方便池子扩容")
    public R<Map<String, Object>> batchImportAccounts(
            @Parameter(description = "Excel文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("开始导入邮箱账号，文件名: {}", file.getOriginalFilename());

        // 调用Producer批量导入
        return emailAccountProducer.batchImportAccounts(file);
    }

    /**
     * 获取客户端真实IP
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

