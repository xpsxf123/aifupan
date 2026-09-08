package com.jiuyu.replay.common.alibaba;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.jiuyu.replay.common.constant.AliOssProperties;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SignUploadUrlVo;
import com.jiuyu.replay.common.vo.TencentTempTokenVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.*;

/**
 * @author ：lujie
 * @description：oss对应的方法
 * @date ：2025/3/12 下午6:05
 */
@Slf4j
@Component
public class OssUtils {

    private static final String ROLE_SESSION_NAME = "aifupan";
    private static final long durationSeconds = 3600;

    private static Map<String, OSS> ossClientMap = new HashMap<>();

    @Resource
    private AliOssProperties aliOssProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    public String completePath(String path) {
        if (StrUtil.isBlank(aliOssProperties.getSavePrefix())) {
            return path;
        }
        return aliOssProperties.getSavePrefix() + "/" + path;
    }

    /**
     * 获取bucket
     * @param bucketName
     * @return
     */
    public AliOssProperties.Bucket getBucket(String bucketName){

        AliOssProperties.Bucket bucket = aliOssProperties.getBuckets().stream().filter(val -> val.getBucketName().equals(bucketName)).findAny().orElse(null);

        RRException.isNotEmpty(bucket, "bucketName错误");

        return bucket;
    }

    /**
     * 获取oss的临时token
     * @param bucketName    桶名
     * @return
     */
    public TencentTempTokenVo getOssTempToken(String bucketName, String prefix){

        String savePrefix = aliOssProperties.getSavePrefix();

        if (ObjectUtil.isNotEmpty(prefix)){
            savePrefix += "/" + prefix;
        }

        AliOssProperties.Bucket bucket = getBucket(bucketName);

        // 查缓存，如果有，直接返回
        String redisKey = bucket.getRedisCosTempTokenKeyPrefix() + "_" + savePrefix;
        Object obj = redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return  (TencentTempTokenVo) obj;
        }

