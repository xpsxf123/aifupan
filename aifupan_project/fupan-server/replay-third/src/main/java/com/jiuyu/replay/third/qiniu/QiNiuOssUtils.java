package com.jiuyu.replay.third.qiniu;

import com.jiuyu.replay.third.constant.QiNiuProperties;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.util.Auth;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class QiNiuOssUtils {

    @Resource
    private QiNiuProperties qiNiuProperties;

    /**
     * 获取临时上传文件的凭证
     * @return
     */
    public String getTempToken() {
        Auth auth = Auth.create(qiNiuProperties.getAccessKey(), qiNiuProperties.getSecretKey());
        return auth.uploadToken(qiNiuProperties.getBucket());
    }

    /**
     * 获取文件的地址
     * @param fileName 文件名
     * @return
     */
    public String getDownloadUrl(String fileName) {
        try {
            // domain   下载 domain, eg: qiniu.com【必须】
            // useHttps 是否使用 https【必须】
            // key      下载资源在七牛云存储的 key【必须】
            DownloadUrl url = new DownloadUrl(qiNiuProperties.getDownloadDomain(), true, fileName);

            // 带有效期
            long expireInSeconds = 3600 * 24 * 3;// 1小时，可以自定义链接过期时间
            long deadline = System.currentTimeMillis() / 1000 + expireInSeconds;
            Auth auth = Auth.create(qiNiuProperties.getAccessKey(), qiNiuProperties.getSecretKey());
            return url.buildURL(auth, deadline);
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }

}
