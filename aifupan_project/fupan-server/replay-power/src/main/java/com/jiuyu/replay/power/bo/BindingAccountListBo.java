package com.jiuyu.replay.power.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 父子绑定记录列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-31 10:18:16
 */
@Data
@Schema(description = "父子绑定记录列表查询参数")
public class BindingAccountListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "父账号id")
	private Long parentUserId;

	@Schema(description = "子账号id")
	private Long childUserId;

	@Schema(description = "子账号id集合")
	private List<Long> childUserIds;

	@Schema(description = "绑定状态 0绑定，1解绑")
	private Integer bindingStatus;


}
