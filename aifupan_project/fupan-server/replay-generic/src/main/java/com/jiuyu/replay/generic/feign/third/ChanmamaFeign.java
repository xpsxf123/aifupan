package com.jiuyu.replay.generic.feign.third;

import com.jiuyu.replay.generic.dto.third.ChanmamaRevisionDto;
import com.jiuyu.replay.generic.dto.third.SendSimilarAnchorDto;
import com.jiuyu.replay.generic.dto.third.ThirdSalesRankingResult;
import com.jiuyu.replay.generic.vo.common.R;

public interface ChanmamaFeign {

    /**
     * 发送修正数据请求
     * @param chanmamaRevisionDto 请求数据
     * @return
     */
    R<Boolean> sendRevisionQuery(ChanmamaRevisionDto chanmamaRevisionDto);

    /**
     * 发送获取相似达人请求
     *
     * @param sendSimilarAnchorDto 请求数据
     * @return
     */
    R<Boolean> sendSimilarAnchorQuery(SendSimilarAnchorDto sendSimilarAnchorDto);


    /**
     * 获取第三方榜单信息
     * @param thirdTradeId 第三方榜单ID
     * @param callbackUrl 回调地址
     * @return 榜单信息
     */
    R<ThirdSalesRankingResult> getSalesRanking(String thirdTradeId, String callbackUrl);
}
