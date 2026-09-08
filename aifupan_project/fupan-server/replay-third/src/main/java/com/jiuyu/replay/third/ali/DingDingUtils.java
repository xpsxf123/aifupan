package com.jiuyu.replay.third.ali;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉机器人工具类
 */
@Slf4j
@Component
public class DingDingUtils {

    /**
     * 复用HttpClient实例，避免频繁创建
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();


    private final String dingTalkWebhookUrl;

    private final boolean prod;

    public DingDingUtils(@Value("${third.ali.ding-ding.proxy-ip-webhook-url:}") String dingTalkWebhookUrl, Environment environment) {
        this.dingTalkWebhookUrl = dingTalkWebhookUrl;
        this.prod = environment.matchesProfiles("prod");
    }

    /**
     * 发送文本消息到钉钉机器人
     *
     * @param webhookUrl 钉钉机器人Webhook地址
     * @param content    消息内容
     * @return 是否发送成功
     */
    public boolean sendTextMessage(String webhookUrl, String content) {
        return sendTextMessage(webhookUrl, content, null, false);
    }

    /**
     * 发送文本消息到钉钉机器人-默认地址
     *
     * @param content    消息内容
     * @return 是否发送成功
     */
    public boolean sendTextMessage(String content) {
        return sendTextMessage(dingTalkWebhookUrl, content, null, false);
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
    public boolean sendTextMessage(String webhookUrl, String content, String[] atMobiles, boolean isAtAll) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "text");

            Map<String, Object> text = new HashMap<>();
            text.put("content", content);
            message.put("text", text);

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && atMobiles.length > 0) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            message.put("at", at);

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
    public boolean sendMarkdownMessage(String webhookUrl, String title, String text) {
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
    public boolean sendMarkdownMessage(String webhookUrl, String title, String text, String[] atMobiles, boolean isAtAll) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("msgtype", "markdown");

            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", title);
            markdown.put("text", text);
            message.put("markdown", markdown);

            Map<String, Object> at = new HashMap<>();
            if (atMobiles != null && atMobiles.length > 0) {
                at.put("atMobiles", atMobiles);
            }
            at.put("isAtAll", isAtAll);
            message.put("at", at);

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
    private boolean sendPostRequest(String webhookUrl, String jsonBody) {
        // 非生产环境跳过消息通知
        if (!prod) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("钉钉机器人响应: code={}, body={}", response.statusCode(), response.body());

            return response.statusCode() == 200 && response.body().contains("\"errcode\":0");
        } catch (Exception e) {
            log.error("发送钉钉消息HTTP请求失败", e);
            return false;
        }
    }
}
