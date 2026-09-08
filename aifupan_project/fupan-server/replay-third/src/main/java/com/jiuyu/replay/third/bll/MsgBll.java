package com.jiuyu.replay.third.bll;

import cn.hutool.core.date.DateUtil;
import com.jiuyu.replay.generic.dto.third.SmsResult;
import com.jiuyu.replay.third.sms.SmsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @author ：lujie
 * @description：发送短信的bll
 * @date ：2025/6/20 上午10:07
 */
@Component
@AllArgsConstructor
public class MsgBll {

    private SmsService sendAliMsg;


    /**
     * 发送验证码短信
     *
     * @param phone 手机号
     * @param code  验证码
     */
    public boolean sendMsg(String phone, String code) {
        HashMap<String, String> params = new HashMap<>();
        params.put("code", code);
        SmsResult verificationCode = sendAliMsg.send("verification_code", phone, params, null);
        return verificationCode.isSuccess();
    }

    /**
     * 发送主播上下播消息
     *
     * @param phone
     * @param type       类型：0上播，1下播
     * @param anchorName
     * @param reason
     * @return
     */
    public boolean sendSwitchAnchorMsg(String phone, Integer type, String anchorName, String reason) {
        HashMap<String, String> params = new HashMap<>();
        params.put("name", anchorName);
        params.put("time", DateUtil.now());
        if (type == 0) {
            // 上播
            SmsResult startRecording = sendAliMsg.send("start_recording", phone, params, null);
            return startRecording.isSuccess();
        } else {
            // 下播
            params.put("reason", reason);
            SmsResult startRecording = sendAliMsg.send("end_recording", phone, params, null);
            return startRecording.isSuccess();
        }
    }

    /**
     * 发送绑定子账号短信
     *
     * @param phone
     * @return
     */
    public boolean sendBindingAccountMsg(String phone) {
        SmsResult startRecording = sendAliMsg.send("account_binding", phone, null, null);
        return startRecording.isSuccess();
    }


}
