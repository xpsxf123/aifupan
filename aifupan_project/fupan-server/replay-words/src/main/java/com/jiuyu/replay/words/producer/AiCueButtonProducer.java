package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.AiCueButtonListVo;
import com.jiuyu.replay.words.vo.AiCueButtonInfoVo;
import com.jiuyu.replay.words.bo.AiCueButtonBo;
import com.jiuyu.replay.words.bo.AiCueButtonListBo;


/**
 * 固定提示按钮

 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
public interface AiCueButtonProducer {


    /**
     * 固定提示按钮
列表
     * @param aiCueButtonListBo 固定提示按钮
列表查询参数
     * @return
     */
    PageUtils<AiCueButtonListVo> queryPage(AiCueButtonListBo aiCueButtonListBo);

    /**
    * 固定提示按钮
信息
    * @param id 固定提示按钮
id
    * @return
    */
    AiCueButtonInfoVo info(Long id);

    /**
     * 新增固定提示按钮

     * @param aiCueButtonBo 固定提示按钮
对象
     * @return
     */
     AiCueButtonInfoVo save(AiCueButtonBo aiCueButtonBo);

    /**
     * 修改固定提示按钮

     * @param aiCueButtonBo 固定提示按钮
对象
     * @return
     */
    void update(AiCueButtonBo aiCueButtonBo);

    /**
     * 删除固定提示按钮

     * @param id 固定提示按钮
id
     * @return
     */
    void deleteById(Long id);


}

