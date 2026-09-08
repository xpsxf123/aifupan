package com.jiuyu.replay.system.producer.impl;

import cn.hutool.core.convert.Convert;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;



import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.system.bo.LoginRotateImageBo;
import com.jiuyu.replay.system.bo.LoginRotateImageListBo;
import com.jiuyu.replay.system.entity.LoginRotateImageEntity;
import com.jiuyu.replay.system.producer.LoginRotateImageProducer;
import com.jiuyu.replay.system.repository.service.LoginRotateImageService;
import com.jiuyu.replay.system.vo.LoginRotateImageInfoVo;
import com.jiuyu.replay.system.vo.LoginRotateImageListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Service
public class LoginRotateImageProducerImpl implements LoginRotateImageProducer {

    @Resource
    private LoginRotateImageService loginRotateImageService;


    @Override
    public PageUtils<LoginRotateImageListVo> queryPage(LoginRotateImageListBo loginRotateImageListBo) {
        QueryWrapper<LoginRotateImageEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(loginRotateImageListBo.getKeyword())){
            wrapper.like("content", loginRotateImageListBo.getKeyword());
            wrapper.orderBy(true,true,"sort");
        }

        IPage<LoginRotateImageEntity> iPage = loginRotateImageService.page(new Query<LoginRotateImageEntity>().getPage(loginRotateImageListBo.getPage(), loginRotateImageListBo.getLimit()), wrapper);

        PageUtils<LoginRotateImageListVo> pageUtils = new PageUtils<>(loginRotateImageListBo.getPage(), loginRotateImageListBo.getLimit(), iPage);

        List<LoginRotateImageEntity> records = iPage.getRecords();
        pageUtils.setList(new ArrayList<>());
        if(records != null && !records.isEmpty()) {
            List<LoginRotateImageListVo> vos = records.stream().map(item -> {
                LoginRotateImageListVo loginRotateImageVo = new LoginRotateImageListVo();
                BeanUtils.copyProperties(item, loginRotateImageVo);
                return loginRotateImageVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }


    @Override
    public List<LoginRotateImageListVo> noPage( ) {
        List<LoginRotateImageEntity> list = loginRotateImageService.lambdaQuery()
                .eq(LoginRotateImageEntity::getImgStatus, 0)
                .orderBy(true,true, LoginRotateImageEntity::getSort)
                .list();
        if (Objects.nonNull(list)&&!list.isEmpty()){
            return Convert.toList(LoginRotateImageListVo.class, list);
        }
        return  new ArrayList<>();
    }


    @Override
    public LoginRotateImageInfoVo info(Long id) {

        LoginRotateImageEntity loginRotateImageEntity = loginRotateImageService.getById(id);
        if(loginRotateImageEntity != null) {
            LoginRotateImageInfoVo loginRotateImageInfoVo = new LoginRotateImageInfoVo();
            BeanUtils.copyProperties(loginRotateImageEntity, loginRotateImageInfoVo);
            return loginRotateImageInfoVo;
        }

        return null;
    }

    @Override
    public LoginRotateImageInfoVo save(LoginRotateImageBo loginRotateImageBo) {

         LoginRotateImageEntity loginRotateImageEntity = new LoginRotateImageEntity();
         BeanUtils.copyProperties(loginRotateImageBo, loginRotateImageEntity);
         loginRotateImageEntity.setId(SnowflakeManager.nextValue());
         loginRotateImageEntity.setCreateDate(new Date());
         loginRotateImageEntity.setUpdateDate(new Date());
         loginRotateImageEntity.setImgStatus(0);
         loginRotateImageEntity.setSort(loginRotateImageEntity.getSort() == null ? 0 :loginRotateImageEntity.getSort());

        boolean save = loginRotateImageService.save(loginRotateImageEntity);
        if (save){
            toUpdateSort(loginRotateImageEntity);
        }
        LoginRotateImageInfoVo loginRotateImageInfoVo = new LoginRotateImageInfoVo();
         BeanUtils.copyProperties(loginRotateImageEntity, loginRotateImageInfoVo);

         return loginRotateImageInfoVo;
     }

     @Transactional(rollbackFor = Exception.class)
     void toUpdateSort(LoginRotateImageEntity entity) {
         List<LoginRotateImageEntity> list = loginRotateImageService.lambdaQuery()
                 .ge(LoginRotateImageEntity::getSort, entity.getSort())
                 // 排除自身
                 .ne(entity.getId() != null, LoginRotateImageEntity::getId, entity.getId())
                 .orderBy(true,true, LoginRotateImageEntity::getSort)
                 .orderBy(true,false, LoginRotateImageEntity::getUpdateDate)
                 .list();
         if(list != null && !list.isEmpty()) {
             for (LoginRotateImageEntity loginRotateImageEntity : list) {
                 loginRotateImageEntity.setSort(loginRotateImageEntity.getSort() + 1);
             }
             loginRotateImageService.updateBatchById(list);
         }
     }

    @Override
    public void update(LoginRotateImageBo loginRotateImageBo) {

        LoginRotateImageEntity loginRotateImageEntity = new LoginRotateImageEntity();
        BeanUtils.copyProperties(loginRotateImageBo, loginRotateImageEntity);
        loginRotateImageEntity.setUpdateDate(new Date());

        boolean updated = loginRotateImageService.updateById(loginRotateImageEntity);
        if (updated){
            toUpdateSort(loginRotateImageEntity);
        }
    }

    @Override
    public void deleteById(Long id) {

        loginRotateImageService.removeById(id);
    }


}

