package com.jiuyu.replay.api.logic.system;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;


/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
public interface DictDataLogic {


    /**
     * 字典列表
     * @param dictDataListBo 字典列表查询参数
     * @return
     */
    R<PageUtils<DictDataListVo>> queryPage(DictDataListBo dictDataListBo);

    /**
    * 字典信息
    * @param id 字典id
    * @return
    */
    R<DictDataInfoVo> info(Long id);

    /**
     * 新增字典
     * @param dictDataBo 字典对象
     * @return
     */
    R<String> save(DictDataBo dictDataBo);

    /**
     * 修改字典
     * @param dictDataBo 字典对象
     * @return
     */
    R<String> update(DictDataBo dictDataBo);

    /**
     * 删除字典
     * @param id 字典id
     * @return
     */
    R<String> delete(Long id);


}

