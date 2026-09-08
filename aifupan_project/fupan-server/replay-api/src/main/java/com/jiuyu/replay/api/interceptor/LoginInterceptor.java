package com.jiuyu.replay.api.interceptor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.api.utils.ResponseMsgUtils;
import com.jiuyu.replay.power.constant.Constant;
import com.jiuyu.replay.power.producer.UserTokenProducer;
import com.jiuyu.replay.power.vo.UserCacheVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * 登录拦截器优化版
 *
 * @author RayChou
 * @date 2025/7/18 17:48
 */
@Slf4j
@Order(1)
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    UserTokenProducer userTokenProducer;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 不需要登录的接口
    private final List<String> excludePathList = Arrays.asList(
            "/replay/time/currentTime", "/replay/user/login", "/replay/user/loginOnline",
            "/replay/dictdata/list", "/replay/user/register", "/replay/user/getPhoneCode",
            "/replay/user/loginByTempToken", "/replay/common/video", "/replay/openapi/clientupdate/getVersionByVersion",
            "/replay/openapi/clientupdate/getPackageVersion", "/replay/openapi/clientupdate/setClientVersion",
            "/replay/vod/getVodUploadSign", "/replay/package/list", "/replay/notify/wechatPayCallback",
            "/replay/notify/alipayPayCallback", "/replay/openapi/v1930/getWebsocketWay",
            "/replay/openapi/v2100/queryDanMuData", "/replay/openapi/v2100/queryOtherDanMuData",
            "/replay/openapi/v2100/getClientConfig", "/replay/anchorurl/clientAnchorList",
            "/replay/loginrotateimage/noPage", "/replay/sales/getCurrentUserSaleByPhone",
            "/replay/sales/getByPhoneUserSale", "/replay/words/analysis/mGetOnlineAnalysis", "/open/callback/anchor/channel-cancel",
            "/replay/words/tradeRank/similarAnchorCallback", "/replay/openapi/v2100/queryDanMuExportGet", "/replay/words/tradeRank/download-template",
            "/replay/words/tradeRank/third-rank-callback", "/internal/crm/**", "/replay/openapi/governance/**", "/internal/ai-agent/**",
            "/replay/user/registerAppointment", "/replay/trade/listSimpleTreeL2", "/internal/data-hub/**",
            // 官网 www.ifupan.com 的 SEO 文章模块，由 SeoSiteController 独占该前缀。
            // 这是本仓库唯一一组对匿名公网开放的业务读接口，加 endpoint 前请先读该类的类注释：
            // 只读、只返回已发布内容、出参只用 vo.site 包下的类。
            "/replay/site/**");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径和心跳状态
        String requestURI = request.getRequestURI();
        boolean isHeartbeatPath = "/replay/user/getUserByToken".equals(requestURI);

        // 获取并处理token
        String requestHeaderToken = request.getHeader("Token");
        String token = StrUtil.isBlank(requestHeaderToken) ? "" : requestHeaderToken.replaceAll("\"", "");

        if (ObjectUtil.isEmpty(token)) {
            String tokenParameter = request.getParameter("token");
            token = StrUtil.isBlank(tokenParameter) ? "" : tokenParameter.replaceAll("\"", "");
        }

        // 检查排除路径
        for (String pattern : excludePathList) {
            if (pathMatcher.match(pattern, requestURI)) {
                // 排除路径但有效token时，也更新token过期时间
                updateTokenIfValid(token, isHeartbeatPath);
                return true;
            }
        }

        // 验证token
        if (StrUtil.isBlank(token)) {
            ResponseMsgUtils.returnMsp(response, Constant.CodeMsgEnum.NO_LOGIN.getCode(), "登录过期");
            return false;
        }

        // 获取并验证用户信息
        UserCacheVo userVo = userTokenProducer.getUserByToken(token);
        if (Objects.isNull(userVo)) {
            ResponseMsgUtils.returnMsp(response, Constant.CodeMsgEnum.NO_LOGIN.getCode(), "登录过期");
            return false;
        }

        // 更新token过期时间
        userTokenProducer.updateTokenExpire(userVo, token, isHeartbeatPath, false);
        return true;
    }

    /**
     * 更新有效token的过期时间
     *
     * @param token           token字符串
     * @param isHeartbeatPath 是否心跳路径
     */
    private void updateTokenIfValid(String token, boolean isHeartbeatPath) {
        if (StrUtil.isNotBlank(token)) {
            UserCacheVo userVo = userTokenProducer.getUserByToken(token);
            if (Objects.nonNull(userVo)) {
                userTokenProducer.updateTokenExpire(userVo, token, isHeartbeatPath, true);
            }
        }
    }
}
