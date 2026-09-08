package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.utils.*;
import com.jiuyu.replay.generic.enums.words.AnchorDeleteStatusEnum;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.words.bo.AnchorUrlPegBo;
import com.jiuyu.replay.words.bo.AnchorUrlUserBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.vo.anchor.AnchorRecordListVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tisheng
 */
@Service
@Slf4j
public class AnchorUrlUserProducerImpl implements AnchorUrlUserProducer {

    @Resource
    AnchorUrlUserService anchorUrlUserService;
    @Resource
    AnchorUrlService anchorUrlService;

    @Override
    public List<AnchorUrlVo> seleByuserId(Long userId) {
        QueryWrapper<AnchorUrlUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        List<AnchorUrlUserEntity> list = anchorUrlUserService.list(queryWrapper);
        if (list != null && !list.isEmpty()) {
            List<String> secUids = list.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
            List<AnchorUrlVo> listsVo = new ArrayList<>();
            if (!secUids.isEmpty()) {
                List<AnchorUrlEntity> secUid = anchorUrlService.list(
                        new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));
                for (AnchorUrlEntity anchorUrlEntity : secUid) {
                    AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                    BeanUtils.copyProperties(anchorUrlEntity, anchorUrlVo);
                    Optional<Long> optionalId = list.stream()
                            .filter(entity -> anchorUrlEntity.getSecUid().equals(entity.getAnchorUrlSecUid()))
                            .map(AnchorUrlUserEntity::getTradeId)
                            .findFirst();
                    anchorUrlVo.setTradeId(optionalId.isPresent() ? optionalId.get() : null);
                    listsVo.add(anchorUrlVo);
                }
            }
            return listsVo;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> saves(List<AnchorUrlUserBo> listAnchorUrlUser, Long userId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<AnchorUrlUserEntity> list = this.anchorUrlUserService.list(wrapper);

        if (list != null && list.size() > 0) {
            List<String> collect = list.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
            listAnchorUrlUser = listAnchorUrlUser.stream()
                    .filter(user -> !collect.contains(user.getAnchorUrlSecUid()))
                    .toList();
        }

        if(listAnchorUrlUser.size() > 0) {
            Set<String> secUids = listAnchorUrlUser.stream().map(AnchorUrlUserBo::getAnchorUrlSecUid).collect(Collectors.toSet());
            List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

            List<AnchorUrlUserEntity> anchorUrlUserEntities = listAnchorUrlUser.stream().map(item -> {
                AnchorUrlUserEntity anchorUrlUserEntity = new AnchorUrlUserEntity();
                BeanUtils.copyProperties(item, anchorUrlUserEntity);
                anchorUrlUserEntity.setId(SnowflakeManager.nextValue());
                anchorUrlUserEntity.setUserId(userId);
                anchorUrlUserEntity.setCreateDate(new Date());
                anchorUrlUserEntity.setUpdateDate(new Date());
                if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                    for (AnchorUrlEntity anchorUrlEntity : anchorUrlEntities) {
                        if(anchorUrlEntity.getSecUid().equals(item.getAnchorUrlSecUid())) {
                            if(!StringUtils.isEmpty(anchorUrlEntity.getAnchorName())) {
                                anchorUrlUserEntity.setFolderName(WindowsFileUtils.sanitizeForFolderName(anchorUrlEntity.getAnchorName()));
                                anchorUrlUserEntity.setRemarksName(anchorUrlEntity.getAnchorName());
                            }
                        }
                    }
                }
                return anchorUrlUserEntity;
            }).toList();
            this.anchorUrlUserService.saveBatch(anchorUrlUserEntities);
        }

        return R.ok("绑定成功");
    }

    /**
     * 删除用户与主播关系
     *
     * @param secUidList
     * @param id
     */
    @Override
    public void deletBySecUid(String secUidList, Long id) {
        boolean remove = anchorUrlUserService.remove(new QueryWrapper<AnchorUrlUserEntity>()
                .eq("user_id", id)
                .eq("anchor_url_sec_uid", secUidList));
    }

