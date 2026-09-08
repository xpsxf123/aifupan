package com.jiuyu.replay.order.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.feign.agent.ChannelFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.order.bo.InvitationCodeBatchBo;
import com.jiuyu.replay.order.bo.InvitationCodeBatchListBo;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.TypeConsumptionBo;
import com.jiuyu.replay.order.producer.InvitationCodeBatchProducer;
import com.jiuyu.replay.order.producer.InvitationCodeProducer;
import com.jiuyu.replay.order.producer.PackageProducer;
import com.jiuyu.replay.order.producer.TypeConsumptionProducer;
import com.jiuyu.replay.order.vo.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 邀请码-批次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Component
public class InvitationCodeBatchBll {

    @Resource
    private InvitationCodeBatchProducer invitationCodeBatchProducer;
    @Resource
    private InvitationCodeProducer invitationCodeProducer;
    @Resource
    private PackageProducer packageProducer;
    @Resource
    private TypeConsumptionProducer typeConsumptionProducer;
    @Autowired
    private ChannelFeign channelFeign;


    /**
     * 邀请码-批次列表
     * @param invitationCodeBatchListBo 邀请码-批次列表查询参数
     * @return
     */
    public R<PageUtils<InvitationCodeBatchListVo>> queryPage(InvitationCodeBatchListBo invitationCodeBatchListBo) {

        PageUtils<InvitationCodeBatchListVo> invitationCodeBatchListVoPageUtils = invitationCodeBatchProducer.queryPage(invitationCodeBatchListBo);

        List<InvitationCodeBatchListVo> list = invitationCodeBatchListVoPageUtils.getList();

        if (list != null && !list.isEmpty()) {
            // 获取邀请码
            List<Long> codeBatchIds = list.stream().map(InvitationCodeBatchListVo::getId).toList();
            Map<Long, InvitationCodeBatchListVo> useStatusMap = this.invitationCodeProducer.codeCountByBatchId(codeBatchIds)
                    .stream()
                    .collect(Collectors.toMap(InvitationCodeBatchVo::getId, Function.identity(), (a, b) -> a));

            List<Long> channelIds = list.stream()
                    .map(InvitationCodeBatchListVo::getChannelId)
                    .filter(ObjectUtil::isNotEmpty)
                    .distinct()
                    .toList();
            Map<Long, String> channelNameMap = channelIds.isEmpty() ? new HashMap<>() : channelFeign.getChannelParentNameByIds(channelIds);

            for (InvitationCodeBatchListVo item : list) {

                // 获取渠道名称
                item.setChannelName(channelNameMap.get(item.getChannelId()));

                // 邀请码使用情况
                item.setCodeCount(0);
                item.setUseCodeCount(0);
                item.setNotUseCodeCount(0);
                InvitationCodeBatchListVo useStatus = useStatusMap.get(item.getId());
                if (ObjectUtil.isNotEmpty(useStatus)) {
                    item.setCodeCount(useStatus.getCodeCount());
                    item.setUseCodeCount(useStatus.getUseCodeCount());
                    item.setNotUseCodeCount(useStatus.getNotUseCodeCount());
                }
            }
        }

        return R.ok("获取成功", invitationCodeBatchListVoPageUtils);
    }

    /**
    * 邀请码-批次信息
    * @param id 邀请码-批次id
    * @return
    */
    public R<InvitationCodeBatchInfoVo> info(Long id) {

        // 获取邀请码批次信息
        InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = invitationCodeBatchProducer.info(id);
        if(invitationCodeBatchInfoVo != null) {
            // 获取邀请码列表
            List<InvitationCodeInfoVo> invitationCodeInfoVos = this.invitationCodeProducer.listByCodeBatchId(invitationCodeBatchInfoVo.getId());
            invitationCodeBatchInfoVo.setCodeList(invitationCodeInfoVos);
        }
        return R.ok("获取成功", invitationCodeBatchInfoVo);
    }

