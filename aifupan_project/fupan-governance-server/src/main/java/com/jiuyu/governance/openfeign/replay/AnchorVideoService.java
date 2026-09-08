package com.jiuyu.governance.openfeign.replay;


import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.openfeign.replay.request.VideoListRequest;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoListItem;
import com.jiuyu.governance.openfeign.replay.response.ReplayCursorPage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  主播场次视频服务
 * @author HeHui
 * @date 2026-08-04 16:43
 */
public interface AnchorVideoService {


    /**
     * 获取主播场次视频列表
     *
     * @param request 请求参数
     * @return 响应结果
     */
    ReplayCursorPage<AnchorVideoListItem> queryVideoList(VideoListRequest request);


    /**
     * 获取视频批次编号
     *
     * @param videoIds 视频ID
     * @param userId
     * @param userType
     * @param tenantId 租户ID
     *
     * @return {@link Map }<{@link String }, {@link String }> 批次编号Map
     */
    default Map<String, String> getVideoBatchNumberMap(List<String> videoIds, Long userId, Integer userType, long tenantId) {
        VideoListRequest request = new VideoListRequest();
        request.setVideoIds(videoIds);
        request.setDataSourceType(2);
        request.setUserType(userType == null ? 0 : userType);
        request.setUserId(userId);
        request.setTenantId(tenantId);
        request.setSimple(true);
        request.setPageSize(videoIds.size() + 1);
        ReplayCursorPage<AnchorVideoListItem> page = queryVideoList(request);
        if (EmptyUtil.isEmpty(page)) {
            return Map.of();
        }
        if (EmptyUtil.isEmpty(page.getList())) {
            return Map.of();
        }
        return page.getList().stream().filter(item -> EmptyUtil.isNotEmpty(item.getBatchNumber())).collect(Collectors.toMap(AnchorVideoListItem::getVideoId, AnchorVideoListItem::getBatchNumber));
    }
}
