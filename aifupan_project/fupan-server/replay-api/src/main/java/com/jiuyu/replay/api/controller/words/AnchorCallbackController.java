package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.anchor.CancelChannelAnchorBo;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 主播回调相关API
 *
 * @author HeHui
 * @date 2025-11-11 11:23
 */
@RestController
@RequestMapping("/open/callback/anchor")
@AllArgsConstructor
@Slf4j
public class AnchorCallbackController {

    private final AnchorUrlUserProducer anchorUrlUserProducer;


    /**
     * 微信视频号-取消授权
     */
    @PostMapping("/channel-cancel")
    public R<Void> cancelChannel(@RequestBody @Validated CancelChannelAnchorBo cancelChannelAnchorBo) {
        log.info("[主播回调] wechat channel callback, user is cancel authorization. param infoId: {}, userIds: {}", cancelChannelAnchorBo.getAuthorizerInfoId(), cancelChannelAnchorBo.getUserIds());
        return anchorUrlUserProducer.cancelChannel(cancelChannelAnchorBo.getAuthorizerInfoId(), cancelChannelAnchorBo.getUserIds());
    }
}
