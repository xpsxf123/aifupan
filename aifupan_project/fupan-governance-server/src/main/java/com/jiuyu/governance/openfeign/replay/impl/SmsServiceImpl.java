package com.jiuyu.governance.openfeign.replay.impl;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.PhoneUtil;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import com.jiuyu.governance.common.replay.ReplayHttpServer;
import com.jiuyu.governance.openfeign.replay.consts.ReplayApiResponseType;
import com.jiuyu.governance.openfeign.replay.SmsService;
import com.jiuyu.governance.openfeign.replay.request.SmsBatchPushRequest;
import com.jiuyu.governance.openfeign.replay.request.SmsPushRequest;
import com.jiuyu.governance.openfeign.replay.response.SmsResponse;
import com.jiuyu.governance.plugins.sms.MockCodeProvide;
import com.jiuyu.governance.plugins.sms.SmsParameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * 短信服务实现
 *
 * @author HeHui
 * @date 2026-03-27 15:32
 */
@Slf4j
public class SmsServiceImpl implements SmsService {

    private final ReplayHttpServer httpServer;

    private final StringRedisTemplate redisTemplate;

    private final Duration smsCodeTimeout; // 10分钟

    private final Duration contextTimeout;

    private final long smsCodeTimeoutMillisecond; // 10分钟

    private final int errorMax;

    private final String prefix;

    private final String verificationTemplateCode;

    private final MockCodeProvide mockCodeProvide;

    public SmsServiceImpl(ReplayHttpServer httpServer, StringRedisTemplate redisTemplate, SmsParameter.Templates templates, Duration codeTimeout, String cachePrefix, int errorMax, MockCodeProvide mockCodeProvide) {
        this.httpServer = httpServer;
        this.redisTemplate = redisTemplate;
        this.smsCodeTimeout = codeTimeout;
        this.smsCodeTimeoutMillisecond = codeTimeout.toMillis();
        this.contextTimeout = codeTimeout.plusMillis(smsCodeTimeoutMillisecond * 5);
        this.verificationTemplateCode = templates.getVerification();
        this.prefix = cachePrefix;
        this.errorMax = errorMax;
        this.mockCodeProvide = mockCodeProvide;
    }

