package com.jiuyu.replay.api.logic.power.impl;

import com.jiuyu.replay.api.logic.power.RoleLogic;
import com.jiuyu.replay.power.bll.RoleBll;
import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class RoleLogicImpl implements RoleLogic {

    @Resource
    private RoleBll roleBll;

    @Override
    public R<PageUtils<RoleVo>> queryPage(RoleListBo roleListBo) {

        return roleBll.queryPage(roleListBo);
    }

    @Override
    public R<RoleInfoVo> info(Long id) {

        return roleBll.info(id);
    }

    @Override
    public R<String> save(RoleInfoBo role) {

        return roleBll.save(role);
    }

    @Override
    public R<String> modify(RoleInfoBo role) {

        return roleBll.modify(role);
    }

    @Override
    public R<String> delete(Long id) {

        return roleBll.delete(id);
    }

}
