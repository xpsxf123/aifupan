package com.jiuyu.replay.common.alibaba;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * @description：ai桶对应的oss操作
 * @date ：2025/3/14 上午11:40
 */
@Component
@Slf4j
public class AiOssUtils {

    public static final String bucketName = "replay-images";
    public static final String bucketNameAi = "replay-ai-data";

    public static final String diagnosisPrefix = "diagnosisFile";
    public static final String dataDiagnosisPrefix = "dataDiagnosisFile";

    @Resource
    private OssUtils ossUtils;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 上传字符串到oss
     * @param key
     * @param content
     * @return
     */
    public String uploadToString(String key, String content){
        return ossUtils.putObjectByString(bucketName, key, content);
    }

    public String getObjectBackStr(String key ) throws IOException {
        return ossUtils.getObjectBackStr(bucketNameAi, key);
    }

    @Cacheable(cacheNames = "AiAnalysisDataCache", key = "#ossKey")
    public List<String> getAiAnalysisData(String ossKey) {

        Object o = redisTemplate.opsForValue().get("replay:words:aiDataCache:" + ossKey);
        if (ObjectUtil.isNotEmpty(o)){
            return (List<String>) o;
        }

        byte[] object = ossUtils.getObject(bucketName, ossKey);
        if (object == null) return null;
        List<String> result = new ArrayList<>();
        // 使用 ByteArrayInputStream 将 byte[] 转换为 InputStream
        ByteArrayInputStream basis = new ByteArrayInputStream(object);
        // 使用 InputStreamReader 指定字符编码
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(basis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine())  != null) {
                List<String> strings = JSON.parseArray(line, String.class);
                result.addAll(strings);
            }
            // 添加到redis中-2小时
            redisTemplate.opsForValue().set("replay:words:aiDataCache:" + ossKey, result, 2, TimeUnit.HOURS);
        } catch (IOException e) {
            log.info("转化：{}", e.getMessage());
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (basis != null) {
                    basis.close();
                }
            } catch (IOException e) {
                log.info("关闭流失败：{}", e.getMessage());
            }
        }
        return result;
    }


    /**
     * 获取上传txt的预签名url
     * @param suffix
     * @return
     */
    public SignUploadUrlVo getSignUploadUrl(String prefix, String suffix){

        if (suffix.startsWith(".")) RRException.create("文件类型不能以‘.’开头");

        String key = UUID.randomUUID().toString().replaceAll("-", "");

        String timeStr = DateUtil.format(new DateTime(), "/yyyy/MM/dd/");
        String ossKey = prefix + timeStr + key + "." + suffix;

        return getSignUploadUrl(ossKey);
    }

    /**
     * 获取上传图片的签名链接
     * @param ossKey
     * @return
     */
    public SignUploadUrlVo getSignUploadUrl(String ossKey){
        SignUploadUrlVo signUploadUrl = ossUtils.getSignUploadUrl(bucketNameAi, ossKey);
        RRException.isNotEmpty(signUploadUrl, "获取上传txt的预签名url失败");
        return signUploadUrl;
    }

    /**
     * 获取预签名下载url
     * @param ossKey
     * @return
     */
    public String getSignDownloadUrl(String ossKey){
        return ossUtils.getSignDownloadUrl(bucketNameAi, ossKey, true);
    }

    /**
     * 获取预签名下载url
     * @param ossKey
     * @return
     */
    public String getSignDownloadUrl(String ossKey, String downloadFileName){
        return ossUtils.getSignDownloadUrl(bucketNameAi, ossKey, true, downloadFileName);
    }


    /**
     * 批量删除文件
     * @param
     * @param ossKeys        keyList
     */
    public boolean deleteObjects(List<String> ossKeys){

        return  ossUtils.deleteObjects(bucketNameAi,ossKeys);
    }


}
