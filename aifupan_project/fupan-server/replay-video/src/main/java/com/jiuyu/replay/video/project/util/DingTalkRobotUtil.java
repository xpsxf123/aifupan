package com.jiuyu.replay.video.project.util;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉机器人工具类
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Slf4j
public class DingTalkRobotUtil {

    /**
     * 发送文本消息到钉钉机器人
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param content    消息内容
     * @return 是否发送成功
     */
    public static boolean sendTextMessage(String webhookUrl, String content) {
        return sendTextMessage(webhookUrl, content, null, false);
    }

    /**
     * 发送文本消息到钉钉机器人（支持@指定人）
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param content    消息内容
     * @param atMobiles  @的手机号列表
     * @param isAtAll    是否@所有人
     * @return 是否发送成功
     */
    public static boolean sendTextMessage(String webhookUrl, String content, String[] atMobiles, boolean isAtAll) {
        try {
            // 构建消息体
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "text");

            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            message.put("text", text);

            // @相关配置
            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && atMobiles.length > 0) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            message.put("at", at);

            // 发送HTTP请求
            return sendPostRequest(webhookUrl, JSON.toJSONString(message));
        } catch (Exception e) {
            log.error("发送钉钉文本消息失败", e);
            return false;
        }
    }

    /**
     * 发送Markdown消息到钉钉机器人
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param title      消息标题
     * @param text       Markdown格式的消息内容
     * @return 是否发送成功
     */
    public static boolean sendMarkdownMessage(String webhookUrl, String title, String text) {
        return sendMarkdownMessage(webhookUrl, title, text, null, false);
    }

    /**
     * 发送Markdown消息到钉钉机器人（支持@指定人）
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param title      消息标题
     * @param text       Markdown格式的消息内容
     * @param atMobiles  @的手机号列表
     * @param isAtAll    是否@所有人
     * @return 是否发送成功
     */
    public static boolean sendMarkdownMessage(String webhookUrl, String title, String text, String[] atMobiles, boolean isAtAll) {
        try {
            // 构建消息体
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", title);
            markdown.put("text", text);
            message.put("markdown", markdown);

            // @相关配置
            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && atMobiles.length > 0) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            message.put("at", at);

            // 发送HTTP请求
            return sendPostRequest(webhookUrl, JSON.toJSONString(message));
        } catch (Exception e) {
            log.error("发送钉钉Markdown消息失败", e);
            return false;
        }
    }

    /**
     * 发送POST请求
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param jsonBody   JSON格式的请求体
     * @return 是否发送成功
     */
    private static boolean sendPostRequest(String webhookUrl, String jsonBody) {
        try {
            // 创建 HttpClient 对象
            HttpClient client = HttpClient.newHttpClient();

            // 创建 HttpRequest 对象
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 发送请求并获取响应
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 打印响应内容
            log.info("钉钉机器人响应: code={}, body={}", response.statusCode(), response.body());

            // 判断是否成功（钉钉返回200且body包含"ok"表示成功）
            return response.statusCode() == 200 && response.body().contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("发送钉钉消息HTTP请求失败", e);
            return false;
        }
    }

    /**
     * 构建邮箱账号池容量告警消息（Markdown格式）
     *
     * @param availableCount 可用账号数量
     * @param totalCount     总账号数量
     * @param threshold      告警阈值
     * @param environment    环境标识
     * @return Markdown格式的消息内容
     */
    public static String buildAccountPoolAlertMessage(int availableCount, int totalCount, int threshold, String environment) {
        return String.format(
                "### 热搜邮箱账号池告警\n\n" +
                        "**告警类型**: 账号池容量不足\n\n" +
                        "**环境**: %s\n\n" +
                        "**告警时间**: %s\n\n" +
                        "**当前状态**:\n" +
                        "- 可用账号数量: %d\n" +
                        "- 总账号数量: %d\n" +
                        "- 告警阈值: %d\n" +
                        "- 使用率: %.2f%%\n\n" +
                        "**建议**: 请及时扩容账号池！",
                environment != null ? environment.toUpperCase() : "UNKNOWN",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                availableCount,
                totalCount,
                threshold,
                (totalCount - availableCount) * 100.0 / totalCount
        );
    }

    /**
     * 构建账号自动修复通知消息（Markdown格式）
     *
     * @param repairedCount 修复的账号数量
     * @param emailList     修复的账号列表
     * @param environment   环境标识
     * @return Markdown格式的消息内容
     */
    public static String buildAccountRepairMessage(int repairedCount, java.util.List<String> emailList, String environment) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 热搜邮箱账号池告警\n\n");
        sb.append("**告警类型**: 账号自动修复通知\n\n");
        sb.append("**环境**: ").append(environment != null ? environment.toUpperCase() : "UNKNOWN").append("\n\n");
        sb.append("**修复时间**: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n\n");
        sb.append("**修复统计**:\n");
        sb.append("- 修复账号数量: ").append(repairedCount).append("\n\n");
        sb.append("**修复账号列表**:\n");
        if (emailList != null && !emailList.isEmpty()) {
            for (int i = 0; i < Math.min(emailList.size(), 10); i++) {
                sb.append((i + 1)).append(". ").append(emailList.get(i)).append("\n");
            }
            if (emailList.size() > 10) {
                sb.append("\n... 还有 ").append(emailList.size() - 10).append(" 个账号");
            }
        }
        return sb.toString();
    }

    /**
     * 构建账号池耗尽兜底告警消息（Markdown格式）
     *
     * @param clientIp         客户端IP
     * @param clientCity       客户端城市
     * @param accountId        兜底账号ID
     * @param email            兜底账号邮箱
     * @param currentUserCount 当前使用数
     * @param environment      环境标识
     * @return Markdown格式的消息内容
     */
    public static String buildAccountFallbackAlertMessage(String clientIp, String clientCity, Long accountId, String email, Integer currentUserCount, String environment) {
        return String.format(
                "### 热搜邮箱账号池告警\n\n" +
                        "**告警类型**: 账号池耗尽，启用兜底逻辑\n\n" +
                        "**环境**: %s\n\n" +
                        "**告警时间**: %s\n\n" +
                        "**客户端信息**:\n" +
                        "- IP: %s\n" +
                        "- 城市: %s\n\n" +
                        "**兜底账号**:\n" +
                        "- 账号ID: %d\n" +
                        "- 邮箱: %s\n" +
                        "- 当前使用数: %d\n\n" +
                        "**建议**: 请尽快检查账号池状态，增加可用账号数量！",
                environment != null ? environment.toUpperCase() : "UNKNOWN",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()),
                clientIp,
                clientCity != null && !clientCity.isEmpty() ? clientCity : "未知",
                accountId,
                email,
                currentUserCount
        );
    }
}

