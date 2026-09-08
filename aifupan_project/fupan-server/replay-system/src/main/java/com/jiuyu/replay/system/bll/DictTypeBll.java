package com.jiuyu.replay.system.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.DictTypeBo;
import com.jiuyu.replay.system.bo.DictTypeListBo;
import com.jiuyu.replay.system.producer.DictTypeProducer;
import com.jiuyu.replay.system.vo.DictTypeInfoVo;
import com.jiuyu.replay.system.vo.DictTypeListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 字典类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Component
public class DictTypeBll {

    @Resource
    private DictTypeProducer dictTypeProducer;


    /**
     * 字典类型列表
     * @param dictTypeListBo 字典类型列表查询参数
     * @return
     */
    public R<PageUtils<DictTypeListVo>> queryPage(DictTypeListBo dictTypeListBo) {

        return R.ok("获取成功", dictTypeProducer.queryPage(dictTypeListBo));
    }

    /**
    * 字典类型信息
    * @param id 字典类型id
    * @return
    */
    public R<DictTypeInfoVo> info(Long id) {

        DictTypeInfoVo dictTypeInfoVo = dictTypeProducer.info(id);
        return R.ok("获取成功", dictTypeInfoVo);
    }

    /**
     * 新增字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    public R<String> save(DictTypeBo dictTypeBo) {

        DictTypeInfoVo dictTypeInfoVo = dictTypeProducer.save(dictTypeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改字典类型
     * @param dictTypeBo 字典类型对象
     * @return
     */
    public R<String> update(DictTypeBo dictTypeBo) {

        dictTypeProducer.update(dictTypeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除字典类型
     * @param id 字典类型id
     * @return
     */
    public R<String> delete(Long id) {

        dictTypeProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

