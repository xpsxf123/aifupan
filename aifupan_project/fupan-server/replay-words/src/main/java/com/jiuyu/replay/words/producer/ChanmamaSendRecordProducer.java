package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
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
public interface ChanmamaSendRecordProducer {


    /**
     * 第三方数据平台发送记录列表
     * @param chanmamaSendRecordListBo 第三方数据平台发送记录列表查询参数
     * @return
     */
    PageUtils<ChanmamaSendRecordListVo> queryPage(ChanmamaSendRecordListBo chanmamaSendRecordListBo);

    /**
    * 第三方数据平台发送记录信息
    * @param id 第三方数据平台发送记录id
    * @return
    */
    ChanmamaSendRecordInfoVo info(Long id);

    /**
     * 新增第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
     ChanmamaSendRecordInfoVo save(ChanmamaSendRecordBo chanmamaSendRecordBo);

    /**
     * 修改第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    void update(ChanmamaSendRecordBo chanmamaSendRecordBo);

    /**
     * 删除第三方数据平台发送记录
     * @param id 第三方数据平台发送记录id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据请求id获取请求记录
     * @param requestId 请求id
     * @return
     */
    ChanmamaSendRecordInfoVo infoByRequestId(String requestId);

    /**
     * 更新请求记录的回调内容
     * @param chanmamaSendRecordInfoVo 请求信息
     * @param code 回调体状态
     * @param body 回调体内容
     * @param dataStatus 数据状态 0：正常数据 1：异常数据但已校验完成 2：异常数据，未校验 3：未校验
     * @param accountType 数据来源 0：蝉妈妈 1：考古家
     * @param roomId 蝉妈妈/考古家的直播间id
     */
    void updateCallbackBody(ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo, Integer code, String body, Integer dataStatus, Integer accountType, String roomId);

    /**
     * 保存蝉妈妈发送记录的修正数据请求信息
     * @param sendRecodeId 蝉妈妈发送记录id
     * @param status 数据状态 @{@link com.jiuyu.replay.words.enums.ChanmamaCallbackDataStatusEnum}
     * @param revisionRequestBody 修正请求的请求体
     * @param revisionResponseBody 修正请求的响应体
     */
    void saveRevisionData(Long sendRecodeId, Integer status, String revisionRequestBody, String revisionResponseBody);
}

