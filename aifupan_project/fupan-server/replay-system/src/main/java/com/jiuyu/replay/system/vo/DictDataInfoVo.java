package com.jiuyu.replay.system.vo;


import com.jiuyu.replay.generic.vo.system.DictDataVo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 字典信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Data
@Schema(description = "字典信息项")
public class DictDataInfoVo extends DictDataVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
