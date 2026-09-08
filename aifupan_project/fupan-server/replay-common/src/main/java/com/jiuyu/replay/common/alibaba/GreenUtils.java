package com.jiuyu.replay.common.alibaba;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.green20220302.models.ImageModerationResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jiuyu.replay.common.constant.AliGreenProperties;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/8 上午11:23
 */
@Slf4j
@Component
public class GreenUtils {

    private static Map<String, Client> clientMap = new HashMap<>();

    @Resource
    private AliGreenProperties aliGreenProperties;

    /**
     * 获取oss客户端
     * @param bucketName 存储桶
     * @param isOuterNet 是否为外网访问
     * @return
     */
    private Client getClient(String bucketName, Boolean isOuterNet) {
        Client client = null;
        // 查看缓存里面有没有
        Client cacheOss = clientMap.get(bucketName+"_"+(isOuterNet ? "1" : "0"));
        if(cacheOss != null) {
            client = cacheOss;
        }else {
            Config config = new Config();
            config.setAccessKeyId(aliGreenProperties.getSecretId());
            config.setAccessKeySecret(aliGreenProperties.getSecretKey());
            // 接入区域和地址请根据实际情况修改
            String endpoint = isOuterNet ? aliGreenProperties.getEndpoint() : aliGreenProperties.getEndpointEms();
            config.setEndpoint(endpoint);
            try{
                client = new Client(config);
            }catch (Exception e){
                RRException.create("创建Client失败");
            }
            clientMap.put(bucketName+"_"+(isOuterNet ? "1" : "0"), client);
        }

        return client;
    }

    /**
     * 图片内容安全检测
     * @param bucketName
     */
    public R<Boolean> imageModerationWithOptions(String bucketName, String serviceName, String ossKey, int retries){
        Client ossClient = getClient(bucketName, aliGreenProperties.getIsOuterNet());
        // 创建RuntimeObject实例并设置运行参数
        RuntimeOptions runtime = new RuntimeOptions();
        // 检测参数构造。
        Map<String, String> serviceParameters = new HashMap<>();
        //待检测数据唯一标识
        serviceParameters.put("dataId", UUID.randomUUID().toString());
        // 待检测文件所在bucket的区域。 示例：cn-shanghai
        serviceParameters.put("ossRegionId", aliGreenProperties.getRegion());
        // 待检测文件所在bucket名称。示例：bucket001
        serviceParameters.put("ossBucketName", bucketName);
        // 待检测文件。 示例：image/001.jpg
        serviceParameters.put("ossObjectName", ossKey);
//        serviceParameters.put("imageUrl", imagesUrl);
        ImageModerationRequest request = new ImageModerationRequest();
        // 图片检测service：内容安全控制台图片增强版规则配置的serviceCode，示例：baselineCheck
        // 支持service请参考：https://help.aliyun.com/document_detail/467826.html?0#p-23b-o19-gff
        request.setService(serviceName);
        request.setServiceParameters(JSON.toJSONString(serviceParameters));

        ImageModerationResponse response = null;

        try {
            response = ossClient.imageModerationWithOptions(request, runtime);

            if (response.getStatusCode() == 200) {
                ImageModerationResponseBody body = response.getBody();
                if (body.getCode() == 200) {
                    ImageModerationResponseBody.ImageModerationResponseBodyData data = body.getData();
                    System.out.println("dataId=" + data.getDataId());
                    List<ImageModerationResponseBody.ImageModerationResponseBodyDataResult> results = data.getResult();
                    ImageModerationResponseBody.ImageModerationResponseBodyDataResult item = null;
                    for (ImageModerationResponseBody.ImageModerationResponseBodyDataResult result : results) {
                        if (result.getLabel().contains("nonLabel")){
                            return R.ok(true);
                        }
                        if (item == null) {
                            item = result;
                        }else if (result.getConfidence() > item.getConfidence()){
                            item = result;
                        }
                    }
                    if (item != null){
                        return R.error(5001, StrUtil.format("图片违规：{}", item.getDescription()), false);
                    }else{
                        return R.ok(true);
                    }
                }else if (body.getCode() == 403){
                    if (retries <= 0){
                        return R.error(5002, StrUtil.format("图片内容检测不通过"), false);
                    }else {
                        return imageModerationWithOptions(bucketName, serviceName, ossKey, retries - 1);
                    }
                }else {
                    log.info("图片内容安全检测不通过, requestId:{}， code={}， msg={}", body.getRequestId(), body.getCode(), body.getMsg());
                    return R.error(5002, StrUtil.format("图片内容检测不通过"), false);
                }
            } else {
                log.info("图片内容安全检测失败 getStatusCode != 200, status:{}", response.getStatusCode());
                return R.error(5002, StrUtil.format("图片内容检测不通过"), false);
            }

        } catch (Exception e) {
            log.info("图片内容安全检测失败,报错Exception, msg:{}", e.getMessage());
            e.printStackTrace();
            return R.error(5002, StrUtil.format("图片内容检测不通过"), false);
        }
    }

}
