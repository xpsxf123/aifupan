package com.jiuyu.replay.words.vo.anchor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 热榜行业信息
 *
 * @author HeHui
 * @date 2026-02-02 10:58
 */
@Getter
@Setter
public class TradeRankInfo {

    /**
     * ID
     */
    private Long id;
    /**
     * 行业名称
     */
    private String name;

    /**
     * 行业热榜生效状态（0:不生效, 1:生效）
     */
    private Integer rankEnabled;

    /**
     * 第三方采集开关
     */
    private Boolean thirdCollect;


    /**
     * 第三方关联行业数量
     */
    private Integer thirdRelationSize;

}
