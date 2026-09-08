package com.jiuyu.replay.common.tencent;

import com.jiuyu.replay.common.constant.TencentVodProperties;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.DeleteMediaRequest;
import com.tencentcloudapi.vod.v20180717.models.DeleteMediaResponse;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;

@Component
public class TencentVodUtils {

    @Resource
    private TencentVodProperties tencentVodProperties;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static final String HMAC_ALGORITHM = "HmacSHA1"; //签名算法
    private static final String CONTENT_CHARSET = "UTF-8";

    /**
     * 删除文件
     * @param fileId 文件id
     * @return
     */
    public String deleteFile(String fileId) {

        try {
            Credential cred = new Credential(tencentVodProperties.getSecretId(), tencentVodProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
//            HttpProfile httpProfile = new HttpProfile();
//            httpProfile.setEndpoint("vod.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
//            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            VodClient client = new VodClient(cred, "", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(fileId);
            req.setSubAppId(Long.valueOf(tencentVodProperties.getSubAppId()));
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            DeleteMediaResponse resp = client.DeleteMedia(req);
            // 输出json格式的字符串回包
            String respStr = AbstractModel.toJsonString(resp);

            if(respStr.contains("Error")) {
                return "Error";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "success";
    }

    /**
     * 生成腾讯vod上传签名
     * @return
     */
    public String getVodUploadSign(Long userId) {
        // 查缓存，如果有，直接返回
        String sign = redisTemplate.opsForValue().get(tencentVodProperties.getRedisVodTempTokenKeyPrefix() + userId);
        if(!StringUtils.isEmpty(sign)) {
            return  sign;
        }

        String strSign = "";
        String contextStr = "";

        try {
            // 生成原始参数字符串
            long currentTime = System.currentTimeMillis() / 1000; // 当前时间戳，秒
            long valid = 3600 * 24; // 有效期，1天
            int random = new Random().nextInt(Integer.MAX_VALUE); // 随机数

            long endTime = (currentTime + valid);
            contextStr += "secretId=" + java.net.URLEncoder.encode(tencentVodProperties.getSecretId(), "utf8");
            contextStr += "&currentTimeStamp=" + currentTime;
            contextStr += "&expireTime=" + endTime;
            contextStr += "&random=" + random;
            contextStr += "&vodSubAppId=" + tencentVodProperties.getSubAppId();


            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(this.tencentVodProperties.getSecretKey().getBytes(CONTENT_CHARSET), mac.getAlgorithm());
            mac.init(secretKey);


            byte[] hash = mac.doFinal(contextStr.getBytes(CONTENT_CHARSET));
            byte[] sigBuf = byteMerger(hash, contextStr.getBytes("utf8"));
            strSign = base64Encode(sigBuf);
            strSign = strSign.replace(" ", "").replace("\n", "").replace("\r", "");

        } catch (Exception e) {
            System.out.println("生成腾讯vod上传签名失败");
            e.printStackTrace();
        }

        // 存到缓存
        redisTemplate.opsForValue().set(tencentVodProperties.getRedisVodTempTokenKeyPrefix() + userId, strSign, Duration.ofSeconds(3600 * 23));

        return strSign;

    }

    public static byte[] byteMerger(byte[] byte1, byte[] byte2) {
        byte[] byte3 = new byte[byte1.length + byte2.length];
        System.arraycopy(byte1, 0, byte3, 0, byte1.length);
        System.arraycopy(byte2, 0, byte3, byte1.length, byte2.length);
        return byte3;
    }

    private String base64Encode(byte[] buffer) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(buffer);
    }


}
