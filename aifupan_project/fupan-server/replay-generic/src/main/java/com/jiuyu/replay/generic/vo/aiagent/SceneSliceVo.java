package com.jiuyu.replay.generic.vo.aiagent;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 *  视频 切片画面 场景解析结果
 * @author HeHui
 * @date 2026-07-20 17:09
 */
@Getter
@Setter
public class SceneSliceVo {

    /**
     * 视频ID
     */
    private String videoId;


    /**
     * 截取秒数(距视频结束)
     */
    private Integer sliceSeconds;


    /**
     * AI分析结果
     */
    private String aiResult;
}
