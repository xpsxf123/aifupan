package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

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
public interface CruxTypeProducer {


    /**
     * 关键词类型列表
     * @param cruxTypeListBo 关键词类型列表查询参数
     * @return
     */
    PageUtils<CruxTypeListVo> queryPage(CruxTypeListBo cruxTypeListBo);

    /**
    * 关键词类型信息
    * @param id 关键词类型id
    * @return
    */
    CruxTypeInfoVo info(Long id);

    /**
     * 新增关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
     CruxTypeInfoVo save(CruxTypeBo cruxTypeBo);

    /**
     * 修改关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    void update(CruxTypeBo cruxTypeBo);

    /**
     * 删除关键词类型
     * @param id 关键词类型id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取在数据罗盘展示的关键词类型列表
     * @return
     */
    List<CruxTypeVo> getShowCompassList();

    /**
     * 关键词类型列表（树形结构）
     * @param childrenNotNull 当没有子关键词类型时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    List<CruxTypeTreeVo> listTree(Integer childrenNotNull);

    /**
     * 获取父级id数组
     * @param id 当前分类id
     * @return
     */
    List<Long> getParentIdArr(Long id);

    /**
     * 获取分类id的父级id集合（包含当前分类id）
     * @param cruxTypeId 分类id
     * @return
     */
    List<Long> getParentIdContainerSelfArr(Long cruxTypeId);

    /**
     * 根据父分类id获取子分类列表
     * @param parentId 父分类id
     * @return
     */
    List<CruxTypeInfoVo> listByParentId(Long parentId);

    /**
     * 获取所有关键词分类列表
     * @return
     */
    List<CruxTypeInfoVo> listAll();

    /**
     * 根据层级获取分类列表
     * @param level 层级
     * @return
     */
    List<CruxTypeInfoVo> listByLevel(int level);

    /**
     * 获取分类下的所有层级的子分类id(包含自身id)
     * @param cruxTypeId 分类id
     * @return
     */
    List<Long> getChildrenIdAndSelfIdList(Long cruxTypeId);
}

