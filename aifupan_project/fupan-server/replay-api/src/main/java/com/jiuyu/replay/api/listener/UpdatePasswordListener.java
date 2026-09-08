package com.jiuyu.replay.api.listener;

import cn.hutool.core.util.DesensitizedUtil;
import com.jiuyu.replay.common.event.UpdatePasswordEvent;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.governance.GovernanceEmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 *  更新密码事件监听器
 * @author HeHui
 * @date 2026-05-23 18:08
 */
@Component
@Slf4j
public class UpdatePasswordListener implements ApplicationListener<UpdatePasswordEvent> {

    private final GovernanceEmployeeService governanceEmployeeService;

    public UpdatePasswordListener(GovernanceEmployeeService governanceEmployeeService) {
        this.governanceEmployeeService = governanceEmployeeService;
    }

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @EventListener(UpdatePasswordEvent.class)
    @Override
    public void onApplicationEvent(UpdatePasswordEvent event) {
        log.info("收到用户更改密码行为 , 操作人: {}, userId: {}, hidePassword: {}, tenantId: {}",  event.getOperatorUserId(), event.getUserId(), DesensitizedUtil.firstMask(event.getNewPassword()), event.getTenantId());
        try {
            R<Void> result = governanceEmployeeService.updatePassword(event.getUserId(), event.getNewPassword(), event.getTenantId());
            if (result == null) {
                log.error("同步更新密码失败-1, 操作人: {}, userId: {}", event.getOperatorUserId(), event.getUserId());
            } else if (!result.success()) {
                log.error("同步更新密码失败-2, 操作人: {}, userId: {}, msg: {}", event.getOperatorUserId(), event.getUserId(), result.getMsg());
            } else {
                log.info("同步更新密码成功, 操作人: {}, userId: {}", event.getOperatorUserId(), event.getUserId());
            }
        } catch (Exception e) {
            log.error("同步更新密码异常, 操作人: {}, userId: {}", event.getOperatorUserId(), event.getUserId(), e);
        }

    }
}
