//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jiuyu.replay.generic.vo.common.code;

/**
 * 全局状态码枚举
 *
 * @author RayChou
 * @date 2025/6/25 11:23
 */
public enum StatusCode implements BaseStatusCode {

    /**
     * 异常返回信息code&msg
     */
    SUCCESS(0, "success"),
    SYSTEM_BUSY(-1, "系统繁忙~请稍后再试~"),
    SYSTEM_TIMEOUT(-2, "系统维护中~请稍后再试~"),
    PARAM_EX(-3, "参数类型解析异常"),
    SQL_EX(-4, "运行SQL出现异常"),
    NULL_POINT_EX(-5, "空指针异常"),
    ILLEGAL_ARGUMENT_EX(-6, "无效参数异常"),
    MEDIA_TYPE_EX(-7, "请求类型异常"),
    LOAD_RESOURCES_ERROR(-8, "加载资源出错"),
    BASE_VALID_PARAM(-9, "统一验证参数异常"),
    OPERATION_EX(-10, "操作异常"),
    SERVICE_MAPPER_ERROR(-11, "Mapper类转换异常"),
    CAPTCHA_ERROR(-12, "验证码校验失败"),
    NON_COMPLETE_INFO_ERROR(-13, "用户资料未完善"),
    IOS_PAY_PRODUCT_FAIL(-14, "产品code不正确"),
    IOS_PAY_PRODUCT_RECEIPT_IS_USE(-15, "交易凭证已失效"),
    OK(200, "OK"),
    BAD_REQUEST(400, "请求方式不正确"),
    UNAUTHORIZED(401, "未经授权"),
    NOT_FOUND(404, "没有找到资源"),
    METHOD_NOT_ALLOWED(405, "不支持当前请求类型"),
    EXPECTATION_FAILED(417, "预期失败"),
    TOO_MANY_REQUESTS(429, "请求超过次数限制"),
    INTERNAL_SERVER_ERROR(500, "内部服务错误"),
    RPC_ERROR(501, "微服务调用失败"),
    BAD_GATEWAY(502, "网关错误"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    FAILED_TO_REQUEST(505, "请求第三方接口失败"),
    REQUIRED_FILE_PARAM_EX(1001, "请求中必须至少包含一个有效文件"),
    DATA_SAVE_ERROR(2000, "新增数据失败"),
    DATA_UPDATE_ERROR(2001, "修改数据失败"),
    DATA_DELETE_ERROR(2001, "删除数据失败"),
    TOO_MUCH_DATA_ERROR(2002, "批量新增数据过多"),
    TOPIC_HAS_BEEN_DELETED_ERROR(3000, "话题已被删除"),
    JWT_BASIC_INVALID(40000, "无效的基本身份验证令牌"),
    JWT_TOKEN_EXPIRED(40001, "会话超时，请重新登录"),
    JWT_SIGNATURE(40002, "不合法的token，请认真比对 token 的签名"),
    JWT_ILLEGAL_ARGUMENT(40003, "缺少token参数"),
    JWT_GEN_TOKEN_FAIL(40004, "生成token失败"),
    JWT_PARSER_TOKEN_FAIL(40005, "解析用户身份错误，请重新登录！"),
    JWT_USER_INVALID(40006, "用户名或密码错误"),
    JWT_USER_ENABLED(40007, "该账号已被冻结，请换一个试试"),
    JWT_OFFLINE(40008, "您已在另一个设备登录！"),
    JWT_USER_LOGIN_OFF_ENABLED(40009, "用户已提交注销申请！"),
    SIGN_PARAMS_TIME_OUT(50001, "签名参数超时"),
    SIGN_PARAMS_ERROR(50002, "签名参数错误"),
    SIGN_PARAMS_NOT_FOUND(50003, "签名参数sign缺失"),
    SIGN_PARAMS_NONCE_NOT_FOUND(50004, "签名参数随机字符串缺失"),
    SIGN_PARAMS_NONCE_ERROR(50005, "签名参数随机字符串不合法"),
    USER_REAL_NAME_ERROR(80001, "未实名认证"),
    USER_REAL_NAME_VERIFY(80002, "认证中"),
    NEED_RETRY(10000, "需要重试业务"),
    LUA_EXECUTE_ERROR(20001, "LUA执行错误"),
    REDIS_OPERATE_ERROR(20002, "Redis操作异常"),
    DATA_NOT_EXIST(30000, "数据不存在"),
    TYPE_NOT_EXIST(30001, "类型不存在"),
    GET_BEAN_NOT_EXIST(30002, "获取bean不存在"),
    DATA_MISMATCH_USER(60001, "当前数据和用户不匹配"),
    USER_PACKAGE_VERSION_UPGRADE(60002, "用户套餐版本需升级"),

    REDIS_LOCK_ERROR(201, "获取分布式锁失败"),
    REDIS_LOCK_INTERRUPT(202, "获取分布式锁被中断"),

    // ========== 2.6.01 话术智能监控业务错误码 70001-70013 ==========
    SCRIPT_MONITOR_TOKEN_NOT_ENOUGH(70001, "您的算力数量不足，请联系产品顾问购买。"),
    SCRIPT_MONITOR_QUOTA_NOT_ENOUGH(70002, "授权数量不足，请联系产品顾问购买"),
    SCRIPT_MONITOR_PACKAGE_NOT_SUPPORT(70003, "当前套餐不支持，请升级"),
    SCRIPT_MONITOR_NOT_OWN_ACCOUNT(70004, "仅自有账号支持AI话术监控功能"),
    SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED(70005, "请先确认标准直播稿"),
    SCRIPT_MONITOR_STANDARD_SCRIPT_INVALID(70006, "标准直播稿无效，请重新确认"),
    SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED(70007, "请先关闭AI话术监控功能后，再修改账号归属类型"),
    SCRIPT_MONITOR_STANDARD_SCRIPT_GENERATE_FAIL(70008, "标准稿生成失败，请重试"),
    SCRIPT_MONITOR_NO_BARRAGE(70010, "本场无弹幕数据，无法生成互动巡检报告"),
    SCRIPT_MONITOR_NO_PERMISSION(70011, "无数据读取权限"),
    SCRIPT_MONITOR_REPORT_NOT_EXIST(70012, "报告不存在"),
    SCRIPT_MONITOR_PARAM_INVALID(70013, "参数校验失败"),
    SCRIPT_MONITOR_FEATURE_NOT_AVAILABLE(70014, "功能未上线"),

    // ========== 2.6.x 场景切片业务错误码 71001-71004 ==========
    SCENE_SLICE_NOT_FOUND(71001, "场景切片记录不存在"),
    SCENE_SLICE_OSS_KEY_EMPTY(71003, "场景切片OSS文件key为空"),
    SCENE_SLICE_AI_FAILED(71004, "场景切片AI分析失败"),

    ;

    private int code;
    private String msg;

    private StatusCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }

    public StatusCode build(String msg, Object... param) {
        this.msg = String.format(msg, param);
        return this;
    }

    public StatusCode param(Object... param) {
        this.msg = String.format(this.msg, param);
        return this;
    }
}
