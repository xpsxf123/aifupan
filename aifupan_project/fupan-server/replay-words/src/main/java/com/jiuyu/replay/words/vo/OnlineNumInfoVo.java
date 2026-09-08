package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 直播实时在线人数信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Data
@Schema(description = "直播实时在线人数信息项")
public class OnlineNumInfoVo extends OnlineNumVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