        try {
            // 发起STS请求所在的地域。建议保留默认值，默认值为空字符串（""）。
            String regionId = bucket.getRegion();
            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(regionId, bucketName, aliOssProperties.getIsOuterNet() ? bucket.getEndpoint() : bucket.getEndpointEms());
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, aliOssProperties.getSecretId(), aliOssProperties.getSecretKey());
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            // 适用于Java SDK 3.12.0及以上版本。
            request.setSysMethod(MethodType.POST);
            request.setRoleArn(aliOssProperties.getArn());
            request.setRoleSessionName(ROLE_SESSION_NAME);
            Map<String, Object> policy = new HashMap<>();
            policy.put("Version", "1");
            ArrayList<Map<String, Object>> value = new ArrayList<>();
            HashMap<String, Object> map = new HashMap<>();
            map.put("Action", Collections.singletonList("*"));
            map.put("Resource", Collections.singletonList(StrUtil.format("acs:oss:*:*:{}/{}/*", bucketName, savePrefix)));
            map.put("Effect", "Allow");
            value.add(map);
            policy.put("Statement", value);
            String jsonString = JSON.toJSONString(policy);
            request.setPolicy(jsonString);
            request.setDurationSeconds(durationSeconds);
            final AssumeRoleResponse response = client.getAcsResponse(request);
            TencentTempTokenVo result = new TencentTempTokenVo();
            result.setToken(response.getCredentials().getSecurityToken());
            result.setTempSecretId(response.getCredentials().getAccessKeyId());
            result.setTempSecretKey(response.getCredentials().getAccessKeySecret());
            result.setSavePrefix(savePrefix);
            // 存到缓存
            redisTemplate.opsForValue().set(redisKey, result, Duration.ofSeconds(durationSeconds - (60 * 5)));
            return  result;
        } catch (ClientException e) {
            log.error("调用oss的sts报错code: {}，message：{}，RequestId：{}", e.getErrCode(), e.getErrMsg(), e.getRequestId());
        }
        RRException.create("调用oss的sts错误");
        return null;
    }

    /**
     * 获取预签名下载url
     * @param bucketName 存储桶
     * @param ossKey 存储的key
     * @param isOuterNet 是否为外网访问
     * @return
     */
    public String getSignDownloadUrl(String bucketName, String ossKey, Boolean isOuterNet) {

//        OSS ossClient = getOSSClient(bucketName, isOuterNet);
//        try {
//            // 设置预签名URL过期时间，单位为毫秒。本示例以设置过期时间为1小时为例。
//            Date expiration = new Date(new Date().getTime() + 3600 * 1000L);
//            // 生成以GET方法访问的预签名URL。本示例没有额外请求头，其他人可以直接通过浏览器访问相关内容。
//            URL url = ossClient.generatePresignedUrl(bucketName, ossKey, expiration);
//
//            return url.toString();
//        } catch (OSSException oe) {
//            System.out.println("Caught an OSSException, which means your request made it to OSS, "
//                    + "but was rejected with an error response for some reason.");
//            System.out.println("Error Message:" + oe.getErrorMessage());
//            System.out.println("Error Code:" + oe.getErrorCode());
//            System.out.println("Request ID:" + oe.getRequestId());
//            System.out.println("Host ID:" + oe.getHostId());
//        }

        return getSignDownloadUrl(bucketName, ossKey, isOuterNet, null);
    }

    public String getSignDownloadUrl(String bucketName, String ossKey, Boolean isOuterNet, String downloadFileName) {

        OSS ossClient = getOSSClient(bucketName, isOuterNet);
        try {
            // 设置预签名URL过期时间，单位为毫秒。本示例以设置过期时间为1小时为例。
            Date expiration = new Date(new Date().getTime() + 3600 * 1000L);
            // 创建 GeneratePresignedUrlRequest 对象
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, ossKey);
            // 设置请求方法为 GET
            request.setMethod(HttpMethod.GET);
            // 设置过期时间
            request.setExpiration(expiration);

            if (ObjectUtil.isNotEmpty(downloadFileName)){
                // 设置 response - content - disposition 参数，指定下载文件名
                String disposition = "attachment;filename=" + downloadFileName;
                request.addQueryParameter("response-content-disposition",  disposition);
            }

            // 生成预签名 URL
            return ossClient.generatePresignedUrl(request).toString();
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        }

        return null;
    }

    /**
     * 获取oss客户端
     * @param bucketName 存储桶
     * @param isOuterNet 是否为外网访问
     * @return
     */
    private OSS getOSSClient(String bucketName, Boolean isOuterNet) {
        OSS ossClient = null;
        // 查看缓存里面有没有
        OSS cacheOss = ossClientMap.get(bucketName+"_"+(isOuterNet ? "1" : "0"));
        if(cacheOss != null) {
            ossClient = cacheOss;
        }else {
            AliOssProperties.Bucket bucket = getBucket(bucketName);

            String endpoint = isOuterNet ? bucket.getEndpoint() : bucket.getEndpointEms();

            DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(aliOssProperties.getSecretId(), aliOssProperties.getSecretKey());

            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            ossClient = OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(bucket.getRegion())
                    .build();

            ossClientMap.put(bucketName+"_"+(isOuterNet ? "1" : "0"), ossClient);
        }

        return ossClient;
    }

    /**
     * putObject
     * @param bucketName        桶名
     * @param putObjectRequest  请求
     * @return
     */
    public Boolean putObject(String bucketName, PutObjectRequest putObjectRequest){
        OSS ossClient = getOSSClient(bucketName, aliOssProperties.getIsOuterNet());
        try {
            // 创建PutObject请求。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            if (result != null && result.getRequestId() != null){
                return true;
            }
        }catch (OSSException oe) {
            log.info("上传文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        }catch (com.aliyun.oss.ClientException oe){
            log.info("上传文件失败,报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }catch (Exception oe){
            log.info("上传文件失败，报错Exception, message: {}", oe.getMessage());
        }
        return false;
    }

    /**
     * 设置ossKey
     * @param ossKey
     * @return
     */
    public String setOssKey(String ossKey){
        RRException.isNotEmpty(ossKey, "ossKey不能为空");
        if (ossKey.startsWith("/")){
            ossKey = ossKey.substring(1);
        }
        return aliOssProperties.getSavePrefix() + "/" +ossKey;
    }

    /**
     * GetObject
     * @param bucketName    桶名
     * @param ossKey        key
     * @return              返回文件的字节数组
     */
    public byte[] getObject(String bucketName, String ossKey){
        OSS ossClient = getOSSClient(bucketName, aliOssProperties.getIsOuterNet());
        try {
            // 创建 GetObjectRequest 对象
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, ossKey);
            // 获取文件流
            OSSObject ossObject = ossClient.getObject(getObjectRequest);
            try(InputStream inputStream = ossObject.getObjectContent()) {
                return inputStream.readAllBytes();
            }catch (IOException ex) {
                log.error("上传文件失败,IOException, message: {}", ex.getMessage());
            }
        }catch (OSSException oe) {
            log.info("上传文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        }catch (com.aliyun.oss.ClientException oe){
            log.info("上传文件失败,报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }catch (Exception oe){
            log.info("上传文件失败，报错Exception, message: {}", oe.getMessage());
        }

        return null;
    }

    /**
     * GetObject
     * @param bucketName    桶名
     * @param ossKey        key
     * @return              返回String
     */
    public String getObjectBackStr(String bucketName, String ossKey) throws IOException {
        OSS ossClient = getOSSClient(bucketName, aliOssProperties.getIsOuterNet());
        BufferedReader reader =null;
        try {
            // 创建 GetObjectRequest 对象
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, ossKey);
            // 获取文件流
            OSSObject ossObject = ossClient.getObject(getObjectRequest);
            try(InputStream inputStream = ossObject.getObjectContent(); ) {
                if (Objects.isNull(inputStream)){
                    log.error("oss文件获取失败,osskey:{},bucketName:{}",ossKey,bucketName);
                    return null;
                }
                reader= new BufferedReader(new InputStreamReader(inputStream));
                // 读取内容
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }catch (IOException ex) {
                log.error("下载文件失败,IOException, message: {}", ex.getMessage());
            }
        }catch (OSSException oe) {
            log.error("下载文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        }catch (com.aliyun.oss.ClientException oe){
            log.error("下载文件失败,报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }catch (Exception oe){
            log.error("下载文件失败，报错Exception, message: {}", oe.getMessage());
        }finally {
            // 关闭资源
            if (reader!=null){
                reader.close();
            }
        }


        return null;
    }


    /**
     * putObject-Stream
     * @param bucketName    桶名
     * @param ossKey        key
     * @param inputStream   流
     * @return              返回真正的ossKey, 上传失败返回null
     */
    public String putObjectByStream(String bucketName, String ossKey, InputStream inputStream){
        String key = setOssKey(ossKey);
        if (putObject(bucketName, new PutObjectRequest(bucketName, key, inputStream))) {
            return  key;
        }
        return null;
    }

    /**
     * putObject-Byte数组
     * @param bucketName    桶名
     * @param ossKey        key
     * @param content       数据
     * @return              返回真正的ossKey, 上传失败返回null
     */
    public String putObjectByByte(String bucketName, String ossKey, byte[] content){
        String key = setOssKey(ossKey);
        if (putObject(bucketName, new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content)))) {
            return key;
        }
        return  null;
    }

    /**
     * putObject-文件
     * @param bucketName    桶名
     * @param ossKey        key
     * @param filePath      文件地址
     * @return              返回真正的ossKey, 上传失败返回null
     */
    public String putObjectByFile(String bucketName, String ossKey, String filePath){

        // 判断文件是否存在
        if (!FileUtil.exist(filePath)) {
            log.error("要上传到oss的文件不存在");
            return null;
        }
        String key = setOssKey(ossKey);
        if (putObject(bucketName, new PutObjectRequest(bucketName, key, new File(filePath)))) {
            return key;
        }else {
            return null;
        }
    }

    /**
     * putObject-字符串
     * @param bucketName    桶名
     * @param ossKey        key
     * @param content       要上传的字符串
     * @return              返回真正的ossKey, 上传失败返回null
     */
    public String putObjectByString(String bucketName, String ossKey, String content){
        String key = setOssKey(ossKey);

        if (putObject(bucketName, new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes())))) {
            return key;
        }
        return null;
    }

    /**
     * 获取上传oss的预签名url
     *
     * @param bucketName 桶名
     * @param ossKey     key
     * @return 有两参数，第一个是预签名url，第二个是ossKey
     */
    public SignUploadUrlVo getSignUploadUrl(String bucketName, String ossKey) {
        return getSignUploadUrl(bucketName, ossKey, null);
    }


    /**
     * 获取上传oss的预签名url
     * @param bucketName        桶名
     * @param ossKey            key
     * @return                  有两参数，第一个是预签名url，第二个是ossKey
     */
    public SignUploadUrlVo getSignUploadUrl(String bucketName, String ossKey, String contentType) {
        OSS ossClient = getOSSClient(bucketName, true);
        ossKey = setOssKey(ossKey);
        try {
            // 指定生成的预签名URL过期时间-10分钟
            Date expiration = DateUtil.offsetMinute(new Date(), 10);

            // 生成预签名URL。
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, ossKey, HttpMethod.PUT);
            // 设置过期时间。
            request.setExpiration(expiration);
            if (ObjectUtil.isNotEmpty(contentType)) {
                request.setContentType(contentType);
            }
            // 通过HTTP PUT请求生成预签名URL。
            URL signedUrl = ossClient.generatePresignedUrl(request);
            SignUploadUrlVo res = new SignUploadUrlVo();
            res.setSignedUrl(signedUrl.toString());
            res.setOssKey(ossKey);
            return res;
        }catch (OSSException oe) {
            log.info("上传文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        }catch (com.aliyun.oss.ClientException oe){
            log.info("上传文件失败,报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }catch (Exception oe){
            log.info("上传文件失败，报错Exception, message: {}", oe.getMessage());
        }
        return null;
    }

    /**
     * 删除文件
     * @param bucketName    桶名称
     * @param ossKey        key
     */
    public boolean deleteObject(String bucketName, String ossKey){
        OSS ossClient = getOSSClient(bucketName, aliOssProperties.getIsOuterNet());
        try {
            ossClient.deleteObject(bucketName, ossKey);
            return true;
        } catch (OSSException oe) {
            log.info("删除文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        } catch (com.aliyun.oss.ClientException oe) {
            log.info("删除文件失败，报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }
        return false;
    }


    /**
     * 批量删除文件
     * @param bucketName    桶名称
     * @param ossKeys        keyList
     */
    public boolean deleteObjects(String bucketName, List<String> ossKeys){
        OSS ossClient = getOSSClient(bucketName, aliOssProperties.getIsOuterNet());
        try {
            // 构建批量删除请求
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName)
                    .withKeys(ossKeys);  // 传入要删除的 Key 列表
            // 执行批量删除
            DeleteObjectsResult result = ossClient.deleteObjects(request);
            // 返回删除成功的 Key（可选）
            List<String> deletedObjects = result.getDeletedObjects();
            log.info("删除文件成功的ossKey：{}",deletedObjects);
            return true;
        } catch (OSSException oe) {
            log.info("删除文件失败，报错OSSException, Code:{}, message: {}, Request ID:{}, Host ID: {}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId(), oe.getHostId());
        } catch (com.aliyun.oss.ClientException oe) {
            log.info("删除文件失败，报错ClientException, Code:{}, message: {}, Request ID:{}", oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
        }
        return false;
    }

}
