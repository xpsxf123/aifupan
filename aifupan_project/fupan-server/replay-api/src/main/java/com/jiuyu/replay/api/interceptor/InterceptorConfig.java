package com.jiuyu.replay.api.interceptor;

import com.jiuyu.replay.common.open.APISecretEnvProvide;
import com.jiuyu.replay.common.open.OauthClientParameter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;


/**
 * InterceptorConfig拦截器配置类
 *
 * @author RayChou
 * @date 2025/7/18 17:48
 */
@Configuration
@EnableConfigurationProperties(OauthClientParameter.class)
public class InterceptorConfig implements WebMvcConfigurer {


    private final OauthClientParameter oauthClientParameter;

    private List<String> loginExcludePathPatterns = Arrays.asList(
            "/replay/user/loadRedisTokensToDatabase", "/doc.html/**",
            "/swagger-ui/index.html", "/swagger-ui/**", "/webjars/**",
            "/swagger-resources", "/favicon.ico", "/v3/api-docs/**",
            "/replay/common/img/**", "/replay/common/serverCurrentTime",
            "/replay/common/signature/**", "/replay/openapi/clientupdate/downloadFile/**",
            "/replay/order/wechatPayCallback", "/replay/videodataviewing/callback",
            "/replay/inviteurlcode/infoByCode", "/replay/test1/test1");

    public InterceptorConfig(OauthClientParameter oauthClientParameter) {
        this.oauthClientParameter = oauthClientParameter;
    }

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor()).addPathPatterns("/**").excludePathPatterns(loginExcludePathPatterns);
        registry.addInterceptor(new APIKeyInterceptor(new APISecretEnvProvide(oauthClientParameter.getApiKeySecret()))).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //第一个方法设置访问路径前缀，第二个方法设置资源路径
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}
