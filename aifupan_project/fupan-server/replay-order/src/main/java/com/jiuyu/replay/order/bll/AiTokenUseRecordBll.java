package com.jiuyu.replay.order.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordListVo;
import com.jiuyu.replay.generic.vo.third.AiTokenUseRecordInfoVo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.order.bo.AiTokenUseRecordListBo;
import com.jiuyu.replay.order.producer.AiTokenUseRecordProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * ai的token使用记录
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:00:33
 */
@Component
public class AiTokenUseRecordBll {

    @Resource
    private AiTokenUseRecordProducer aiTokenUseRecordProducer;


    /**
     * ai的token使用记录列表
     * @param aiTokenUseRecordListBo ai的token使用记录列表查询参数
     * @return
     */
    public R<PageUtils<AiTokenUseRecordListVo>> queryPage(AiTokenUseRecordListBo aiTokenUseRecordListBo) {

        return R.ok("获取成功", aiTokenUseRecordProducer.queryPage(aiTokenUseRecordListBo));
    }

    /**
    * ai的token使用记录信息
    * @param id ai的token使用记录id
    * @return
    */
    public R<AiTokenUseRecordInfoVo> info(Long id) {

        AiTokenUseRecordInfoVo aiTokenUseRecordInfoVo = aiTokenUseRecordProducer.info(id);
        return R.ok("获取成功", aiTokenUseRecordInfoVo);
    }

    /**
     * 新增ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    public R<AiTokenUseRecordInfoVo> save(AiTokenUseRecordBo aiTokenUseRecordBo) {

        return R.ok(aiTokenUseRecordProducer.save(aiTokenUseRecordBo));
    }

    /**
     * 修改ai的token使用记录
     * @param aiTokenUseRecordBo ai的token使用记录对象
     * @return
     */
    public R<String> update(AiTokenUseRecordBo aiTokenUseRecordBo) {

        aiTokenUseRecordProducer.update(aiTokenUseRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除ai的token使用记录
     * @param id ai的token使用记录id
     * @return
     */
    public R<String> delete(Long id) {

        aiTokenUseRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