    /**
     * 服务端根据用户id分页查询绑定主播列表
     *
     * @param anchorUrlPegBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> seletByUserId(AnchorUrlPegBo anchorUrlPegBo) {
        //根据主播名称查询
        QueryWrapper<AnchorUrlEntity> wrapper = new QueryWrapper<>();
        if (anchorUrlPegBo.getAnchorName() != null) {
            wrapper.like("anchor_name", anchorUrlPegBo.getAnchorName());
        }
        if (anchorUrlPegBo.getPlatform() != null) {
            wrapper.eq("platform", anchorUrlPegBo.getPlatform());
        }
        if (anchorUrlPegBo.getTradeId() != null) {

        }
        List<AnchorUrlEntity> list1 = anchorUrlService.list(wrapper);

        QueryWrapper<AnchorUrlUserEntity> queryWrapper = new QueryWrapper<>();

        if (anchorUrlPegBo.getAnchorName() != null || anchorUrlPegBo.getPlatform() != null || anchorUrlPegBo.getTradeId() != null) {

            if (list1 != null && list1.size() > 0) {
                List<String> list = list1.stream().map(AnchorUrlEntity::getSecUid).toList();
                queryWrapper.in("anchor_url_sec_uid", list);
            } else {
                return R.ok(null);
            }
        }
        if (anchorUrlPegBo.getUserIds() != null && anchorUrlPegBo.getUserIds().size() > 0) {
            queryWrapper.in("user_id", anchorUrlPegBo.getUserIds());
        }
        queryWrapper.orderByDesc("create_date");
        IPage<AnchorUrlUserEntity> iPage = anchorUrlUserService.page(new Query<AnchorUrlUserEntity>().getPage(anchorUrlPegBo.getPage(),
                anchorUrlPegBo.getLimit()), queryWrapper);
        PageUtils<AnchorUrlVo> pageUtils = new PageUtils<>(anchorUrlPegBo.getPage(), anchorUrlPegBo.getLimit(), iPage);

        List<AnchorUrlUserEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<AnchorUrlVo> listVo = records.stream().map(item -> {
                AnchorUrlVo anchorUrlVo = new AnchorUrlVo();
                QueryWrapper<AnchorUrlEntity> wrappers = new QueryWrapper<>();
                wrappers.eq("sec_uid", item.getAnchorUrlSecUid());
                AnchorUrlEntity one = anchorUrlService.getOne(wrappers);
                BeanUtils.copyProperties(one, anchorUrlVo);
                anchorUrlVo.setIsDeleted(item.getIsDeleted());
                anchorUrlVo.setUpdateDate(item.getUpdateDate());
                anchorUrlVo.setCreateDate(item.getCreateDate());
                anchorUrlVo.setUserId(item.getUserId());
                return anchorUrlVo;
            }).toList();
            pageUtils.setList(listVo);
        }
        return R.ok(pageUtils);

    }

    /***
     * 根据用户id 查看用户绑定了多少主播
     * @param userId
     * @return
     */
    @Override
    public Integer sum(Long userId) {
        return anchorUrlUserService.sum(userId);
    }

    /**
     * 服务端根据seu_uid获取用户信息
     *
     * @param secUid
     * @return
     */
    @Override
    public List<AnchorUrlUserEntity> selectBySecUid(String secUid) {
        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("anchor_url_sec_uid", secUid);
        List<AnchorUrlUserEntity> list = anchorUrlUserService.list(wrapper);

        return list;
    }

    @Override
    public AnchorUrlUserVo getByUserIdAndSecUid(Long userId, String anchorUrlSecUid) {

        AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(
                new QueryWrapper<AnchorUrlUserEntity>().eq("anchor_url_sec_uid", anchorUrlSecUid).eq("user_id", userId));

        if (anchorUrlUserEntity != null) {
            AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
            BeanUtils.copyProperties(anchorUrlUserEntity, anchorUrlUserVo);
            return anchorUrlUserVo;
        }

        return null;
    }

