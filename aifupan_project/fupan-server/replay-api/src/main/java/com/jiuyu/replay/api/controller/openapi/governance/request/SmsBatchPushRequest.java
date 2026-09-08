package com.jiuyu.replay.api.controller.openapi.governance.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 短信批量推送请求
 *
 * @author HeHui
 * @date 2026-03-24 18:02
 */
@Getter
@Setter
public class SmsBatchPushRequest {


    /**
     * 短信模版ID
     */
    @NotBlank(message = "缺少短信模版ID")
    private String templateId;

    /**
     * 手机号
     */
    @NotEmpty(message = "缺少手机号")
    private List<String> mobileList;

    /**
     * 短信模版参数
     */
    @Valid
    private List<KV> params;


    /**
     * 短信服务提供商名称
     */
    private String providerName;


    /**
     * 短信模版参数健值对
     */
    @Getter
    @Setter
    public static class KV {

        /**
         * 参数名
         */
        @NotBlank(message = "缺少参数名")
        private String key;

        /**
         * 参数值
         */
        @NotBlank(message = "缺少参数值")
        private String value;
    }

}
