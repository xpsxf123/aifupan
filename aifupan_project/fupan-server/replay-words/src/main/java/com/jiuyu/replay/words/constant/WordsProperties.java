package com.jiuyu.replay.words.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "words")
public class WordsProperties {

    /**
     * 用户关键词redis前缀
     */
    private String userCruxRedisPrefix;
    /**
     * 用户敏感词redis前缀
     */
    private String userSensitiveRedisPrefix;
    /**
     * 分析结果缓存前缀
     */
    private String saveAnalysisCachePrefix;
    /**
     * 遗留的部分文字 redis-key
     */
    private String redisRemainSentenceKeyPrefix;
    /**
     * 每段音频的长度，单位：毫秒
     */
    private Long audioLength;
    /**
     * 直播实时在线人数redis前缀
     */
    private String redisOnlineNumInfo;
    /**
     * 视频分析数据存储路径
     */
    private String videoAnalysisStorePath;
    /**
     * 上传websocket采集文件的路径-服务器
     */
    private String serverWebsocketPath;
    /**
     * 文件分析数据存储路径
     */
    private String fileAnalysisStorePath;
    /**
     * 视频AI分析内容存放路径
     */
    private String videoAiContentStorePath;
    /**
     * 文件AI分析内容存放路径
     */
    private String fileAiContentStorePath;
    /**
     * 昨日录制数据redis key前缀
     */
    private String yesterdayRecordRedisKey;
    /**
     * 云空间链接地址
     */
    private String cloudSpaceUrl;
}
