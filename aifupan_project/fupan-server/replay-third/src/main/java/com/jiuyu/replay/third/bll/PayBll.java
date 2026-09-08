package com.jiuyu.replay.third.bll;

import com.alipay.api.AlipayApiException;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.ali.Alipay;
import com.jiuyu.replay.third.bo.NativePayBo;
import com.jiuyu.replay.third.bo.WechatPayCallbackHandleBo;
import com.jiuyu.replay.third.constant.AliPayProperties;
import com.jiuyu.replay.third.constant.Constant;
import com.jiuyu.replay.third.constant.WeChatPayProperties;
import com.jiuyu.replay.third.tencent.WeChatPay;
import com.jiuyu.replay.third.vo.QueryOrderStatus;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.nativepay.model.Transaction;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

//@Component
public class PayBll {

    @Resource
    private WeChatPay weChatPay;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private WeChatPayProperties weChatPayProperties;
    @Resource
    private Config weChatPayConfig;
    @Resource
    private Alipay alipay;
    @Resource
    private AliPayProperties aliPayProperties;
    @Resource
    private CommonProperties commonProperties;

    /**
     * 获取支付二维码
     * @param nativePayBo 支付请求数据
     * @return
     */
    @SneakyThrows
    public R<String> getPayCode(NativePayBo nativePayBo) {
        nativePayBo.setMoney(1);
        if(nativePayBo.getPayType() == 0) {

            // 查询缓存里面是否有，有则直接返回
            Object obj = redisTemplate.opsForValue().get(weChatPayProperties.getCodeUrlRedisKey() + nativePayBo.getOrderId());
            if(obj != null) {
                return R.ok("获取成功", (String) obj);
            }

            // 微信支付
            String cordUrl = weChatPay.nativePay(nativePayBo.getOrderId(), nativePayBo.getMoney(), nativePayBo.getTitle(), nativePayBo.getCallbackAddress());
            // 存到缓存
            redisTemplate.opsForValue().set(weChatPayProperties.getCodeUrlRedisKey() + nativePayBo.getOrderId(), cordUrl, Duration.ofMinutes(commonProperties.getOrderTimeoutMinutes()));
            return R.ok("获取成功", cordUrl);

        }else if(nativePayBo.getPayType() == 1) {
            Object objAli = redisTemplate.opsForValue().get(aliPayProperties.getCodeUrlRedisKey() + nativePayBo.getOrderId());
            if(objAli != null) {
                return R.ok("获取成功", (String) objAli);
            }
            // 支付宝支付
            String body = alipay.nativePay(nativePayBo.getOrderId(), nativePayBo.getMoney(), nativePayBo.getTitle(), nativePayBo.getCallbackAddress());
            redisTemplate.opsForValue().set(aliPayProperties.getCodeUrlRedisKey() + nativePayBo.getOrderId(), body, Duration.ofMinutes(commonProperties.getOrderTimeoutMinutes()));
            return R.ok("获取成功",body);
        }
        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "获取失败，请稍后再试");

    }

    public R<QueryOrderStatus> queryOrderStatus(Long orderId, Integer payType) throws AlipayApiException {
        if(payType == 0) {
            // 微信支付
            QueryOrderStatus res = weChatPay.queryOrderStatus(orderId);
            return R.ok("获取成功",res);
        }else if(payType == 1) {
            // 支付宝支付
            QueryOrderStatus res = alipay.queryOrderStatus(orderId);
            return R.ok("获取成功", res);
        }
        return R.ok();
    }


    /**
     * 微信支付回调处理
     * @param wechatPayCallbackHandleBo 微信回调数据
     * @return
     */
    public Transaction wechatPayCallbackHandle(WechatPayCallbackHandleBo wechatPayCallbackHandleBo) {

        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPayCallbackHandleBo.getSerialNo())
                .nonce(wechatPayCallbackHandleBo.getNonceStr())
                .signature(wechatPayCallbackHandleBo.getWechatSign())
                .timestamp(wechatPayCallbackHandleBo.getTimestamp())
                .body(wechatPayCallbackHandleBo.getBody())
                .build();

        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser((NotificationConfig) weChatPayConfig);

        // 验签、解密并转换成 Transaction
        return parser.parse(requestParam, Transaction.class);

    }

    public R<Boolean> closeOrder(Long orderPayId, Integer payType) throws AlipayApiException {
        if(payType == 0) {
            Boolean flag = weChatPay.closeOrder(orderPayId);
            return R.ok("获取成功",flag);
        }else if(payType == 1) {
            Boolean flag = alipay.closeOrder(orderPayId);
            return R.ok("获取成功",flag);
        }
        return R.ok(false);
    }
}
