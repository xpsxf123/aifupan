package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordBo;
import com.jiuyu.replay.words.bo.viewing.ChanmamaSendRecordListBo;
import com.jiuyu.replay.words.producer.ChanmamaSendRecordProducer;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordInfoVo;
import com.jiuyu.replay.words.vo.viewing.ChanmamaSendRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 第三方数据平台发送记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@Component
public class ChanmamaSendRecordBll {

    @Resource
    private ChanmamaSendRecordProducer chanmamaSendRecordProducer;


    /**
     * 第三方数据平台发送记录列表
     * @param chanmamaSendRecordListBo 第三方数据平台发送记录列表查询参数
     * @return
     */
    public R<PageUtils<ChanmamaSendRecordListVo>> queryPage(ChanmamaSendRecordListBo chanmamaSendRecordListBo) {

        return R.ok("获取成功", chanmamaSendRecordProducer.queryPage(chanmamaSendRecordListBo));
    }

    /**
    * 第三方数据平台发送记录信息
    * @param id 第三方数据平台发送记录id
    * @return
    */
    public R<ChanmamaSendRecordInfoVo> info(Long id) {

        ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = chanmamaSendRecordProducer.info(id);
        return R.ok("获取成功", chanmamaSendRecordInfoVo);
    }

    /**
     * 新增第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    public R<String> save(ChanmamaSendRecordBo chanmamaSendRecordBo) {

        ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = chanmamaSendRecordProducer.save(chanmamaSendRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改第三方数据平台发送记录
     * @param chanmamaSendRecordBo 第三方数据平台发送记录对象
     * @return
     */
    public R<String> update(ChanmamaSendRecordBo chanmamaSendRecordBo) {

        chanmamaSendRecordProducer.update(chanmamaSendRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除第三方数据平台发送记录
     * @param id 第三方数据平台发送记录id
     * @return
     */
    public R<String> delete(Long id) {

        chanmamaSendRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 根据请求id获取请求记录
     * @param requestId 请求id
     * @return
     */
    public R<ChanmamaSendRecordInfoVo> infoByRequestId(String requestId) {

        ChanmamaSendRecordInfoVo chanmamaSendRecordInfoVo = this.chanmamaSendRecordProducer.infoByRequestId(requestId);

        return R.ok(chanmamaSendRecordInfoVo);
    }
}

