package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.ModelCruxListVo;
import com.jiuyu.replay.words.vo.ModelCruxInfoVo;
import com.jiuyu.replay.words.bo.ModelCruxBo;
import com.jiuyu.replay.words.bo.ModelCruxListBo;
import com.jiuyu.replay.words.producer.ModelCruxProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 模型-关键词类型-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Component
public class ModelCruxBll {

    @Resource
    private ModelCruxProducer modelCruxProducer;


    /**
     * 模型-关键词类型-关联表列表
     * @param modelCruxListBo 模型-关键词类型-关联表列表查询参数
     * @return
     */
    public R<PageUtils<ModelCruxListVo>> queryPage(ModelCruxListBo modelCruxListBo) {

        return R.ok("获取成功", modelCruxProducer.queryPage(modelCruxListBo));
    }

    /**
    * 模型-关键词类型-关联表信息
    * @param id 模型-关键词类型-关联表id
    * @return
    */
    public R<ModelCruxInfoVo> info(Long id) {

        ModelCruxInfoVo modelCruxInfoVo = modelCruxProducer.info(id);
        return R.ok("获取成功", modelCruxInfoVo);
    }

    /**
     * 新增模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    public R<String> save(ModelCruxBo modelCruxBo) {

        ModelCruxInfoVo modelCruxInfoVo = modelCruxProducer.save(modelCruxBo);
        return R.ok("添加成功");
    }

    /**
     * 修改模型-关键词类型-关联表
     * @param modelCruxBo 模型-关键词类型-关联表对象
     * @return
     */
    public R<String> update(ModelCruxBo modelCruxBo) {

        modelCruxProducer.update(modelCruxBo);
        return R.ok("修改成功");
    }

    /**
     * 删除模型-关键词类型-关联表
     * @param id 模型-关键词类型-关联表id
     * @return
     */
    public R<String> delete(Long id) {

        modelCruxProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

