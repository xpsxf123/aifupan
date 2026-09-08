package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.CruxTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.SensitiveWordsProducer;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.bo.CruxTypeBo;
import com.jiuyu.replay.words.bo.CruxTypeListBo;
import com.jiuyu.replay.words.producer.CruxTypeProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Component
public class CruxTypeBll {

    @Resource
    private CruxTypeProducer cruxTypeProducer;
    @Resource
    private SensitiveWordsProducer sensitiveWordsProducer;


    /**
     * 关键词类型列表
     * @param cruxTypeListBo 关键词类型列表查询参数
     * @return
     */
    public R<PageUtils<CruxTypeListVo>> queryPage(CruxTypeListBo cruxTypeListBo) {

        return R.ok("获取成功", cruxTypeProducer.queryPage(cruxTypeListBo));
    }

    /**
    * 关键词类型信息
    * @param id 关键词类型id
    * @return
    */
    public R<CruxTypeInfoVo> info(Long id) {

        CruxTypeInfoVo cruxTypeInfoVo = cruxTypeProducer.info(id);

        if(cruxTypeInfoVo != null) {
            // 封装父级id数组
            List<Long> parentIdArr = cruxTypeProducer.getParentIdArr(id);
            cruxTypeInfoVo.setParentIdArr(parentIdArr);
        }

        return R.ok("获取成功", cruxTypeInfoVo);
    }

    /**
     * 新增关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    public R<String> save(CruxTypeBo cruxTypeBo) {

        CruxTypeInfoVo cruxTypeInfoVo = cruxTypeProducer.save(cruxTypeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    public R<String> update(CruxTypeBo cruxTypeBo) {

        cruxTypeProducer.update(cruxTypeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除关键词类型
     * @param id 关键词类型id
     * @return
     */
    public R<String> delete(Long id) {

        List<CruxTypeInfoVo> cruxTypeInfoVos = this.cruxTypeProducer.listByParentId(id);
        if(cruxTypeInfoVos != null && cruxTypeInfoVos.size() > 0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该分类下存在子分类，删除失败");
        }

        List<SensitiveWordsInfoVo> sensitiveWordsInfoVos = sensitiveWordsProducer.listByCruxTypeId(id);
        if(sensitiveWordsInfoVos != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "该分类下存在关键词，删除失败");
        }

        cruxTypeProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取在数据罗盘展示的关键词类型列表
     * @return
     */
    public R<List<CruxTypeVo>> getShowCompassList() {

        List<CruxTypeVo> cruxTypeVoList = cruxTypeProducer.getShowCompassList();

        return R.ok(cruxTypeVoList);
    }

    /**
     * 关键词类型列表（树形结构）
     * @param childrenNotNull 当没有子关键词类型时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    public R<List<CruxTypeTreeVo>> listTree(Integer childrenNotNull) {

        List<CruxTypeTreeVo> cruxTypeTreeVoList = this.cruxTypeProducer.listTree(childrenNotNull);

        return R.ok(cruxTypeTreeVoList);
    }

    /**
     * 获取所有关键词分类列表
     * @return
     */
    public R<List<CruxTypeInfoVo>> listAll() {

        return R.ok(this.cruxTypeProducer.listAll());
    }
}

