package com.jiuyu.replay.agent.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 代理商佣金列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Data
@Schema(description = "代理商佣金列表项")
public class AgentCommissionListVo extends AgentCommissionVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
