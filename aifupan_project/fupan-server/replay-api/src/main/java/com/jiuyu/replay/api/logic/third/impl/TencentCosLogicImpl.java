package com.jiuyu.replay.api.logic.third.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.api.logic.third.TencentCosLogic;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.qcloud.cos.model.ciModel.job.FileProcessJobDetail;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/20 下午3:00
 */
@Component
public class TencentCosLogicImpl  implements TencentCosLogic {

    @Resource
    private TencentCosProperties tencentCosProperties;

    @Override
    public R<TencentCosTokenVo> cosPublicReadTempToken() throws TencentCloudSDKException {
        TencentTempTokenVo publicCosUploadTempToken = TencentCosUtils.getPublicCosUploadTempToken();
        TencentCosTokenVo result = BeanUtil.copyProperties(publicCosUploadTempToken, TencentCosTokenVo.class);
        result.setRegion(tencentCosProperties.getPublicBucket().getRegion());
        result.setBucketName(tencentCosProperties.getPublicBucket().getBucketName());
        return R.ok(result);
    }

    @Override
    public R<SignUploadUrlVo> getUpgradePackageUploadSignUrl() {
        String bucketName = TencentCosUtils.getPublicCosBucketName();
        String cosKey = StrUtil.format("updatePackage/{}.zip", SnowflakeManager.nextValue());
        SignUploadUrlVo result = TencentCosUtils.getUploadSignUrl(bucketName, cosKey);
        return R.ok(result);
    }

    @Override
    public R<SignUploadUrlVo> getUploadSignUrl(String cosKey) {
        if (ObjectUtil.isEmpty(cosKey)) {
            throw new BusinessException("ocsKey不能为空");
        }
        String bucketName = TencentCosUtils.getPublicCosBucketName();
        SignUploadUrlVo result = TencentCosUtils.getUploadSignUrl(bucketName, cosKey);
        return R.ok(result);
    }

    @Override
    public R<List<BatchUploadUrlVo>> getBatchUploadSignUrl(List<String> keys) {
        if (ObjectUtil.isEmpty(keys)) {
            throw new BusinessException("keys不能为空");
        }
        String bucketName = TencentCosUtils.getPublicCosBucketName();
        String prefix = StrUtil.format("updatePackage/{}/", SnowflakeManager.nextValue());
        List<BatchUploadUrlVo> result = TencentCosUtils.getBatchUploadSignUrl(bucketName, prefix, keys);
        return R.ok(result);
    }

    @Override
    public R<List<CosFileInfoVo>> unzipCosPackage(String cosKey) {
        if (ObjectUtil.isEmpty(cosKey)) {
            throw new BusinessException("cosKey不能为空");
        }
        String bucketName = TencentCosUtils.getPublicCosBucketName();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String targetPrefix = StrUtil.format("updatePackageNew/{}/", uuid);

        // 提交解压任务
        String jobId = TencentCosUtils.unzipOnCosServer(bucketName, cosKey, targetPrefix);

        // 轮询等待任务完成
        int maxWait = 60;
        int interval = 2;
        for (int i = 0; i < maxWait / interval; i++) {
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("等待解压任务完成时被中断");
            }
            FileProcessJobDetail detail = TencentCosUtils.describeFileProcessJob(bucketName, jobId);
            if ("Success".equals(detail.getState())) {
                break;
            }
            if ("Failed".equals(detail.getState())) {
                throw new BusinessException("COS解压任务失败: " + detail.getMessage());
            }
        }

        // 列出目标目录下的所有文件
        List<CosFileInfoVo> files = TencentCosUtils.listObjectInfo(bucketName, targetPrefix);
        if (files.isEmpty()) {
            return R.ok(files);
        }

        // COS CI解压会自动多加一层目录，找出并剥掉
        String relativePath = files.get(0).getKey().substring(targetPrefix.length());
        int firstSlash = relativePath.indexOf('/');
        String extraPrefix = "";
        if (firstSlash > 0) {
            String candidate = relativePath.substring(0, firstSlash + 1);
            boolean allMatch = files.stream()
                    .map(f -> f.getKey().substring(targetPrefix.length()))
                    .allMatch(p -> p.startsWith(candidate));
            if (allMatch) {
                extraPrefix = candidate;
            }
        }

        String accessUrl = tencentCosProperties.getPublicBucket().getAccessUrl();
        for (CosFileInfoVo f : files) {
            f.setUrl(accessUrl + "/" + f.getKey());
            f.setZipPath(f.getKey().substring(targetPrefix.length() + extraPrefix.length()));
        }
        System.out.println(JSONUtil.toJsonStr(files));
        return R.ok(files);
    }
}