    @Override
    public void updateById(AnchorUrlUserBo anchorUrlUserBo) {

        AnchorUrlUserEntity anchorUrlUserEntity = new AnchorUrlUserEntity();
        BeanUtils.copyProperties(anchorUrlUserBo, anchorUrlUserEntity);
        anchorUrlUserEntity.setUpdateDate(new Date());

        if(anchorUrlUserBo.getIsRemoveRecord() != null && anchorUrlUserBo.getIsRemoveRecord().equals(AnchorDeleteStatusEnum.RECORD_LIST_DELETE.getStatus())) {
            anchorUrlUserEntity.setDeleteDate(new Date());
        }

        this.anchorUrlUserService.updateById(anchorUrlUserEntity);
    }

    @Override
    public Long updateBarrageMonitoring(Long userId, Long tenantId, String secUid, Integer isBarrageMonitoring, Long currentPropertyNum) {
        // 获取当前的弹幕监控位数量
        AnchorUrlUserEntity anchorUrlUser = this.anchorUrlUserService.getOne(new QueryWrapper<AnchorUrlUserEntity>()
                        .select("IFNULL(count(*), 0) as user_id")
                        .lambda()
//                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                        .eq(AnchorUrlUserEntity::getIsBarrageMonitoring, 1)
        );

        // 开启弹幕需要校验是否超出限制
        if (isBarrageMonitoring == 1 && ObjectUtil.compare(currentPropertyNum, anchorUrlUser.getUserId()) <= 0) {
            RRException.create("监控弹幕直播间数量不足");
        }

        this.anchorUrlUserService.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .set(AnchorUrlUserEntity::getIsBarrageMonitoring, isBarrageMonitoring)
        );

