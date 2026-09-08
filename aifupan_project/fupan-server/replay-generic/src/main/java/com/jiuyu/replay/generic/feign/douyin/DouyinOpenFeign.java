package com.jiuyu.replay.generic.feign.douyin;


import com.jiuyu.replay.generic.bo.douyin.DouyinAnchorBo;
import com.jiuyu.replay.generic.bo.douyin.DouyinOpenResult;

/**
 * 内部  抖音开放API
 *
 * @author HeHui
 * @date 2026-02-06 10:25
 */
public interface DouyinOpenFeign {

    /**
     * 搜索抖音主播
     *
     * @param keyword   关键字 抖音号或昵称
     * @param isUnique  是否启用精准匹配，精准匹配只限于搜抖音号 0: 否 1: 是
     * @return 抖音主播信息
     */
    DouyinOpenResult<DouyinAnchorBo> searchAnchor(String keyword, boolean isUnique);
}
