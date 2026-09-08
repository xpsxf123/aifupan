package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.SensitiveWordsClientBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientListBo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientInfoVo;

import java.util.List;


/**
 * 客户端自定义词语
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
public interface SensitiveWordsClientLogic {


    /**
     * 客户端自定义词语列表
     * @param sensitiveWordsListBo 客户端自定义词语列表查询参数
     * @return
     */
    R<PageUtils<SensitiveWordsClientListVo>> queryPage(SensitiveWordsClientListBo sensitiveWordsListBo);

    /**
    * 客户端自定义词语信息
    * @param id 客户端自定义词语id
    * @return
    */
    R<SensitiveWordsClientInfoVo> info(Long id);

    /**
     * 新增客户端自定义词语
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    R<List<String>> save(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 修改客户端自定义词语
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    R<String> update(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 删除客户端自定义词语
     * @param id 客户端自定义词语id
     * @return
     */
    R<String> delete(Long id);


}

