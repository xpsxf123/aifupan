package com.jiuyu.replay.api.logic.agent.impl;

import com.jiuyu.replay.agent.bll.AgentPlatformSaleBll;
import com.jiuyu.replay.agent.bll.InviteUrlCodeBll;
import com.jiuyu.replay.agent.vo.AgentPlatformSaleInfoVo;
import com.jiuyu.replay.api.logic.agent.InviteUrlCodeLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeBo;
import com.jiuyu.replay.generic.bo.agent.InviteUrlCodeListBo;
import com.jiuyu.replay.generic.feign.common.FileFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeListVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 邀请链接的code
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-05-10 19:12:27
 */
@Service
public class InviteUrlCodeLogicImpl implements InviteUrlCodeLogic {

    @Resource
    private InviteUrlCodeBll inviteUrlCodeBll;
    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private AgentPlatformSaleBll agentPlatformSaleBll;
    @Resource
    private FileBll fileBll;
    @Resource
    private SystemKvBll systemKvBll;
    @Resource
    private FileFeign fileFeign;


    @Override
    public R<PageUtils<InviteUrlCodeListVo>> queryPage(InviteUrlCodeListBo inviteUrlCodeListBo) {

        return inviteUrlCodeBll.queryPage(inviteUrlCodeListBo);
    }

    @Override
    public R<InviteUrlCodeInfoVo> info(Long id) {

        return inviteUrlCodeBll.info(id);
    }

    @Override
    public R<String> save(InviteUrlCodeBo inviteUrlCodeBo) {

        return inviteUrlCodeBll.save(inviteUrlCodeBo);
    }

    @Override
    public R<String> update(InviteUrlCodeBo inviteUrlCodeBo) {

        return inviteUrlCodeBll.update(inviteUrlCodeBo);
    }

    @Override
    public R<String> delete(Long id) {

        return inviteUrlCodeBll.delete(id);
    }

    @Override
    public R<InviteUrlCodeInfoVo> infoByCode(String code) {

        R<InviteUrlCodeInfoVo> inviteUrlCodeInfoVoR = this.inviteUrlCodeBll.infoByCode(code);

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = inviteUrlCodeInfoVoR.getData();
        if(inviteUrlCodeInfoVo != null) {

            if(inviteUrlCodeInfoVo.getCodeType() == 2) {
                // 客户端的用户邀请链接，优先分配邀请用户所属的平台销售id
                Long userId = inviteUrlCodeInfoVo.getUserId();
                R<UserDetailsInfoVo> userDetailsInfoVoR = this.userDetailsBll.getByUserId(userId);
                if(userDetailsInfoVoR.getData() != null) {
                    UserDetailsInfoVo userDetailsInfoVo = userDetailsInfoVoR.getData();
                    if(userDetailsInfoVo.getSaleId() != null && !userDetailsInfoVo.getSaleId().equals(0L)) {
                        // 判断当前销售是否有关联到当前代理商
                        Long saleId = userDetailsInfoVo.getSaleId();
                        R<AgentPlatformSaleInfoVo> platformSaleInfoVoR = this.agentPlatformSaleBll.infoByAgentIdAndSaleId(inviteUrlCodeInfoVo.getAgentId(), saleId);
                        if(platformSaleInfoVoR.getData() != null) {
                            // 查询平台销售信息
                            SalesInfoVo sales = ResultUtil.getResult(this.salesBll.info(saleId));
                            if (sales != null && sales.getIsChoose() == 1) {
                                // 设置销售id
                                inviteUrlCodeInfoVo.setSaleId(saleId);
                                // 设置销售的图片
                                R<SalesInfoVo> salesInfoVoR = this.salesBll.info(saleId);
                                SalesInfoVo salesInfoVo = salesInfoVoR.getData();
                                if (salesInfoVo != null && salesInfoVo.getQrcodeImgId() != null) {
                                    inviteUrlCodeInfoVo.setSaleQrcodeImg(this.fileBll.infoByFileId(salesInfoVo.getQrcodeImgId()));
                                }
                            }
                        }
                    }
                }
            }

            // 根据代理商设置平台销售
//            if(inviteUrlCodeInfoVo.getSaleId() == null) {
//                // 轮询分配平台销售
//                R<AgentPlatformSaleInfoVo> platformSaleInfoVoR = this.agentPlatformSaleBll.getPollingSaleId(inviteUrlCodeInfoVo.getAgentId());
//                if(platformSaleInfoVoR.getData() != null) {
//                    setInviteUrlCodeAndSaleQrcodeImg(inviteUrlCodeInfoVo, platformSaleInfoVoR.getData().getSaleId());
//                }
//            }

            // 如果还是没有销售，就轮询全平台的销售
//            if (inviteUrlCodeInfoVo.getSaleId() == null) {
//                setInviteUrlCodeAndSaleQrcodeImg(inviteUrlCodeInfoVo, this.salesBll.getPollingSaleId());
//            }

            // 全平台的销售也没有就使用保底二维码
//            if(inviteUrlCodeInfoVo.getSaleQrcodeImg() == null) {
//                // 分配一个默认二维码图片
//                R<SystemKvInfoVo> kvInfoVoR = this.systemKvBll.getByKey("h5_default_qrcode_img_id");
//                if(kvInfoVoR.getData() != null) {
//                    SystemKvInfoVo systemKvInfoVo = kvInfoVoR.getData();
//                    inviteUrlCodeInfoVo.setSaleQrcodeImg(this.fileBll.infoByFileId(Long.valueOf(systemKvInfoVo.getKvValue())));
//                }
//            }
        }

        return inviteUrlCodeInfoVoR;
    }

    /**
     * 根据平台销售id设置对应的销售二维码图片
     *
     * @param inviteUrlCodeInfoVo obj
     * @param saleId              销售id
     */
    private void setInviteUrlCodeAndSaleQrcodeImg(InviteUrlCodeInfoVo inviteUrlCodeInfoVo, Long saleId) {
        if (saleId != null) {
            inviteUrlCodeInfoVo.setSaleId(saleId);
            // 封装销售二维码图片
            R<SalesInfoVo> salesInfoVoR = this.salesBll.info(saleId);
            SalesInfoVo salesInfoVo = salesInfoVoR.getData();
            if (salesInfoVo != null && salesInfoVo.getQrcodeImgId() != null) {
                inviteUrlCodeInfoVo.setSaleQrcodeImg(this.fileBll.infoByFileId(salesInfoVo.getQrcodeImgId()));
            }
        }
    }

    @Override
    public R<String> getUserInviteUrl() {

//        UserCacheVo user = GlobalObject.getLocalUser();
//        Long tenantId = user.getActiveTenantId();
//        Long userId = user.getId();
//        if(user.getUserType() == 2) {
//            // 子账号，用父账号的用户id
//            userId = user.getParentId();
//        }
//
//        return this.inviteUrlCodeBll.getUserInviteUrl(userId, tenantId);
        return R.ok();
    }


}

