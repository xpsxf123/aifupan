package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "客户端上报计算机配置BO")
public class ComputerConfigBo {

    @Schema(description = "机器码MD5")
    private String cpuId;

    @Schema(description = "CPU型号")
    private String cpuModel;

    @Schema(description = "CPU物理核心数")
    private Integer cpuCores;

    @Schema(description = "CPU逻辑线程数")
    private Integer cpuThreads;

    @Schema(description = "CPU主频(MHz)")
    private Integer cpuFrequencyMhz;

    @Schema(description = "物理内存(GB)")
    private BigDecimal ramGb;

    @Schema(description = "GPU型号（多个用;分隔）")
    private String gpuModel;

    @Schema(description = "GPU显存(GB)")
    private BigDecimal vramGb;

    @Schema(description = "系统盘总空间(GB)")
    private BigDecimal diskTotalGb;

    @Schema(description = "系统盘剩余空间(GB)")
    private BigDecimal diskFreeGb;

    @Schema(description = "操作系统版本")
    private String osVersion;

    @Schema(description = "客户端版本号")
    private String clientVersion;

    @Schema(description = "物理网卡MAC地址")
    private String macAddress;

    @Schema(description = "计算机名")
    private String computerName;

    @Schema(description = "本机IPv4地址")
    private String ipAddress;

    @Schema(description = "主屏幕分辨率")
    private String screenResolution;
}
