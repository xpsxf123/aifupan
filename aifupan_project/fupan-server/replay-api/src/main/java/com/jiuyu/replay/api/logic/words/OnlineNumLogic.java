package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.OnlineNumListVo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.bo.OnlineNumBo;
import com.jiuyu.replay.words.bo.OnlineNumListBo;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
public interface OnlineNumLogic {


    /**
     * 直播实时在线人数列表
     * @param onlineNumListBo 直播实时在线人数列表查询参数
     * @return
     */
    R<PageUtils<OnlineNumListVo>> queryPage(OnlineNumListBo onlineNumListBo);

    /**
    * 直播实时在线人数信息
    * @param id 直播实时在线人数id
    * @return
    */
    R<OnlineNumInfoVo> info(Long id);

    /**
     * 新增直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    R<String> save(OnlineNumBo onlineNumBo);

    /**
     * 修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    R<String> update(OnlineNumBo onlineNumBo);

    /**
     * 删除直播实时在线人数
     * @param id 直播实时在线人数id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 新增或修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    R<String> saveOrUpdate(OnlineNumBo onlineNumBo) throws Exception;
}

