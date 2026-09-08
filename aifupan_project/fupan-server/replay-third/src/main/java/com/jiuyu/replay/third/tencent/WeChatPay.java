package com.jiuyu.replay.third.tencent;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.third.constant.WeChatPayProperties;
import com.jiuyu.replay.third.vo.QueryOrderStatus;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Component
public class WeChatPay {

    private static final Logger log = LoggerFactory.getLogger(WeChatPay.class);
    @Resource
    private Config weChatPayConfig;
    @Resource
    private WeChatPayProperties weChatPayProperties;
    @Resource
    private CommonProperties commonProperties;

    /**
     * Native支付
     * @param orderId 订单id
     * @param money 订单金额，单位：分
     * @param title 订单标题
     * @return
     */
    public String nativePay(Long orderId, Integer money, String title, String notifyUrl) {
        // 构建service
        NativePayService service = new NativePayService.Builder().config(weChatPayConfig).build();
        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(money);
        request.setAmount(amount);
        request.setAppid(weChatPayProperties.getAppid());
        request.setMchid(weChatPayProperties.getMerchantId());
        request.setDescription(title);
        request.setNotifyUrl(commonProperties.getCurrentSystemAddress()+ (ObjectUtil.isNotEmpty(notifyUrl) ? notifyUrl : ""));
        request.setOutTradeNo(orderId.toString());
        Long codeUrlTimeout = commonProperties.getOrderTimeoutMinutes();
        DateTime offset = new DateTime().offset(DateField.MINUTE, ObjectUtil.defaultIfNull(codeUrlTimeout, 30L).intValue());
        request.setTimeExpire(offset.toString("yyyy-MM-dd$HH:mm:ss+08:00").replace("$", "T"));
        // 调用下单方法，得到应答
        PrepayResponse response = service.prepay(request);
        // 使用微信扫描 code_url 对应的二维码，即可体验Native支付
        return response.getCodeUrl();
    }

    public QueryOrderStatus queryOrderStatus(Long orderId) {
        // 构建service
        NativePayService service = new NativePayService.Builder().config(weChatPayConfig).build();
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(weChatPayProperties.getMerchantId());
        request.setOutTradeNo(orderId.toString());
        Transaction transaction = service.queryOrderByOutTradeNo(request);
        log.info("orderId={}，transaction:{}",orderId, transaction);
        if (transaction != null){
            QueryOrderStatus result = new QueryOrderStatus();
            result.setOrderId(orderId);
            if (transaction.getTradeState().equals(Transaction.TradeStateEnum.SUCCESS)){
                result.setStatus(0);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.REFUND)){
                result.setStatus(1);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.NOTPAY)){
                result.setStatus(2);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.CLOSED)){
                result.setStatus(3);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.REVOKED)){
                result.setStatus(4);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.USERPAYING)){
                result.setStatus(5);
            }else if (transaction.getTradeState().equals(Transaction.TradeStateEnum.PAYERROR)){
                result.setStatus(6);
            }else{
                RRException.create("未知交易状态");
            }
            result.setTransactionId(transaction.getTransactionId());
            result.setJsonString(JSONUtil.toJsonStr(transaction));
            return result;
        }else {
            RRException.create("查询订单状态失败");
        }
        return null;
    }

    public Boolean closeOrder(Long orderPayId) {
        try{
            NativePayService service = new NativePayService.Builder().config(weChatPayConfig).build();
            CloseOrderRequest request = new CloseOrderRequest();
            request.setMchid(weChatPayProperties.getMerchantId());
            request.setOutTradeNo(orderPayId.toString());
            service.closeOrder(request);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
