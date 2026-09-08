package com.jiuyu.replay.order.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.order.vo.IncrementListVo;
import com.jiuyu.replay.order.vo.IncrementInfoVo;
import com.jiuyu.replay.order.bo.IncrementBo;
import com.jiuyu.replay.order.bo.IncrementListBo;

import java.util.List;


/**
 * 增量包表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
public interface IncrementProducer {


    /**
     * 增量包表列表
     * @param incrementListBo 增量包表列表查询参数
     * @return
     */
    PageUtils<IncrementListVo> queryPage(IncrementListBo incrementListBo);

    /**
    * 增量包表信息
    * @param id 增量包表id
    * @return
    */
    IncrementInfoVo info(Long id);

    /**
     * 新增增量包表
     * @param incrementBo 增量包表对象
     * @return
     */
     IncrementInfoVo save(IncrementBo incrementBo);

    /**
     * 修改增量包表
     * @param incrementBo 增量包表对象
     * @return
     */
    void update(IncrementBo incrementBo);

    /**
     * 删除增量包表
     * @param id 增量包表id
     * @return
     */
    void deleteById(Long id);

    /**
     * 批量删除增量包表
     *
     * @param ids 增量包表id列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据套餐id删除
     * @param packageId
     */
    void deleteByPackageId(Long packageId);

    /**
     * 根据套餐id查询
     *
     * @param packageId
     * @return
     */
    List<IncrementInfoVo> listByPackageId(Long packageId);

    /**
     * 批量根据套餐id查询
     * @param packageId
     * @return
     */
    List<IncrementInfoVo> listByPackageId(List<Long> packageId);

    /**
     * 批量新增
     * @param incrementList
     */
    void saveBatch(List<IncrementBo> incrementList);
}

