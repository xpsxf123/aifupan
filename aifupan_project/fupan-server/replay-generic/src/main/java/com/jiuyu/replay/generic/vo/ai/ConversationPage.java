package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/23 上午10:47
 */
@Data
@Schema(description = "ai记录-分页")
public class ConversationPage<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否有上一页")
    private Boolean existPreviousPage;

    @Schema(description = "每页记录数")
    private List<T> list;

}
