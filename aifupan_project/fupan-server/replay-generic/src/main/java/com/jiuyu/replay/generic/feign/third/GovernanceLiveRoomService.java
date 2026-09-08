package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.bo.governance.GovernanceAddAnchorBo;
import com.jiuyu.replay.generic.vo.common.R;

/**
 * 企业管理服务端-直播间主播服务
 * 封装调用企业管理服务端添加主播的逻辑
 */
public interface GovernanceLiveRoomService {

    /**
     * 通知企业管理服务端添加主播
     *
     * @param bo 添加主播请求参数
     * @return 调用结果
     */
    R<Void> addAnchor(GovernanceAddAnchorBo bo);
}
