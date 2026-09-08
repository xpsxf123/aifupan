package com.jiuyu.replay.common.alibaba;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.constant.AliOssProperties;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.FileUploadVo;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author ：lujie
 * @description：关于文件的oss操作
 * @date ：2025/3/14 上午9:58
 */
@Component
public class ImgOssUtils {

    private static final String bucketName = "replay-images";

    @Resource
    private OssUtils ossUtils;
    @Resource
    private GreenUtils greenUtils;

    /**
     * 上传图片
     * @param key ossKey
     * @param data 图片的字节数组
     * @return 返回真正的ossKey, 上传失败返回null
     */
    public FileUploadVo uploadToImg(String key, byte[] data){
        return assemblyData(ossUtils.putObjectByByte(bucketName, key, data));
    }

    /**
     * 上传字符串
     *
     * @param key  ossKey
     * @param data 要上传的字符串
     * @return 返回真正的ossKey, 上传失败返回null
     */
    public FileUploadVo uploadToString(String key, String data) {
        return assemblyData(ossUtils.putObjectByString(bucketName, key, data));
    }

    private FileUploadVo assemblyData(String ossKey){
        // 如果上传失败，返回null
        if (ossKey == null) return  null;

        FileUploadVo result = new FileUploadVo();

        result.setKey(ossKey);

        AliOssProperties.Bucket bucket = ossUtils.getBucket(bucketName);

        result.setUrl(bucket.getAccessUrl() + "/" + ossKey);

        return result;
    }

    /**
     * 获取图片的url
     * @param ossKey ossKey
     * @return  url
     */
    public String getUrl(String ossKey){
        return getAccessUrl() + "/" + ossKey;
    }

    /**
     * 获取图片的访问域名
     *
     * @return 域名
     */
    public String getAccessUrl() {
        AliOssProperties.Bucket bucket = ossUtils.getBucket(bucketName);
        return bucket.getAccessUrl();
    }

    /**
     * 获取上传图片的预签名url
     * @param suffix
     * @return
     */
    public SignUploadUrlVo getDateSignUploadUrl(String prefix, String suffix){

        if (suffix.startsWith(".")) RRException.create("文件类型不能以‘.’开头");

        String key = UUID.randomUUID().toString().replaceAll("-", "");

        String timeStr = DateUtil.format(new DateTime(), "/yyyy/MM/dd/");
        String ossKey = prefix + timeStr + key + "." + suffix;

        SignUploadUrlVo signUploadUrl = ossUtils.getSignUploadUrl(bucketName, ossKey);
        RRException.isNotEmpty(signUploadUrl, "获取上传的预签名url失败");
        return signUploadUrl;
    }

    /**
     * 获取上传的预签名url
     *
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 预签名url
     */
    public SignUploadUrlVo getSignUploadUrl(String prefix, String suffix) {
        String ossKey = uploadKey(prefix, "yyyy/MM/dd", suffix);
        return ossUtils.getSignUploadUrl(bucketName, ossKey);
    }

    /**
     * 获取上传的预签名url
     *
     * @param prefix      前缀
     * @param suffix      后缀
     * @param contentType 文件类型
     * @return 预签名url
     */
    public SignUploadUrlVo getSignUploadUrl(String prefix, String suffix, String contentType) {
        String ossKey = uploadKey(prefix, "yyyy/MM/dd", suffix);
        return ossUtils.getSignUploadUrl(bucketName, ossKey, contentType);
    }

    /**
     * 生成上传的key
     *
     * @param prefix 前缀
     * @param format 时间格式
     * @param suffix 后缀
     * @return 生成的key
     */
    public String uploadKey(String prefix, String format, String suffix) {
        long id = SnowflakeManager.nextValue();
        return StrUtil.format("{}/{}/{}", prefix, DateUtil.format(new DateTime(), format), id + "." + suffix);
    }

    /**
     * 获取数据截图上传图片的预签名url
     * @param suffix
     * @return
     */
    public SignUploadUrlVo getDataScreenshotUploadUrl(String suffix) {
        return getDateSignUploadUrl("dataScreenshot", suffix);
    }

    /**
     * 图片内容安全检测
     * @param ossKey
     * @return
     */
    public R<Boolean> imageModerationWithOptions(String ossKey){

        return greenUtils.imageModerationWithOptions(bucketName, "baselineCheck", ossKey, 200);
    }

    /**
     * 删除图片
     * @param ossKey
     * @return
     */
    public boolean deleteObject(String ossKey){
        return ossUtils.deleteObject(bucketName, ossKey);
    }

}
