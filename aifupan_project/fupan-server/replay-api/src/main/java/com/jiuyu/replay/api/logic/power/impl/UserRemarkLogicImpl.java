package com.jiuyu.replay.api.logic.power.impl;


import com.jiuyu.replay.api.logic.power.UserRemarkLogic;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.system.DictDataVo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserRemarkBll;
import com.jiuyu.replay.power.bo.UserRemarkBo;
import com.jiuyu.replay.power.bo.UserRemarkListBo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserRemarkInfoVo;
import com.jiuyu.replay.power.vo.UserRemarkListVo;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 用户备注表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Service
public class UserRemarkLogicImpl implements UserRemarkLogic {

    @Resource
    private UserRemarkBll userRemarkBll;
    @Resource
    private UserBll userBll;
    @Resource
    private DictDataFeign dictDataFeign;


    /**
     * 跟进记录类型字典 typeLogo
     */
    private static final String USER_FOLLOW_TYPE = "user_follow_type";

    /**
     * 分页查询用户跟进记录
     * @param userRemarkListBo
     * @return
     */
    @Override
    public R<PageUtils<UserRemarkListVo>> queryPage(UserRemarkListBo userRemarkListBo) {
        R<PageUtils<UserRemarkListVo>> pageUtilsR = userRemarkBll.queryPage(userRemarkListBo);
        if (ObjectUtils.isEmpty(pageUtilsR)|| ObjectUtils.isEmpty(pageUtilsR.getData())||ObjectUtils.isEmpty(pageUtilsR.getData().getList())) {
            return pageUtilsR;
        }
        List<UserRemarkListVo> list = pageUtilsR.getData().getList();
        //收集 用户ID（创建者、修改者）
        Set<Long> userIds = list.stream()
                .flatMap(item -> {
                    List<Long> ids = new ArrayList<>();
                    if (item.getCreateId() != null) {
                        ids.add(item.getCreateId());
                    }
                    if (item.getUpdateId() != null) {
                        ids.add(item.getUpdateId());
                    }
                    return ids.stream();
                })
                .collect(Collectors.toSet());

        //填充或有 [创建、修改者名称]、[跟进记录类型名称] 所以不能先判断没有就先返回
        //得到收集 的用户id
        Map<Long, String> nickNameMap = new HashMap<>();
        if (ObjectUtils.isNotEmpty(userIds)) {
            //根据userIds 查询user数据
            R<List<UserListVo>> listByIds = userBll.listByIds(new ArrayList<>(userIds));
            if (ObjectUtils.isNotEmpty(listByIds) && ObjectUtils.isNotEmpty(listByIds.getData())) {
                // 映射
                nickNameMap = listByIds.getData().stream()
                        .collect(Collectors.toMap(UserListVo::getId, UserListVo::getNickName, (oldValue, newValue) -> newValue));
            }
        }


        //根据跟进记录类型字典 typeLogo 查跟进记录类数据
        Map<String, String> followTypeNameMap = new HashMap<>();
        List<DictDataListVo> dictDataListVos = dictDataFeign.dictDataListByCode(USER_FOLLOW_TYPE);
        if (ObjectUtils.isNotEmpty(dictDataListVos)){
            // 映射
            followTypeNameMap = dictDataListVos.stream()
                    .collect(Collectors.toMap(DictDataListVo::getValue, DictDataListVo::getLabel, (oldValue, newValue) -> newValue));
        }

        Map<String, String> systemAccordingStatusMap = dictDataFeign.dictDataParentLabelByCode("system_according_status")
                .stream().collect(Collectors.toMap(DictDataVo::getValue, DictDataVo::getLabel, (oldValue, newValue) -> newValue));

        // 填充 创建人、修改人名称、跟进记录类型名称
        for (UserRemarkListVo item : list) {
            // 填充创建者名称
            item.setUpdateName(nickNameMap.get(item.getUpdateId()));
            item.setCreateName(nickNameMap.get(item.getCreateId()));
            // 填充跟进记录类型名称
            if (item.getFollowType() != null) {
                String type = item.getFollowType().toString();
                item.setFollowTypeName(followTypeNameMap.get(type));
            }
            // 填充跟进记录状态名称
            if (item.getFollowStatus() != null) {
                String status = item.getFollowStatus().toString();
                item.setFollowStatusName(systemAccordingStatusMap.get(status));
            }
        }

        return pageUtilsR;
    }


    /**
     * 通过id查询跟进记录，并填充创建者姓名，修改者姓名
     * @param id 用户备注表id
     * @return
     */
    @Override
    public R<UserRemarkInfoVo> info(Long id) {
        R<UserRemarkInfoVo> info = userRemarkBll.info(id);
        if (ObjectUtils.isEmpty(info)|| ObjectUtils.isEmpty(info.getData())) {
            return info;
        }

        UserRemarkInfoVo data = info.getData();

        // 收集创建者ID和修改者ID（去重）
        Set<Long> userIds = new HashSet<>(2);
        if (data.getCreateId() != null) {
            userIds.add(data.getCreateId());
        }
        if (data.getUpdateId() != null) {
            userIds.add(data.getUpdateId());
        }
        //如果没有，则返回跟进记录，不填充创建者修改者姓名
        if (userIds.isEmpty()) {
            return info;
        }

        // 根据用户ID查询用户信息
        R<List<UserListVo>> userList = userBll.listByIds(new ArrayList<>(userIds));
        if (ObjectUtils.isEmpty(userList)|| ObjectUtils.isEmpty(userList.getData())) {
            return info;
        }

        // 将用户信息映射为Map，键为用户ID，值为用户昵称
        Map<Long, String> nameMap = userList.getData().stream()
                .collect(Collectors.toMap(UserListVo::getId, UserListVo::getNickName, (oldValue, newValue) -> newValue));
        //填充创建者名称
        String createName = nameMap.get(data.getCreateId());
        data.setCreateName(createName);
        //填充修改者名称
        String updateName =  nameMap.get(data.getUpdateId());
        data.setUpdateName(updateName);
        return info;
    }

    @Override
    public R<String> save(UserRemarkBo userRemarkBo) {

        return userRemarkBll.save(userRemarkBo);
    }

    @Override
    public R<String> update(UserRemarkBo userRemarkBo) {

        return userRemarkBll.update(userRemarkBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userRemarkBll.delete(id);
    }


}

