package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.common.vo.BatchUploadUrlVo;
import com.jiuyu.replay.common.vo.CosFileInfoVo;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.TencentCosTokenVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

/**
 * cos
 */
public interface TencentCosLogic {

    /**
     * 获取cos的私有写，公有读cos临时token
     * @return
     */
    R<TencentCosTokenVo> cosPublicReadTempToken() throws TencentCloudSDKException;

    /**
     * 获取升级包上传的预签名url
     *
     * @return 预签名url和key
     */
    R<SignUploadUrlVo> getUpgradePackageUploadSignUrl();

    /**
     * 获取cos上传的预签名链接
     *
     * @param cosKey cosKey
     * @return 预签名url和key
     */
    R<SignUploadUrlVo> getUploadSignUrl(String cosKey);

    /**
     * 批量获取cos上传的预签名链接
     *
     * @param cosKeys cosKey列表
     * @return 预签名url和key列表
     */
    R<java.util.List<BatchUploadUrlVo>> getBatchUploadSignUrl(java.util.List<String> keys);

    /**
     * COS服务端解压zip包
     *
     * @param cosKey 源zip文件的cosKey
     * @return 解压后的文件信息列表(含url、md5、大小)
     */
    R<java.util.List<CosFileInfoVo>> unzipCosPackage(String cosKey);
}
