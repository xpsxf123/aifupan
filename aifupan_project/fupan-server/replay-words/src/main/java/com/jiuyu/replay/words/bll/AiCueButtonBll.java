package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.AiCueButtonListVo;
import com.jiuyu.replay.words.vo.AiCueButtonInfoVo;
import com.jiuyu.replay.words.bo.AiCueButtonBo;
import com.jiuyu.replay.words.bo.AiCueButtonListBo;
import com.jiuyu.replay.words.producer.AiCueButtonProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 固定提示按钮

 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Component
public class AiCueButtonBll {

    @Resource
    private AiCueButtonProducer aiCueButtonProducer;


    /**
     * 固定提示按钮
列表
     * @param aiCueButtonListBo 固定提示按钮
列表查询参数
     * @return
     */
    public R<PageUtils<AiCueButtonListVo>> queryPage(AiCueButtonListBo aiCueButtonListBo) {

        return R.ok("获取成功", aiCueButtonProducer.queryPage(aiCueButtonListBo));
    }

    /**
    * 固定提示按钮
信息
    * @param id 固定提示按钮
id
    * @return
    */
    public R<AiCueButtonInfoVo> info(Long id) {

        AiCueButtonInfoVo aiCueButtonInfoVo = aiCueButtonProducer.info(id);
        return R.ok("获取成功", aiCueButtonInfoVo);
    }

    /**
     * 新增固定提示按钮

     * @param aiCueButtonBo 固定提示按钮
对象
     * @return
     */
    public R<String> save(AiCueButtonBo aiCueButtonBo) {

        AiCueButtonInfoVo aiCueButtonInfoVo = aiCueButtonProducer.save(aiCueButtonBo);
        return R.ok("添加成功");
    }

    /**
     * 修改固定提示按钮

     * @param aiCueButtonBo 固定提示按钮
对象
     * @return
     */
    public R<String> update(AiCueButtonBo aiCueButtonBo) {

        aiCueButtonProducer.update(aiCueButtonBo);
        return R.ok("修改成功");
    }

    /**
     * 删除固定提示按钮

     * @param id 固定提示按钮
id
     * @return
     */
    public R<String> delete(Long id) {

        aiCueButtonProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

