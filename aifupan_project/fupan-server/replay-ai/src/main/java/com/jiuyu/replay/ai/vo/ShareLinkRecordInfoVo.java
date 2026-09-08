package com.jiuyu.replay.ai.vo;


import com.jiuyu.replay.generic.vo.ai.ConversationVo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 分享链接记录信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Data
@Schema(description = "分享链接记录信息项")
public class ShareLinkRecordInfoVo extends ShareLinkRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * ai问答记录
	 */
	@Schema(description = "ai问答记录")
	List<ConversationVo> conversationVoList;
}
