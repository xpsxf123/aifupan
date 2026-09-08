package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.controller.openapi.governance.response.IdName;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.producer.UserProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *  ai 智能体 账户相关API
 * @author HeHui
 * @date 2026-07-27 11:14
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent/account")
public class AiAgentAccountController {

    private final UserProducer userProducer;


    /**
     * 获取用户名称
     * @param userIds 用户ID
     * @return 用户名称
     */
    @PostMapping("/user-name")
    public R<List<IdName>> getUserName(@RequestBody List<Long> userIds) {
        return R.ok(userProducer.getUserNameMap(userIds).entrySet().stream().map(entry -> new IdName(entry.getKey(), entry.getValue())).toList());
    }
}
