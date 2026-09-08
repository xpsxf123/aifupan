package com.jiuyu.replay.third.api;

import com.jiuyu.replay.generic.dto.third.ChanmamaRevisionDto;
import com.jiuyu.replay.generic.dto.third.SendSimilarAnchorDto;
import com.jiuyu.replay.generic.dto.third.ThirdSalesRankingResult;
import com.jiuyu.replay.generic.feign.third.ChanmamaFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.third.chanmama.ThirdDataUtils;
import jakarta.annotation.Resource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ChanmamaApi implements ChanmamaFeign {

    private final ThirdDataUtils chanmamaUtils;

    private final boolean isDev;

    public ChanmamaApi(ThirdDataUtils chanmamaUtils, Environment environment) {
        this.chanmamaUtils = chanmamaUtils;
        this.isDev = environment.getActiveProfiles().length > 0 && Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Override
    public R<Boolean> sendRevisionQuery(ChanmamaRevisionDto chanmamaRevisionDto) {
        return chanmamaUtils.sendRevisionQuery(chanmamaRevisionDto);
    }

    @Override
    public R<Boolean> sendSimilarAnchorQuery(SendSimilarAnchorDto sendSimilarAnchorDto) {
        return chanmamaUtils.sendSimilarAnchorQuery(sendSimilarAnchorDto);
    }


    /**
     * 获取第三方榜单信息
     *
     * @param thirdTradeId 第三方榜单ID
     * @param callbackUrl   回调地址
     * @return 榜单信息
     */
    @Override
    public R<ThirdSalesRankingResult> getSalesRanking(String thirdTradeId, String callbackUrl) {
        try {
            if (isDev) {
                return R.error(1, "额度不足请充值");
            }
            return chanmamaUtils.getSalesRanking(thirdTradeId, callbackUrl);
        } catch (Exception e) {
            return R.error("获取第三方榜单信息失败: " + e.getMessage());
        }
    }
}
