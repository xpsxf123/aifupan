package com.jiuyu.replay.api.logic.power.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.activity.bll.UserInviteBll;
import com.jiuyu.replay.agent.bll.*;
import com.jiuyu.replay.agent.bo.AgentPromotionBo;
import com.jiuyu.replay.agent.producer.AgentProducer;
import com.jiuyu.replay.agent.producer.ChannelProducer;
import com.jiuyu.replay.agent.vo.AgentInfoVo;
import com.jiuyu.replay.agent.vo.AgentSaleInfoVo;
import com.jiuyu.replay.agent.vo.ChannelInfoVo;
import com.jiuyu.replay.agent.vo.InviteUrlPromotionVo;
import com.jiuyu.replay.ai.bll.ConversationBll;
import com.jiuyu.replay.api.constant.Constant;
import com.jiuyu.replay.api.controller.openapi.governance.response.OpenGovernanceUserDetailsInfo;
import com.jiuyu.replay.api.logic.power.UserLogic;
import com.jiuyu.replay.api.utils.GetIPUtils;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.common.cache.ResilientRedisTemplate;
import com.jiuyu.replay.api.service.agentusage.AgentUsageService;
import com.jiuyu.replay.api.service.agentusage.dto.AgentUsage;
import com.jiuyu.replay.api.service.agentusage.dto.UserTenantPair;
import com.jiuyu.replay.common.constant.UserEnums;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.properties.RocketMqProperties;
import com.jiuyu.replay.common.repository.service.CacheFallbackDataService;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.activity.UserInviteBo;
import com.jiuyu.replay.generic.constant.activity.InviteRewardRuleCodeEnum;
import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeAndPromotionName;
import com.jiuyu.replay.generic.vo.agent.InviteUrlCodeInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderDetailInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderListVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.RegisterVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.*;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.CreateOrderBo;
import com.jiuyu.replay.order.bo.OrderBo;
import com.jiuyu.replay.order.bo.SubAccountListBo;
import com.jiuyu.replay.order.producer.PackageProducer;
import com.jiuyu.replay.order.vo.*;
import com.jiuyu.replay.power.bll.*;
import com.jiuyu.replay.power.bo.*;
import com.jiuyu.replay.power.constant.PowerProperties;
import com.jiuyu.replay.power.entity.UserEntity;
import com.jiuyu.replay.power.producer.SalesProducer;
import com.jiuyu.replay.power.producer.UserProducer;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.*;
import com.jiuyu.replay.third.bll.MsgBll;
import com.jiuyu.replay.video.project.producer.VideoExtractProducer;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import com.jiuyu.replay.words.bll.SyncContrastBll;
import com.jiuyu.replay.words.bll.TradeBll;
import com.jiuyu.replay.words.bll.UserAnalysisRollupBll;
import com.jiuyu.replay.words.bo.AnchorUrlBo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.entity.UserAnalysisRollupEntity;
import com.jiuyu.replay.words.producer.TradeProducer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class UserLogicImpl implements UserLogic {

    @Resource
    private UserBll userBll;
    @Resource
    private MsgBll msgBll;
    @Resource
    private AnchorUrlBll anchorUrlBll;


    @Resource
    private FileBll fileBll;
    @Resource
    private CommodityBll commodityBll;
    @Resource
    private PackageBll packageBll;
    @Resource
    private PackageProducer packageProducer;
    @Resource
    TradeBll tradeBll;
    @Resource
    TradeProducer tradeProducer;
    @Resource
    CompanyBll companyBll;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private OrderBll orderBll;
    @Resource
    private InvitationCodeBll invitationCodeBll;
    @Resource
    private InvitationCodeBatchBll invitationCodeBatchBll;
    @Autowired
    private UserPropertyBll userPropertyBll;
    @Resource
    private PowerProperties powerProperties;
    @Autowired
    private UserDetailsBll userDetailsBll;
    @Autowired
    private BindingAccountBll bindingAccountBll;
    @Autowired
    private UserLoginLogBll userLoginLogBll;
    @Resource
    private UserService userService;
    @Resource
    private TenantBll tenantBll;
    @Resource
    private ChannelBll channelBll;
    @Resource
    private ChannelProducer channelProducer;
    @Resource
    private SalesBll salesBll;
    @Resource
    private UserAnalysisRollupBll userAnalysisRollupBll;
    @Resource
    private SyncContrastBll syncContrastBll;
    @Autowired
    private OrderDetailBll orderDetailBll;
    @Resource
    private AgentBll agentBll;
    @Resource
    private AgentPromotionBll agentPromotionBll ;
    @Resource
    private InviteUrlCodeBll inviteUrlCodeBll;
    @Resource
    private AgentSaleBll agentSaleBll;
    @Resource
    private UserFeign userFeign;
    @Resource
    private UserInviteBll userInviteBll;
    @Resource
    private OrderFeign orderFeign;
    @Resource
    RocketMqBll rocketMqBll;
    @Resource
    RocketMqProperties rocketMqProperties;
    @Resource
    CacheFallbackDataService cacheFallbackDataService;
    @Resource
    ResilientRedisTemplate<String, Object> resilientRedisTemplate;
    @Resource
    AgentUsageService agentUsageService;

    @Resource
    private AgentPlatformSaleBll agentPlatformSaleBll;
    @Resource
    private VideoExtractProducer videoExtractProducer;
    @Resource
    private UserProducer userProducer;
    @Resource
    private SalesProducer salesProducer;
    @Resource
    private FileProducer fileProducer;
    @Autowired
    private SystemKvProducer systemKvProducer;
    @Resource
    private AgentProducer agentProducer;
    @Autowired
    private UserPropertyFeign userPropertyFeign;
    @Autowired
    private ConversationBll conversationBll;
    @Resource
    private UserRemarkBll userRemarkBll;

    @Override
    public R<UserLoginVo> login(LoginBo loginBo, HttpServletRequest request) {

        String loginIp = GetIPUtils.getIpAddr(request);

        return userBll.login(loginBo, loginIp);
    }

    @Override
    public R<String> updatePassword(UpdatePasswordBo updatePasswordBo) {

        return userBll.updatePassword(updatePasswordBo);
    }

    @Override
    public R<PageUtils<UserListVo>> selectClientList(UserListBo userListBo) {

        return userBll.selectClientList(userListBo);
    }

    @Override
    public R<String> resetPassword(Long id) {

        return userBll.resetPassword(id);
    }

    @Override
    public R<UserInfoVo> info(Long id, boolean loadPassword) {

        return userBll.info(id, loadPassword);
    }

    @Override
    @Transactional
    public R<String> delete(Long id) {
        // 检查是否后台管理员，
        UserInfoVo user = ResultUtil.getResult(userBll.info(id, false));
        if (user == null) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "用户不存在");
        }

        if (ObjectUtil.equals(user.getUserType(), UserEnums.userType.MANAGE_ADMIN_USER.getCode())) {
            // 检查是否关联销售或代理商
            if (user.getAdminUserType() == UserEnums.adminUserType.ADMIN.getCode() || user.getAdminUserType() == UserEnums.adminUserType.AGENT_SALE.getCode()) {
                SalesInfoVo sales = salesProducer.getBySalesUserId(id);
                if (sales != null) {
                    throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "后台用户已关联销售，请先删除对应的销售");
                }
            } else if (user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode()) {
                AgentInfoVo agent = agentProducer.getBySalesUserId(id);
                if (agent != null) {
                    throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "后台用户已关联代理商，不能删除");
                }
            }
        }


        // 删除用户
        R<String> delete = userBll.delete(id);

        // 删除用户资产
        userPropertyBll.deleteByUserId(id);
        return delete;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<RegisterVo> register(RegisterBo registerBo) {
        // 校验验证码
        R<String> stringR = this.userBll.registerTheCheck(registerBo);
        if (stringR.getCode() != 0) return R.error(stringR.getCode(), stringR.getMsg());

        // 判断邀请码是否可以用
        if (ObjectUtil.isNotEmpty(registerBo.getInvitationCode())) {
            R<InvitationCodeInfoVo> invitationCodeInfoVoR1 = invitationCodeBll.infoByCode(registerBo.getInvitationCode());
            if (invitationCodeInfoVoR1.getCode() != 0 || invitationCodeInfoVoR1.getData() == null)
                return R.error(3001, "邀请码无效");
            if (invitationCodeInfoVoR1.getData().getStatus() == 1) return R.error(3001, "邀请码无效");
            InvitationCodeInfoVo invitationCode = invitationCodeInfoVoR1.getData();

            R<InvitationCodeBatchInfoVo> batchInfoVoR = invitationCodeBatchBll.info(invitationCode.getBatchId());
            if (batchInfoVoR.getCode() != 0 || batchInfoVoR.getData() == null) return R.error(3001, "邀请码无效");
            if (batchInfoVoR.getData().getStatus() == 1) return R.error(3001, "邀请码无效");
            InvitationCodeBatchInfoVo invitationCodeBatch = batchInfoVoR.getData();

            if (invitationCode.getUseStatus() == 1) {
                return R.error(3001, "当前邀请码已使用过，不能再次使用");
            }

            long currentTime = System.currentTimeMillis();
            if (invitationCode.getValidityStartDate().getTime() > currentTime) {
                return R.error(3001, "邀请码未到使用时间");
            }
            if (invitationCode.getValidityEndDate().getTime() < currentTime) {
                return R.error(3001, "邀请码已过期");
            }
        }

        InviteUrlCodeInfoVo inviteUrlCodeInfoVo = null;
        if (!StringUtils.isEmpty(registerBo.getInviteUrlCode())) {
            log.info("[轮询销售] 开始获取销售，邀请链接的code = {}, phone = {}",  registerBo.getInviteUrlCode(), registerBo.getPhone());
            R<InviteUrlCodeInfoVo> inviteUrlCodeR = this.inviteUrlCodeBll.getByInviteUrlCode(registerBo.getInviteUrlCode());
            if (inviteUrlCodeR.getData() != null) {
                inviteUrlCodeInfoVo = inviteUrlCodeR.getData();
                if (inviteUrlCodeInfoVo.getCodeType() == 3) {
                    registerBo.setAgentSaleId(inviteUrlCodeInfoVo.getAgentSaleId());
                }
                R<AgentInfoVo> agentInfoVoR = agentBll.info(inviteUrlCodeInfoVo.getAgentId());
                if (agentInfoVoR.getData() != null) {
                    AgentInfoVo agentInfoVo = agentInfoVoR.getData();
                    registerBo.setChannelId(agentInfoVo.getChannelId());
                    registerBo.setAgentId(agentInfoVo.getId());
                }

                if (registerBo.getSaleId() != null) {
                    log.info("[轮询销售] registerBo.getSaleId() != null, saleId = {}",  registerBo.getSaleId());
                    SalesInfoVo info = salesProducer.info(registerBo.getSaleId());
                    if (info == null || ObjectUtil.equals(info.getIsChoose(), 0)) {
                        log.warn("[爱复盘用户注册] 没有查询到销售或者对应的销售已关闭分配线索 salesId = {}", registerBo.getSaleId());
                        registerBo.setSaleId(null);
                    }
                }

                // 处理有邀请链接的code没有平台销售的情况
                if (registerBo.getSaleId() == null) {
                    log.info("[轮询销售] 有邀请链接的code没有平台销售，开始轮询分配平台销售");
                    // 轮询分配平台销售
                    registerBo.setSaleId(this.agentPlatformSaleBll.getPollingSaleId(inviteUrlCodeInfoVo.getAgentId()));
                    log.info("[轮询销售] 平台销售为 id = {}", registerBo.getSaleId());
                }
            } else {
                registerBo.setInviteUrlCode(null);
            }
        }

        // 用户注册
        R<UserVo> userVoR = userBll.register(registerBo);
        if (userVoR.getCode() == 0 && userVoR.getData() != null) {
            UserVo userVo = userVoR.getData();

            if (inviteUrlCodeInfoVo != null) {
                // 保存邀请记录
                UserInviteBo userInviteBo = new UserInviteBo();
                userInviteBo.setInviteUserId(inviteUrlCodeInfoVo.getUserId());
                userInviteBo.setInviteSubUserId(inviteUrlCodeInfoVo.getSubUserId());
                userInviteBo.setPassiveUserId(userVo.getId());
                userInviteBo.setInviteCode(registerBo.getInviteUrlCode());
                userInviteBo.setInviteType(inviteUrlCodeInfoVo.getCodeType());
                this.userInviteBll.save(userInviteBo);
            }

            // 创建资产
            userPropertyBll.checkUserPropertyAndCreate(List.of(userVo.getId()), 1);

            // 开始时用户会自动下一单免费的订单
            CreateOrderBo cBo = new CreateOrderBo();
            cBo.setUserId(userVo.getId());
            cBo.setUserName(userVo.getNickName());
            // 检查
            R<PackageInfoVo> gratisPackage = packageBll.getPackageInit();
            if (gratisPackage.getCode() != 0) return R.error(3001, gratisPackage.getMsg());
            PackageInfoVo packageData = gratisPackage.getData();
            cBo.setBeforeUpgrading(null);
            cBo.setCommodityId(packageData.getId());
            cBo.setCommodityPriceId(packageData.getCommodityPriceList().get(0).getId());
            cBo.setDiscountRate(0);
            cBo.setSource(1);
            cBo.setOrderType(0);
            cBo.setCommodityType(1);
            // 邀请有礼相关MQ消息dto
            UserInviteMqDto userInviteMqDto = new UserInviteMqDto(InviteRewardRuleCodeEnum.REGISTER.getCode(), userVo.getId(), RequestContext.getFingerprint());
            cBo.setUserInviteMqDto(userInviteMqDto);
            // 下单&邀请有关逻辑
            rocketMqBll.syncSendNormalMessage(userVo.getId(), rocketMqProperties.getTagUserFirstOrder(), IdUtil.simpleUUID(), JSON.toJSONString(cBo));

//            CreateOrderVo order = orderBll.createOrder(cBo);
//            // 手动的单，直接订单完成接口
//            if (ObjectUtil.isNotEmpty(order)) {
//                orderBll.successOrder(order.getOrderId(), true);
//            }

            // 注册成功后，发送销售获客助手短信
            rocketMqBll.syncSendNormalMessage(userVo.getId(), rocketMqProperties.getTagUserRegisterSendEmail(), IdUtil.simpleUUID(), userVo.getId() + "", LocalDateTime.now().plusMinutes(2));

            // 邀请码
            if (!StringUtils.isEmpty(registerBo.getInvitationCode())) {
                // 检查邀请码是否可用
                R<InvitationCodeInfoVo> invitationCodeInfoVoR = this.invitationCodeBll.checkInvitationCode(userVo.getId(), registerBo.getInvitationCode());
                if (invitationCodeInfoVoR.getCode() == 0) {
                    InvitationCodeInfoVo data = invitationCodeInfoVoR.getData();

                    CreateOrderBo orderBo = createOrderBo(data, userVo.getId(), userVo.getNickName());
                    // 下单
                    CreateOrderVo orderVo = orderBll.createOrder(orderBo);
                    // 完成订单-更新数据库
                    // 手动的单，直接订单完成接口
                    if (ObjectUtil.isNotEmpty(orderVo)) {
                        orderBll.successOrder(orderVo.getOrderId(), orderBo.getOrderType() != 3);
                    }

                    // 跟新邀请码状态-是否是无限次
                    if (data.getInvitationCodeBatchVo().getIsInfinite() == 0) {
                        invitationCodeBll.useInvitation(userVo.getId(), orderVo.getOrderId(), data.getId());
                    }

                }
            }

            // 返回销售二维码
            RegisterVo result = new RegisterVo();
            if (ObjectUtil.isNotEmpty(registerBo.getSaleId())) {
                result.setSaleId(registerBo.getSaleId());
                SalesInfoVo sales = salesProducer.info(result.getSaleId());
                if (sales != null && ObjectUtil.isNotEmpty(sales.getQrcodeImgId())) {
                    result.setSaleQrcodeImg(this.fileBll.infoByFileId(sales.getQrcodeImgId()));
                }
            }
            if (ObjectUtil.isEmpty(result.getSaleQrcodeImg())) {
                SystemKvInfoVo kv = systemKvProducer.getByKey("h5_default_qrcode_img_id");
                if (kv != null) {
                    result.setSaleQrcodeImg(this.fileBll.infoByFileId(Long.valueOf(kv.getKvValue())));
                }
            }

            return R.ok(userVoR.getMsg(), result);
        }

        return R.error(userVoR.getCode(), userVoR.getMsg());
    }

    /**
     * 构建下单的参数
     *
     * @param data
     * @return
     */
    public CreateOrderBo createOrderBo(InvitationCodeInfoVo data, Long userId, String nickName) {

        CreateOrderBo result = new CreateOrderBo();
        result.setUserId(userId);
        result.setUserName(nickName);

        // 价格
        CommodityPriceInfoVo priceVo = new CommodityPriceInfoVo();
        priceVo.setValidityNum(data.getInvitationCodeBatchVo().getCommodityValidityNum());
        priceVo.setValidityUnit(data.getInvitationCodeBatchVo().getCommodityValidityUnit());
        priceVo.setDiscount(BigDecimal.valueOf(1));
        priceVo.setOriginalPrice(data.getInvitationCodeBatchVo().getCommodityRealPrice());
        priceVo.setRealPrice(data.getInvitationCodeBatchVo().getCommodityRealPrice());
        result.setPriceVo(priceVo);

        result.setSource(2);
        result.setPayType(-1);
        // 设置类型
        if (data.getInvitationCodeBatchVo().getType() == 2) {
            R<OrderInfoVo> currentOrder = orderBll.currentOrderByUserId(userId);
            result.setBeforeUpgrading(currentOrder.getData().getId());
            result.setCommodityType(1);
            result.setOrderType(0);
        } else {
            if (data.getInvitationCodeBatchVo().getCommodityType() == 1) {
                // 这里是活动的订单
                result.setCommodityType(2);
                result.setOrderType(5);
            } else {
                R<OrderInfoVo> currentOrder = orderBll.currentOrderByUserId(userId);
                // 判断当前用户版本的level是否大于0，如果是-1或者是0，订单就是升级订单
                if (currentOrder.getData().getLevel() <= 0) {
                    result.setBeforeUpgrading(currentOrder.getData().getId());
                    result.setCommodityType(1);
                    result.setOrderType(2);
                } else {
                    // 如果大于0，用户版本等于邀请码版本，就是续费订单，否则就是不给下单
                    R<PackageInfoVo> bllBtId = packageBll.getBtId(data.getInvitationCodeBatchVo().getCommodityId());
                    if (bllBtId.getCode() != 0 || ObjectUtil.isEmpty(bllBtId.getData()))
                        RRException.create("商品查询失败");
                    PackageInfoVo packageBo = bllBtId.getData();

                    if (Objects.equals(currentOrder.getData().getLevel(), packageBo.getLevel())) {
                        result.setBeforeUpgrading(currentOrder.getData().getId());
                        result.setCommodityType(1);
                        result.setOrderType(3);
                    } else {
                        RRException.create("邀请码版本和用户版本不一致，不能使用");
                    }
                }
            }
        }
        result.setCommodityId(data.getInvitationCodeBatchVo().getCommodityId());
        result.setDiscountRate(0);
        result.setSource(2);
        return result;
    }

    @Override
    public R<String> getPhoneCode(String phone) {
        R<String> phoneCode = userBll.getPhoneCode(phone);
        // 发送验证码
        msgBll.sendMsg(phone, phoneCode.getData());

        return R.ok("获取验证码成功");
    }

    @Override
    public R<String> save(UserAddBo userAddBo) {
        // 保存用户信息
        R<UserVo> userVoR = userBll.save(userAddBo);
        if (userVoR.getCode() == 0 && userVoR.getData() != null) {
            UserVo userVo = userVoR.getData();
            return R.ok(userVoR.getMsg());
        }

        return R.error(userVoR.getCode(), userVoR.getMsg());
    }

    @Override
    public R<String> update(UserUpdateBo userUpdateBo) {

        UserInfoVo user = ResultUtil.getResult(userBll.info(userUpdateBo.getId(), false));
        if (user == null) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "用户不存在");
        }
        // 同步修改代理商和销售的名称和手机号
        if (user.getAdminUserType() == UserEnums.adminUserType.ADMIN.getCode() || user.getAdminUserType() == UserEnums.adminUserType.AGENT_SALE.getCode()) {
            salesBll.updateNameAndPhoneByUserId(userUpdateBo.getId(), userUpdateBo.getNickName(), userUpdateBo.getPhone());
        } else if (user.getAdminUserType() == UserEnums.adminUserType.AGENT.getCode()) {
            agentBll.updateNameAndPhoneByUserId(userUpdateBo.getId(), userUpdateBo.getNickName(), userUpdateBo.getPhone());
        }
        return userBll.update(userUpdateBo);
    }

    @Override
    public R<UserInfoVo> updateByClient(UserUpdateClientBo userUpdateClientBo) {

        R<UserInfoVo> res = userBll.updateByClient(userUpdateClientBo);
        if (!res.success()) {
            return res;
        }
        return infoByClient();
    }

    /**
     * 修改手机号
     *
     * @param userId    用户id
     * @param newMobile 新手机号
     * @param tenantId  租户id
     *
     * @return {@link R }<{@link Void }>
     */
    @Override
    public R<Void> updateMobile(long userId, String newMobile, Long tenantId) {
        return userBll.updateMobile(userId, newMobile, tenantId);
    }

    @Override
    public R<UserInfoVo> infoByClient() {

        UserCacheVo localUser = GlobalObject.getLocalUser();
        R<UserInfoVo> userInfoVoR = userBll.infoByClient(localUser.getId());
        if (userInfoVoR.getCode() == 0 && userInfoVoR.getData() != null) {

            userInfoVoR.getData().setNormalPhone(localUser.getPhone());

            Long parentUserId = userInfoVoR.getData().getParentId();
            R<List<OrderInfoVo>> orderByUserR = orderBll.getOrderByUserId(parentUserId == null || parentUserId == 0L ? userInfoVoR.getData().getId() : parentUserId);
            if (orderByUserR.getCode() == 0 && ObjectUtil.isNotEmpty(orderByUserR.getData())) {
                OrderInfoVo endOrder = orderByUserR.getData().get(orderByUserR.getData().size() - 1);
                OrderInfoVo one = orderByUserR.getData().get(0);
                userInfoVoR.getData().setPackageLevel(one.getLevel());
                userInfoVoR.getData().setPackageName(one.getCommodityName());
                userInfoVoR.getData().setExpirationDate(CommonUtils.getResourceUpdateTime(endOrder.getEndDate()));
                OrderInfoVo startOrder = orderByUserR.getData()
                        .stream()
                        .filter(item -> ObjectUtil.equals(item.getStatus(), 2))
                        .findFirst().orElse(null);
                R<List<OrderDetailInfoVo>> listR = orderDetailBll.listByOrderId(startOrder.getId());
                userInfoVoR.getData().setResourceUpdateTime(CommonUtils.getResourceUpdateTime(endOrder.getEndDate()));
                if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())) {
                    OrderDetailInfoVo orderDetail = listR.getData().stream().filter(item -> ObjectUtil.equals(item.getCommodityTypeReset(), 1) && ObjectUtil.isNotEmpty(item.getNextReset())).findFirst().orElse(null);
                    if (orderDetail != null) {
                        Date resourceUpdateTime = CommonUtils.getResourceUpdateTime(orderDetail.getNextReset());
                        if (resourceUpdateTime != null && resourceUpdateTime.getTime() < userInfoVoR.getData().getExpirationDate().getTime()) {
                            userInfoVoR.getData().setResourceUpdateTime(resourceUpdateTime);
                        }
                    }
                }

                R<PackageInfoVo> info = packageBll.getBtId(one.getCommodityId());
                if (info.getCode() == 0 && ObjectUtil.isNotEmpty(info.getData())) {
                    userInfoVoR.getData().setLogoImgAddress(info.getData().getLogoImgs());
                }

            }
        }
        return userInfoVoR;
    }

    @Override
    public R<String> checkPhoneCode(String phone, String code, Integer isNewPhone) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return userBll.checkPhoneCode(phone, code, user.getPhone(), isNewPhone);
    }

    @Override
    public R<String> logout(HttpServletRequest request) {

        String ip = GetIPUtils.getIpAddr(request);
        String token = request.getHeader("Token");
        log.info("退出登录，token={}, ip={}", token, ip);
        return userBll.logout(token, ip);
    }

    /**
     * 查询用户资产信息
     *
     * @param userListBo
     * @return
     */
    @Override
    public R<String> userPropertyList(UserListBo userListBo) {
        return R.ok();
    }

    /**
     * 条件分页查询用户列表
     *
     * @param userListBo
     * @return
     */
    @Override
    public R<PageUtils<UserListVo>> pageList(UserListBo userListBo) {
        // 权限
        UserCacheVo user = GlobalObject.getLocalUser();
        if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT.getCode()) || ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
            userListBo.setAgentId(user.getAgentId());
            if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
                UserDetailsInfoVo userDetail = ResultUtil.getResult(userDetailsBll.getByUserId(user.getId()));
                if (userDetail == null || userDetail.getSaleId() == null) {
                    PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                    objectPageUtils.setList(new ArrayList<>());
                    objectPageUtils.setTotalCount(0);
                    return R.ok(objectPageUtils);
                }
                userListBo.setSalesId(userDetail.getSaleId());
            }
        }

