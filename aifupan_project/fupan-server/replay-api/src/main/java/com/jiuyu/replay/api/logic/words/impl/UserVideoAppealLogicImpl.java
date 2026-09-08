package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.UserVideoAppealLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.UserVideoAppealBll;
import com.jiuyu.replay.words.bo.UserVideoAppealBo;
import com.jiuyu.replay.words.bo.UserVideoAppealClientBo;
import com.jiuyu.replay.words.bo.UserVideoAppealListBo;
import com.jiuyu.replay.words.vo.UserVideoAppealInfoVo;
import com.jiuyu.replay.words.vo.UserVideoAppealListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;


/**
 * 用户视频申述表
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-11-14 12:12:11
 */
@Service
public class UserVideoAppealLogicImpl implements UserVideoAppealLogic {

    @Resource
    private UserVideoAppealBll userVideoAppealBll;


    @Override
    public R<PageUtils<UserVideoAppealListVo>> queryPage(UserVideoAppealListBo userVideoAppealListBo) {

        return userVideoAppealBll.queryPage(userVideoAppealListBo);
    }

    @Override
    public R<UserVideoAppealInfoVo> info(Long id) {

        return userVideoAppealBll.info(id);
    }

    @Override
    public R<String> save(UserVideoAppealBo userVideoAppealBo) {

        return userVideoAppealBll.save(userVideoAppealBo);
    }

    @Override
    public R<String> update(UserVideoAppealBo userVideoAppealBo) {

        return userVideoAppealBll.update(userVideoAppealBo);
    }

    @Override
    public R<String> delete(Long id) {

        return userVideoAppealBll.delete(id);
    }

    @Override
    public R<String> uploadVideoAppeal(UserVideoAppealClientBo bo) {
        if (ObjectUtil.isEmpty(bo.getLiveUserName())) return R.error(3001, "直播间账号昵称为空");
        if (ObjectUtil.isEmpty(bo.getCompanyName())) return R.error(3001, "公司名称为空");
        if (ObjectUtil.isEmpty(bo.getContacts())) return R.error(3001, "联系人为空");
        if (ObjectUtil.isEmpty(bo.getPhone())) return R.error(3001, "手机号为空");
        if (ObjectUtil.isEmpty(bo.getAnchorUrlId())) return R.error(3001, "申述的主播id为空");
        if (ObjectUtil.isEmpty(bo.getAnchorVideoId())) return R.error(3001, "申述的视频id为空");
        UserVideoAppealBo appealBo = BeanUtil.copyProperties(bo, UserVideoAppealBo.class);
        appealBo.setStatus(0);
        UserCacheVo user = GlobalObject.getLocalUser();
        appealBo.setUserId(user.getId());
        appealBo.setNickName(user.getNickName());
        return userVideoAppealBll.save(appealBo);
    }

    @Override
    public R<String> handleAppeal(UserVideoAppealBo userVideoAppealBo) {
        UserVideoAppealBo appealBo = new UserVideoAppealBo();
        appealBo.setId(userVideoAppealBo.getId());
        appealBo.setHandleRemarks(userVideoAppealBo.getHandleRemarks());
        appealBo.setHandleDate(new Date());
        UserCacheVo localUser = GlobalObject.getLocalUser();
        appealBo.setHandleUserId(localUser.getId());
        appealBo.setStatus(1);
        userVideoAppealBll.update(appealBo);
        return R.ok("处理成功");
    }
}

