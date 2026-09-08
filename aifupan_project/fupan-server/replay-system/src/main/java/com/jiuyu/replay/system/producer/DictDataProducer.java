package com.jiuyu.replay.system.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;

import java.util.List;


/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
public interface DictDataProducer {


    /**
     * 字典列表
     * @param dictDataListBo 字典列表查询参数
     * @return
     */
    PageUtils<DictDataListVo> queryPage(DictDataListBo dictDataListBo);

    /**
    * 字典信息
    * @param id 字典id
    * @return
    */
    DictDataInfoVo info(Long id);

    /**
     * 新增字典
     * @param dictDataBo 字典对象
     * @return
     */
     DictDataInfoVo save(DictDataBo dictDataBo);

    /**
     * 修改字典
     * @param dictDataBo 字典对象
     * @return
     */
    void update(DictDataBo dictDataBo);

    /**
     * 删除字典
     * @param id 字典id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据字典标识获取字典列表
     * @param typeLogo 字典标识
     * @return
     */
    List<DictDataListVo> listByTypeLogo(String typeLogo);

    /**
     * 获取字典的树形结构
     *
     * @param typeId 类型id
     * @return 列表
     */
    List<DictDataListVo> listDictDataTree(Long typeId);

    /**
     * 根据ids获取字典列表
     *
     * @param ids 字典id列表
     * @return
     */
    List<DictDataListVo> dictDataListByIds(List<Long> ids);

}

