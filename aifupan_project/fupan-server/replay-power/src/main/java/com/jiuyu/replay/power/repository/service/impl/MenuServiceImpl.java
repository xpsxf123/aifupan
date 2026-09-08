package com.jiuyu.replay.power.repository.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.power.entity.MenuEntity;
import com.jiuyu.replay.power.repository.dao.MenuDao;
import com.jiuyu.replay.power.repository.service.MenuService;
import org.springframework.stereotype.Service;


@Service("menuService")
public class MenuServiceImpl extends ServiceImpl<MenuDao, MenuEntity> implements MenuService {

}