package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "客户端最新版本信息")
public class ClientUpdateNewestInfoVo {

    /**
     * 版本号
     */
    @Schema(description = "版本号")
    private String versionNum;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private String updateTime;

    /**
     * 更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁
     */
    @Schema(description = "更新类型 0爱复盘主程序更新, 1更新程序更新，2爱复盘补丁")
    private Integer isFront;

    /**
     * 是否强制更新 0否 1是
     */
    @Schema(description = "是否强制更新 0否 1是")
    private Integer updateType;

    /**
     * 版本编号 数字越大，版本越新
     */
    @Schema(description = "版本编号 数字越大，版本越新")
    private Double version;
    /**
     * 文件的md5
     */
    @Schema(description = "文件的md5")
    private String fileMd5;

    /**
     * 文件上传到cos的key
     */
    @Schema(description = "文件的cosKey")
    private String cosKey;

    /**
     * 客户端更新包COS key
     */
    @Schema(description = "客户端更新包COS key")
    private String clientCosKey;

    /**
     * COS解压后的文件路径(JSON)
     */
    @Schema(description = "COS解压后的文件路径(JSON)")
    private String clientFilesPath;

    /**
     * 更新文件的下载地址列表
     */
    @Schema(description = "更新文件的下载地址列表")
    private List<String> fileDownLoadUrls;
    /**
     * 更新描述
     */
    @Schema(description = "更新描述")
    private String updateInfo;

}
