package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.UserVideoAppealBo;
import com.jiuyu.replay.words.bo.UserVideoAppealListBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.entity.UserVideoAppealEntity;
import com.jiuyu.replay.words.producer.UserVideoAppealProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.repository.service.UserVideoAppealService;
import com.jiuyu.replay.words.vo.UserVideoAppealInfoVo;
import com.jiuyu.replay.words.vo.UserVideoAppealListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Service
public class UserVideoAppealProducerImpl implements UserVideoAppealProducer {

    @Resource
    private UserVideoAppealService userVideoAppealService;

    @Resource
    private AnchorUrlService anchorUrlService;

    @Resource
    private AnchorVideoService anchorVideoService;


    @Override
    public PageUtils<UserVideoAppealListVo> queryPage(UserVideoAppealListBo userVideoAppealListBo) {
        LambdaQueryWrapper<UserVideoAppealEntity> wrapper = new LambdaQueryWrapper<UserVideoAppealEntity>()
                // 用户id
                .eq(ObjectUtil.isNotEmpty(userVideoAppealListBo.getUserId()), UserVideoAppealEntity::getUserId, userVideoAppealListBo.getUserId())
                // 昵称
                .like(ObjectUtil.isNotEmpty(userVideoAppealListBo.getNickName()), UserVideoAppealEntity::getNickName, userVideoAppealListBo.getNickName())
                // 直播间账号昵称
                .like(ObjectUtil.isNotEmpty(userVideoAppealListBo.getLiveUserName()), UserVideoAppealEntity::getLiveUserName, userVideoAppealListBo.getLiveUserName())
                // 公司名称
                .like(ObjectUtil.isNotEmpty(userVideoAppealListBo.getCompanyName()), UserVideoAppealEntity::getCompanyName, userVideoAppealListBo.getCompanyName())
                // 联系人
                .like(ObjectUtil.isNotEmpty(userVideoAppealListBo.getContacts()), UserVideoAppealEntity::getContacts, userVideoAppealListBo.getContacts())
                // 手机号码
                .like(ObjectUtil.isNotEmpty(userVideoAppealListBo.getPhone()), UserVideoAppealEntity::getPhone, userVideoAppealListBo.getPhone())
                // 状态 0待处理，1已处理
                .eq(ObjectUtil.isNotEmpty(userVideoAppealListBo.getStatus()), UserVideoAppealEntity::getStatus, userVideoAppealListBo.getStatus())
                // 开始的创建时间
                .ge(ObjectUtil.isNotEmpty(userVideoAppealListBo.getStartCreateDate()), UserVideoAppealEntity::getCreateDate, userVideoAppealListBo.getStartCreateDate())
                // 结束的创建时间
                .le(ObjectUtil.isNotEmpty(userVideoAppealListBo.getEndCreateDate()), UserVideoAppealEntity::getCreateDate, userVideoAppealListBo.getEndCreateDate())
                ;

        IPage<UserVideoAppealEntity> iPage = userVideoAppealService.page(new Query<UserVideoAppealEntity>().getPage(userVideoAppealListBo.getPage(), userVideoAppealListBo.getLimit()), wrapper);

        PageUtils<UserVideoAppealListVo> pageUtils = new PageUtils<>(userVideoAppealListBo.getPage(), userVideoAppealListBo.getLimit(), iPage);

        List<UserVideoAppealEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UserVideoAppealListVo> vos = records.stream().map(item -> {
                UserVideoAppealListVo userVideoAppealVo = new UserVideoAppealListVo();
                BeanUtils.copyProperties(item, userVideoAppealVo);
                return userVideoAppealVo;
            }).collect(Collectors.toList());

            // 查询别的值
            if (ObjectUtil.isNotEmpty(vos)){
                List<String> anchorUrlIds = vos.stream().map(UserVideoAppealListVo::getAnchorUrlId).filter(ObjectUtil::isNotEmpty).distinct().toList();

                // 回显直播名称
                if (ObjectUtil.isNotEmpty(anchorUrlIds)){
                    List<AnchorUrlEntity> anchorUrlNames = anchorUrlService.list(new LambdaQueryWrapper<AnchorUrlEntity>()
                            .in(AnchorUrlEntity::getSecUid, anchorUrlIds));
                    DataUtils.setFieldNameById(vos, "anchorUrlId", "anchorUrlName", anchorUrlNames, "secUid", "anchorName");
                }

                // 回显视频名称
                List<String> anchorVideoIds = vos.stream().map(UserVideoAppealListVo::getAnchorVideoId).filter(ObjectUtil::isNotEmpty).distinct().toList();
                if (ObjectUtil.isNotEmpty(anchorVideoIds)){
                    List<AnchorVideoEntity> anchorVideoNames = anchorVideoService.list(new LambdaQueryWrapper<AnchorVideoEntity>()
                            .in(AnchorVideoEntity::getVideoId, anchorVideoIds));
                    DataUtils.setFieldNameById(vos, "anchorVideoId", "anchorVideoName", anchorVideoNames, "videoId", "videoName");
                }
            }

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserVideoAppealInfoVo info(Long id) {

        UserVideoAppealEntity userVideoAppealEntity = userVideoAppealService.getById(id);
        if(userVideoAppealEntity != null) {
            UserVideoAppealInfoVo userVideoAppealInfoVo = new UserVideoAppealInfoVo();
            BeanUtils.copyProperties(userVideoAppealEntity, userVideoAppealInfoVo);
            return userVideoAppealInfoVo;
        }

        return null;
    }

    /**
     * 新增用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
     public UserVideoAppealInfoVo save(UserVideoAppealBo userVideoAppealBo) {

         UserVideoAppealEntity userVideoAppealEntity = new UserVideoAppealEntity();
         BeanUtils.copyProperties(userVideoAppealBo, userVideoAppealEntity);
         userVideoAppealEntity.setId(SnowflakeManager.nextValue());
         userVideoAppealEntity.setCreateDate(new Date());
         userVideoAppealEntity.setUpdateDate(new Date());

         userVideoAppealService.save(userVideoAppealEntity);

         UserVideoAppealInfoVo userVideoAppealInfoVo = new UserVideoAppealInfoVo();
         BeanUtils.copyProperties(userVideoAppealEntity, userVideoAppealInfoVo);

         return userVideoAppealInfoVo;
     }

    /**
     * 修改用户视频申述表
     * @param userVideoAppealBo 用户视频申述表对象
     * @return
     */
    public void update(UserVideoAppealBo userVideoAppealBo) {

        UserVideoAppealEntity userVideoAppealEntity = new UserVideoAppealEntity();
        BeanUtils.copyProperties(userVideoAppealBo, userVideoAppealEntity);
        userVideoAppealEntity.setUpdateDate(new Date());

        userVideoAppealService.updateById(userVideoAppealEntity);
    }

    /**
     * 删除用户视频申述表
     * @param id 用户视频申述表id
     * @return
     */
    public void deleteById(Long id) {

        userVideoAppealService.removeById(id);
    }


}

