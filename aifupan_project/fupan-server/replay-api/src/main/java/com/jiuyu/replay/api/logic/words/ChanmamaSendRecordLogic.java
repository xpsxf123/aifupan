package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordListBo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordInfoVo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordListVo;


/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
public interface ChanmamaSendRecordLogic {


    /**
     * 第三方数据平台发送记录列表
     * @param chanmamaSendRecordListBo 第三方数据平台发送记录列表查询参数
     * @return
     */
    R<PageUtils<ChanmamaSendRecordListVo>> queryPage(ChanmamaSendRecordListBo chanmamaSendRecordListBo);

    /**
    * 第三方数据平台发送记录信息
    * @param id 第三方数据平台发送记录id
    * @return
    */
    R<ChanmamaSendRecordInfoVo> info(Long id);

    /**
     * 新增第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    R<String> save(ChanmamaSendRecordBo chanmamaSendRecordBo);

    /**
     * 修改第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    R<String> update(ChanmamaSendRecordBo chanmamaSendRecordBo);

    /**
     * 删除第三方数据平台发送记录
     * @param id 第三方数据平台发送记录id
     * @return
     */
    R<String> delete(Long id);


}