    /**
     * 发送模板短信
     *
     * @param templateCode 模板代码
     * @param param        模版参数
     * @param mobiles      手机
     *
     * @return {@link ApiResponse }<{@link SmsResponse }>
     */
    @Override
    public ApiResponse<SmsResponse> sendTemplate(String templateCode, Map<String, String> param, Collection<String> mobiles) {
        if (EmptyUtil.isEmpty(templateCode)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "模板代码不能为空");
        }
        if (EmptyUtil.isEmpty(mobiles)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码不能为空");
        }
        boolean single = mobiles.size() == 1;
        if (single) {
            String mobile = mobiles.stream().findAny().get();
            if (!PhoneUtil.isMobile(mobile)) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
            }
            SmsPushRequest pushRequest = new SmsPushRequest();
            pushRequest.setTemplateId(templateCode);
            pushRequest.setMobile(mobile);
            if (EmptyUtil.isNotEmpty(param)) {
                List<SmsPushRequest.KV> kvList = param.entrySet().stream().map(entry -> {
                    SmsPushRequest.KV kv = new SmsPushRequest.KV();
                    kv.setKey(entry.getKey());
                    kv.setValue(entry.getValue());
                    return kv;
                }).toList();
                pushRequest.setParams(kvList);
            }
            log.info("[短信] push sms {} for {}", templateCode, DesensitizedUtil.mobilePhone(mobile));
            return httpServer.post("/replay/openapi/governance/sms/push", pushRequest, null)
                .retrieve().body(ReplayApiResponseType.SMS_RESPONSE_TYPE);
        } else {
            List<String> allowMobiles = mobiles.stream().filter(PhoneUtil::isMobile).toList();
            if (EmptyUtil.isEmpty(allowMobiles)) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "手机号码格式错误");
            }
            SmsBatchPushRequest batchPushRequest = new SmsBatchPushRequest();
            batchPushRequest.setTemplateId(templateCode);
            batchPushRequest.setMobileList(allowMobiles);
            if (EmptyUtil.isNotEmpty(param)) {
                List<SmsBatchPushRequest.KV> kvList = param.entrySet().stream().map(entry -> {
                    SmsBatchPushRequest.KV kv = new SmsBatchPushRequest.KV();
                    kv.setKey(entry.getKey());
                    kv.setValue(entry.getValue());
                    return kv;
                }).toList();
                batchPushRequest.setParams(kvList);
            }
            log.info("[短信] batch push sms {} for {}", templateCode, allowMobiles.stream().map(DesensitizedUtil::mobilePhone).collect(Collectors.joining(",")));
            return httpServer.post("/replay/openapi/governance/batch-push", batchPushRequest, null)
                .retrieve().body(ReplayApiResponseType.SMS_RESPONSE_TYPE);
        }
    }

    /**
     * 发送验证码方法
     * 该方法用于生成并发送短信验证码，同时确保验证码发送的频率限制和最大尝试次数
     *
     * @param scene        场景，用于区分验证码使用的场景，如登录、注册等
     * @param mobile       手机号码，用于接收验证码
     * @param codeGenerate 代码生成器，用于生成验证码
     *
     * @return {@link ApiResponse }<{@link Void }> 返回一个ApiResponse对象，表示验证码发送是否成功
     */
    @Override
    public ApiResponse<Void> sendCode(String scene, String mobile, Supplier<String> codeGenerate) {
        // 生成缓存键
        String key = getKey(scene, mobile);
        // 检查验证码发送锁是否存在，以防止频繁发送
        long lockExpire = redisTemplate.getExpire(key + ":lock", TimeUnit.SECONDS);
        if (lockExpire > 0) {
            // 返回错误信息，提示用户锁定时间
            return ApiResponse.failed(SystemErrorCode.TOO_MANY_REQUESTS.getCode(), String.format("您短期内频繁错误提交验证码,请于%s重试！", this.format(lockExpire)));
        }

        // 检查每日验证码发送次数限制
        String dayKey = key + ":num";
        Long dayNum = redisTemplate.opsForValue().increment(dayKey, 1);
        redisTemplate.expire(dayKey, contextTimeout);
        int dayMax = 10; // maxSendTimeout 时间范围内最大短信发送次数
        if (dayNum != null && dayNum > dayMax) {
            String str = this.format(contextTimeout.toSeconds());
            log.warn("[验证码] user get code fail. prefix:{}, mobile:[{}]. The current attempts to obtain SMS verification codes have been exhausted. Please try again in {} ", prefix, mobile, str);
            // 如果超过每日最大发送次数，返回错误信息
            return ApiResponse.failed(BizErrorCode.RATE_LIMIT.getCode(), String.format("当前获取短信验证码次数已用尽,请于%s后重试", str));
        }

        // 检查验证码是否在有效期内，以防止频繁发送
        Long expire = redisTemplate.getExpire(key);
        //有效期超过55秒
        long smsCodeInterval = smsCodeTimeoutMillisecond - 55000;
        if (expire > smsCodeInterval) {
            log.warn("[验证码] user get code fail. prefix:{}, mobile:[{}]. The current attempts to obtain SMS verification codes have been exhausted. Please try again in {} minutes", prefix, mobile, expire);
            // 如果验证码在有效期内，返回错误信息提示用户
            return ApiResponse.failed(BizErrorCode.RATE_LIMIT.getCode(), "请勿频繁点击");
        }
        // 生成验证码
        String code;
        if (mockCodeProvide != null) {
            code = mockCodeProvide.getCode(mobile);
        } else {
            code = codeGenerate.get();
        }
        // 发送验证码
        ApiResponse<SmsResponse> apiResponse = sendTemplate(verificationTemplateCode, Map.of("code", code), List.of(mobile));
        if (apiResponse.ok() && apiResponse.getData() != null && apiResponse.getData().isSuccess()) {
            // 如果发送成功，将验证码和有效期存入Redis
            redisTemplate.opsForValue().set(key, code, smsCodeTimeout);
            // 记录日志
            log.info("[验证码] user get code. prefix:{}, mobile:[{}], code:[{}]", prefix, mobile, code);
            return ApiResponse.success();
        }
        if (apiResponse.failed()) {
            log.warn("[验证码] user get code fail. prefix:{}, mobile:[{}],  msg:[{}]", prefix, mobile, apiResponse.getMsg());
            return ApiResponse.failed(apiResponse.getCode(), apiResponse.getMsg());
        }

        log.warn("[验证码] user get code fail. prefix:{}, mobile:[{}], massage:[{}]", prefix, mobile, apiResponse.getData().getMessage());
        // 如果发送失败，归还每日发送次数
        redisTemplate.opsForValue().increment(dayKey, -1);
        return ApiResponse.failed(SystemErrorCode.UNPROCESSABLE_ENTITY.getCode(), apiResponse.getData().getMessage() == null ? "发送失败" : apiResponse.getData().getMessage());
    }

    /**
     * 验证验证码是否正确的通用方法
     *
     * @param scene      场景值，用于区分不同业务场景的验证码
     * @param mobile     手机号，用于接收验证码
     * @param code       验证码，用户输入的验证码值
     * @param successful 验证成功时的回调，用于处理验证成功后的逻辑
     * @param error      验证失败时的回调，用于处理验证失败后的逻辑
     *
     * @return {@link S } 返回成功或失败处理后的结果
     */
    @Override
    public <S> S checkCode(String scene, String mobile, String code, Supplier<S> successful, Function<Long, S> error) {
        // 生成缓存键，用于在Redis中存储验证码
        String cacheKey = getKey(scene, mobile);
        // 检查缓存中是否存在对应的验证码
        return Optional.ofNullable(redisTemplate.opsForValue().get(cacheKey)).map(checkCode -> {
            // 如果验证码匹配，则删除缓存的验证码，并调用成功回调
            if (Objects.equals(checkCode, code)) {
                redisTemplate.delete(cacheKey);
                return successful.get();
            }
            // 如果验证码不匹配，记录错误次数
            String errorKey = cacheKey + ":error";
            Long errorNum = redisTemplate.opsForValue().increment(errorKey, 1);
            log.error("[验证码] request code check error. cache:[{}], code:[{}],errorNum:[{}] cacheCode:{}", cacheKey, code, errorNum, checkCode);
            redisTemplate.expire(errorKey, smsCodeTimeout);
            // 如果错误次数达到最大值，则清空验证码，并设置锁定
            if (errorNum != null && errorNum > errorMax) {
                redisTemplate.delete(cacheKey);
                redisTemplate.opsForValue().set(cacheKey + ":lock", errorNum.toString(), (errorNum - errorMax) * smsCodeTimeoutMillisecond, TimeUnit.MILLISECONDS);
                log.error("[验证码] request code check error. cache:[{}], code:[{}],errorNum:[{}]", cacheKey, code, errorNum);
            }
            // 调用错误回调，返回错误信息
            return error.apply(errorNum);
        }).orElseGet(() -> error.apply(-1L));
    }


    /**
     * 根据场景和手机号生成唯一的键
     * 此方法用于组合一个在特定场景下与手机号相关联的唯一键
     * 主要用于在缓存或其他数据存储中标识特定用户的相关信息
     *
     * @param scene  使用场景的标识符，例如“login”或“register”
     * @param mobile 用户的手机号，作为生成键的一部分
     *
     * @return 返回生成的唯一键，格式为“prefix:scene:mobile”
     */
    private String getKey(final String scene, final String mobile) {
        return prefix + ":" + scene + ":" + mobile;
    }


    /**
     * 格式化时间
     *
     * @param time 时间对象
     *
     * @return 返回格式化后的时间字符串
     */
    private String format(long time) {
        // 计算剩余锁定时间
        long i = time / 60;
        StringBuilder sb = new StringBuilder();
        if (i > 60) {
            sb.append(i / 60).append("小时");
            i = i % 60;
        }
        if (i > 0) {
            sb.append(i).append("分");
            time = time - i * 60;
        }
        if (time > 0) {
            sb.append(time).append("秒");
        }
        return sb.toString();
    }
}
