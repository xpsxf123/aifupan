package com.jiuyu.replay.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 *  更新密码请求
 * @author HeHui
 * @date 2026-05-23 17:59
 */
@Getter
public class UpdatePasswordEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 7175214568639215718L;
    private final long userId;

    private final String newPassword;

    private final long tenantId;


    public UpdatePasswordEvent(long operatorUserId, long userId, String newPassword, long tenantId) {
        super(operatorUserId);
        this.userId = userId;
        this.newPassword = newPassword;
        this.tenantId = tenantId;
    }


    public Long getOperatorUserId() {
        return (Long) super.getSource();
    }
}
