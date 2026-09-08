package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.governance.openfeign.replay.request.TenantAnchorQueryRequest;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoInfoResponse;
import com.jiuyu.governance.openfeign.replay.response.TenantAnchorInfoResponse;

import java.util.List;

/**
 *  主播服务
 * @author HeHui
 * @date 2026-04-24 15:00
 */
public interface AnchorInfoService {


    /**
     * 获取租户主播信息
     *
     * @param queryRequest 查询参数
     *
     * @return {@link List<TenantAnchorInfoResponse>}
     */
    List<TenantAnchorInfoResponse> getTenantAnchors(TenantAnchorQueryRequest queryRequest);

    /**
     * 根据视频ID获取视频完整信息
     *
     * @param videoId 爱复盘视频唯一标识
     * @return 视频信息（含 tenantId 等全部字段），不存在返回 null
     */
    AnchorVideoInfoResponse getVideoByVideoId(String videoId);
}
