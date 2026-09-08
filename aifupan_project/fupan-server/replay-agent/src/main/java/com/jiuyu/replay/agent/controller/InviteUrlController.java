package com.jiuyu.replay.agent.controller;

import com.jiuyu.replay.agent.bll.InviteUrlCodeBll;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("replay/agent/inviteurlcode")
@Tag(name = "邀请链接的code")
public class InviteUrlController {

    @Resource
    private InviteUrlCodeBll inviteUrlCodeBll;

    /**
     * 获取当前用户的邀请链接
     * @return
     */
    @GetMapping("/getUserInviteUrl")
    @Operation(summary = "获取当前用户的邀请链接")
    public R<String> getUserInviteUrl(){

        return inviteUrlCodeBll.getUserInviteUrl();
    }

}
