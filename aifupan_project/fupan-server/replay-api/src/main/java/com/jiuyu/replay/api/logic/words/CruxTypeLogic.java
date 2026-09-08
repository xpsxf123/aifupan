package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.CruxTypeListVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeInfoVo;
import com.jiuyu.replay.words.bo.CruxTypeBo;
import com.jiuyu.replay.words.bo.CruxTypeListBo;
import com.jiuyu.replay.words.vo.CruxTypeTreeVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeVo;

import java.util.List;


/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
public interface CruxTypeLogic {


    /**
     * 关键词类型列表
     * @param cruxTypeListBo 关键词类型列表查询参数
     * @return
     */
    R<PageUtils<CruxTypeListVo>> queryPage(CruxTypeListBo cruxTypeListBo);

    /**
    * 关键词类型信息
    * @param id 关键词类型id
    * @return
    */
    R<CruxTypeInfoVo> info(Long id);

    /**
     * 新增关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    R<String> save(CruxTypeBo cruxTypeBo);

    /**
     * 修改关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    R<String> update(CruxTypeBo cruxTypeBo);

    /**
     * 删除关键词类型
     * @param id 关键词类型id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取在数据罗盘展示的关键词类型列表
     * @return
     */
    R<List<CruxTypeVo>> getShowCompassList();

    /**
     * 关键词类型列表（树形结构）
     * @param childrenNotNull 当没有子关键词类型时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    R<List<CruxTypeTreeVo>> listTree(Integer childrenNotNull);
}

