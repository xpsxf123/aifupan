package com.jiuyu.replay.third.shlianlu;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.third.constant.LianLuProperties;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * 联麓短信服务处理器，用于通过联麓平台发送短信。
 * <p>
 * 该类封装了与联麓短信 API 的交互逻辑，包括签名生成、请求参数组装和 HTTP 请求发送。
 * 具体接口文档请参考：<a href="https://www.shlianlu.com/console/document/api_4_4">联麓短信发送接口文档</a>
 * </p>
 *
 * <h3>使用说明：</h3>
 * <ul>
 *     <li>{@link #send(String, String, Map)} 用于向单个手机号发送短信。</li>
 *     <li>{@link #send(String, Collection, Map)} 用于向多个手机号批量发送短信。</li>
 * </ul>
 *
 * <h3>关键字段说明：</h3>
 * <ul>
 *     <li>{@code url}：短信发送接口地址。</li>
 *     <li>{@code appId}：应用 ID，用于身份认证。</li>
 *     <li>{@code appSecret}：应用密钥，用于签名生成。</li>
 *     <li>{@code mchId}：商户号，用于标识发送方。</li>
 *     <li>{@code restClient}：HTTP 客户端，用于发送请求。</li>
 *     <li>{@code templateParamSortMap}：模版参数排序策略映射表，用于按模板 ID 获取参数顺序。</li>
 * </ul>
 *
 * @author HeHui
 * @date 2022-03-29 22:16
 */
@Slf4j
public class LianLuSmsHandler {

    @Resource
    private DictDataFeign dictDataFeign;


    /**
     * 短信发送接口地址。
     */
    private final String url = "https://apis.shlianlu.com/sms/trade/template/send";


    private final List<LianLuProperties.LianLuMsg> msgConfig;

    /**
     * HTTP 客户端，用于发送请求。
     */
    private final RestClient restClient;

    /**
     * 模版参数排序策略映射表，用于按模板 ID 获取参数顺序。
     * <p>示例：模板 ID "模版ID1" 对应的参数顺序为 ["模版参数1", "模版参数2"]</p>
     */
    private final Map<String, List<String>> templateParamSortMap = Map.of("模版ID1", List.of("模版参数1", "模版参数2"));


    /**
     * 初始化联麓短信服务处理器。
     *
     * @param msgConfig         配置
     * @param restClientBuilder HTTP 客户端构建器
     */
    public LianLuSmsHandler(List<LianLuProperties.LianLuMsg> msgConfig, ObjectProvider<RestClient.Builder> restClientBuilder) {
        this.msgConfig = msgConfig;
        this.restClient = restClientBuilder.getIfAvailable(RestClient::builder).build();
    }

    private LianLuConfigDto getTemplate(String templateId) {
        List<DictDataListVo> dictDataListVos = dictDataFeign.dictDataListByCode("lianlu_template_params");
        if (dictDataListVos.isEmpty()) {
            log.error("联麓短信配置错误-没有配置lianlu_template_params字典");
            return null;
        }
        Map<String, LianLuConfigDto> map = new HashMap<>();
        dictDataListVos.forEach(item -> {
            try {
                LianLuConfigDto lianLuMsg = JSONObject.parseObject(item.getValue(), LianLuConfigDto.class);
                map.put(item.getLabel(), lianLuMsg);
            } catch (Exception e) {
                log.error("联麓短信模版对应的参数 转化报错", e);
            }
        });
        LianLuConfigDto lianLuConfigDto = map.get(templateId);
        if (ObjectUtil.isNull(lianLuConfigDto)) {
            return null;
        }
        return lianLuConfigDto;
    }


    /**
     * 根据模板 ID 获取排序后的模版参数值集合。
     *
     * <p>若参数为空或只有一个，则直接返回其值；否则按照配置的参数顺序获取对应的值。</p>
     *
     * @param param      参数键值对
     * @param templateId 模板 ID
     * @return 排序后的参数值集合
     */
    private Collection<String> templateParamSort(Map<String, String> param, String templateId) {
        if (param == null || param.isEmpty()) {
            return List.of();
        }
        if (param.size() == 1) {
            return param.values();
        }
        LianLuConfigDto template = getTemplate(templateId);
        if (ObjectUtil.isNull(template)) {
            return List.of();
        }
        List<String> paramKeys = template.getTemplateParam();
        if (paramKeys == null || paramKeys.isEmpty()) {
            return List.of();
        }
        return paramKeys.stream().map(key -> param.getOrDefault(key, "")).toList();
    }


    /**
     * 向指定手机号发送短信。
     *
     * <p>如果手机号格式不正确或模板 ID 为空，则返回错误结果。</p>
     *
     * @param templateId 模板 ID
     * @param mobile     目标手机号
     * @param param      模板参数
     * @return 发送结果对象 {@link Result}
     */
    public Result send(String templateId, String mobile, Map<String, String> param) {
        if (!PhoneUtil.isMobile(mobile)) {
            Result result = new Result();
            result.setStatus("400");
            result.setMessage("手机号码格式错误");
            return result;
        }
        if (StrUtil.isBlank(templateId)) {
            Result result = new Result();
            result.setStatus("400");
            result.setMessage("缺少模版ID");
            return result;
        }
        return send(templateId, List.of(mobile), param);
    }

    public LianLuProperties.LianLuMsg getConfig(String templateId) {
        LianLuConfigDto lianLuConfigDto = getTemplate(templateId);
        int id;
        if (lianLuConfigDto != null && lianLuConfigDto.getTemplateId() != null) {
            id = lianLuConfigDto.getTemplateId();
        } else {
            id = 0;
        }

        return msgConfig.stream()
                .filter(item -> ObjectUtil.equals(id, item.getId()))
                .findFirst()
                .orElse(null);
    }


    /**
     * 批量向多个手机号发送短信。
     *
     * <p>构造请求参数并生成签名，调用联麓短信接口发送短信。</p>
     *
     * @param templateId 模板 ID
     * @param mobileList 目标手机号集合
     * @param param      模板参数
     * @return 发送结果对象 {@link Result}
     */
    public Result send(String templateId, Collection<String> mobileList, Map<String, String> param) {
        if (CollUtil.isEmpty(mobileList)) {
            Result result = new Result();
            result.setStatus("400");
            result.setMessage("手机号码列表为空");
            return result;
        }
        List<String> allowMobileList = mobileList.stream().filter(PhoneUtil::isMobile).distinct().toList();
        if (allowMobileList.size() != mobileList.size()) {
            Result result = new Result();
            result.setStatus("400");
            result.setMessage("手机号码格式错误");
            result.setIllegalMobiles(mobileList.stream().filter(mobile -> !allowMobileList.contains(mobile)).toList());
            return result;
        }
        // 获取配置
        LianLuProperties.LianLuMsg config = getConfig(templateId);
        if (config == null) {
            log.error("没有成功获取联麓的配置");
            Result result = new Result();
            result.setMessage("获取联麓配置失败");
            result.setStatus("400");
            return result;
        }

        // 组装参数
        Map<String, Object> paramMap = new HashMap<>(12, 1F);
        paramMap.put("AppId", config.getAppId());
        paramMap.put("MchId", config.getMchId());
        paramMap.put("TemplateId", templateId);
        paramMap.put("TemplateParamSet", this.templateParamSort(param, templateId));
        long current = System.currentTimeMillis();
        paramMap.put("PhoneNumberSet", mobileList);
        paramMap.put("SignType", "MD5");
        paramMap.put("Type", "3");
        paramMap.put("TimeStamp", current);
        paramMap.put("Version", "1.2.0");

        // 签名
        String sb = "AppId=" + config.getAppId() +
                "&MchId=" + config.getMchId() +
                "&SignType=" + "MD5" +
                "&TemplateId=" + templateId +
                "&TimeStamp=" + current +
                "&Type=" + "3" +
                "&Version=" + "1.2.0" +
                "&key=" + config.getAppSecret();
        byte[] digest = SecureUtil.md5().digest(sb);
        StringBuilder signature = new StringBuilder();
        for (byte b : digest) {
            signature.append(String.format("%02x", b & 0xff));
        }
        paramMap.put("Signature", signature.toString().toUpperCase());
        try {
            ResponseEntity<Result> responseEntity = restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .body(paramMap)
                    .retrieve().toEntity(Result.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                log.info("联麓短信发送成功, 手机号: {}, 模板ID: {}", mobileList, templateId);
                return responseEntity.getBody();
            }
            log.info("[短信服务] send sms provider for 联麓, templateId: {}, mobile:{}. abnormal http status: {}", templateId, mobileList, responseEntity.getStatusCode());
            Result result = new Result();
            result.setStatus(responseEntity.getStatusCode().toString());
            result.setMessage("发送短信异常,请联系管理员排查");
            return result;
        } catch (Exception e) {
            log.info("[短信服务] send sms provider for 联麓, templateId: {}, mobile:{}. error", templateId, mobileList, e);
            Result result = new Result();
            result.setStatus("500");
            result.setMessage("发送短信异常,请联系管理员排查");
            return result;
        }

    }


    /**
     * 短信发送结果对象，包含请求状态、消息、数量等信息。
     *
     * <p>该类实现 {@link Serializable} 接口，支持序列化。</p>
     */
    @Getter
    @Setter
    public static class Result implements Serializable {

        @Serial
        private static final long serialVersionUID = 8058571490278620153L;

        /**
         * 唯一请求 ID，每次请求都会返回。定位问题时需要提供该次请求的 RequestId。
         */
        private String taskId;

        /**
         * 错误信息
         */
        private String message;

        /**
         * UNIX 时间戳
         */
        private String timestamp;

        /**
         * 状态码
         */
        private String status;

        /**
         * 短信发送数量
         */
        private Integer count;


        /**
         * 非法的手机号
         */
        private List<String> illegalMobiles;

        public boolean success() {
            return Objects.equals("00", status);
        }
    }
}
