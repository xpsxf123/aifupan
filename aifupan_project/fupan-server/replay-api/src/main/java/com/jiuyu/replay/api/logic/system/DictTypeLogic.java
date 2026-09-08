package com.jiuyu.replay.api.logic.system;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.system.vo.DictTypeListVo;
import com.jiuyu.replay.system.vo.DictTypeInfoVo;
import com.jiuyu.replay.system.bo.DictTypeBo;
import com.jiuyu.replay.system.bo.DictTypeListBo;


/**
 * 字典类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
public interface DictTypeLogic {


    /**
     * 字典类型列表
     * @param dictTypeListBo 字典类型列表查询参数
     * @return
     */
    R<PageUtils<DictTypeListVo>> queryPage(DictTypeListBo dictTypeListBo);

    /**
    * 字典类型信息
    * @param id 字典类型id
    * @return
    */
    R<DictTypeInfoVo> info(Long id);

    /**
     * 新增字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    R<String> save(DictTypeBo dictTypeBo);

    /**
     * 修改字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    R<String> update(DictTypeBo dictTypeBo);

    /**
     * 删除字典类型
     * @param id 字典类型id
     * @return
     */
    R<String> delete(Long id);


}

