package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.DeptUserEntity;
import com.jiuyu.replay.power.repository.dao.DeptUserDao;
import com.jiuyu.replay.power.repository.service.DeptUserService;
import org.springframework.stereotype.Service;

/**
 * 部门对应下的用户
 *
 * @author jxy
 * @date 2024-07-08
 */
@Service("deptUserService")
public class DeptUserServiceImpl extends ServiceImpl<DeptUserDao, DeptUserEntity> implements DeptUserService {

}
