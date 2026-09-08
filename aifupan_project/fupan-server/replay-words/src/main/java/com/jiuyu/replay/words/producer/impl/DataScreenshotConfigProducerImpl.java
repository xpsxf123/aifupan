package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;
import com.jiuyu.replay.words.entity.DataScreenshotConfigEntity;
import com.jiuyu.replay.words.producer.DataScreenshotConfigProducer;
import com.jiuyu.replay.words.repository.service.DataScreenshotConfigService;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Service
public class DataScreenshotConfigProducerImpl implements DataScreenshotConfigProducer {

    @Resource
    private DataScreenshotConfigService dataScreenshotConfigService;


    @Override
    public PageUtils<DataScreenshotConfigListVo> queryPage(DataScreenshotConfigListBo dataScreenshotConfigListBo) {
        QueryWrapper<DataScreenshotConfigEntity> wrapper = new QueryWrapper<>();
        wrapper.lambda()
                .orderByAsc(DataScreenshotConfigEntity::getSort);


        IPage<DataScreenshotConfigEntity> iPage = dataScreenshotConfigService.page(new Query<DataScreenshotConfigEntity>().getPageNoSort(dataScreenshotConfigListBo.getPage(), dataScreenshotConfigListBo.getLimit()), wrapper);

        PageUtils<DataScreenshotConfigListVo> pageUtils = new PageUtils<>(dataScreenshotConfigListBo.getPage(), dataScreenshotConfigListBo.getLimit(), iPage);

        List<DataScreenshotConfigEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<DataScreenshotConfigListVo> vos = records.stream().map(item -> {
                DataScreenshotConfigListVo dataScreenshotConfigVo = new DataScreenshotConfigListVo();
                BeanUtils.copyProperties(item, dataScreenshotConfigVo);
                return dataScreenshotConfigVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public DataScreenshotConfigInfoVo info(Long id) {

        DataScreenshotConfigEntity dataScreenshotConfigEntity = dataScreenshotConfigService.getById(id);
        if(dataScreenshotConfigEntity != null) {
            DataScreenshotConfigInfoVo dataScreenshotConfigInfoVo = new DataScreenshotConfigInfoVo();
            BeanUtils.copyProperties(dataScreenshotConfigEntity, dataScreenshotConfigInfoVo);
            return dataScreenshotConfigInfoVo;
        }

        return null;
    }

    /**
     * 新增数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
     public DataScreenshotConfigInfoVo save(DataScreenshotConfigBo dataScreenshotConfigBo) {

         DataScreenshotConfigEntity dataScreenshotConfigEntity = new DataScreenshotConfigEntity();
         BeanUtils.copyProperties(dataScreenshotConfigBo, dataScreenshotConfigEntity);
         dataScreenshotConfigEntity.setId(SnowflakeManager.nextValue());
         dataScreenshotConfigEntity.setCreateDate(new Date());
         dataScreenshotConfigEntity.setUpdateDate(new Date());

         dataScreenshotConfigService.save(dataScreenshotConfigEntity);

         DataScreenshotConfigInfoVo dataScreenshotConfigInfoVo = new DataScreenshotConfigInfoVo();
         BeanUtils.copyProperties(dataScreenshotConfigEntity, dataScreenshotConfigInfoVo);

         return dataScreenshotConfigInfoVo;
     }

    /**
     * 修改数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    public void update(DataScreenshotConfigBo dataScreenshotConfigBo) {

        DataScreenshotConfigEntity dataScreenshotConfigEntity = new DataScreenshotConfigEntity();
        BeanUtils.copyProperties(dataScreenshotConfigBo, dataScreenshotConfigEntity);
        dataScreenshotConfigEntity.setUpdateDate(new Date());

        dataScreenshotConfigService.updateById(dataScreenshotConfigEntity);
    }

    /**
     * 删除数据截图配置
     * @param id 数据截图配置id
     * @return
     */
    public void deleteById(Long id) {

        dataScreenshotConfigService.removeById(id);
    }

    /**
     * 查询所有数据截图配置
     * @return
     */
    @Override
    public List<DataScreenshotConfigInfoVo> listAllSort() {
        List<DataScreenshotConfigEntity> list = dataScreenshotConfigService.list(new LambdaQueryWrapper<DataScreenshotConfigEntity>()
                .orderByAsc(DataScreenshotConfigEntity::getSort)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, DataScreenshotConfigInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public DataScreenshotConfigInfoVo getByCode(String screenshotCode) {
        DataScreenshotConfigEntity one = dataScreenshotConfigService.getOne(new LambdaQueryWrapper<DataScreenshotConfigEntity>()
                .eq(DataScreenshotConfigEntity::getScreenshotCode, screenshotCode)
        );
        if (ObjectUtil.isNotEmpty(one)){
            return BeanUtil.copyProperties(one, DataScreenshotConfigInfoVo.class);
        }
        return null;
    }
}