        // 查询当前用户开启的弹幕监控数量
        AnchorUrlUserEntity serviceOne = this.anchorUrlUserService.getOne(new QueryWrapper<AnchorUrlUserEntity>()
                        .select("IFNULL(count(*), 0) as user_id")
                        .lambda()
//                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                        .eq(AnchorUrlUserEntity::getIsBarrageMonitoring, 1)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
        );
        RRException.isNotEmpty(serviceOne, "查询主播数量失败");
        return serviceOne.getUserId();
    }

    @Override
    public void updateAutoUploadCloud(Long userId, String secUid, Integer isAutoUploadCloud, Long tenantId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("anchor_url_sec_uid", secUid);
        wrapper.eq("tenant_id", tenantId);
        AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(wrapper);
        if (anchorUrlUserEntity != null) {
            anchorUrlUserEntity.setIsAutoUploadCloud(isAutoUploadCloud);
            this.anchorUrlUserService.updateById(anchorUrlUserEntity);
        }

    }

    @Override
    public void closeAnchorBarrageNum(List<Long> userIds) {
        anchorUrlUserService.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                .in(AnchorUrlUserEntity::getUserId, userIds)
                .set(AnchorUrlUserEntity::getIsBarrageMonitoring, 0)
        );
    }

    @Override
    public void closeAutoUploadCloud(List<Long> userIds) {
        anchorUrlUserService.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                .in(AnchorUrlUserEntity::getUserId, userIds)
                .set(AnchorUrlUserEntity::getIsAutoUploadCloud, 0)
        );
    }

    @Override
    public Long queryBarrageMonitoring(AnchorUrlUserBo anchorUrlUserBo) {
        if (ObjectUtil.equals(anchorUrlUserBo.getIsRemoveRecord(), 1)) {
            anchorUrlUserService.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                    .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, anchorUrlUserBo.getAnchorUrlSecUid())
                    .eq(AnchorUrlUserEntity::getUserId, anchorUrlUserBo.getUserId())
                    .set(AnchorUrlUserEntity::getIsBarrageMonitoring, 0)
            );
        }
        AnchorUrlUserEntity serviceOne = anchorUrlUserService.getOne(new QueryWrapper<AnchorUrlUserEntity>()
                .select("count(*) as is_barrage_monitoring")
                .lambda()
                .eq(AnchorUrlUserEntity::getUserId, anchorUrlUserBo.getUserId())
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                .eq(AnchorUrlUserEntity::getIsBarrageMonitoring, 1)
        );
        return ObjectUtil.isNotEmpty(serviceOne) ? serviceOne.getIsBarrageMonitoring().longValue() : 0L;
    }

    @Override
    public List<AnchorUrlUserVo> listByUserId(Long userId) {
        List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(
                new QueryWrapper<AnchorUrlUserEntity>().eq("user_id", userId));

        if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
            List<AnchorUrlUserVo> anchorUrlUserVos = anchorUrlUserEntities.stream().map(item -> {
                AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
                BeanUtils.copyProperties(item, anchorUrlUserVo);
                return anchorUrlUserVo;
            }).toList();

            return anchorUrlUserVos;
        }
        return null;
    }

    @Override
    public Boolean updateAnchorTop(Long userId, Long tenantId, String secUid, Integer action, String addTopTime) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("anchor_url_sec_uid", secUid);
        wrapper.eq("tenant_id", tenantId);
        AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(wrapper);
        if (anchorUrlUserEntity != null) {
            anchorUrlUserEntity.setIsTop(action);
            anchorUrlUserEntity.setAddTopTime(addTopTime);
            this.anchorUrlUserService.updateById(anchorUrlUserEntity);
            return true;
        }

        return false;
    }

    @Override
    public void updateAnchorLastRecordTime(Long userId, String secUid, String lastRecordTime, Long tenantId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("anchor_url_sec_uid", secUid);
        wrapper.eq("tenant_id", tenantId);
        AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(wrapper);
        if (anchorUrlUserEntity != null) {
            anchorUrlUserEntity.setLastRecordTime(lastRecordTime);
            this.anchorUrlUserService.updateById(anchorUrlUserEntity);
        }
    }

    @Override
    public PageUtils<AnchorRecordListVo> clientVideoList(ClientAnchorListBo clientAnchorListBo) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientAnchorListBo.getUserId())) {
            wrapper.eq("user_id", clientAnchorListBo.getUserId());
        }
        if(!StringUtils.isEmpty(clientAnchorListBo.getTenantId())) {
            wrapper.eq("tenant_id", clientAnchorListBo.getTenantId());
        }
        if(clientAnchorListBo.getSecUidArr() != null && clientAnchorListBo.getSecUidArr().size() > 0) {
            wrapper.in("anchor_url_sec_uid", clientAnchorListBo.getSecUidArr());
        }
        if(!StringUtils.isEmpty(clientAnchorListBo.getRecordStatus())) {
            wrapper.eq("record_status", clientAnchorListBo.getRecordStatus());
        }
        if(!StringUtils.isEmpty(clientAnchorListBo.getIsRemoveRecord())) {
            wrapper.eq("is_remove_record", clientAnchorListBo.getIsRemoveRecord());
        }
        if(!StringUtils.isEmpty(clientAnchorListBo.getTradeId())) {
            wrapper.eq("trade_id", clientAnchorListBo.getTradeId());
        }

        wrapper.orderByDesc("is_top", "add_top_time", "last_record_time");

        IPage<AnchorUrlUserEntity> iPage = anchorUrlUserService.page(new Query<AnchorUrlUserEntity>().getPageNoSort(clientAnchorListBo.getPage(), clientAnchorListBo.getLimit()), wrapper);

        PageUtils<AnchorRecordListVo> pageUtils = new PageUtils<>(clientAnchorListBo.getPage(), clientAnchorListBo.getLimit(), iPage);

        List<AnchorUrlUserEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AnchorRecordListVo> vos = records.stream().map(item -> {
                AnchorRecordListVo anchorInfoVo = new AnchorRecordListVo();
                BeanUtils.copyProperties(item, anchorInfoVo);
                return anchorInfoVo;
            }).toList();

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AnchorUrlUserVo getUserAnchorBySecUid(String secUid, Long userId, Long tenantId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("anchor_url_sec_uid", secUid);
        if(userId != null) {
            wrapper.eq("user_id", userId);
        }
        if(tenantId != null) {
            wrapper.eq("tenant_id", tenantId);
        }
        List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(wrapper);

        if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
            AnchorUrlUserEntity anchorUrlUserEntity = anchorUrlUserEntities.get(0);
            AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
            BeanUtils.copyProperties(anchorUrlUserEntity, anchorUrlUserVo);
            return anchorUrlUserVo;
        }

        return null;
    }

    @Override
    public List<AnchorUrlUserVo> listByUser(Long userId, Long tenantId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        if(userId != null) {
            wrapper.eq("user_id", userId);
        }
        if(tenantId != null) {
            wrapper.eq("tenant_id", tenantId);
        }
        List<AnchorUrlUserEntity> anchorUrlUserList = this.anchorUrlUserService.list(wrapper);

        if (anchorUrlUserList != null && !anchorUrlUserList.isEmpty()) {

            List<String> secUids = anchorUrlUserList.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
            List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

            return anchorUrlUserList.stream().map(item -> {
                AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
                BeanUtils.copyProperties(item, anchorUrlUserVo);

                if (anchorUrlEntities != null && !anchorUrlEntities.isEmpty()) {
                    for (AnchorUrlEntity anchorUrlEntity : anchorUrlEntities) {
                        if (anchorUrlEntity.getSecUid().equals(anchorUrlUserVo.getAnchorUrlSecUid())) {
                            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                            BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
                            anchorUrlUserVo.setAnchorInfo(anchorUrlInfoVo);
                            break;
                        }
                    }
                }

                return anchorUrlUserVo;
            }).filter(item -> item.getAnchorInfo() != null).toList();
        }

        return null;
    }

    @Override
    public List<AnchorUrlUserVo> listByUserSimply(Long userId, Long tenantId) {
        BusinessException.requireNonEmpty(userId, "userId不能为空");
        BusinessException.requireNonEmpty(tenantId, "tenantId不能为空");
        List<AnchorUrlUserEntity> list = this.anchorUrlUserService.list(new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
        );
        if (ObjectUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return BeanUtil.copyToList(list, AnchorUrlUserVo.class);
    }

    @Override
    public List<AnchorUrlUserVo> monitoringPositionByUser(Long userId, Long tenantId) {
        BusinessException.requireNonEmpty(userId, "userId不能为空");
        BusinessException.requireNonEmpty(tenantId, "tenantId不能为空");
        List<AnchorUrlUserEntity> list = this.anchorUrlUserService.list(new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .select(AnchorUrlUserEntity::getId,
                        AnchorUrlUserEntity::getUserId,
                        AnchorUrlUserEntity::getTenantId,
                        AnchorUrlUserEntity::getAnchorUrlSecUid,
                        AnchorUrlUserEntity::getIsBarrageMonitoring,
                        AnchorUrlUserEntity::getIsDataViewing)
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
        );
        if (ObjectUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        return BeanUtil.copyToList(list, AnchorUrlUserVo.class);
    }

    @Override
    public List<AnchorUrlUserVo> listBySecUids(List<String> secUidList, Long userId, Long tenantId) {

        QueryWrapper<AnchorUrlUserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);
        wrapper.in("anchor_url_sec_uid", secUidList);
        List<AnchorUrlUserEntity> anchorUrlUserList = this.anchorUrlUserService.list(wrapper);

        if (anchorUrlUserList != null && !anchorUrlUserList.isEmpty()) {

            List<String> secUids = anchorUrlUserList.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).toList();
            List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

            return anchorUrlUserList.stream().map(item -> {
                AnchorUrlUserVo anchorUrlUserVo = new AnchorUrlUserVo();
                BeanUtils.copyProperties(item, anchorUrlUserVo);

                if (anchorUrlEntities != null && !anchorUrlEntities.isEmpty()) {
                    for (AnchorUrlEntity anchorUrlEntity : anchorUrlEntities) {
                        if (anchorUrlEntity.getSecUid().equals(anchorUrlUserVo.getAnchorUrlSecUid())) {
                            AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                            BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
                            anchorUrlUserVo.setAnchorInfo(anchorUrlInfoVo);
                            break;
                        }
                    }
                }

                return anchorUrlUserVo;
            }).toList();
        }

        return null;
    }

    @Override
    public void save(AnchorUrlUserBo anchorUrlUserBo) {

        AnchorUrlUserEntity anchorUrlUserEntity = new AnchorUrlUserEntity();
        BeanUtils.copyProperties(anchorUrlUserBo, anchorUrlUserEntity);
        anchorUrlUserEntity.setId(SnowflakeManager.nextValue());
        anchorUrlUserEntity.setCreateDate(new Date());
        anchorUrlUserEntity.setUpdateDate(new Date());

        this.anchorUrlUserService.save(anchorUrlUserEntity);
    }

    @Override
    public List<AnchorUrlUserVo> listByTenantId(Long tenantId) {

        return this.listByUser(null, tenantId);
    }

    @Override
    public boolean updateDataViewing(Long userId, Long tenantId, String secUid, Integer status) {
        // 判断空
        if (ObjectUtil.isEmpty(userId) || ObjectUtil.isEmpty(tenantId) || ObjectUtil.isEmpty(secUid) || ObjectUtil.isEmpty(status)) {
            return false;
        }
        return anchorUrlUserService.update(new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .set(AnchorUrlUserEntity::getIsDataViewing, status)
        );
    }

    @Override
    public List<AnchorUrlUserVo> listBySecUidsAndUserIds(List<String> secUidList, List<Long> userIds) {

        List<AnchorUrlUserEntity> list = anchorUrlUserService.list(new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .in(AnchorUrlUserEntity::getUserId, userIds)
                .in(AnchorUrlUserEntity::getAnchorUrlSecUid, secUidList)
        );

        if (ObjectUtil.isNotEmpty(list)) {
            return BeanConvertUtils.convertList(list, AnchorUrlUserVo.class);
        }

        return new ArrayList<>();
    }


    /**
     * 视频号取消授权处理
     *
     * @param channelId 视频号id
     * @param userIds   用户ids
     *
     * @return 是否成功
     */
    @Override
    public R<Void> cancelChannel(String channelId, List<Long> userIds) {
        if (EmptyUtil.isEmpty(channelId) || EmptyUtil.isEmpty(userIds)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "参数错误");
        }
        boolean isChannel = anchorUrlService.lambdaQuery().eq(AnchorUrlEntity::getSecUid, channelId)
            .eq(AnchorUrlEntity::getPlatform, 2)
            .exists();
        if (Boolean.FALSE.equals(isChannel)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "视频号不存在");
        }
        boolean update = anchorUrlUserService.lambdaUpdate().in(AnchorUrlUserEntity::getUserId, userIds)
            .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, channelId)
            .set(AnchorUrlUserEntity::getIsDataViewing, 0)
            .set(AnchorUrlUserEntity::getIsAutoRecord, 0)
            .set(AnchorUrlUserEntity::getIsAutoUploadCloud, 0)
            .set(AnchorUrlUserEntity::getIsBarrageMonitoring, 0)
            .set(AnchorUrlUserEntity::getIsAutoDiagnosis, 0)
            .set(AnchorUrlUserEntity::getAuthChannelStatus, 2)
            .update();
        return R.ok();
    }

    /**
     * 减少数据诊断生成数量
     *
     * @param secUid   主播secUid
     * @param userId   用户id
     * @param tenantId 租户id
     * @param num      减少数量
     */
    @Override
    public void minusDataDiagnosisGenerateNum(String secUid, Long userId, Long tenantId, int num) {
        AnchorUrlUserEntity one = anchorUrlUserService.lambdaQuery()
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .last("limit 1")
                .one();
        if (one == null || one.getDiagnosisGenerateNum() == null || one.getDiagnosisGenerateNum() <= 0) {
            return;
        }
        log.info("减少数据诊断生成数量，secUid:{}, userId:{}, tenantId:{}, num:{}", secUid, userId, tenantId, num);
        anchorUrlUserService.lambdaUpdate()
                .eq(AnchorUrlUserEntity::getId, one.getId())
                .setSql("diagnosis_generate_num = if(diagnosis_generate_num - {0} < 0, 0, diagnosis_generate_num - {1})", num, num)
                .update();
    }

    @Override
    public Integer countByTenantId(Long tenantId) {
        if (ObjectUtil.isEmpty(tenantId)) {
            return 0;
        }
        Long count = anchorUrlUserService.lambdaQuery()
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getIsDeleted, 0)
                .count();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public Integer countByTenantIdAndAccountType(Long tenantId, Integer accountType) {
        if (ObjectUtil.isEmpty(tenantId) || ObjectUtil.isEmpty(accountType)) {
            return 0;
        }
        Long count = anchorUrlUserService.lambdaQuery()
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getAccountType, accountType)
                .eq(AnchorUrlUserEntity::getIsDeleted, 0)
                .count();
        return count == null ? 0 : count.intValue();
    }

    /**
     * 统计当前用户名下指定开关字段为 1 的有效直播间数量（按 tenantId 隔离 + isRemoveRecord=0 过滤）
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param switchField 开关字段名（"isScriptQualityInspection" / "isScriptFidelityMonitor" / "isInteractionPatrol"）
     * @return 该开关已开启的直播间数量
     */
    @Override
    public Long countOpenSwitch(Long userId, Long tenantId, String switchField) {
        LambdaQueryWrapper<AnchorUrlUserEntity> wrapper = new LambdaQueryWrapper<AnchorUrlUserEntity>()
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0);
        switch (switchField) {
            case "isScriptQualityInspection":
                wrapper.eq(AnchorUrlUserEntity::getIsScriptQualityInspection, 1);
                break;
            case "isScriptFidelityMonitor":
                wrapper.eq(AnchorUrlUserEntity::getIsScriptFidelityMonitor, 1);
                break;
            case "isInteractionPatrol":
                wrapper.eq(AnchorUrlUserEntity::getIsInteractionPatrol, 1);
                break;
            default:
                throw new IllegalArgumentException("不支持的开关字段：" + switchField);
        }
        return anchorUrlUserService.count(wrapper);
    }

    @Override
    public java.util.List<com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo> listDataHubAccountsByTenantIds(
            java.util.Collection<Long> tenantIds, boolean includeRemoved) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return java.util.List.of();
        }
        List<AnchorUrlUserEntity> relations = anchorUrlUserService.lambdaQuery()
                .in(AnchorUrlUserEntity::getTenantId, tenantIds)
                .eq(AnchorUrlUserEntity::getIsDeleted, 0)
                .eq(!includeRemoved, AnchorUrlUserEntity::getIsRemoveRecord, 0)
                .list();
        if (EmptyUtil.isEmpty(relations)) {
            return java.util.List.of();
        }
        // 账号档案是 DB 结果派生的无界列表，按 500 分批捞（tb_anchor_url 无租户维度）
        List<String> secUids = relations.stream()
                .map(AnchorUrlUserEntity::getAnchorUrlSecUid)
                .filter(ObjectUtil::isNotEmpty)
                .distinct()
                .toList();
        java.util.Map<String, AnchorUrlEntity> anchorMap = new java.util.HashMap<>();
        for (List<String> part : cn.hutool.core.collection.CollUtil.split(secUids, 500)) {
            anchorUrlService.lambdaQuery()
                    .select(AnchorUrlEntity::getSecUid, AnchorUrlEntity::getAnchorName, AnchorUrlEntity::getPlatform)
                    .in(AnchorUrlEntity::getSecUid, part)
                    .list()
                    .forEach(a -> anchorMap.putIfAbsent(a.getSecUid(), a));
        }
        return relations.stream().map(rel -> {
            com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo vo =
                    new com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo();
            vo.setTenantId(rel.getTenantId());
            vo.setSecUid(rel.getAnchorUrlSecUid());
            vo.setAccountType(rel.getAccountType());
            vo.setAddedByUserId(rel.getUserId());
            vo.setIsRemoveRecord(rel.getIsRemoveRecord());
            vo.setCreateDate(rel.getCreateDate());
            vo.setDeleteDate(rel.getDeleteDate());
            vo.setAuthJlbyStatus(rel.getAuthJlbyStatus());
            vo.setAuthJlbyStatusTime(rel.getAuthJlbyStatusTime());
            vo.setAuthQcStatus(rel.getAuthQcStatus());
            vo.setAuthQcStatusTime(rel.getAuthQcStatusTime());
            vo.setAuthChannelStatus(rel.getAuthChannelStatus());
            vo.setAuthLifeStatus(rel.getAuthLifeStatus());
            vo.setAuthLifeStatusTime(rel.getAuthLifeStatusTime());
            AnchorUrlEntity anchor = anchorMap.get(rel.getAnchorUrlSecUid());
            if (anchor != null) {
                vo.setAnchorName(anchor.getAnchorName());
                vo.setPlatform(anchor.getPlatform());
            }
            return vo;
        }).toList();
    }
}
