package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.AiTrainLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.bll.AiTrainBll;
import com.jiuyu.replay.words.bo.AiTrainBo;
import com.jiuyu.replay.words.bo.AiTrainListBo;
import com.jiuyu.replay.words.vo.AiTrainInfoVo;
import com.jiuyu.replay.words.vo.AiTrainListVo;
import com.jiuyu.replay.words.vo.AiTrainVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * AI训练
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Service
public class AiTrainLogicImpl implements AiTrainLogic {

    @Resource
    private AiTrainBll aiTrainBll;
    @Resource
    private UserBll userBll;


    @Override
    public R<PageUtils<AiTrainListVo>> queryPage(AiTrainListBo aiTrainListBo) {

        R<PageUtils<AiTrainListVo>> pageUtilsR = aiTrainBll.queryPage(aiTrainListBo);

        // 封装用户名称
        if(pageUtilsR.getCode() == 0) {
            PageUtils<AiTrainListVo> pageUtilsRData = pageUtilsR.getData();

            List<AiTrainListVo> list = pageUtilsRData.getList();

            if(list != null && list.size() > 0) {

                Set<Long> userIds = list.stream().map(AiTrainVo::getUserId).collect(Collectors.toSet());
                R<List<UserListVo>> userListR = this.userBll.listByIds(userIds);
                List<UserListVo> userList = userListR.getData();

                if(userList != null && userList.size() > 0) {
                    for (AiTrainListVo aiTrainListVo : list) {
                        for (UserListVo userListVo : userList) {
                            if(aiTrainListVo.getUserId().equals(userListVo.getId())) {
                                aiTrainListVo.setUserNickName(userListVo.getNickName());
                                break;
                            }
                        }
                    }
                }

            }
        }

        return pageUtilsR;
    }

    @Override
    public R<AiTrainInfoVo> info(Long id) {

        return aiTrainBll.info(id);
    }

    @Override
    public R<String> save(AiTrainBo aiTrainBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        aiTrainBo.setUserId(user.getId());
        aiTrainBo.setTenantId(user.getActiveTenantId());

        return aiTrainBll.save(aiTrainBo);
    }

    @Override
    public R<String> update(AiTrainBo aiTrainBo) {

        return aiTrainBll.update(aiTrainBo);
    }

    @Override
    public R<String> delete(Long id) {

        return aiTrainBll.delete(id);
    }

    @Override
    public R<AiTrainInfoVo> infoByVideoId(String videoId) {

        return aiTrainBll.infoByVideoId(videoId);
    }

    @Override
    public R<String> completeTrain(Long id) {

        return aiTrainBll.completeTrain(id);
    }


}

