package com.jiuyu.governance.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.pojo.bo.ExportData;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 系统公共控制器
 *
 * @author lujie
 * @date 2026/3/31 18:42
 */
@RestController
@RequestMapping("/api/governance/system-common")
@RequiredArgsConstructor
public class SystemCommonController {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String EXPORT_KEY_PREFIX = "export:";

    /**
     * 导出文件（从Redis获取ExportData并写入响应流）
     * <p>
     * Redis存储结构（String）：
     * - key: export:{id}
     * - value: ExportData JSON序列化字符串，byte[] 字段由Jackson自动进行Base64编解码
     *
     * @param id       导出任务ID
     * @param response HTTP响应
     */
    @GetMapping("/export/{id}")
    public void export(@PathVariable String id, HttpServletResponse response) throws IOException {
        String key = EXPORT_KEY_PREFIX + id;

        // 从Redis读取JSON字符串
        if (!stringRedisTemplate.hasKey(key)) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "导出文件不存在或已过期");
        }
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "导出文件不存在或已过期");
        }

        // 反序列化为ExportData
        ExportData exportData = objectMapper.readValue(json, ExportData.class);

        byte[] data = exportData.getData();
        if (data == null || data.length == 0) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "导出文件数据为空");
        }

        String filename = StringUtils.hasText(exportData.getFilename()) ? exportData.getFilename() : "导出数据.xlsx";
        String contentType = StringUtils.hasText(exportData.getContentType())
                ? exportData.getContentType()
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        // 文件名URL编码（支持中文）
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        // 设置响应头
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(data.length);

        // 写入响应流
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

}
