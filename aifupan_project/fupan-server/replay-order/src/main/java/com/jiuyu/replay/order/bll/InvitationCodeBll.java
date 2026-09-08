package com.jiuyu.replay.order.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.order.bo.InvitationCodeBo;
import com.jiuyu.replay.order.bo.InvitationCodeListBo;
import com.jiuyu.replay.order.constant.Constant;
import com.jiuyu.replay.order.entity.InvitationUsageRecordEntity;
import com.jiuyu.replay.order.producer.InvitationCodeBatchProducer;
import com.jiuyu.replay.order.producer.InvitationCodeProducer;
import com.jiuyu.replay.order.repository.service.InvitationUsageRecordService;
import com.jiuyu.replay.order.vo.InvitationCodeBatchInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeInfoVo;
import com.jiuyu.replay.order.vo.InvitationCodeListVo;
import com.jiuyu.replay.order.vo.InvitationCodeVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 邀请码
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-15 15:57:30
 */
@Component
public class InvitationCodeBll {

    @Resource
    private InvitationCodeProducer invitationCodeProducer;
    @Resource
    private InvitationCodeBatchProducer invitationCodeBatchProducer;
    @Resource
    private InvitationUsageRecordService invitationUsageRecordService;


    /**
     * 邀请码列表
     * @param invitationCodeListBo 邀请码列表查询参数
     * @return
     */
    public R<PageUtils<InvitationCodeListVo>> queryPage(InvitationCodeListBo invitationCodeListBo) {

        PageUtils<InvitationCodeListVo> invitationCodeListVoPageUtils = invitationCodeProducer.queryPage(invitationCodeListBo);

        List<InvitationCodeListVo> list = invitationCodeListVoPageUtils.getList();
        if(list != null && list.size() > 0) {
            // 封装批次名称
            Set<Long> codeBatchIds = list.stream().map(InvitationCodeVo::getBatchId).collect(Collectors.toSet());
            List<InvitationCodeBatchInfoVo> invitationCodeBatchInfoVos = this.invitationCodeBatchProducer.listByIds(codeBatchIds);
            if(invitationCodeBatchInfoVos != null && invitationCodeBatchInfoVos.size() > 0) {
                for (InvitationCodeListVo invitationCodeListVo : list) {
                    for (InvitationCodeBatchInfoVo invitationCodeBatchInfoVo : invitationCodeBatchInfoVos) {
                        if(invitationCodeListVo.getBatchId().equals(invitationCodeBatchInfoVo.getId())) {
                            invitationCodeListVo.setBatchName(invitationCodeBatchInfoVo.getName());
                            invitationCodeListVo.setCommodityValidityNum(invitationCodeBatchInfoVo.getCommodityValidityNum());
                            invitationCodeListVo.setCommodityValidityUnit(invitationCodeBatchInfoVo.getCommodityValidityUnit());
                            invitationCodeListVo.setPackageName(invitationCodeBatchInfoVo.getCommodityName());
                            invitationCodeListVo.setValidityStartDate(invitationCodeBatchInfoVo.getValidityStartDate());
                            invitationCodeListVo.setValidityEndDate(invitationCodeBatchInfoVo.getValidityEndDate());
                            break;
                        }
                    }
                }
            }

        }

        return R.ok("获取成功", invitationCodeListVoPageUtils);
    }

    /**
    * 邀请码信息
    * @param id 邀请码id
    * @return
    */
    public R<InvitationCodeInfoVo> info(Long id) {

        InvitationCodeInfoVo invitationCodeInfoVo = invitationCodeProducer.info(id);
        return R.ok("获取成功", invitationCodeInfoVo);
    }

    /**
     * 新增邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    public R<String> save(InvitationCodeBo invitationCodeBo) {

        InvitationCodeInfoVo invitationCodeInfoVo = invitationCodeProducer.save(invitationCodeBo);
        return R.ok("添加成功");
    }

    /**
     * 修改邀请码
     * @param invitationCodeBo 邀请码对象
     * @return
     */
    public R<String> update(InvitationCodeBo invitationCodeBo) {

        invitationCodeProducer.update(invitationCodeBo);
        return R.ok("修改成功");
    }

