package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;


/**
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
public interface TotalOnlineNumLogic {


    /**
     * 直播总观看人次列表
     * @param totalOnlineNumListBo 直播总观看人次列表查询参数
     * @return
     */
    R<PageUtils<TotalOnlineNumListVo>> queryPage(TotalOnlineNumListBo totalOnlineNumListBo);

    /**
    * 直播总观看人次信息
    * @param id 直播总观看人次id
    * @return
    */
    R<TotalOnlineNumInfoVo> info(Long id);

    /**
     * 新增直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    R<String> save(TotalOnlineNumBo totalOnlineNumBo);

    /**
     * 修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    R<String> update(TotalOnlineNumBo totalOnlineNumBo);

    /**
     * 删除直播总观看人次
     * @param id 直播总观看人次id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 新增或修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    R<String> saveOrUpdate(TotalOnlineNumBo totalOnlineNumBo);
}

