package com.jiuyu.replay.api.logic.third.impl;

import com.jiuyu.replay.api.logic.third.TencentVodLogin;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.common.constant.TencentVodProperties;
import com.jiuyu.replay.common.tencent.TencentVodUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class TencentVodLoginImpl implements TencentVodLogin {

    @Resource
    private TencentVodUtils tencentVodUtils;
    @Resource
    private TencentVodProperties tencentVodProperties;

    @Override
    public R<String> getVodUploadSign() {
        UserCacheVo user = GlobalObject.getLocalUser();
        String sign = tencentVodUtils.getVodUploadSign(user.getId());

//        Signature sign = new Signature();
//        // 设置 App 的云 API 密钥
//        sign.setSecretId(tencentVodProperties.getSecretId());
//        sign.setSecretKey(tencentVodProperties.getSecretKey());
//        sign.setCurrentTime(System.currentTimeMillis() / 1000);
//        sign.setRandom(new Random().nextInt(java.lang.Integer.MAX_VALUE));
//        sign.setSignValidDuration(3600 * 24 * 2); // 签名有效期：2天
//        String signature = sign.getUploadSignature();

        return R.ok("获取签名成功", sign);
    }
}