    /**
     * 删除邀请码
     * @param id 邀请码id
     * @return
     */
    public R<String> delete(Long id) {

        invitationCodeProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 检查邀请码是否可用
     * @param invitationCode 邀请码
     * @return
     *
     */
    public R<InvitationCodeInfoVo> infoByCode(String invitationCode) {
        InvitationCodeInfoVo invitationCodeInfoVo = invitationCodeProducer.infoByCode(invitationCode);
        return R.ok("获取成功", invitationCodeInfoVo);
    }


    /**
     * 检查邀请码是否可用
     * @param userId 用户id
     * @param invitationCode 邀请码
     * @return
     */
    public R<InvitationCodeInfoVo> checkInvitationCode(Long userId, String invitationCode) {
        // 获取邀请码信息
        InvitationCodeInfoVo invitationCodeInfoVo = this.invitationCodeProducer.infoByCode(invitationCode);
        if (invitationCodeInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码不正确，请重新输入");
        }
        if (invitationCodeInfoVo.getStatus() != 0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码已被禁用，请重新输入");
        }
        if (invitationCodeInfoVo.getUseStatus() == 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码已被使用，请重新输入");
        }
        long currentTime = new Date().getTime();
        if (invitationCodeInfoVo.getValidityStartDate().getTime() > currentTime) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码还没有到使用时间，请稍后再试");
        }
        if (invitationCodeInfoVo.getValidityEndDate().getTime() < currentTime) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码已过期，请重新输入");
        }
        // 获取邀请码批次信息
        InvitationCodeBatchInfoVo invitationCodeBatchInfoVo = this.invitationCodeBatchProducer.info(invitationCodeInfoVo.getBatchId());
        if (invitationCodeBatchInfoVo == null || invitationCodeBatchInfoVo.getStatus() == 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码批次禁用，请换一批邀请使用");
        }
        // 判断是否使用过免费的邀请码
        if (invitationCodeBatchInfoVo.getIsGratis() == 1) {
            // 当前邀请码是免费的，查找用户之前有没有已经使用过免费的邀请码，使用过则不允许使用
            List<InvitationCodeInfoVo> invitationCodeInfoList = this.invitationCodeProducer.listByUserId(userId);
            if (invitationCodeInfoList != null && !invitationCodeInfoList.isEmpty()) {
                List<Long> batchIds = invitationCodeInfoList.stream().map(InvitationCodeVo::getBatchId).toList();
                List<InvitationCodeBatchInfoVo> invitationCodeBatchInfoVoList = this.invitationCodeBatchProducer.listByIds(batchIds);
                // 当前用户有使用过免费的邀请码，不允许使用
                if (invitationCodeBatchInfoVoList.stream().anyMatch(item -> item.getIsGratis() == 1)) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "您已经使用过免费的邀请码，不能在次使用");
                }
            }
        }

        // 检查是否已经使用过
        long count = invitationUsageRecordService.count(new LambdaQueryWrapper<InvitationUsageRecordEntity>()
                .eq(InvitationUsageRecordEntity::getUserId, userId)
                .eq(InvitationUsageRecordEntity::getInvitationCodeId, invitationCodeInfoVo.getId())
        );
        if (count >= 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "邀请码您已使过，不能重复使用");
        }
        invitationCodeInfoVo.setInvitationCodeBatchVo(invitationCodeBatchInfoVo);
        return R.ok(invitationCodeInfoVo);
    }

    /***
     * 使用邀请码
     * @param userId 用户id
     * @param orderId 订单id
     * @param codeId 邀请码id
     */
    public R<String> useInvitation(Long userId, Long orderId, Long codeId){
        this.invitationCodeProducer.useInvitation(userId, orderId, codeId);
        return R.ok("使用成功");
    }



    /**
     * 邀请码导出
     * @param invitationCodeBo
     * @return
     */
    public R<List<InvitationCodeListVo>> exportInvitation(List<InvitationCodeBo> invitationCodeBo) {
        return invitationCodeProducer.exportInvitation(invitationCodeBo);
    }

    /**
     * 修改已经导出的邀请码状态
     * @param ids
     * @return
     */
    public R<String> updateIsLssued(List<Long> ids) {
        invitationCodeProducer.updateIsLssued(ids);
        return R.ok("导出成功");
    }

    public void saveInvitationUsageRecord(InvitationUsageRecordEntity entity) {
        entity.setId(SnowflakeManager.nextValue());
        Date now = new Date();
        entity.setCreateDate(now);
        entity.setUpdateDate(now);
        invitationUsageRecordService.save(entity);
    }
}

