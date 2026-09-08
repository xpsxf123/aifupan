package com.jiuyu.replay.api.logic.order;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.order.bo.AiTokenUseRecordListBo;


/**
 * ai的token使用记录
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
public interface AiTokenUseRecordLogic {


    /**
     * ai的token使用记录列表
     * @param aiTokenUseRecordListBo ai的token使用记录列表查询参数
     * @return
     */
    R<PageUtils<AiTokenUseRecordListVo>> queryPage(AiTokenUseRecordListBo aiTokenUseRecordListBo);

    /**
    * ai的token使用记录信息
    * @param id ai的token使用记录id
    * @return
    */
    R<AiTokenUseRecordInfoVo> info(Long id);

    /**
     * 新增ai的token使用记录
     *
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    R<AiTokenUseRecordInfoVo> save(AiTokenUseRecordBo aiTokenUseRecordBo);

    /**
     * 修改ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    R<String> update(AiTokenUseRecordBo aiTokenUseRecordBo);

    /**
     * 删除ai的token使用记录
     * @param id ai的token使用记录id
     * @return
     */
    R<String> delete(Long id);


}

