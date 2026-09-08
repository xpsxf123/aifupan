package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.dto.words.BasicSettingsBaseDto;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.bo.video.UpdateAiPartialBo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author ：lujie
 * @description：基础设置业务对象
 * @date ：2025/1/7
 */
@Data
@Schema(description = "基础设置业务对象")
@AllArgsConstructor
@NoArgsConstructor
public class BasicSettingsBo extends BasicSettingsBaseDto {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "来源id")
    @NotNull(message = "来源id不能为空")
    private String sourceId;

    @Schema(description = "来源类型 0：主播，1：视频，2：文件，")
    @NotNull(message = "来源类型不能为空")
    private Integer sourceType;

    @Schema(description = "用户表id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;

    public BasicSettingsBo(AddOrUpdateAnchorBo bo) {
        this.userId = bo.getUserId();
        this.tenantId = bo.getTenantId();
        this.sourceType = WordsEnum.basicSettingsType.ANCHOR.getCode();
        this.sourceId = bo.getSecUid();

        // 设置值
        setValues(bo);
    }

    public BasicSettingsBo(UpdateAiPartialBo bo) {
        this.userId = bo.getUserId();
        this.tenantId = bo.getTenantId();
        this.sourceType = WordsEnum.basicSettingsType.ANCHOR.getCode();
        this.sourceId = bo.getSecUid();
        this.accountType = bo.getAccountType();
        this.accountStage = bo.getAccountStage();
        this.accountWaterLevel = bo.getAccountWaterLevel();
        this.accountFlow = bo.getAccountFlow();
        this.anchorSituation = bo.getAnchorSituation();
        this.roiAccuracy = bo.getRoiAccuracy();
    }

    public <T extends BasicSettingsBaseDto> void setValues(T bo) {
        this.accountType = bo.getAccountType();
        this.premiereDate = bo.getPremiereDate();
        this.accountStage = bo.getAccountStage();
        this.accountWaterLevel = bo.getAccountWaterLevel();
        this.accountFlow = bo.getAccountFlow();
        this.livingTarget = bo.getLivingTarget();
        this.livingModality = bo.getLivingModality();
        this.marketing = bo.getMarketing();
        this.optimizeDirection = bo.getOptimizeDirection();
        this.learning = bo.getLearning();
        this.livingMode = bo.getLivingMode();
        this.anchorSituation = bo.getAnchorSituation();
        this.roiAccuracy = bo.getRoiAccuracy();
    }

}