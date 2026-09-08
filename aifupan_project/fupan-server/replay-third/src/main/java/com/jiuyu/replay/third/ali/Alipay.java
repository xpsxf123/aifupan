package com.jiuyu.replay.third.ali;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.third.config.AliBabaPayConfig;
import com.jiuyu.replay.third.constant.AliPayProperties;
import com.jiuyu.replay.third.vo.QueryOrderStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Alipay {

    @Resource
    private AliBabaPayConfig aliBabaPayConfig;
    @Resource
    private AliPayProperties aliPayProperties;
    @Resource
    private CommonProperties commonProperties;


    public String nativePay(Long orderId, Integer money, String title, String notifyUrl) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(aliBabaPayConfig.getAlipayConfig());
        // 构造请求参数以调用接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest ();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        // 设置商户订单号
        model.setOutTradeNo(String.valueOf(orderId));
        // 设置订单总金额
        model.setTotalAmount(BigDecimal.valueOf(money).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toString());
        // 设置订单标题
        model.setSubject(title);
        // 设置产品码
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        Long codeUrlTimeout = commonProperties.getOrderTimeoutMinutes();
        DateTime offset = new DateTime().offset(DateField.MINUTE, ObjectUtil.defaultIfNull(codeUrlTimeout, 30L).intValue());
        model.setTimeExpire(offset.toString("yyyy-MM-dd HH:mm:ss"));
        model.setQrPayMode("4");
        model.setQrcodeWidth(134L);
        request.setBizModel(model);
        request.setNotifyUrl(commonProperties.getCurrentSystemAddress() + notifyUrl);

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request,"POST");

        if (response.isSuccess()){
            return response.getBody();
        } else{
            RRException.create("下单失败");
            return "failure";
        }

    }

    /**
     * 支付宝的订单关闭接口，订单关闭后，无法再次支付
     * 如果没有人去扫码是不会查询到订单的，会提示交易不存在
     * @param orderId
     * @return
     * @throws AlipayApiException
     */
    public QueryOrderStatus queryOrderStatus(Long orderId) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(aliBabaPayConfig.getAlipayConfig());
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
        bizModel.setOutTradeNo(orderId.toString());
        request.setBizModel(bizModel);
        AlipayTradeQueryResponse response = alipayClient.certificateExecute(request);
        if (response.isSuccess()){
            QueryOrderStatus result = new QueryOrderStatus();
            result.setOrderId(orderId);
            String tradeStatus = response.getTradeStatus();
            if ("WAIT_BUYER_PAY".equals(tradeStatus)){
                result.setStatus(2);
            }else if ("TRADE_CLOSED".equals(tradeStatus)){
                result.setStatus(3);
            }else if ("TRADE_SUCCESS".equals(tradeStatus)){
                result.setStatus(0);
            }else if ("TRADE_FINISHED".equals(tradeStatus)){
                result.setStatus(3);
            }else{
                RRException.create("未知交易状态");
            }
            result.setTransactionId(response.getTradeNo());
            result.setJsonString(response.getBody());
            return result;
        }else {
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                QueryOrderStatus result = new QueryOrderStatus();
                result.setStatus(2);
                return result;
            }
            RRException.create("查询订单状态失败");
        }
        return null;
    }

    public Boolean closeOrder(Long orderPayId) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(aliBabaPayConfig.getAlipayConfig());

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        AlipayTradeCloseModel bizModel = new AlipayTradeCloseModel();
        bizModel.setOutTradeNo(orderPayId.toString());
        request.setBizModel(bizModel);
        AlipayTradeCloseResponse response = alipayClient.certificateExecute(request);
        if (response.isSuccess()){
            return response.getMsg().equals("Success");
        }
        return false;
    }
}
