package com.jiuyu.replay.generic.dto.words.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "巨量用户画像")
public class UserPortraitDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 年龄画像
     */
    @Schema(description = "年龄画像")
    private List<UserPortraitItemDto> agePortrait;

    /**
     * 性别画像
     */
    @Schema(description = "性别画像")
    private List<UserPortraitItemDto> genderPortrait;

    /**
     * 城市画像
     */
    @Schema(description = "城市画像")
    private List<UserPortraitItemDto> provincePortrait;
}