    /**
     * 新增邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(InvitationCodeBatchBo invitationCodeBatchBo) {
        boolean flag = false;
        // 获取版本信息
        if (invitationCodeBatchBo.getCommodityId() != null){
            PackageInfoVo info = packageProducer.info(invitationCodeBatchBo.getCommodityId());
            if (ObjectUtil.isNotEmpty(info)){
                invitationCodeBatchBo.setCommodityLevel(info.getLevel());
                invitationCodeBatchBo.setCommodityName(info.getName());
                flag = true;
            }
        }

        // 保存批次信息
        InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = invitationCodeBatchProducer.save(invitationCodeBatchBo);
        if (flag){
            // 添加邀请码资源
            List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(invitationCodeBatchBo.getCommodityId(), 1);
            if (ObjectUtil.isEmpty(typeConsumptionVos)) RRException.create("当前版本资源为空");
            typeConsumptionVos.forEach(item -> {
                item.setId(SnowflakeManager.nextValue());
                item.setType(2);
                item.setSourceId(invitationCodeBatchInfoVo.getId());
            });
            typeConsumptionProducer.saveOrUpdateBatch(BeanUtil.copyToList(typeConsumptionVos, TypeConsumptionBo.class));
        }

        // 保存邀请码列表
        List<InvitationCodeBo> invitationCodeBos = new LinkedList<>();
        for (int i = 0; i < invitationCodeBatchBo.getQuantity(); i++) {
            InvitationCodeBo invitationCodeBo = new InvitationCodeBo();
            invitationCodeBo.setBatchId(invitationCodeBatchInfoVo.getId());
            invitationCodeBo.setValidityStartDate(invitationCodeBatchBo.getValidityStartDate());
            invitationCodeBo.setValidityEndDate(invitationCodeBatchBo.getValidityEndDate());
            invitationCodeBo.setStatus(invitationCodeBatchBo.getStatus());
            invitationCodeBos.add(invitationCodeBo);
        }
        this.invitationCodeProducer.saveBatch(invitationCodeBos);

        return R.ok("添加成功");
    }

    /**
     * 修改邀请码-批次
     * @param invitationCodeBatchBo 邀请码-批次对象
     * @return
     */
    public R<String> update(InvitationCodeBatchBo invitationCodeBatchBo) {
        boolean flag = false;
        // 获取版本信息
        if (invitationCodeBatchBo.getCommodityId() != null){
            PackageInfoVo info = packageProducer.info(invitationCodeBatchBo.getCommodityId());
            if (ObjectUtil.isNotEmpty(info)){
                invitationCodeBatchBo.setCommodityLevel(info.getLevel());
                invitationCodeBatchBo.setCommodityName(info.getName());
                flag = true;
            }
        }

        invitationCodeBatchProducer.update(invitationCodeBatchBo);

        if (flag) {
            // 删除邀请码资源
            typeConsumptionProducer.deleteByIds(invitationCodeBatchBo.getId(), 2);
            // 添加邀请码资源
            List<TypeConsumptionVo> typeConsumptionVos = typeConsumptionProducer.listBySourceId(invitationCodeBatchBo.getCommodityId(), 1);
            if (ObjectUtil.isEmpty(typeConsumptionVos)) RRException.create("当前版本资源为空");
            typeConsumptionVos.forEach(item -> {
                item.setId(SnowflakeManager.nextValue());
                item.setType(2);
                item.setSourceId(invitationCodeBatchBo.getId());
            });
            typeConsumptionProducer.saveOrUpdateBatch(BeanUtil.copyToList(typeConsumptionVos, TypeConsumptionBo.class));
        }
        return R.ok("修改成功");
    }

    /**
     * 删除邀请码-批次
     * @param id 邀请码-批次id
     * @return
     */
    public R<String> delete(Long id) {

        invitationCodeBatchProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据邀请码批次导出该批次的所有邀请码
     * @param batchId
     * @return
     */
    public R<List<InvitationCodeListVo>> invitationByBatchId(Long batchId) {
        return invitationCodeBatchProducer.invitationByBatchId(batchId);
    }

}

