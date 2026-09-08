package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.openfeign.replay.response.SmsResponse;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 短信服务
 *
 * @author HeHui
 * @date 2026-03-27 15:27
 */
public interface SmsService {

    /**
     * 发送模板短信
     *
     * @param templateCode 模板代码
     * @param param        模版参数
     * @param mobiles      手机
     *
     * @return {@link ApiResponse }<{@link SmsResponse }>
     */
    ApiResponse<SmsResponse> sendTemplate(String templateCode, Map<String, String> param, Collection<String> mobiles);

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
    ApiResponse<Void> sendCode(String scene, String mobile, Supplier<String> codeGenerate);


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
    <S> S checkCode(String scene, String mobile, String code, Supplier<S> successful, Function<Long, S> error);
}
