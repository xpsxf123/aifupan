package com.jiuyu.replay.common.aop;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.annotation.AgentQueryUserCheck;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.power.UserDetailsInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/28 15:38
 */
@Slf4j
@Aspect
@Component
public class AgentQueryUserCheckAspect {

    @Resource
    private UserFeign userFeign;

    @Around("@annotation(agentQueryUserCheck)")
    public Object around(ProceedingJoinPoint joinPoint, AgentQueryUserCheck agentQueryUserCheck) throws Throwable {
        // 1. 解析锁的Key（支持Spring EL表达式）
        String userIdStr = resolveLockKey(joinPoint, agentQueryUserCheck.checkUserId());

        // 如果是null就直接通过
        if (userIdStr == null) {
            return joinPoint.proceed();
        }

        Long userId = NumberUtil.parseLong(userIdStr, null);

        if (userId == null) {
            log.error("[AgentQueryUserCheckAspect] string转long失败, string = {}", userIdStr);
            throw new BusinessException(StatusCode.PARAM_EX.getCode(), "用户无权限查看当前数据");
        }

        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        if (ObjectUtil.equals(user.getUserType(), UserEnums.userType.MANAGE_ADMIN_USER.getCode())) {
            if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT.getCode())) {
                UserDetailsInfoVo userDetail = userFeign.getUserDetailByUserId(userId);
                if (userDetail == null || !ObjectUtil.equals(userDetail.getAgentId(), user.getAgentId())) {
                    throw new BusinessException(StatusCode.PARAM_EX.getCode(), "用户无权限查看当前数据");
                }
            } else if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
                UserDetailsInfoVo userDetail = userFeign.getUserDetailByUserId(userId);
                UserDetailsInfoVo loginUserDetail = userFeign.getUserDetailByUserId(user.getId());
                if (userDetail == null || loginUserDetail == null || !ObjectUtil.equals(userDetail.getSaleId(), loginUserDetail.getSaleId())) {
                    throw new BusinessException(StatusCode.PARAM_EX.getCode(), "用户无权限查看当前数据");
                }
            }
        }

        // 2. 获取Redisson锁
        return joinPoint.proceed();

    }

    /**
     * 解析锁的Key（支持Spring EL表达式）
     * 例如：key = "lock:#{T(java.util.UUID).randomUUID()}"
     */
    private String resolveLockKey(ProceedingJoinPoint joinPoint, String key) {
        if (key.isEmpty()) {
            // 默认使用类名+方法名+参数的MD5（保证唯一性）
            return null;
        } else {
            // 使用SpEL解析表达式
            SpelExpressionParser parser = new SpelExpressionParser();
            // 表达式内容不包含 #{} 边界符
            Expression expression = parser.parseExpression(key);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", joinPoint.getArgs());
            return expression.getValue(context, String.class);
        }
    }

}
