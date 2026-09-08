package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.SensitiveWordsClientLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.bll.SensitiveWordsClientBll;
import com.jiuyu.replay.words.bo.SensitiveWordsClientBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientListBo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientInfoVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
public class SensitiveWordsLogicClientImpl implements SensitiveWordsClientLogic {

    @Resource
    private SensitiveWordsClientBll sensitiveWordsClientBll;
    @Resource
    private UserBll userBll;

    @Override
    public R<PageUtils<SensitiveWordsClientListVo>> queryPage(SensitiveWordsClientListBo sensitiveWordsListBo) {

        R<PageUtils<SensitiveWordsClientListVo>> pageUtilsR = sensitiveWordsClientBll.queryPage(sensitiveWordsListBo);

        PageUtils<SensitiveWordsClientListVo> pageUtils = pageUtilsR.getData();

        List<SensitiveWordsClientListVo> sensitiveWordsClientListVos = pageUtils.getList();

        if(sensitiveWordsClientListVos != null && sensitiveWordsClientListVos.size() > 0) {
            // 封装用户信息
            Set<Long> userIds = sensitiveWordsClientListVos.stream().map(SensitiveWordsClientVo::getUserId).collect(Collectors.toSet());

            R<List<UserListVo>> userListR = this.userBll.listByIds(userIds);

            List<UserListVo> userList = userListR.getData();

            if(userList != null && userList.size() > 0) {
                for (SensitiveWordsClientListVo sensitiveWordsClientListVo : sensitiveWordsClientListVos) {
                    for (UserListVo userListVo : userList) {
                        if(sensitiveWordsClientListVo.getUserId().equals(userListVo.getId())) {
                            sensitiveWordsClientListVo.setUserNickName(userListVo.getNickName());
                            sensitiveWordsClientListVo.setUserPhone(userListVo.getPhone());
                            break;
                        }
                    }

                }
            }
        }

        return pageUtilsR;
    }

    @Override
    public R<SensitiveWordsClientInfoVo> info(Long id) {

        return sensitiveWordsClientBll.info(id);
    }

    @Override
    public R<List<String>> save(SensitiveWordsClientBo sensitiveWordsBo) {

        if(sensitiveWordsBo.getResourceType() == 0) {
            UserCacheVo user = GlobalObject.getLocalUser();
            sensitiveWordsBo.setUserId(user.getId());
        }else {
            UserCacheVo user = GlobalObject.getLocalUser();
            R<Long> parentR = userBll.getCacheUserParentId(user.getId());
            if(parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
                sensitiveWordsBo.setUserId(parentR.getData());
            }else {
                sensitiveWordsBo.setUserId(user.getId());
            }
        }


        return sensitiveWordsClientBll.save(sensitiveWordsBo);
    }

    @Override
    public R<String> update(SensitiveWordsClientBo sensitiveWordsBo) {

        if(sensitiveWordsBo.getResourceType() == 0) {
            UserCacheVo user = GlobalObject.getLocalUser();
            sensitiveWordsBo.setUserId(user.getId());
        }else {
            UserCacheVo user = GlobalObject.getLocalUser();
            R<Long> parentR = userBll.getCacheUserParentId(user.getId());
            if(parentR.getCode() == 0 && !StringUtils.isEmpty(parentR.getData())) {
                sensitiveWordsBo.setUserId(parentR.getData());
            }else {
                sensitiveWordsBo.setUserId(user.getId());
            }
        }

        return sensitiveWordsClientBll.update(sensitiveWordsBo);
    }

    @Override
    public R<String> delete(Long id) {

        return sensitiveWordsClientBll.delete(id);
    }


}

