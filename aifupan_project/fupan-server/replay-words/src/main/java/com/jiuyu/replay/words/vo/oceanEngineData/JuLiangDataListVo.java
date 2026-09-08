package com.jiuyu.replay.words.vo.oceanEngineData;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：liwj
 * @description：TODO
 * @date ：2025/8/8 17:27
 */
@Data
@Schema(description = "巨量数据列表")
public class JuLiangDataListVo {

    private String dateTime;

    private String date;

    private Integer payComboCnt;

    private Double salesCount;

}
