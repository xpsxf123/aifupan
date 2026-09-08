package com.jiuyu.replay.generic.vo.aiagent;

import com.jiuyu.replay.generic.dto.words.viewing.UserPortraitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 视频在线曲线 + 人群画像。
 *
 * @author fupan-server
 */
@Data
public class OnlineCurveVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== ① 概览 =====

    /**
     * 累计观看人数
     */
    private Integer totalViewersNum;

    /**
     * 最大在线人数
     */
    private Integer maxOnlineNum;

    /**
     * 弹幕总数
     */
    private Integer totalBarrageNum;

    // ===== ② 折线组 =====

    /**
     * 在线人数折线
     */
    private List<CurvePointVo> onlineDataList;

    /**
     * 进场人数折线
     */
    private List<CurvePointVo> approachDataList;

    /**
     * 离场人数折线
     */
    private List<CurvePointVo> exitPeopleDataList;

    /**
     * 弹幕折线
     */
    private List<CurvePointVo> barrageDataList;

    /**
     * 成交折线
     */
    private List<CurvePointVo> payComboCntDataList;

    /**
     * 成交金额折线
     */
    private List<CurvePointVo> payAmtDataList;

    /**
     * 新增粉丝折线
     */
    private List<CurvePointVo> followAnchorUcntDataList;

    // ===== ③ 人群画像 =====

    /**
     * 看播人群画像（年龄/性别/省份分布）；无数据时为 null
     */
    private UserPortraitDto watchUserPortrait;

    /**
     * 成交人群画像；无数据时为 null
     */
    private UserPortraitDto payUserPortrait;


    /**
     * 投放消耗折线数据
     */
    private List<CurvePointDoubleVo> qianchuanCostDataList;

    /**
     * 退款金额折线数据
     */
    private List<CurvePointDoubleVo> refundAmtDataList;

    /**
     * 净成交ROI折线数据
     */
    private List<CurvePointDoubleVo> netTransactionRoiDataList;
}
