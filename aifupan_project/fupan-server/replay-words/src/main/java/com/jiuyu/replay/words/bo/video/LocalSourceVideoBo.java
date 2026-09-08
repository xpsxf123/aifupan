package com.jiuyu.replay.words.bo.video;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 客户端分页获取本地源视频列表查询参数（自动删除本地视频专用）。
 *
 * <p>仅分页字段（page/limit）；userId / tenantId 由服务端从 JWT 解析，不接收外部传入，
 * 保证租户隔离与越权防护。</p>
 */
@Data
@Schema(description = "客户端分页获取本地源视频列表查询参数")
public class LocalSourceVideoBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;
}
