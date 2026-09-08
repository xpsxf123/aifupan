package com.jiuyu.replay.common.tencent;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.vo.BatchUploadUrlVo;
import com.jiuyu.replay.common.vo.CosFileInfoVo;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.common.constant.TencentCosProperties;
import com.jiuyu.replay.common.utils.ApplicationContextUtil;
import com.jiuyu.replay.common.vo.TencentCosTokenVo;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.common.MediaInputObject;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.job.FileProcessJobDetail;
import com.qcloud.cos.model.ciModel.job.FileProcessJobResponse;
import com.qcloud.cos.model.ciModel.job.FileProcessJobType;
import com.qcloud.cos.model.ciModel.job.FileProcessOperation;
import com.qcloud.cos.model.ciModel.job.FileProcessRequest;
import com.qcloud.cos.model.ciModel.job.FileUnCompressConfig;
import com.qcloud.cos.region.Region;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sts.v20180813.models.Credentials;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenRequest;
import com.tencentcloudapi.sts.v20180813.models.GetFederationTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class TencentCosUtils {

    public static Map<String, COSClient> cosClientMap = new ConcurrentHashMap<>();

    /**
     * 获取腾讯云存储cos上传接口临时调用凭证
     */
    public static TencentCosTokenVo privateCosUploadTempToken() {
        RedisTemplate redisTemplate = (RedisTemplate) ApplicationContextUtil.getBean("redisTemplate");
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");

        // 查缓存，如果有，直接返回
        Object obj = redisTemplate.opsForValue().get(tencentCosProperties.getPrivateBucket().getRedisCosTempTokenKeyPrefix());
        if(obj != null) {
            TencentCosTokenVo obj1 = (TencentCosTokenVo) obj;
            obj1.setBucketName(tencentCosProperties.getPrivateBucket().getBucketName());
            obj1.setRegion(tencentCosProperties.getPrivateBucket().getRegion());
            return obj1;
        }

        Credential credential = new Credential(tencentCosProperties.getSecretId(), tencentCosProperties.getSecretKey());

        String region = ObjectUtil.defaultIfEmpty(tencentCosProperties.getPrivateBucket().getRegion(), "ap-shanghai");
        TencentStsClient tencentStsClient = new TencentStsClient(credential, region);
        GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest();
        getFederationTokenRequest.setName("jiuyu"); // 调用方名称，可自定义
        getFederationTokenRequest.setDurationSeconds(600L); // 指定临时证书的有效期，单位：秒，默认1800秒

        // 构建Policy权限策略参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", "2.0"); // 描述策略语法版本。该元素是必填项。目前仅允许值为“2.0”。
        LinkedList<HashMap<String, Object>> statementList = new LinkedList<>(); // 描述一条或多条权限的详细信息

        HashMap<String, Object> statementMap = new HashMap<>(); // 描述策略授权的实体
        statementMap.put("effect", "allow"); // 描述声明产生的结果是“允许”还是“显式拒绝”。包括 allow（允许）和 deny （显式拒绝）两种情况。该元素是必填项。
        statementMap.put("resource", "*"); // 为 * 时代表授予所有资源的操作权限
        statementMap.put("action", List.of("cos:PutObject", "cos:GetObject")); // 描述允许或拒绝的操作
        statementList.add(statementMap);

        jsonObject.put("statement", statementList);
        getFederationTokenRequest.setPolicy(jsonObject.toJSONString());

        // 获取到临时凭证
        try {
            GetFederationTokenResponse federationToken = tencentStsClient.getFederationToken(getFederationTokenRequest);
            Credentials credentials = federationToken.getCredentials();

            TencentCosTokenVo cosTempTokenVo = new TencentCosTokenVo();
            cosTempTokenVo.setToken(credentials.getToken());
            cosTempTokenVo.setTempSecretId(credentials.getTmpSecretId());
            cosTempTokenVo.setTempSecretKey(credentials.getTmpSecretKey());
            cosTempTokenVo.setRegion(region);
            cosTempTokenVo.setBucketName(tencentCosProperties.getPrivateBucket().getBucketName());

            // 存到缓存
            redisTemplate.opsForValue().set(tencentCosProperties.getPrivateBucket().getRedisCosTempTokenKeyPrefix(), cosTempTokenVo, Duration.ofSeconds(500));

            return cosTempTokenVo;
        } catch (Exception e) {
            log.info("获取腾讯云存储cos上传接口临时调用凭证报错");
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 获取腾讯云存储cos上传接口临时调用凭证
     */
    public static TencentCosTokenVo getPublicCosUploadTempToken() throws TencentCloudSDKException {

        RedisTemplate redisTemplate = (RedisTemplate) ApplicationContextUtil.getBean("redisTemplate");
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");

        // 查缓存，如果有，直接返回
        Object obj = redisTemplate.opsForValue().get(tencentCosProperties.getPublicBucket().getRedisCosTempTokenKeyPrefix());
        if(obj != null) {
            return  (TencentCosTokenVo) obj;
        }

        Credential credential = new Credential(tencentCosProperties.getSecretId(), tencentCosProperties.getSecretKey());

        TencentStsClient tencentStsClient = new TencentStsClient(credential, tencentCosProperties.getPublicBucket().getRegion());
        GetFederationTokenRequest getFederationTokenRequest = new GetFederationTokenRequest();
        getFederationTokenRequest.setName("jiuyu"); // 调用方名称，可自定义
        getFederationTokenRequest.setDurationSeconds(1800L); // 指定临时证书的有效期，单位：秒，默认1800秒

        // 构建Policy权限策略参数
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("version", "2.0"); // 描述策略语法版本。该元素是必填项。目前仅允许值为“2.0”。
        LinkedList<HashMap<String, Object>> statementList = new LinkedList<>(); // 描述一条或多条权限的详细信息

        HashMap<String, Object> statementMap = new HashMap<>(); // 描述策略授权的实体
        statementMap.put("effect", "allow"); // 描述声明产生的结果是“允许”还是“显式拒绝”。包括 allow（允许）和 deny （显式拒绝）两种情况。该元素是必填项。
        statementMap.put("resource", "*"); // 为 * 时代表授予所有资源的操作权限
        statementMap.put("action", List.of("cos:PutObject")); // 描述允许或拒绝的操作
        statementList.add(statementMap);

        jsonObject.put("statement", statementList);
        getFederationTokenRequest.setPolicy(jsonObject.toJSONString());

        // 获取到临时凭证
        GetFederationTokenResponse federationToken = tencentStsClient.getFederationToken(getFederationTokenRequest);
        Credentials credentials = federationToken.getCredentials();

        TencentCosTokenVo cosTempTokenVo = new TencentCosTokenVo();
        cosTempTokenVo.setToken(credentials.getToken());
        cosTempTokenVo.setTempSecretId(credentials.getTmpSecretId());
        cosTempTokenVo.setTempSecretKey(credentials.getTmpSecretKey());
        cosTempTokenVo.setBucketName(tencentCosProperties.getPublicBucket().getBucketName());
        cosTempTokenVo.setRegion(tencentCosProperties.getPublicBucket().getRegion());

        // 存到缓存
        redisTemplate.opsForValue().set(tencentCosProperties.getPublicBucket().getRedisCosTempTokenKeyPrefix(), cosTempTokenVo, Duration.ofSeconds(1500));

        return cosTempTokenVo;

    }

    /**
     * 获取所有bucket
     *
     * @return bucketMap
     */
    public static Map<String, TencentCosProperties.Bucket> getBucketMap() {
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");
        Map<String, TencentCosProperties.Bucket> bucketMap = new HashMap<>();
        bucketMap.put(tencentCosProperties.getPrivateBucket().getBucketName(), tencentCosProperties.getPrivateBucket());
        bucketMap.put(tencentCosProperties.getPublicBucket().getBucketName(), tencentCosProperties.getPublicBucket());
        return bucketMap;
    }

    /**
     * 获取cosClient
     *
     * @param bucketName bucketName
     * @return cosClient
     */
    public static COSClient getCosClient(String bucketName) {
        if (cosClientMap.containsKey(bucketName)) {
            return cosClientMap.get(bucketName);
        }
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");

        Map<String, TencentCosProperties.Bucket> bucketMap = getBucketMap();
        TencentCosProperties.Bucket bucket = bucketMap.get(bucketName);
        if (bucket == null) {
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "bucketName不存在");
        }

        // 1. 认证信息
        BasicCOSCredentials cred = new BasicCOSCredentials(tencentCosProperties.getSecretId(), tencentCosProperties.getSecretKey());
        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(bucket.getRegion())));

        cosClientMap.putIfAbsent(bucketName, cosClient);

        return cosClient;
    }

    /**
     * 获取私有bucketName
     *
     * @return bucketName
     */
    public static String getPrivateCosBucketName() {
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");
        return tencentCosProperties.getPrivateBucket().getBucketName();
    }

    /**
     * 获取公开bucketName
     *
     * @return bucketName
     */
    public static String getPublicCosBucketName() {
        TencentCosProperties tencentCosProperties = (TencentCosProperties) ApplicationContextUtil.getBean("tencentCosProperties");
        return tencentCosProperties.getPublicBucket().getBucketName();
    }

    /**
     * 上传本地文件到cos
     * @param cosTempTokenVo
     * @param localPath
     * @param key
     * @return
     */
    public static Boolean putLocalFileObject(TencentCosTokenVo cosTempTokenVo, String localPath, String key){
        BasicSessionCredentials cred = new BasicSessionCredentials(cosTempTokenVo.getTempSecretId(), cosTempTokenVo.getTempSecretKey(), cosTempTokenVo.getToken());
        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(cosTempTokenVo.getRegion())));
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosTempTokenVo.getBucketName(), key, new File(localPath));
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            if (putObjectResult != null && putObjectResult.getRequestId() != null){
                System.out.println(JSONUtil.toJsonStr(putObjectResult));
                return true;
            }
        } catch (CosServiceException cse) {
            log.error("error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("error:{}", cce.getMessage());
        }finally {
            cosClient.shutdown();
        }
        return false;
    }

    /**
     * 上传本地文件到cos
     *
     * @param bucketName 桶名
     * @param localPath  本地文件路径
     * @param key        cos保存的key
     * @return true/false
     */
    public static Boolean putLocalFileObject(String bucketName, String localPath, String key) {
        COSClient cosClient = getCosClient(bucketName);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, new File(localPath));
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            if (putObjectResult != null && putObjectResult.getRequestId() != null) {
                System.out.println(JSONUtil.toJsonStr(putObjectResult));
                return true;
            }
        } catch (CosServiceException cse) {
            log.error("CosServiceException-put-cos-error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("CosClientException-put-cos-error:{}", cce.getMessage());
        }
        return false;
    }

    /**
     * 上传流到cos
     * @param cosTempTokenVo
     * @param inputStream
     * @param key
     * @return
     */
    public static Boolean putStreamObject(TencentCosTokenVo cosTempTokenVo, ByteArrayInputStream inputStream, String key){
        BasicSessionCredentials cred = new BasicSessionCredentials(cosTempTokenVo.getTempSecretId(), cosTempTokenVo.getTempSecretKey(), cosTempTokenVo.getToken());
        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(cosTempTokenVo.getRegion())));
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosTempTokenVo.getBucketName(), key, inputStream, null);
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            if (putObjectResult != null && putObjectResult.getRequestId() != null){
                System.out.println(JSONUtil.toJsonStr(putObjectResult));
                return true;
            }
        } catch (CosServiceException cse) {
            log.error("CosServiceException-put-stream-cos-error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("CosClientException-put-stream-cos-error:{}", cce.getMessage());
        }finally {
            cosClient.shutdown();
        }
        return false;
    }

    /**
     * 上传流到cos
     *
     * @param bucketName  桶名
     * @param inputStream 输入流
     * @param key         cos保存的key
     * @return true/false
     */
    public static Boolean putStreamObject(String bucketName, ByteArrayInputStream inputStream, String key) {
        COSClient cosClient = getCosClient(bucketName);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, null);
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            if (putObjectResult != null && putObjectResult.getRequestId() != null) {
                System.out.println(JSONUtil.toJsonStr(putObjectResult));
                return true;
            }
        } catch (CosServiceException cse) {
            log.error("CosServiceException-put-stream-cos-error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("CosClientException-put-stream-cos-error:{}", cce.getMessage());
        }
        return false;
    }

    /***
     * 下载cos文件
     * @param cosTempTokenVo
     * @param localPath
     * @param key
     * @return
     */
    public static Boolean downloadCosFile(TencentCosTokenVo cosTempTokenVo, String localPath, String key){
        BasicSessionCredentials cred = new BasicSessionCredentials(cosTempTokenVo.getTempSecretId(), cosTempTokenVo.getTempSecretKey(), cosTempTokenVo.getToken());
        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(cosTempTokenVo.getRegion())));
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosTempTokenVo.getBucketName(), key);
        try {
            ObjectMetadata cosObject = cosClient.getObject(getObjectRequest, new File(localPath));
            if (cosObject != null && FileUtil.exist(localPath)){
                return true;
            }
        } catch (CosServiceException cse) {
            cse.printStackTrace();
            log.error("error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("error:{}", cce.getMessage());
        }finally {
            cosClient.shutdown();
        }
        return false;
    }

    /***
     * 下载cos文件
     * @param bucketName 桶名
     * @param localPath 本地保存路径
     * @param key cos保存的key
     * @return true/false
     */
    public static Boolean downloadCosFile(String bucketName, String localPath, String key) {
        COSClient cosClient = getCosClient(bucketName);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        try {
            ObjectMetadata cosObject = cosClient.getObject(getObjectRequest, new File(localPath));
            if (cosObject != null && FileUtil.exist(localPath)) {
                return true;
            }
        } catch (CosServiceException cse) {
            log.error("CosServiceException-get-stream-cos-error:{}", cse.getErrorMessage());
        } catch (CosClientException cce) {
            log.error("CosClientException-get-stream-cos-error:{}", cce.getMessage());
        }
        return false;
    }

    /**
     * 将字符串内容写入zip压缩包并转成ByteArrayInputStream流
     * @param content 内容
     * @return
     */
    public static ByteArrayInputStream convertToZipStream(String content) {
        // 创建字节数组输出流用于存储ZIP数据
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 创建ZIP条目并添加到压缩包
            ZipEntry entry = new ZipEntry("analysis.txt");
            zos.putNextEntry(entry);

            // 将字符串内容写入ZIP条目
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            zos.write(contentBytes, 0, contentBytes.length);

            // 关闭当前ZIP条目
            zos.closeEntry();

        } catch (IOException e) {
            log.info("将字符串写入压缩包并转成ByteArrayInputStream流报错");
            e.printStackTrace();
            return null;
        }

        // 将ZIP数据转换为输入流
        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * 根据cosKey获取下载链接
     * @param cosTempTokenVo 调用凭证
     * @param cosSaveKey cosKey
     * @return
     */
    public static String getDownloadUrl(TencentCosTokenVo cosTempTokenVo, String cosSaveKey) {
        BasicSessionCredentials cred = new BasicSessionCredentials(cosTempTokenVo.getTempSecretId(), cosTempTokenVo.getTempSecretKey(), cosTempTokenVo.getToken());
        COSClient cosClient = new COSClient(cred, new ClientConfig(new Region(cosTempTokenVo.getRegion())));

        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();

        URL url = cosClient.generatePresignedUrl(cosTempTokenVo.getBucketName(), cosSaveKey, null, HttpMethodName.GET, headers, params);

        cosClient.shutdown();

        return url.toString();
    }

    /**
     * 获取上传cos的预签名url
     *
     * @param bucketName 桶名
     * @param cosKey     key
     * @return 预签名url和key
     */
    public static SignUploadUrlVo getUploadSignUrl(String bucketName, String cosKey) {
        COSClient cosClient = getCosClient(bucketName);
        Date expiration = DateUtil.offsetMinute(new Date(), 10);
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        URL url = cosClient.generatePresignedUrl(bucketName, cosKey, expiration, HttpMethodName.PUT, headers, params);
        SignUploadUrlVo res = new SignUploadUrlVo();
        res.setSignedUrl(url.toString());
        res.setOssKey(cosKey);
        return res;
    }

    /**
     * 批量获取上传cos的预签名url
     *
     * @param bucketName 桶名
     * @param prefix     cosKey前缀
     * @param keys       客户端传入的文件key列表
     * @return 预签名url和key列表
     */
    public static List<BatchUploadUrlVo> getBatchUploadSignUrl(String bucketName, String prefix, List<String> keys) {
        COSClient cosClient = getCosClient(bucketName);
        Date expiration = DateUtil.offsetMinute(new Date(), 10);
        List<BatchUploadUrlVo> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            String cosKey = prefix + key;
            URL url = cosClient.generatePresignedUrl(bucketName, cosKey, expiration, HttpMethodName.PUT, new HashMap<>(), new HashMap<>());
            BatchUploadUrlVo vo = new BatchUploadUrlVo();
            vo.setCosKey(cosKey);
            vo.setKey(key);
            vo.setSignedUrl(url.toString());
            result.add(vo);
        }
        return result;
    }

    /**
     * COS服务端解压（需要桶开通CI数据万象文件处理功能）
     *
     * @param bucketName   桶名
     * @param sourceKey    源zip文件的cosKey
     * @param targetPrefix 解压目标目录前缀
     * @return jobId
     */
    public static String unzipOnCosServer(String bucketName, String sourceKey, String targetPrefix) {
        COSClient cosClient = getCosClient(bucketName);
        String region = getBucketMap().get(bucketName).getRegion();

        FileProcessRequest request = new FileProcessRequest();
        request.setBucketName(bucketName);
        request.setTag(FileProcessJobType.FileUncompress);

        MediaInputObject input = new MediaInputObject();
        input.setObject(sourceKey);
        request.setInput(input);

        FileProcessOperation operation = new FileProcessOperation();
        FileUnCompressConfig unCompressConfig = new FileUnCompressConfig();
        unCompressConfig.setPrefix(targetPrefix);
        unCompressConfig.setPrefixReplaced("0");
        operation.setFileUnCompressConfig(unCompressConfig);

        MediaOutputObject output = new MediaOutputObject();
        output.setBucket(bucketName);
        output.setRegion(region);
        operation.setOutput(output);

        request.setOperation(operation);

        try {
            FileProcessJobResponse response = cosClient.createFileProcessJob(request);
            String jobId = response.getJobDetail().getJobId();
            log.info("COS解压任务已提交: jobId={}, source={}, target={}", jobId, sourceKey, targetPrefix);
            return jobId;
        } catch (Exception e) {
            log.error("COS服务端解压失败: sourceKey={}, targetPrefix={}", sourceKey, targetPrefix, e);
            throw new BusinessException("COS服务端解压失败: " + e.getMessage());
        }
    }

    /**
     * 查询文件处理任务详情
     */
    public static FileProcessJobDetail describeFileProcessJob(String bucketName, String jobId) {
        COSClient cosClient = getCosClient(bucketName);
        FileProcessRequest request = new FileProcessRequest();
        request.setBucketName(bucketName);
        request.setJobId(jobId);
        FileProcessJobResponse response = cosClient.describeFileProcessJob(request);
        return response.getJobDetail();
    }

    /**
     * 列出指定前缀下的所有对象信息
     */
    public static List<CosFileInfoVo> listObjectInfo(String bucketName, String prefix) {
        COSClient cosClient = getCosClient(bucketName);
        List<CosFileInfoVo> result = new ArrayList<>();
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(bucketName);
        request.setPrefix(prefix);
        ObjectListing objectListing = cosClient.listObjects(request);
        for (COSObjectSummary summary : objectListing.getObjectSummaries()) {
            CosFileInfoVo vo = new CosFileInfoVo();
            vo.setKey(summary.getKey());
            vo.setSize(summary.getSize());
            String etag = summary.getETag();
            if (etag != null && etag.contains("-")) {
                vo.setMd5(calcObjectMd5(cosClient, bucketName, summary.getKey()));
            } else {
                vo.setMd5(etag);
            }
            result.add(vo);
        }
        return result;
    }

    private static String calcObjectMd5(COSClient cosClient, String bucketName, String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            COSObject cosObject = cosClient.getObject(bucketName, key);
            try (InputStream is = cosObject.getObjectContent()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算文件MD5失败: bucket={}, key={}", bucketName, key, e);
            throw new BusinessException("计算文件MD5失败: " + e.getMessage());
        }
    }

}
