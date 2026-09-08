package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * lujie
 * 2024-12-05
 */
@Data
@Schema(description = "websocket采集数据-过程")
public class SocketProcessDataBo  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "时间")
    public String time;

    @Schema(description = "在线人数")
    public String renshu;

    @Schema(description = "累计观看人数")
    public String leijiguankanrenshu;

    @Schema(description = "场观人数")
    public String changguan;

    @Schema(description = "点赞人数")
    public String dianzan;

    @Schema(description = "关注数")
    public String guanzhu;

    @Schema(description = "粉丝团数")
    public String fenshituan;
}
