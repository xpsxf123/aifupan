package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.words.bo.SocketCollectMessageBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;
import com.jiuyu.replay.words.entity.TotalOnlineNumEntity;
import com.jiuyu.replay.words.producer.SocketCollectMessageProducer;
import com.jiuyu.replay.words.producer.TotalOnlineNumProducer;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;


/**
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
@Component
public class TotalOnlineNumBll {

    @Resource
    private TotalOnlineNumProducer totalOnlineNumProducer;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;


    /**
     * 直播总观看人次列表
     * @param totalOnlineNumListBo 直播总观看人次列表查询参数
     * @return
     */
    public R<PageUtils<TotalOnlineNumListVo>> queryPage(TotalOnlineNumListBo totalOnlineNumListBo) {

        return R.ok("获取成功", totalOnlineNumProducer.queryPage(totalOnlineNumListBo));
    }

    /**
    * 直播总观看人次信息
    * @param id 直播总观看人次id
    * @return
    */
    public R<TotalOnlineNumInfoVo> info(Long id) {

        TotalOnlineNumInfoVo totalOnlineNumInfoVo = totalOnlineNumProducer.info(id);
        return R.ok("获取成功", totalOnlineNumInfoVo);
    }

    /**
     * 新增直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    public R<String> save(TotalOnlineNumBo totalOnlineNumBo) {
        update(totalOnlineNumBo);
        return R.ok("添加成功");
    }

    /**
     * 修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    public R<String> update(TotalOnlineNumBo totalOnlineNumBo) {
        RRException.isNotEmpty(totalOnlineNumBo.getBatchNumber(), "直播场次号不能为空");
        RRException.isNotEmpty(totalOnlineNumBo.getUserId(), "用户不能为空");
        RRException.isNotEmpty(totalOnlineNumBo.getVideoId(), "视频不能为空");
        String[] split = totalOnlineNumBo.getVideoId().split("_");
        for (String s : split) {
            if (ObjectUtil.isNotEmpty(s)){
                SocketCollectMessageInfoVo byBatch = socketCollectMessageProducer.getByBatch(totalOnlineNumBo.getBatchNumber(), totalOnlineNumBo.getUserId(), s);
                SocketCollectMessageBo bo = new SocketCollectMessageBo();
                bo.setUserId(totalOnlineNumBo.getUserId());
                bo.setSecUid(totalOnlineNumBo.getSecUid());
                bo.setBatchNumber(totalOnlineNumBo.getBatchNumber());
                bo.setTotalOnlineNum(CommonUtils.removeWanAdd(totalOnlineNumBo.getPeopleNum()));
                Date now = new Date();
                bo.setUpdateDate(now);
                bo.setVideoId(s);
                bo.setTenantId(totalOnlineNumBo.getTenantId());
                if (byBatch != null){
                    SocketCollectMessageBo bo1 = new SocketCollectMessageBo();
                    bo1.setId(byBatch.getId());
                    bo1.setTotalOnlineNum(bo.getTotalOnlineNum());
                    socketCollectMessageProducer.update(bo1);
                }else{
                    bo.setFileAddress("");
                    bo.setStartDate(now);
                    bo.setEndDate(now);
                    bo.setCreateDate(now);
                    socketCollectMessageProducer.save(bo);
                }
            }
        }
        return R.ok("修改成功");
    }

    /**
     * 删除直播总观看人次
     * @param id 直播总观看人次id
     * @return
     */
    public R<String> delete(Long id) {

        totalOnlineNumProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 新增或修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    public R<String> saveOrUpdate(TotalOnlineNumBo totalOnlineNumBo) {
        return update(totalOnlineNumBo);
    }

    public List<TotalOnlineNumEntity> listAllNum() {
        return totalOnlineNumProducer.listAllNum();
    }
}

