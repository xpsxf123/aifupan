package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.DeptEntity;
import com.jiuyu.replay.power.repository.dao.DeptDao;
import com.jiuyu.replay.power.repository.service.DeptService;
import org.springframework.stereotype.Service;

/**
 * 部门表
 *
 * @author jxy
 * @date 2024-07-08
 */
@Service("deptService")
public class DeptServiceImpl extends ServiceImpl<DeptDao, DeptEntity> implements DeptService {

}
