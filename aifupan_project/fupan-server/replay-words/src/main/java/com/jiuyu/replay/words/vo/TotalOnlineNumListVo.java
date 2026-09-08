package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 直播总观看人次列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Data
@Schema(description = "直播总观看人次列表项")
public class TotalOnlineNumListVo extends TotalOnlineNumVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
