package com.jiuyu.governance.openfeign.collect;


import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinAnchorBo;
import com.jiuyu.governance.openfeign.collect.pojo.response.DouyinOpenResult;

/**
 * 各平台主播开放接口
 *
 * @author HeHui
 * @date 2026-03-25 14:40
 */
public interface LiveAnchorOpenService {

    /**
     * 搜索主播
     *
     * @param platformType 平台类型
     * @param keyword      关键字 抖音号或昵称
     * @param isUnique     是否启用精准匹配，精准匹配只限于搜抖音号 0: 否 1: 是
     *
     * @return {@link DouyinOpenResult }<{@link DouyinAnchorBo }>
     */
    DouyinOpenResult<DouyinAnchorBo> searchAnchor(LivePlatformType platformType, String keyword, boolean isUnique);
}