//        if (ObjectUtil.isEmpty(userListBo.getUserIds())) {
//            userListBo.setUserIds(new ArrayList<>());
//        }
        boolean selectUserId = false;
        List<Long> userIdsByPackage = new ArrayList<>();
        List<Long> userIdsByExpireTime = new ArrayList<>();
        List<Long> userIdsBySalesId = new ArrayList<>();
        List<Long> userIdsByDayAnalysis = new ArrayList<>();
        List<Long> userIdsByNotAnalysis = new ArrayList<>();
        List<Long> userIdsByWxName = new ArrayList<>();
        List<Long> userIdsByChannelId = new ArrayList<>();
        List<Long> userIdsByDetail = new ArrayList<>();
        List<Long> userIdsByOwnCount = new ArrayList<>();
        List<Long> userIdsByTrialOrder = new ArrayList<>();
        // 根据渠道查询
        if (userListBo.getChannelId() != null) {
            R<List<Long>> channelIdsR = this.channelBll.getChannelAllChildId(userListBo.getChannelId());
            List<Long> channelIds = channelIdsR.getData();
            if (channelIds != null && channelIds.size() > 0) {
                R<List<UserInfoVo>> userListR = this.userBll.listByChannelIds(channelIds);
                List<UserInfoVo> userList = userListR.getData();
                if (userList != null && userList.size() > 0) {
                    userIdsByChannelId = userList.stream().map(UserVo::getId).collect(Collectors.toList());
                } else {
                    PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                    objectPageUtils.setList(new ArrayList<>());
                    objectPageUtils.setTotalCount(0);
                    return R.ok(objectPageUtils);
                }
            } else {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }

        }
        //根据版本查询
        if (ObjectUtil.isNotEmpty(userListBo.getPackageId())) {
            selectUserId = true;
            userIdsByPackage = orderBll.getUserIdByPackageId(userListBo.getPackageId());
            if (userIdsByPackage == null || userIdsByPackage.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
            R<List<Long>> userChild = userBll.getUserChild(userIdsByPackage);
            List<Long> data = userChild.getData();
            if (data != null && !data.isEmpty()) {
//                userListBo.getUserIds().addAll(data);
                userIdsByPackage.addAll(data);
            }
        }

        // 根据付费到期时间查询
        if (ObjectUtil.isNotEmpty(userListBo.getExpireTime())) {
            selectUserId = true;
            OrderTimeVo orderTimeVo = orderBll.getUserIdByExpireTime(userListBo.getExpireTime());
            userIdsByExpireTime = orderTimeVo.getUserIdsByExpireTime();
            if (userIdsByExpireTime == null || userIdsByExpireTime.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }

        // 根据销售人员ID查询
        if (ObjectUtil.isNotEmpty(userListBo.getSalesId())) {
            selectUserId = true;
            userIdsBySalesId = userDetailsBll.getUserIdsBySalesId(userListBo.getSalesId());
            if (userIdsBySalesId == null || userIdsBySalesId.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }

        // 根据日平均分析查询
        if (userListBo.getStartDayAnalysis() != null || userListBo.getEndDayAnalysis() != null) {
            selectUserId = true;
            userIdsByDayAnalysis = userAnalysisRollupBll.getUserIdsByDayAnalysis(userListBo.getStartDayAnalysis(), userListBo.getEndDayAnalysis());
            if (userIdsByDayAnalysis == null || userIdsByDayAnalysis.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }

        // 根据多久未分析去查询用户
        if (userListBo.getStartLongNotAnalysis() != null || userListBo.getEndLongNotAnalysis() != null) {
            selectUserId = true;
            userIdsByNotAnalysis = userAnalysisRollupBll.getUserIdsByNotAnalysis(userListBo.getStartLongNotAnalysis(), userListBo.getEndLongNotAnalysis());
            if (userIdsByNotAnalysis == null || userIdsByNotAnalysis.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }

        // 根据微信名称查询用户
        if (ObjectUtil.isNotEmpty(userListBo.getUserWxName())) {
            selectUserId = true;
            userIdsByWxName = userDetailsBll.getUserByWxName(userListBo.getUserWxName());
            if (userIdsByWxName == null || userIdsByWxName.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }


        //根据用户意向 或 客户类型 或 是否登录 查询用户
        if(userListBo.getUserBelongType()!=null || StringUtil.isNotBlank(userListBo.getUserAmbition()) || userListBo.getIsLoggedIn()!=null){
            selectUserId = true;
            //根据赛选条件查询用户详情
            List<UserDetailsVo> userDetailsVos =userDetailsBll.selectByQuery(userListBo);
            //获取用户详情的userId
            userIdsByDetail=userDetailsVos.stream().map(UserDetailsVo::getUserId).filter(Objects::nonNull).distinct().toList();
            if (userIdsByDetail.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }

        //模糊查询 渠道明细 名称
        if (StringUtil.isNotBlank(userListBo.getPromotionName())){
            AgentPromotionBo agentPromotionBo = new AgentPromotionBo();
            agentPromotionBo.setPromotionName(userListBo.getPromotionName());
            List<Long> promotionIds = agentPromotionBll.selectQuery(agentPromotionBo);
            List<String> codes = inviteUrlCodeBll.selectQuery(promotionIds);
            if (promotionIds.isEmpty()||codes.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
            userListBo.setInviteUrlCodes(codes);
        }

        //根据自有账号数量去筛选
        if (userListBo.getOwnCount()!=null){
            selectUserId = true;
            AnchorUrlBo anchorUrlBo = new AnchorUrlBo();
            anchorUrlBo.setOwnCount(userListBo.getOwnCount());
            userIdsByOwnCount = anchorUrlBll.selectQuery(anchorUrlBo);
            if (userIdsByOwnCount.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }



        //根据是否是试用/正式版订单 筛选
        if (userListBo.getTrialOrder()!=null){
            selectUserId = true;
            OrderBo orderBo = new OrderBo();
            orderBo.setTrialOrder(userListBo.getTrialOrder());
            orderBo.setStatus(2);
            orderBo.setCommodityType(1);
            List<OrderListVo> orderListVos = orderBll.selectByQuery(orderBo);
            userIdsByTrialOrder= orderListVos.stream().map(OrderListVo::getUserId).toList();
            if (userIdsByTrialOrder.isEmpty()) {
                PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                objectPageUtils.setList(new ArrayList<>());
                objectPageUtils.setTotalCount(0);
                return R.ok(objectPageUtils);
            }
        }




        // 取联合查询出来的用户集合的交集
        Collection<Long> result = intersectionOrSingleSet(Arrays.asList(userIdsByPackage, userIdsByExpireTime, userIdsBySalesId, userIdsByDayAnalysis, userIdsByNotAnalysis, userIdsByWxName, userIdsByChannelId,userIdsByDetail,userIdsByOwnCount,userIdsByTrialOrder));
        if (selectUserId && result.isEmpty()) {
            userListBo.setUserIds(new LinkedList<>());
            userListBo.getUserIds().add(0L);
        } else if (!result.isEmpty()) {
            userListBo.setUserIds(new LinkedList<>());
            userListBo.getUserIds().addAll(result);
        }

        // 查询所有用户的分析汇总
        List<UserAnalysisRollupEntity> rollupEntities = userAnalysisRollupBll.listAll();
        // 查询所有用户的对比分析
        List<SyncContrastEntity> syncContrastEntities = syncContrastBll.listAllContrast();
        if (syncContrastEntities == null) syncContrastEntities = new ArrayList<>();
        Map<Long, List<SyncContrastEntity>> SyncContrastMap = syncContrastEntities.stream().collect(Collectors.groupingBy(SyncContrastEntity::getUserId));

        //获取用户
        int page = ObjectUtil.defaultIfNull(userListBo.getPage(), 1);
        int limit = ObjectUtil.defaultIfNull(userListBo.getLimit(), 10);
        R<PageUtils<UserListVo>> pageUtilsR = userBll.selectClientList(userListBo);

        if (ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())) {
            //获取行业信息
            List<Long> tradeIds = pageUtilsR.getData().getList().stream().map(UserListVo::getTradeId).toList();
            R<List<TradeListVo>> listR = tradeBll.listByIds(tradeIds);

            // 获取用户来源渠道信息
            Set<Long> channelIds = pageUtilsR.getData().getList().stream().map(UserListVo::getChannelId).collect(Collectors.toSet());
            R<List<ChannelInfoVo>> channelInfoVos = this.channelBll.listByIds(channelIds);
            List<ChannelInfoVo> channelInfoVoList = channelInfoVos.getData();

            // 获取代理商销售信息
            Set<Long> agentSaleIds = pageUtilsR.getData().getList().stream().map(UserListVo::getAgentSaleId).collect(Collectors.toSet());
            R<List<AgentSaleInfoVo>> agentSaleList = this.agentSaleBll.listByIds(agentSaleIds);
            List<AgentSaleInfoVo> agentSaleInfoVoList = agentSaleList.getData();

            // 获取所有用户和用户对应的订单有效时间
            OrderTimeVo userIdByExpireTime = orderBll.getUserIdByExpireTime(0L);
            Map<Long, Long> finalUserOrderTimeMap = userIdByExpireTime.getUserOrderTime();
//            List<Long> userIds = pageUtilsR.getData().getList().stream().map(UserVo::getId).distinct().toList();
//            List<OrderInfoVo> orderList = ResultUtil.getResult(orderFeign.orderByUserIds(userIds));
//            Map<Long, OrderInfoVo> orderMap;
//            if (orderList != null) {
//                orderMap = orderList.stream().collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> n));
//            } else {
//                orderMap = new HashMap<>();
//            }


            //获取用户绑定的推广渠道（渠道明细）
            List<String> inUrlCodes = pageUtilsR.getData().getList().stream().map(UserListVo::getInviteUrlCode).toList();
            List<InviteUrlPromotionVo> urlPromotionVos = inviteUrlCodeBll.getByCodes(inUrlCodes);
            List<InviteUrlPromotionVo> inviteUrlPromotionVos= agentPromotionBll.getNameById(urlPromotionVos);

            List<Long> userIds = pageUtilsR.getData().getList().stream()
                    .map(item -> item.getParentId() != null && item.getParentId() != 0 ? item.getParentId() : item.getId())
                    .distinct().toList();
//            Map<Long, OrderInfoVo> orderUserMap = orderBll.currentOrderByUserIds(userIds).stream()
//                    .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> o));

            //获取自有账号数量
            List<Long> longList = pageUtilsR.getData().getList().stream().map(UserListVo::getId).toList();
            Map<Long, Long> iHave = anchorUrlBll.countIHave(longList);


            //获取是否是试用/正式版订单
            List<OrderListVo> orderListVos = orderBll.selectByUserIds(longList);
            // 根据 userListBo 的 trialOrder 决定优先级
            int trialOrder =  (userListBo.getTrialOrder() != null && userListBo.getTrialOrder() == 1) ? 1 : 0;
            Map<Long, Integer> tMap = orderListVos.stream()
                    .collect(Collectors.toMap(
                            OrderListVo::getUserId,
                            OrderListVo::getTrialOrder,
                            (existing, replacement) -> {
                                // 保留 preferredValue 的值优先级更高
                                if (existing == trialOrder) {
                                    return existing;
                                }
                                if (replacement == trialOrder) {
                                    return replacement;
                                }
                                // 否则保留现有值
                                return existing;
                            }
                    ));

            //查询用户意向、客户类型、是否登录
            List<UserDetailsVo> userDetailsVos =userDetailsBll.selectByUserIds(longList);
            Map<Long, UserDetailsVo> finalMap = userDetailsVos.stream().collect(Collectors.toMap(UserDetailsVo::getUserId, Function.identity(), (key1, key2) -> key2));

            // 获取用户订单信息
            Map<Long, OrderInfoVo> orderMap = orderFeign.currentOrderByUserIds(userIds)
                    .stream()
                    .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> o));

            pageUtilsR.getData().getList().forEach(item -> {
                   Long id = item.getId();
                   Long parentId = item.getParentId();
                if (parentId != null && parentId != 0) id = parentId;
                //去拿生效订单信息
                OrderInfoVo order = orderMap.get(id);
                if (order != null) {
                    item.setPackageId(order.getId());
                    item.setPackageName(order.getCommodityName());
                    item.setPackageLevel(order.getLevel());
                }
                //填充行业名称
                if (listR.getData() != null) {
                       List<TradeListVo> list = listR.getData().stream().filter(entiy -> entiy.getId().equals(item.getTradeId())).toList();
                       if(!list.isEmpty()){
                           item.setTradeName(list.get(0).getName());
                       }
                }
                // 填充渠道信息
                if (channelInfoVoList != null && channelInfoVoList.size() > 0) {
                       for (ChannelInfoVo channelInfoVo : channelInfoVoList) {
                           if(channelInfoVo.getId().equals(item.getChannelId())) {
                               item.setChannelName(channelInfoVo.getChannelName());
                               break;
                           }
                       }
                }
                // 填充代理商销售信息
                if (agentSaleInfoVoList != null && agentSaleInfoVoList.size() > 0) {
                       for (AgentSaleInfoVo agentSaleInfoVo : agentSaleInfoVoList) {
                           if(agentSaleInfoVo.getId().equals(item.getAgentSaleId())) {
                               item.setAgentSaleName(agentSaleInfoVo.getSaleName());
                               break;
                           }
                       }
                }
                //填充推广渠道信息（渠道明细）
                if (inviteUrlPromotionVos != null && !inviteUrlPromotionVos.isEmpty()) {
                       Map<String, InviteUrlPromotionVo> collect = inviteUrlPromotionVos.stream().collect(Collectors.toMap(InviteUrlPromotionVo::getUrlCode, Function.identity(),(o,n)->n));
                       InviteUrlPromotionVo inviteUrlPromotionVo = collect.get(item.getInviteUrlCode());
                       if (inviteUrlPromotionVo != null){
                           item.setPromotionId(inviteUrlPromotionVo.getPromotionId());
                           item.setPromotionName(inviteUrlPromotionVo.getPromotionName());
                       }
                }
                //填充用户意向、客户类型、是否登录
                if (!finalMap.isEmpty()){
                    UserDetailsVo userDetailsVo = finalMap.get(item.getId());
                    if (userDetailsVo != null){
                        item.setUserAmbition(userDetailsVo.getUserAmbition());
                        item.setUserBelongType(userDetailsVo.getUserBelongType());
                        item.setIsLoggedIn(userDetailsVo.getIsLoggedIn());
                    }
                }

                //填充自有账号数量
                if (!iHave.isEmpty()){
                    Long aLong = iHave.get(item.getId());
                    item.setOwnCount(aLong==null?0:Math.toIntExact(aLong));
                }

                //填充是否试用订单
                if (!tMap.isEmpty()){
                    item.setTrialOrder(tMap.get(item.getId()));
                }

                // 填充用户全部有效付费订单剩余时间
                item.setExpireTime(finalUserOrderTimeMap.get(item.getId()));
                // 填充用户分析总数,日平均分析,多久未分析
                UserAnalysisRollupEntity rollup = rollupEntities.stream().filter(rol -> rol.getUserId().equals(item.getId())).findFirst().orElse(null);
                if (rollup != null) {
                       item.setSumAnalysis(Long.valueOf(rollup.getAnalysisSum()));
                       item.setDayAnalysis(Long.valueOf(rollup.getDayAverageAnalysis()));
                       Date currentDate = new Date();
                       if (rollup.getLastAnalysis() != null){
                           long finalTime = currentDate.getTime() - rollup.getLastAnalysis().getTime();
                           if (finalTime > 0){
                               long days = TimeUnit.MILLISECONDS.toDays(finalTime);
                               item.setLongNotAnalysis(days);
                           }
                       }
                }
                // 填充用户的对比分析条数
                List<SyncContrastEntity> syncContrastEntityList = SyncContrastMap.get(item.getId());
                if (syncContrastEntityList != null && syncContrastEntityList.size() > 0) {
                       item.setContrastAnalysis((long) syncContrastEntityList.size());
                } else {
                       item.setContrastAnalysis(0L);
                }
               });

            // 特殊升序或降序排序
            if (userListBo.getSpecialSorting() != null && userListBo.getSpecialSorting() == 1) {
                switch (userListBo.getSearchUpOrDown()) {
                    // 根据付费到期时间
                    case 0:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getExpireTime, Comparator.nullsLast(Comparator.naturalOrder())));
                        break;
                    case 1:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getExpireTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                    // 根据总分析条数
                    case 2:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getSumAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())));
                        break;
                    case 3:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getSumAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                    // 根据日分析条数
                    case 4:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getDayAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())));
                        break;
                    case 5:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getDayAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                    // 根据多久未分析
                    case 6:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getLongNotAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())));
                        break;
                    case 7:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getLongNotAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                    // 根据对比分析条数
                    case 8:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getContrastAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())));
                        break;
                    case 9:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getContrastAnalysis, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                    //根据自有账号数量排序
                    case 10:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getOwnCount, Comparator.nullsFirst(Comparator.naturalOrder())));
                        break;
                    case 11:
                        pageUtilsR.getData().getList().sort(Comparator.comparing(UserListVo::getOwnCount, Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        break;
                }
                // 根据当前页截取对应的数据展示
                List<UserListVo> list = pageUtilsR.getData().getList();
                List<UserListVo> pageList = ListUtil.page(page - 1, limit, list);
                pageUtilsR.getData().setList(pageList);
            }

        }
        return pageUtilsR;
    }

    @Override
    public R<PageUtils<UserListVo>> pageListNew(UserListBo userListBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT.getCode()) || ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
            userListBo.setAgentId(user.getAgentId());
            if (ObjectUtil.equals(user.getAdminUserType(), UserEnums.adminUserType.AGENT_SALE.getCode())) {
                UserDetailsInfoVo userDetail = ResultUtil.getResult(userDetailsBll.getByUserId(user.getId()));
                if (userDetail == null || userDetail.getSaleId() == null) {
                    PageUtils<UserListVo> objectPageUtils = new PageUtils<>();
                    objectPageUtils.setList(new ArrayList<>());
                    objectPageUtils.setTotalCount(0);
                    return R.ok(objectPageUtils);
                }
                userListBo.setSalesId(userDetail.getSaleId());
            }
        }

        // 校验排序字段
        if (ObjectUtil.isNotEmpty(userListBo.getSortField()) && !Arrays.asList("ownCount", "packageEndDate", "sumAnalysis", "dayAnalysis", "contrastAnalysis", "lastAnalysis").contains(userListBo.getSortField())) {
            return R.error(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "排序字段未知");
        }
        if (ObjectUtil.isEmpty(userListBo.getSortOrder())) {
            userListBo.setSortOrder("asc");
        }
        if (ObjectUtil.isNotEmpty(userListBo.getSortOrder()) && !Arrays.asList("asc", "desc").contains(userListBo.getSortOrder())) {
            return R.error(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "排序类型未知");
        }

        PageUtils<UserListVo> res = userBll.pageListNew(userListBo);

        if (ObjectUtil.isNotEmpty(res.getList())) {
            List<UserListVo> list = res.getList();

            //获取行业信息
            Set<Long> tradeIds = list.stream().map(UserListVo::getTradeId).collect(Collectors.toSet());
            Map<Long, TradeListVo> tradeMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(tradeIds)) {
                tradeMap = tradeProducer.listByIds(tradeIds)
                        .stream()
                        .collect(Collectors.toMap(TradeVo::getId, Function.identity()));
            }

            // 获取用户来源渠道信息
            Set<Long> channelIds = list.stream().map(UserListVo::getChannelId).collect(Collectors.toSet());
            Map<Long, ChannelInfoVo> channelMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(channelIds)) {
                channelMap = channelProducer.listByIds(channelIds)
                        .stream()
                        .collect(Collectors.toMap(ChannelInfoVo::getId, Function.identity()));
            }

            // 获取版本信息
            List<Long> userIds = list.stream()
                    .flatMap(vo -> {
                        List<Long> temp = new ArrayList<>(2);
                        temp.add(vo.getId());
                        temp.add(vo.getParentId());
                        return temp.stream();
                    })
                    .filter(item -> ObjectUtil.isNotEmpty(item) && item.compareTo(0L) > 0)
                    .collect(Collectors.toList());
            // 获取用户订单信息
            Map<Long, OrderInfoVo> orderMap = orderFeign.currentOrderByUserIds(userIds)
                    .stream()
                    .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> o));

            // 渠道明细
            List<String> inviteUrlCodes = list.stream().map(UserListVo::getInviteUrlCode).toList();
            Map<String, String> inviteUrlCodeMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(inviteUrlCodes)) {
                inviteUrlCodeMap = inviteUrlCodeBll.inviteCodeAndPromotionName(inviteUrlCodes).stream()
                        .collect(Collectors.toMap(InviteUrlCodeAndPromotionName::getUrlCode, InviteUrlCodeAndPromotionName::getPromotionName));
            }

            // 最新跟进记录（本页一次批量取，循环内不再查库）；异常降级为空 Map，不阻断列表主数据
            List<Long> remarkUserIds = list.stream()
                    .map(UserListVo::getId)
                    .filter(ObjectUtil::isNotEmpty)
                    .collect(Collectors.toList());
            Map<Long, LatestRemarkVo> latestRemarkMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(remarkUserIds)) {
                try {
                    latestRemarkMap = userRemarkBll.batchLatestByUserIds(remarkUserIds);
                } catch (Exception e) {
                    log.error("[用户列表] 批量获取最新跟进记录失败，本次降级为空 userIds.size={}", remarkUserIds.size(), e);
                }
            }

            // 智能体用量（会话数+Token，用户/租户两维度）：本页一次批量实时调外部接口，用户/租户两路并行；
            // 任何失败/超时在 service 内已降级为 0，这里再包一层兜底为空 Map，绝不阻断列表主数据。
            // 用户维度用接口5「用户×租户」对(tenantId=active_tenant_id)——只算用户在当前租户下的量，避免跨租户糊成一笔。
            Map<String, AgentUsage> userAgentUsageMap = new HashMap<>();
            Map<Long, AgentUsage> tenantAgentUsageMap = new HashMap<>();
            List<UserTenantPair> agentUserPairs = list.stream()
                    .filter(i -> i.getId() != null && i.getActiveTenantId() != null && i.getActiveTenantId() > 0)
                    .map(i -> new UserTenantPair(i.getId(), i.getActiveTenantId()))
                    .collect(Collectors.toList());
            List<Long> agentTenantIds = list.stream()
                    .map(UserListVo::getActiveTenantId)
                    .filter(tid -> tid != null && tid > 0)
                    .distinct()
                    .collect(Collectors.toList());
            try {
                CompletableFuture<Map<String, AgentUsage>> userFuture =
                        CompletableFuture.supplyAsync(() -> agentUsageService.batchUserTenantUsage(agentUserPairs));
                CompletableFuture<Map<Long, AgentUsage>> tenantFuture =
                        CompletableFuture.supplyAsync(() -> agentUsageService.batchTenantUsage(agentTenantIds));
                userAgentUsageMap = userFuture.join();
                tenantAgentUsageMap = tenantFuture.join();
            } catch (Exception e) {
                log.error("[用户列表] 批量获取智能体用量失败，本次降级为0 pairs.size={}, tenantIds.size={}",
                        agentUserPairs.size(), agentTenantIds.size(), e);
            }

            for (UserListVo item : list) {

                // 行业名称
                TradeListVo tradeVo = tradeMap.get(ObjectUtil.defaultIfNull(item.getTradeId(), -1L));
                if (ObjectUtil.isNotEmpty(tradeVo)) {
                    item.setTradeName(tradeVo.getName());
                }

                // 渠道名称
                ChannelInfoVo channel = channelMap.get(ObjectUtil.defaultIfNull(item.getChannelId(), -1L));
                if (ObjectUtil.isNotEmpty(channel)) {
                    item.setChannelName(channel.getChannelName());
                }

                // 渠道明细
                String name = inviteUrlCodeMap.get(item.getInviteUrlCode());
                if (ObjectUtil.isNotEmpty(name)) {
                    item.setPromotionName(name);
                }

                // 版本
                Long packageUserId = item.getParentId() != null && item.getParentId() != 0 ? item.getParentId() : item.getId();
                //去拿生效订单信息
                OrderInfoVo order = orderMap.get(packageUserId);
                if (order != null) {
                    item.setPackageId(order.getId());
                    item.setPackageName(order.getCommodityName());
                    item.setPackageLevel(order.getLevel());
                    if (item.getPackageLevel() < 0) {
                        item.setExpireTime(null);
                    }
                }

                // 最新跟进记录（key 为用户自身 id，不用 packageUserId）
                item.setLatestRemark(latestRemarkMap.get(item.getId()));

                // 设置默认值
                item.setSumAnalysis(ObjectUtil.defaultIfNull(item.getSumAnalysis(), 0L));
                item.setDayAnalysis(ObjectUtil.defaultIfNull(item.getDayAnalysis(), 0L));
                item.setContrastAnalysis(ObjectUtil.defaultIfNull(item.getContrastAnalysis(), 0L));
                // 算力消耗随主查询 LEFT JOIN 带出，未命中兜底 0L
                item.setUserPowerConsume(ObjectUtil.defaultIfNull(item.getUserPowerConsume(), 0L));
                item.setTenantPowerConsume(ObjectUtil.defaultIfNull(item.getTenantPowerConsume(), 0L));

                // 智能体用量：本页批量实时取，未命中/降级兜底 0L
                // 用户维度 key 用「用户×租户」对(userId+active_tenant_id)，租户维度 key 用 active_tenant_id
                AgentUsage userAgent = item.getActiveTenantId() == null ? null
                        : userAgentUsageMap.get(UserTenantPair.key(item.getId(), item.getActiveTenantId()));
                item.setUserAgentMessageCount(userAgent == null ? 0L : userAgent.getUserMessageCount());
                item.setUserAgentTokens(userAgent == null ? 0L : userAgent.getBilledTokens());
                AgentUsage tenantAgent = item.getActiveTenantId() == null ? null : tenantAgentUsageMap.get(item.getActiveTenantId());
                item.setTenantAgentMessageCount(tenantAgent == null ? 0L : tenantAgent.getUserMessageCount());
                item.setTenantAgentTokens(tenantAgent == null ? 0L : tenantAgent.getBilledTokens());
            }
        }

        return R.ok(res);
    }

    /**
     * 取联合查询出来的用户ID集合的交集
     *
     * @param collections 所有集合
     * @param <T>
     * @return
     */
    public static <T> Collection<T> intersectionOrSingleSet(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Set<T>> filteredSets = new ArrayList<>();
        for (Collection<T> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                filteredSets.add(new HashSet<>(collection)); // 确保所有的集合都是 Set 类型
            }
        }

        int nonEmptyCount = filteredSets.size();
        if (nonEmptyCount == 0) {
            return Collections.emptyList(); // 所有集合都是 null 或空
        } else if (nonEmptyCount == 1) {
            // 如果只有一个非空集合，返回它的所有元素
            return filteredSets.get(0);
        } else {
            // 否则计算所有非空集合的交集
            Set<T> result = new HashSet<>(filteredSets.get(0));
            for (int i = 1; i < filteredSets.size(); i++) {
                result.retainAll(filteredSets.get(i));
            }
            return result;
        }
    }


    /**
     * 服务端冻结与解冻和用户
     *
     * @param userBo
     * @return
     */
    @Override
    public R<String> updateUser(UserBo userBo) {
        return userBll.updateUser(userBo);
    }


    /**
     * 服务端批量删除用户
     *
     * @param ids
     * @return
     */
    @Override
    @Transactional
    public R<String> deleteByIds(List<Long> ids) {
        // 用户资产删除前的校验，当前用户是子账号或者是父账号不能删除
        R<List<UserListVo>> listR = userBll.listByIds(ids);
        if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())) {
            List<UserListVo> userList = listR.getData();
            List<Long> userIds = userList.stream().map(UserVo::getId).toList();
            R<List<UserInfoVo>> listR1 = userBll.listParentUser(userIds);
            for (UserListVo user : userList) {
                if (user.getUserType() == 2) {
                    return R.error(3001, StrUtil.format("{}是子账号，不能删除", user.getNickName()));
                }
                if (listR1.getCode() == 0 && ObjectUtil.isNotEmpty(listR1.getData())) {
                    UserInfoVo userInfoVo = listR1.getData().stream().filter(item -> item.getId().equals(user.getId())).findFirst().orElse(null);
                    if (userInfoVo != null) {
                        if (userInfoVo.getChildAccountCount() > 0) {
                            return R.error(3002, StrUtil.format("{}有子账号，不能删除", user.getNickName()));
                        }
                    }
                }
            }
        }
        // 删除用户资产
        ids.forEach(userPropertyBll::deleteByUserId);
        return userBll.deleteByIds(ids);
    }

    /**
     * 服务端修改用户账号类型
     *
     * @param userUpdateBo
     * @return
     */
    @Override
    public R<String> updateByUserId(UserUpdateBo userUpdateBo) {
        return userBll.updateByUserId(userUpdateBo);
    }

    /**
     * 服务端根据seu_uid获取用户信息
     */
    @Override
    public R<PageUtils<UserListVo>> selectByuseId(UserListBo userListBo) {
        List<AnchorUrlUserEntity> anchorUrlUserEntities = anchorUrlBll.selectBySecUid(userListBo.getSecUid());

        //获取用户信息
        if (anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
            List<Long> list1 = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getUserId).toList();
            R<PageUtils<UserListVo>> pageUtilsR = userBll.selectByIds(userListBo.getLimit(), userListBo.getPage(), list1);
            List<UserListVo> list = pageUtilsR.getData().getList();
            if (list != null && list.size() > 0) {
                list.stream().map(item -> {
                    List<AnchorUrlUserEntity> list2 = anchorUrlUserEntities.stream().filter(entity -> entity.getUserId().equals(item.getId())).toList();
                    if (list2 != null && list2.size() > 0) {
                        R<TradeInfoVo> info = tradeBll.info(list2.get(0).getTradeId());
                        item.setTradeName(info.getData() != null ? info.getData().getName() : null);
                    }

                    return item;
                }).toList();
            }

            return pageUtilsR;
        }

        return R.ok(new PageUtils<>());
    }

    @Override
    public R<Integer> checkFreeze() {
        UserCacheVo user = GlobalObject.getLocalUser();
        if (user == null) {
            return R.ok(1);
        }
        return R.ok(0);
    }

    @Override
    public R<UserVo> getUserByToken(HttpServletRequest request) {
        String token = request.getHeader("Token");
        String ip = GetIPUtils.getIpAddr(request);
        return userBll.getUserByToken(token, ip);
    }

    @Override
    public R<String> updateTrade(Long userId, Long tradeId) {
        return userBll.updateTrade(userId, tradeId);
    }

    @Override
    @Transactional
    public R<String> setUpASubAccount(bindingSubAccountBo aSubAccountBo, boolean isVerifyCode) {
        if (ObjectUtil.isEmpty(aSubAccountBo.getSubPhone())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请输入手机号");
        }
//        if (ObjectUtil.isEmpty(aSubAccountBo.getSubUserName())) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请输入用户名");
//        }
        if (isVerifyCode && ObjectUtil.isEmpty(aSubAccountBo.getCode())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请输入验证码");
        }
        // 校验验证码
        String codeKey = null;
        if (isVerifyCode) {
            codeKey = powerProperties.getPhoneCodeRedisKey() + aSubAccountBo.getSubPhone();
            String code = !resilientRedisTemplate.isDegraded() ? (String) resilientRedisTemplate.getRedisTemplate().opsForValue().get(codeKey) : (String) cacheFallbackDataService.getCacheValueByCache(codeKey);
            if (StrUtil.isBlank(code) || !code.equals(aSubAccountBo.getCode())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "验证码错误");
            }
        }

        String userName = null;
        if (ObjectUtil.isEmpty(aSubAccountBo.getCurrentUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            aSubAccountBo.setCurrentUserId(localUser.getId());
            userName = localUser.getNickName();
        } else {
            R<UserInfoVo> info = userBll.info(aSubAccountBo.getCurrentUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                userName = info.getData().getNickName();
            }
        }

        checkParams(aSubAccountBo);

        // 查询是否有子账号，如果没有就为子账号注册
        UserInfoVo user = userBll.setUpASubAccount2(aSubAccountBo);
        if (user == null) return R.error(3001, "注册用户失败");

        if (ObjectUtil.equals(user.getId(), aSubAccountBo.getCurrentUserId())) {
            RRException.create("无法将自己绑定为子账号");
        }

        Long parentId = user.getParentId();
        if (parentId != null && parentId != 0L) {
            if (!parentId.equals(aSubAccountBo.getCurrentUserId())) {
                RRException.create("当前账号已经存在绑定关系");
            } else {
                RRException.create("已经绑定过了，无需再次绑定");
            }
        }

        // 校验当前用户是否有未过期订单
        orderBll.checkSetUpASubAccountOrder(user.getId());

        // 使用资产
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(aSubAccountBo.getCurrentUserId());
        assets.setUserName(userName);
        assets.setNum(-1L);
        assets.setCode("subAccountCount");
        UserPropertyImpl.use(assets);

        // 设置子账号前的操作
        userPropertyBll.setUpASubAccount(aSubAccountBo.getCurrentUserId(), user.getId());

        // 修改用户的账号类型、parentId字段
        UserBo userBo = new UserBo();
        userBo.setUserId(user.getId());
        userBo.setUserType(2);
        userBo.setParentId(aSubAccountBo.getCurrentUserId());
        userBll.updateParentId(userBo);

        // 添加绑定记录
        BindingAccountBo bo = new BindingAccountBo();
        bo.setParentUserId(aSubAccountBo.getCurrentUserId());
        bo.setParentUserName(userName);
        bo.setChildUserId(user.getId());
        bo.setChildUserName(user.getNickName());
        bo.setBindingDate(new Date());
        bindingAccountBll.save(bo);

        // 更新子账号的主播数量和弹幕监控位
        updateUserAnchorNum(user.getId());

        // 修改子账号的租户id
        // 当前为子账号，将租户id设置成主账号的租户
        R<TenantInfoVo> tenantInfoVoR = this.tenantBll.infoByUserId(aSubAccountBo.getCurrentUserId());
        if (tenantInfoVoR.getCode() != 0) {
            RRException.create("获取父租户信息失败");
        }
        TenantInfoVo tenantInfoVo = tenantInfoVoR.getData();
        this.userBll.updateActiveTenantId(user.getId(), tenantInfoVo.getId());

        // 更新缓存
//        redisTemplate.opsForValue().set(RedisCacheKey.getRedisKey(RedisCacheKey.userParentIdCacheKey, user.getId()), aSubAccountBo.getCurrentUserId());

        // 删除验证码
        if (codeKey != null) {
            if (!resilientRedisTemplate.isDegraded() && resilientRedisTemplate.getRedisTemplate().hasKey(codeKey)) {
                resilientRedisTemplate.getRedisTemplate().delete(codeKey);
            } else {
                cacheFallbackDataService.removeCacheFallbackDataByCacheKey(codeKey);
            }
        }

        // 子账号短信通知
        msgBll.sendBindingAccountMsg(aSubAccountBo.getSubPhone());

        return R.ok("设置成功");
    }


    /**
     * 添加子账号 并返回userId
     *
     * @param aSubAccountBo
     * @param verifyCode
     */
    @Transactional(rollbackFor = Throwable.class)
    @Override
    public R<Long> setUpASubAccountResultUserId(bindingSubAccountBo aSubAccountBo, boolean verifyCode) {
        R<String> result = this.setUpASubAccount(aSubAccountBo, verifyCode);
        if (result.fail()) {
            return R.error(result.getCode(), result.getMsg());
        }
        if (aSubAccountBo.getSubUserId() != null) {
            return R.ok(aSubAccountBo.getSubUserId());
        }
        Optional<Long> optional = userService.lambdaQuery()
            .eq(UserEntity::getPhone, aSubAccountBo.getSubPhone())
            .select(UserEntity::getId).oneOpt().map(UserEntity::getId);
        if (optional.isPresent()) {
            return R.ok(optional.get());
        }
        throw new BusinessException("获取用户id失败");
    }

    /**
     * 校验参数
     *
     * @param aSubAccountBo 参数
     */
    private void checkParams(bindingSubAccountBo aSubAccountBo) {

        if (aSubAccountBo.getCurrentUserId() == null) {
            throw new BusinessException("主账号不能为空");
        }
        UserInfoVo user = ResultUtil.getResult(userBll.info(aSubAccountBo.getCurrentUserId(), false));
        if (user == null) {
            throw new BusinessException("主账号不存在");
        }
        if (!ObjectUtil.equals(user.getUserType(), UserEnums.userType.CLIENT_USER.getCode())) {
            throw new BusinessException("当前账号不是爱复盘主账号，不能绑定子账号");
        }
        UserPropertyTypeInfoVo userPropertyByCode = userPropertyBll.getUserPropertyByCode(aSubAccountBo.getCurrentUserId(), "subAccountCount");
        if (userPropertyByCode == null || userPropertyByCode.getUseQuantity() >= userPropertyByCode.getTotalQuantity()) {
            throw new BusinessException("子账号数量不足");
        }
    }

    /**
     * 更新用户主播数量和弹幕监控位
     *
     * @param userId
     */
    private void updateUserAnchorNum(Long userId) {
        R<UserVo> userVoR = userBll.getById(userId);
        if (userVoR.getData() != null) {
            R<List<AnchorUrlUserVo>> listR = anchorUrlBll.clientAnchorList(userId, userVoR.getData().getActiveTenantId());
            List<AnchorUrlUserVo> urlVos = listR.getData();
            long num = 0L;
            long anchorBarrageNum = 0L;
            if (urlVos != null && !urlVos.isEmpty()) {
                long total = this.getUserAnchorNum(userId);
                long size = urlVos.stream().filter(item -> item.getIsRemoveRecord() == 0).toList().size();
                num = Math.min(size, total);

                anchorBarrageNum = urlVos.stream().filter(item -> item.getIsRemoveRecord() == 0 && item.getIsBarrageMonitoring() == 1).toList().size();
            }
            //log.info("修改用户主播数量userId={}， 数量={}", userId, num);
            //log.info("修改用户主播弹幕监控位数量userId={}， 数量={}", userId, anchorBarrageNum);
            userPropertyBll.updateByPropertyNum(userId, "anchorNum", num);

            userPropertyBll.updateByPropertyNum(userId, "anchorBarrageNum", anchorBarrageNum);
        }
    }

    private long getUserAnchorNum(Long userId) {
        // 获取用户可添加主播的数量
        long total = 0L;
        List<UserPropertyTypeInfoVo> userProperty = userPropertyBll.getUserProperty(userId);
        if (ObjectUtil.isNotEmpty(userProperty)) {
            UserPropertyTypeInfoVo typeInfoVo = userProperty.stream().filter(item -> item.getCommodityTypeCode().equals("anchorNum")).findFirst().orElse(null);
            if (ObjectUtil.isNotEmpty(typeInfoVo)) {
                total = typeInfoVo.getTotalQuantity();
            }
        }
        return total;
    }

    @Override
    @Transactional
    public R<String> unbindingSubAccount(bindingSubAccountBo aSubAccountBo) {
        if (ObjectUtil.isEmpty(aSubAccountBo.getSubUserId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "子账号不能为空");
        }
        Long currentTenantId = null;
        String userName = null;
        if (ObjectUtil.isEmpty(aSubAccountBo.getCurrentUserId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            aSubAccountBo.setCurrentUserId(localUser.getId());
            userName = localUser.getNickName();
            currentTenantId = localUser.getActiveTenantId();
        } else {
            R<UserInfoVo> info = userBll.info(aSubAccountBo.getCurrentUserId(), false);
            if (info.getCode() == 0 && info.getData() != null) {
                userName = info.getData().getNickName();
                currentTenantId = info.getData().getActiveTenantId();
            }
        }

        Long parentId = ResultUtil.getResult(userFeign.getUserParentId(aSubAccountBo.getSubUserId()));
        if (parentId == null) RRException.create(Constant.CodeMsgEnum.ALLOW_SKIP.getCode(),"账号已经解绑");
        if (!parentId.equals(aSubAccountBo.getCurrentUserId())) RRException.create("和当前子账号无绑定关系");

        // 增减资产
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(aSubAccountBo.getCurrentUserId());
        assets.setUserName(userName);
        assets.setNum(1L);
        assets.setCode("subAccountCount");
        UserPropertyImpl.use(assets);

        // 设置子账号前的操作
        userPropertyBll.unbindingSubAccount(aSubAccountBo.getCurrentUserId(), aSubAccountBo.getSubUserId());

        // 检测订单中的资产在当月是否过期了
        OrderInfoVo orderInfoVo = orderFeign.currentOrderByUserId(aSubAccountBo.getSubUserId());
        if (orderInfoVo != null) {
            orderDetailBll.expiredOrderProcessing(orderInfoVo.getId());
        }

        // 修改绑定记录
        bindingAccountBll.unbindingSubAccount(aSubAccountBo.getCurrentUserId(), aSubAccountBo.getSubUserId(), ObjectUtil.defaultIfEmpty(aSubAccountBo.getUnbindReason(), "正常解绑"));


        // 解绑后把短视频订阅的达人和爆款删除
        videoExtractProducer.unbindDelInfluencerAndHot(aSubAccountBo.getSubUserId(), currentTenantId);

        // 修改用户的账号类型、parentId字段
        UserBo userBo = new UserBo();
        userBo.setUserId(aSubAccountBo.getSubUserId());
        userBo.setUserType(0);
        userBo.setParentId(0L);
        userBll.updateParentId(userBo);

        // 把子账号的弹幕监控位也删除
        anchorUrlBll.closeAnchorBarrageNum(Collections.singletonList(aSubAccountBo.getSubUserId()));

        // 更新子账号的主播数量和弹幕监控位
        updateUserAnchorNum(aSubAccountBo.getSubUserId());

        // 解除租户绑定关系
        this.tenantBll.unbind(aSubAccountBo.getCurrentUserId(), aSubAccountBo.getSubUserId());

        // 删除绑定关系缓存
//        redisTemplate.delete(RedisCacheKey.getRedisKey(RedisCacheKey.userParentIdCacheKey, aSubAccountBo.getSubUserId()));
        return R.ok("设置成功");
    }

    @Override
    public R<PageUtils<UserListVo>> subAccountList(SubAccountListBo bo) {
        UserListBo userListBo = new UserListBo();
        BeanUtil.copyProperties(bo, userListBo);
        if (ObjectUtil.isEmpty(bo.getParentId())) {
            UserCacheVo localUser = GlobalObject.getLocalUser();
            userListBo.setParentId(localUser.getId());
        }
        userListBo.setUserType(2);
        R<PageUtils<UserListVo>> pageUtilsR = this.selectClientList(userListBo);
        if (pageUtilsR.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR.getData()) && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())) {
            List<UserListVo> list = pageUtilsR.getData().getList();
            List<Long> userIds = list.stream().map(UserVo::getId).toList();
            BindingAccountListBo listBo = new BindingAccountListBo();
            listBo.setChildUserIds(userIds);
            listBo.setBindingStatus(0);
            if (ObjectUtil.isNotEmpty(bo.getParentId())) listBo.setParentUserId(bo.getParentId());
            listBo.setLimit(-1);
            R<PageUtils<BindingAccountListVo>> pageUtilsR1 = bindingAccountBll.queryPage(listBo);
            if (pageUtilsR1.getCode() == 0 && ObjectUtil.isNotEmpty(pageUtilsR1.getData()) && ObjectUtil.isNotEmpty(pageUtilsR1.getData().getList())) {
                List<BindingAccountListVo> bindingList = pageUtilsR1.getData().getList();
                for (UserListVo user : list) {
                    bindingList.stream().filter(item -> item.getChildUserId().equals(user.getId()) && item.getParentUserId().equals(user.getParentId())).findFirst().ifPresent(item -> user.setCreateDate(item.getBindingDate()));
                }
            }
        }
        return pageUtilsR;
    }

    @Override
    public R<UserDetailsInfoVo> userDetailByUserId(Long userId) {
        R<UserDetailsInfoVo> userInfoVoR = userDetailsBll.userDetailByUserId(userId);
        if (userInfoVoR.getCode() == 0 && userInfoVoR.getData() != null) {
            UserDetailsInfoVo userDetailsInfoVo = userInfoVoR.getData();
            Long parentUserId = userInfoVoR.getData().getParentId();
            R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(parentUserId == null || parentUserId == 0L ? userId : parentUserId);
            if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
                OrderInfoVo orderInfoVo = orderByUserId.getData().get(orderByUserId.getData().size() - 1);
                OrderInfoVo one = orderByUserId.getData().get(0);
                userDetailsInfoVo.setPackageLevel(one.getLevel());
                userDetailsInfoVo.setPackageName(one.getCommodityName());
                userDetailsInfoVo.setExpirationDate(orderInfoVo.getEndDate());
            }
            // 根据来源渠道ID查询
            if (userDetailsInfoVo.getChannelId() != null && !userDetailsInfoVo.getChannelId().equals(0L)) {
                R<ChannelInfoVo> channelInfoR = channelBll.info(userDetailsInfoVo.getChannelId());
                ChannelInfoVo channelInfo = channelInfoR.getData();
                if (channelInfo != null) {
                    userDetailsInfoVo.setChannelName(channelInfo.getChannelName());
                }
            }
            if (userDetailsInfoVo.getSaleId() != null && !userDetailsInfoVo.getSaleId().equals(0L)) {
                // 根据销售人员ID查询
                R<SalesInfoVo> salesInfoR = salesBll.info(userDetailsInfoVo.getSaleId());
                SalesInfoVo salesInfo = salesInfoR.getData();
                if (salesInfo != null) {
                    userDetailsInfoVo.setSaleName(salesInfo.getSalesName());
                }
            }
            // 代理销售, 默认值是0
            if (userDetailsInfoVo.getAgentSaleId() != null && !userDetailsInfoVo.getAgentSaleId().equals(0L)) {
                R<AgentSaleInfoVo> agentSaleInfoVoR = this.agentSaleBll.info(userDetailsInfoVo.getAgentSaleId());
                if (agentSaleInfoVoR.getData() != null) {
                    AgentSaleInfoVo agentSaleInfoVo = agentSaleInfoVoR.getData();
                    userDetailsInfoVo.setAgentSaleName(agentSaleInfoVo.getSaleName());
                }
            } else {
                userDetailsInfoVo.setAgentSaleId(null);
                userDetailsInfoVo.setAgentSaleName(null);
            }
        }
        return userInfoVoR;
    }


    /**
     * 根据手机号获取用户信息
     *
     * @param phone 手机号
     *
     * @return {@link R }<{@link OpenGovernanceUserDetailsInfo }>
     */
    @Override
    public R<OpenGovernanceUserDetailsInfo> getMobileAccount(String phone) {
        R<UserVo> byPhone = userBll.getByPhone(phone);
        if (byPhone.fail()) {
            return R.error(byPhone.getCode(), byPhone.getMsg());
        }
        if (byPhone.getData() == null) {
            return R.error("账户不存在");
        }
        UserVo user = byPhone.getData();
        OpenGovernanceUserDetailsInfo userDetailInfo = new OpenGovernanceUserDetailsInfo();
        userDetailInfo.setUserId(user.getId());
        userDetailInfo.setTenantId(user.getActiveTenantId());
        userDetailInfo.setParentId(user.getParentId());
        userDetailInfo.setUserType(user.getUserType());
        userDetailInfo.setUsername(user.getUsername());
        userDetailInfo.setNickName(user.getNickName());
        userDetailInfo.setPhone(user.getPhone());
        userDetailInfo.setPassword(user.getPassword());
        userDetailInfo.setRegisterDate(user.getCreateDate());
        userDetailInfo.setUserStatus(user.getStatus());

        BeanUtil.copyProperties(user, userDetailInfo);
        R<UserDetailsInfoVo> userDetailResult = userDetailsBll.getByUserId(user.getId());
        if (userDetailResult.getData() != null) {
            UserDetailsInfoVo userDetail = userDetailResult.getData();
            userDetailInfo.setId(userDetail.getId());
            userDetailInfo.setTradeId(userDetail.getTradeId());
            userDetailInfo.setCompanyId(userDetail.getCompanyId());
            userDetailInfo.setAnchorType(userDetail.getAnchorType());
            userDetailInfo.setPosition(userDetail.getPosition());
            userDetailInfo.setEmail(userDetail.getEmail());
            userDetailInfo.setRealName(userDetail.getRealName());
            userDetailInfo.setSex(userDetail.getSex());
            userDetailInfo.setBirthday(userDetail.getBirthday());
            userDetailInfo.setAddress(userDetail.getAddress());
            userDetailInfo.setChannelId(userDetail.getChannelId());
            userDetailInfo.setSaleId(userDetail.getSaleId());
            userDetailInfo.setWxName(userDetail.getWxName());
            userDetailInfo.setAgentSaleId(userDetail.getAgentSaleId());
            userDetailInfo.setAgentSaleName(userDetail.getAgentSaleName());
            userDetailInfo.setIsShow(userDetail.getIsShow());
            userDetailInfo.setVideoMeetPath(userDetail.getVideoMeetPath());
            userDetailInfo.setUserAmbition(userDetail.getUserAmbition());
            userDetailInfo.setUserBelongType(userDetail.getUserBelongType());
            userDetailInfo.setIsLoggedIn(userDetail.getIsLoggedIn());
            userDetailInfo.setAgentId(userDetail.getAgentId());
        }

        Long parentUserId = user.getParentId();
        R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(parentUserId == null || parentUserId == 0L ? user.getId() : parentUserId);
        if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
            OrderInfoVo orderInfoVo = orderByUserId.getData().get(orderByUserId.getData().size() - 1);
            OrderInfoVo one = orderByUserId.getData().get(0);
            userDetailInfo.setPackageLevel(one.getLevel());
            userDetailInfo.setPackageName(one.getCommodityName());
            userDetailInfo.setExpirationDate(orderInfoVo.getEndDate());
        }
        return R.ok(userDetailInfo);
    }


    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     *
     * @return {@link R }<{@link OpenGovernanceUserDetailsInfo }>
     */
    @Override
    public R<OpenGovernanceUserDetailsInfo> getMobileAccount(long userId) {
        R<UserVo> byPhone = userBll.getById(userId);
        if (byPhone.fail()) {
            return R.error(byPhone.getCode(), byPhone.getMsg());
        }
        UserVo user = byPhone.getData();
        OpenGovernanceUserDetailsInfo userDetailInfo = new OpenGovernanceUserDetailsInfo();
        userDetailInfo.setUserId(user.getId());
        userDetailInfo.setTenantId(user.getActiveTenantId());
        userDetailInfo.setParentId(user.getParentId());
        userDetailInfo.setUserType(user.getUserType());
        userDetailInfo.setUsername(user.getUsername());
        userDetailInfo.setNickName(user.getNickName());
        userDetailInfo.setPhone(user.getPhone());
        userDetailInfo.setPassword(user.getPassword());
        userDetailInfo.setRegisterDate(user.getCreateDate());
        userDetailInfo.setUserStatus(user.getStatus());

        BeanUtil.copyProperties(user, userDetailInfo);
        R<UserDetailsInfoVo> userDetailResult = userDetailsBll.getByUserId(user.getId());
        if (userDetailResult.getData() != null) {
            UserDetailsInfoVo userDetail = userDetailResult.getData();
            userDetailInfo.setId(userDetail.getId());
            userDetailInfo.setTradeId(userDetail.getTradeId());
            userDetailInfo.setCompanyId(userDetail.getCompanyId());
            userDetailInfo.setAnchorType(userDetail.getAnchorType());
            userDetailInfo.setPosition(userDetail.getPosition());
            userDetailInfo.setEmail(userDetail.getEmail());
            userDetailInfo.setRealName(userDetail.getRealName());
            userDetailInfo.setSex(userDetail.getSex());
            userDetailInfo.setBirthday(userDetail.getBirthday());
            userDetailInfo.setAddress(userDetail.getAddress());
            userDetailInfo.setChannelId(userDetail.getChannelId());
            userDetailInfo.setSaleId(userDetail.getSaleId());
            userDetailInfo.setWxName(userDetail.getWxName());
            userDetailInfo.setAgentSaleId(userDetail.getAgentSaleId());
            userDetailInfo.setAgentSaleName(userDetail.getAgentSaleName());
            userDetailInfo.setIsShow(userDetail.getIsShow());
            userDetailInfo.setVideoMeetPath(userDetail.getVideoMeetPath());
            userDetailInfo.setUserAmbition(userDetail.getUserAmbition());
            userDetailInfo.setUserBelongType(userDetail.getUserBelongType());
            userDetailInfo.setIsLoggedIn(userDetail.getIsLoggedIn());
            userDetailInfo.setAgentId(userDetail.getAgentId());
        }

        Long parentUserId = user.getParentId();
        R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(parentUserId == null || parentUserId == 0L ? user.getId() : parentUserId);
        if (ObjectUtil.isNotEmpty(orderByUserId.getData())) {
            OrderInfoVo orderInfoVo = orderByUserId.getData().get(orderByUserId.getData().size() - 1);
            OrderInfoVo one = orderByUserId.getData().get(0);
            userDetailInfo.setPackageLevel(one.getLevel());
            userDetailInfo.setPackageName(one.getCommodityName());
            userDetailInfo.setExpirationDate(orderInfoVo.getEndDate());
        }
        return R.ok(userDetailInfo);
    }


    /**
     * 获取租户下所有用户列表
     *
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link List<OpenGovernanceUserDetailsInfo> }>
     */
    @Override
    public R<List<OpenGovernanceUserDetailsInfo>> getAllUserList(long tenantId) {
        List<UserEntity> userList = new BatchQuery<>((limit, idx) -> {
            return userService.lambdaQuery().eq(UserEntity::getActiveTenantId, tenantId)
                .eq(UserEntity::getIsDeleted, 0)
                .ge(idx != null, UserEntity::getId, idx)
                .last("limit " + limit)
                .list();
        }, UserEntity::getId).get();
        if (EmptyUtil.isEmpty(userList)) {
            return R.error("无数据");
        }
        Optional<UserEntity> optionalUser = userList.stream().filter(user -> Objects.equals(user.getUserType(), 0)).findFirst();
        if (optionalUser.isEmpty()) {
            return R.error("找不到主账户");
        }
        UserEntity user = optionalUser.get();
        R<List<OrderInfoVo>> orderByUserId = orderBll.getOrderByUserId(user.getId());
        List<Long> userIds = userList.stream().map(UserEntity::getId)
            .toList();
        Map<Long, UserDetailsVo> userDEtailMap = CollUtil.split(userIds, 1000)
            .stream().flatMap(ids -> {
                List<UserDetailsVo> detailsVoList = userDetailsBll.listByUserIds(ids);
                if (detailsVoList == null) {
                    return Stream.empty();
                }
                return detailsVoList.stream();
            }).collect(Collectors.toMap(UserDetailsVo::getUserId, Function.identity(), FunctionUtil::mergeFirst));

        // 组装返回数据
        List<OpenGovernanceUserDetailsInfo> resultList = userList.stream()
            .map(userEntity -> buildUserDetailInfo(userEntity, userDEtailMap.get(userEntity.getId()), orderByUserId))
            .toList();

        return R.ok(resultList);
    }

    /**
     * 构建用户详细信息对象
     *
     * @param userEntity 用户实体
     * @param userDetailsVo 用户详情VO
     * @param orderResult 订单结果
     * @return OpenGovernanceUserDetailsInfo
     */
    private OpenGovernanceUserDetailsInfo buildUserDetailInfo(UserEntity userEntity,
                                                               UserDetailsVo userDetailsVo,
                                                               R<List<OrderInfoVo>> orderResult) {
        OpenGovernanceUserDetailsInfo userDetailInfo = new OpenGovernanceUserDetailsInfo();

        // 设置用户基本信息
        userDetailInfo.setUserId(userEntity.getId());
        userDetailInfo.setTenantId(userEntity.getActiveTenantId());
        userDetailInfo.setParentId(userEntity.getParentId());
        userDetailInfo.setUserType(userEntity.getUserType());
        userDetailInfo.setUsername(userEntity.getUsername());
        userDetailInfo.setNickName(userEntity.getNickName());
        userDetailInfo.setPhone(userEntity.getPhone());
        userDetailInfo.setPassword(userEntity.getPassword());
        userDetailInfo.setRegisterDate(userEntity.getCreateDate());
        userDetailInfo.setUserStatus(userEntity.getStatus());

        // 设置用户详细信息
        if (userDetailsVo != null) {
            userDetailInfo.setId(userDetailsVo.getId());
            userDetailInfo.setTradeId(userDetailsVo.getTradeId());
            userDetailInfo.setCompanyId(userDetailsVo.getCompanyId());
            userDetailInfo.setAnchorType(userDetailsVo.getAnchorType());
            userDetailInfo.setPosition(userDetailsVo.getPosition());
            userDetailInfo.setEmail(userDetailsVo.getEmail());
            userDetailInfo.setRealName(userDetailsVo.getRealName());
            userDetailInfo.setSex(userDetailsVo.getSex());
            userDetailInfo.setBirthday(userDetailsVo.getBirthday());
            userDetailInfo.setAddress(userDetailsVo.getAddress());
            userDetailInfo.setChannelId(userDetailsVo.getChannelId());
            userDetailInfo.setSaleId(userDetailsVo.getSaleId());
            userDetailInfo.setWxName(userDetailsVo.getWxName());
            userDetailInfo.setAgentSaleId(userDetailsVo.getAgentSaleId());
            userDetailInfo.setAgentSaleName(userDetailsVo.getAgentSaleName());
            userDetailInfo.setIsShow(userDetailsVo.getIsShow());
            userDetailInfo.setVideoMeetPath(userDetailsVo.getVideoMeetPath());
            userDetailInfo.setUserAmbition(userDetailsVo.getUserAmbition());
            userDetailInfo.setUserBelongType(userDetailsVo.getUserBelongType());
            userDetailInfo.setIsLoggedIn(userDetailsVo.getIsLoggedIn());
            userDetailInfo.setAgentId(userDetailsVo.getAgentId());
        }

        // 设置订单相关信息
        if (orderResult != null && ObjectUtil.isNotEmpty(orderResult.getData())) {
            List<OrderInfoVo> orderList = orderResult.getData();
            OrderInfoVo firstOrder = orderList.get(0);
            OrderInfoVo lastOrder = orderList.get(orderList.size() - 1);
            userDetailInfo.setPackageLevel(firstOrder.getLevel());
            userDetailInfo.setPackageName(firstOrder.getCommodityName());
            userDetailInfo.setExpirationDate(lastOrder.getEndDate());
        }

        return userDetailInfo;
    }

    @Override
    public R<UserLoginVo> loginOnline(LoginBo loginBo, HttpServletRequest request) {
        String loginIp = GetIPUtils.getIpAddr(request);

        return userBll.loginOnline(loginBo, loginIp);
    }

    @Override
    public R<PageUtils<UserListManageVo>> listManage(UserListBo userListBo) {
        return userBll.listManage(userListBo);
    }

    @Override
    public R<List<UserInfoExportVo>> exportUserInfoList(UserListBo userListBo) {

        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
//        wrapper.ge("create_date",userListBo.getStartTime());
//        wrapper.le("create_date",userListBo.getEndTime());
        wrapper.between("create_date", userListBo.getStartTime(), userListBo.getEndTime());

        List<UserEntity> userList = this.userService.list(wrapper);

        List<com.jiuyu.replay.generic.vo.power.UserVo> userInfoVo = BeanUtil.copyToList(userList, com.jiuyu.replay.generic.vo.power.UserVo.class);

        return anchorUrlBll.exportUserInfoList(userInfoVo);
    }

    @Override
    public R<String> getLoginTempToken() {
        UserCacheVo user = GlobalObject.getLocalUser();
        return userBll.getLoginTempToken(user.getId());
    }

    @Override
    public R<UserLoginVo> loginByTempToken(String tempToken, String loginIp) {

        return userBll.loginByTempToken(tempToken, loginIp);
    }

    @Override
    public R<String> updatePasswordByClient(UpdatePasswordByClientBo updatePasswordBo) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return userBll.updatePasswordByClient(updatePasswordBo, user.getId());
    }


    /**
     * 更新密码 从企业后台同步
     *
     * @param userId   用户ID
     * @param rawPassword 密码
     * @param tenantId 租户ID
     *
     * @return {@link R }<{@link Void }>
     */
    @Override
    public R<Void> updatePasswordByGovernance(long userId, String rawPassword, long tenantId) {
        return userBll.updatePasswordByGovernance(userId, rawPassword, tenantId);
    }

    @Override
    public R<String> getSelfPhone() {
        UserCacheVo user = GlobalObject.getLocalUser();
        return R.ok("获取成功", user.getPhone());
    }

    @Override
    public R<List<UserVo>> platformOperationList() {

        return userBll.listByRoleId(powerProperties.getPlatformOperationRoleId());
    }

    @Override
    public String loadRedisTokensToDatabase() {

        return userBll.loadRedisTokensToDatabase();
    }


    /**
     * 获取后台管理员列表
     *
     * @param userListBo
     * @return
     */
    @Override
    public R<PageUtils<UserVo>> listAdmin(UserListBo userListBo) {
        return userBll.listAdmin(userListBo);
    }

    /**
     * 重置密码（随机六位数）
     *
     * @param id
     * @return
     */
    @Override
    public R<String> resetRandomPassword(Long id) {
        return userBll.resetRandomPassword(id);
    }

    @Override
    public void clientLogoPost(String clientVersion) {
        UserCacheVo user = GlobalObject.getLocalUser();

        // 保存客户端版本
        userBll.saveClientVersion(user.getId(), clientVersion);

        // 同步子账号数量
        userPropertyFeign.syncSubAccountCount(user.getId());

        // 重新统计用户的资产-短视频相关的
        videoExtractProducer.syncUserShortVideoProperty(user.getId(), user.getActiveTenantId());

        // 把所有的生成的html状态修改为失败
        conversationBll.updateUserHtmlFail(user.getId(), user.getActiveTenantId());

        // 把所有生成ai纠正状态修改为失败
        conversationBll.updateUserCorrectFail(user.getId(), user.getActiveTenantId());
    }

    /**
     * 根据手机号获取用户id
     *
     * @param phone 手机号
     *
     * @return {@link Optional }<{@link Long }>
     */
    @Override
    public Optional<Long> getPhoneUserId(String phone) {
        if (EmptyUtil.isEmpty(phone)) {
            return Optional.empty();
        }
        return userService.lambdaQuery()
            .eq(UserEntity::getPhone, phone)
            .select(UserEntity::getId)
            .oneOpt().map(UserEntity::getId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<RegisterVo> registerAppointment(RegisterReqVo reqVo) {
        // 校验验证码
        RegisterBo checkBo = new RegisterBo();
        checkBo.setPhone(reqVo.getPhone());
        checkBo.setCode(reqVo.getCode());
        // 不不校验手机号
        R<String> checkR = userBll.registerTheCheck(checkBo, false);
        if (checkR.getCode() != 0) {
            return R.error(checkR.getCode(), checkR.getMsg());
        }

        // 查手机号是否已注册
        UserVo existingUser = userProducer.getByPhoneAndType(reqVo.getPhone(), 0);
        if (existingUser != null) {
            UserDetailsBo temp = new UserDetailsBo();
            temp.setUserId(existingUser.getId());
            temp.setIsSubmitAppointment(1);
            userDetailsBll.saveUserDetails(temp);
            return buildRegisterResult(existingUser.getId());
        }

        // 从 tb_system_kv 获取 inviteUrlCode
        String inviteUrlCode = null;
        if(StringUtil.isNotEmpty(reqVo.getInviteUrlCode())) {
            inviteUrlCode = reqVo.getInviteUrlCode();
        }
        else {
            SystemKvInfoVo kv = systemKvProducer.getByKey("submit_appointment_invite_url_code");
            if (kv != null) {
                inviteUrlCode = kv.getKvValue();
            }
        }

        // 组装 RegisterBo
        RegisterBo registerBo = new RegisterBo();
        registerBo.setPhone(reqVo.getPhone());
        registerBo.setCode(reqVo.getCode());
        registerBo.setInviteUrlCode(inviteUrlCode);
        registerBo.setAdminUserType(0);

        // 注册
        R<RegisterVo> result = this.register(registerBo);
        if (result.getCode() != 0 || result.getData() == null) {
            return result;
        }

        // 注册成功 → 回填公司/行业/预约状态
        UserVo newUser = userProducer.getByPhoneAndType(reqVo.getPhone(), 0);
        UserDetailsBo detailsBo = new UserDetailsBo();
        detailsBo.setUserId(newUser.getId());
        detailsBo.setTradeId(reqVo.getTradeId());
        detailsBo.setIsSubmitAppointment(1);
        if (ObjectUtil.isNotEmpty(reqVo.getCompanyName())) {
            detailsBo.setAnchorType(1);
            CompanyBo companyBo = new CompanyBo();
            companyBo.setName(reqVo.getCompanyName());
            detailsBo.setCompany(companyBo);
        }
        userDetailsBll.saveUserDetails(detailsBo);

        return result;
    }

    private R<RegisterVo> buildRegisterResult(Long userId) {
        RegisterVo result = new RegisterVo();
        UserDetailsInfoVo userDetails = ResultUtil.getResult(userDetailsBll.userDetailByUserId(userId));
        if (userDetails != null && ObjectUtil.isNotEmpty(userDetails.getSaleId())) {
            result.setSaleId(userDetails.getSaleId());
            SalesInfoVo sales = salesProducer.info(result.getSaleId());
            if (sales != null && ObjectUtil.isNotEmpty(sales.getQrcodeImgId())) {
                result.setSaleQrcodeImg(fileBll.infoByFileId(sales.getQrcodeImgId()));
            }
        }
        if (ObjectUtil.isEmpty(result.getSaleQrcodeImg())) {
            SystemKvInfoVo kv = systemKvProducer.getByKey("h5_default_qrcode_img_id");
            if (kv != null) {
                result.setSaleQrcodeImg(fileBll.infoByFileId(Long.valueOf(kv.getKvValue())));
            }
        }
        return R.ok(result);
    }
}
