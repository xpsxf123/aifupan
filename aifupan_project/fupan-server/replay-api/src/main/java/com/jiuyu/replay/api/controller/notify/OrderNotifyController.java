package com.jiuyu.replay.api.controller.notify;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.internal.util.AlipaySignature;
import com.jiuyu.replay.api.logic.order.OrderLogic;
import com.jiuyu.replay.third.constant.AliPayProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("replay/notify")
@Tag(name = "回调方法")
@AllArgsConstructor
public class OrderNotifyController {

    private static final Logger log = LoggerFactory.getLogger(OrderNotifyController.class);
    private final OrderLogic orderLogic;
    private final AliPayProperties aliPayProperties;

    /**
     * 微信支付回调
     * @return
     */
    @PostMapping("/wechatPayCallback")
    @Operation(summary = "微信支付回调")
    public String wechatPayCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(this.orderLogic.wechatPayCallbackHandle(request)) {
            // 处理成功
            response.setStatus(200);
            return "";
        }

        // 处理失败
        Map<String, String> resultMap = new HashMap<>();
        response.setStatus(500);
        resultMap.put("code", "FAIL");
        resultMap.put("message", "失败");
        return JSONObject.toJSONString(resultMap);
    }

    @PostMapping("/alipayPayCallback")
    @Operation(summary = "支付宝支付回调")
    public String alipayCallback(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String,String> paramsMap = new HashMap<>();
        for(Map.Entry<String,String[]> entry : parameterMap.entrySet()){
            paramsMap.put(entry.getKey(), entry.getValue()[0]);
        }

        //验签
        boolean signVerified = AlipaySignature.rsaCertCheckV1(paramsMap, aliPayProperties.getAlipayPublicCertPath(), aliPayProperties.getCharset(), aliPayProperties.getSignType());
        if (signVerified){
            // 处理支付结果
            String tradeStatus = paramsMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus)){
                // 更新订单状态
                if (!orderLogic.alipayPayCallbackHandle(Long.valueOf(paramsMap.get("out_trade_no")), paramsMap.get("trade_no"), JSONUtil.toJsonStr(paramsMap))) {
                    log.info("支付宝支付回调失败，");
                    return "failure";
                }
            }
            return "success";
        }
        return "failure";
    }

}
