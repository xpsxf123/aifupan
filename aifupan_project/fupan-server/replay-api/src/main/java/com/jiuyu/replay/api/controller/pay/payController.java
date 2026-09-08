package com.jiuyu.replay.api.controller.pay;


import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.third.ali.Alipay;
import com.jiuyu.replay.third.config.AliBabaPayConfig;
import com.jiuyu.replay.third.constant.AliPayProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("replay/pay")
public class payController {

    @Resource
    private AliPayProperties aliPayProperties;
    @Resource
    private AliBabaPayConfig aliBabaPayConfig;
    @Resource
    private Alipay alipay;


    @PostMapping("/alipay")
    public String alipay(Long orderId) throws AlipayApiException {
        return alipay.nativePay(orderId, 1, "测试", "https://daidu.com");
    }

    // 支付宝支付接口
    @PostMapping("/payment")
    public String payment() throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(aliBabaPayConfig.getAlipayConfig());
        // 构造请求参数以调用接口
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest ();
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        // 设置商户订单号
        model.setOutTradeNo("20150320010301001");
        // 设置订单总金额
        model.setTotalAmount("88.88");
        // 设置订单标题
        model.setSubject("Iphone6 16G");
        // 设置产品码
        model.setProductCode("QR_CODE_OFFLINE");
//        Long codeUrlTimeout = aliPayProperties.getCodeUrlTimeout();
//        DateTime offset = new DateTime().offset(DateField.MINUTE, ObjectUtil.defaultIfNull(codeUrlTimeout, 30L).intValue());
//        model.setTimeExpire(offset.toString("yyyy-MM-dd$HH:mm:ss+08:00").replace("$", "T"));

        request.setBizModel(model);
//        request.setNotifyUrl("");

        AlipayTradePrecreateResponse response = alipayClient.certificateExecute(request);
        System.out.println(response.getQrCode());
        System.out.println(response.getBody());
        if (response.isSuccess()){
            return "success";
        }else{
            RRException.create("下单失败");
            return "failure";
        }
    }

    @SneakyThrows
    @PostMapping("/notify")
    public String notifyPay(HttpServletRequest request, HttpServletResponse response){
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
                System.out.println("订单：" + paramsMap.get("out_trade_no") + "，状态更新为：" + tradeStatus);
                // 回复支付宝
                return "success";
            }
        }else{
            // 回复支付宝
            return "failure";
        }
        return "failure";
    }

    private AlipayConfig getAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        // 数据格式
        alipayConfig.setFormat(aliPayProperties.getFormat());
        // 字符编码
        alipayConfig.setCharset(aliPayProperties.getCharset());
        //支付宝网关地址
        alipayConfig.setServerUrl(aliPayProperties.getServerUrl());
        //appId
        alipayConfig.setAppId(aliPayProperties.getAppId());
        //设置签名类型
        alipayConfig.setSignType(aliPayProperties.getSignType());
        //应用私钥
        alipayConfig.setPrivateKey(aliPayProperties.getPrivateKey());
        //设置应用公钥证书路径
        alipayConfig.setAppCertPath(aliPayProperties.getAppCertPath());
        //设置支付宝公钥证书路径
        alipayConfig.setAlipayPublicCertPath(aliPayProperties.getAlipayPublicCertPath());
        //设置支付宝根证书路径
        alipayConfig.setRootCertPath(aliPayProperties.getRootCertPath());
        return alipayConfig;
    }

}
