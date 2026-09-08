package com.jiuyu.replay.generic.dto.words;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户主播数量DTO
 *
 * @author AI Assistant
 */
@Data
public class UserAnchorCountDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 主播数量
     */
    private Integer anchorCount;
}
