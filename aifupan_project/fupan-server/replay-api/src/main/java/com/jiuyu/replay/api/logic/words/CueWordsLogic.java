package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
public interface CueWordsLogic {


    /**
     * 提示词列表
     * @param cueWordsListBo 提示词列表查询参数
     * @return
     */
    R<PageUtils<CueWordsListVo>> queryPage(CueWordsListBo cueWordsListBo);

    /**
    * 提示词信息
    * @param id 提示词id
    * @return
    */
    R<CueWordsInfoVo> info(Long id);

    /**
     * 新增提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    R<String> save(CueWordsBo cueWordsBo);

    /**
     * 修改提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    R<String> update(CueWordsBo cueWordsBo);

    /**
     * 删除提示词
     * @param id 提示词id
     * @return
     */
    R<String> delete(Long id);


}

