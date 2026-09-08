package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("tb_computer_config")
public class ComputerConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String cpuId;
    private Long userId;
    private String cpuModel;
    private Integer cpuCores;
    private Integer cpuThreads;
    private Integer cpuFrequencyMhz;
    private BigDecimal ramGb;
    private String gpuModel;
    private BigDecimal vramGb;
    private BigDecimal diskTotalGb;
    private BigDecimal diskFreeGb;
    private String osVersion;
    private String clientVersion;
    private String macAddress;
    private String computerName;
    private String ipAddress;
    private String screenResolution;
    private Date createDate;
    private Date updateDate;
    private Integer isDeleted;
}
