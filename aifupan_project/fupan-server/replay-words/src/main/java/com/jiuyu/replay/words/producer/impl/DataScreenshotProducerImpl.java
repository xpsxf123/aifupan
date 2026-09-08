package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotListBo;
import com.jiuyu.replay.words.entity.DataScreenshotEntity;
import com.jiuyu.replay.words.producer.DataScreenshotProducer;
import com.jiuyu.replay.words.repository.service.DataScreenshotService;
import com.jiuyu.replay.words.vo.DataScreenshotInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 数据截图记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Service
public class DataScreenshotProducerImpl implements DataScreenshotProducer {

    @Resource
    private DataScreenshotService dataScreenshotService;

    private LambdaQueryWrapper<DataScreenshotEntity> getWrapper(DataScreenshotListBo dataScreenshotListBo) {
        return new LambdaQueryWrapper<DataScreenshotEntity>()
                .eq(ObjectUtil.isNotEmpty(dataScreenshotListBo.getSourceType()), DataScreenshotEntity::getSourceType, dataScreenshotListBo.getSourceType())
                .eq(ObjectUtil.isNotEmpty(dataScreenshotListBo.getUserId()), DataScreenshotEntity::getUserId, dataScreenshotListBo.getUserId())
                .eq(ObjectUtil.isNotEmpty(dataScreenshotListBo.getSourceId()), DataScreenshotEntity::getSourceId, dataScreenshotListBo.getSourceId())
                .eq(ObjectUtil.isNotEmpty(dataScreenshotListBo.getTenantId()), DataScreenshotEntity::getTenantId, dataScreenshotListBo.getTenantId())
                .eq(ObjectUtil.isNotEmpty(dataScreenshotListBo.getScreenshotStatus()), DataScreenshotEntity::getScreenshotStatus, dataScreenshotListBo.getScreenshotStatus())
                ;
    }

    @Override
    public PageUtils<DataScreenshotListVo> queryPage(DataScreenshotListBo dataScreenshotListBo) {

        LambdaQueryWrapper<DataScreenshotEntity> wrapper = getWrapper(dataScreenshotListBo)
                .select(
                        DataScreenshotEntity::getId,
                        DataScreenshotEntity::getTenantId,
                        DataScreenshotEntity::getUserId,
                        DataScreenshotEntity::getScreenshotCode,
                        DataScreenshotEntity::getSourceType,
                        DataScreenshotEntity::getSourceId,
                        DataScreenshotEntity::getSourceImagesType,
                        DataScreenshotEntity::getSourceImagesAddress,
                        DataScreenshotEntity::getScreenshotStatus,
                        DataScreenshotEntity::getUpdateDate,
                        DataScreenshotEntity::getCreateDate
                        )
                ;

        IPage<DataScreenshotEntity> iPage = dataScreenshotService.page(new Query<DataScreenshotEntity>().getPage(dataScreenshotListBo.getPage(), dataScreenshotListBo.getLimit()), wrapper);

        PageUtils<DataScreenshotListVo> pageUtils = new PageUtils<>(dataScreenshotListBo.getPage(), dataScreenshotListBo.getLimit(), iPage);

        List<DataScreenshotEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<DataScreenshotListVo> vos = records.stream().map(item -> {
                DataScreenshotListVo dataScreenshotVo = new DataScreenshotListVo();
                BeanUtils.copyProperties(item, dataScreenshotVo);
                return dataScreenshotVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public List<DataScreenshotListVo> list(DataScreenshotListBo dataScreenshotListBo) {
        List<DataScreenshotEntity> records = dataScreenshotService.list(getWrapper(dataScreenshotListBo));
        return ObjectUtil.isNotEmpty(records) ? BeanUtil.copyToList(records, DataScreenshotListVo.class) : new ArrayList<>();
    }

    @Override
    public DataScreenshotInfoVo info(Long id) {

        DataScreenshotEntity dataScreenshotEntity = dataScreenshotService.getById(id);
        if(dataScreenshotEntity != null) {
            DataScreenshotInfoVo dataScreenshotInfoVo = new DataScreenshotInfoVo();
            BeanUtils.copyProperties(dataScreenshotEntity, dataScreenshotInfoVo);
            return dataScreenshotInfoVo;
        }

        return null;
    }

    /**
     * 新增数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
     public DataScreenshotInfoVo save(DataScreenshotBo dataScreenshotBo) {

         DataScreenshotEntity dataScreenshotEntity = new DataScreenshotEntity();
         BeanUtils.copyProperties(dataScreenshotBo, dataScreenshotEntity);
         dataScreenshotEntity.setId(SnowflakeManager.nextValue());
         dataScreenshotEntity.setCreateDate(new Date());
         dataScreenshotEntity.setUpdateDate(new Date());

         dataScreenshotService.save(dataScreenshotEntity);

         DataScreenshotInfoVo dataScreenshotInfoVo = new DataScreenshotInfoVo();
         BeanUtils.copyProperties(dataScreenshotEntity, dataScreenshotInfoVo);

         return dataScreenshotInfoVo;
     }

    /**
     * 修改数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    public void update(DataScreenshotBo dataScreenshotBo) {

        DataScreenshotEntity dataScreenshotEntity = new DataScreenshotEntity();
        BeanUtils.copyProperties(dataScreenshotBo, dataScreenshotEntity);
        dataScreenshotEntity.setUpdateDate(new Date());

        dataScreenshotService.updateById(dataScreenshotEntity);
    }

    /**
     * 删除数据截图记录
     * @param id 数据截图记录id
     * @return
     */
    public void deleteById(Long id) {

        dataScreenshotService.removeById(id);
    }

    /**
     * 数据截图上传
     *
     * @param uploadBo
     */
    @Override
    public void screenshotUpload(DataScreenshotBo uploadBo) {
        RRException.isNotEmpty(uploadBo.getSourceType(), "批次号不能为空");
        RRException.isNotEmpty(uploadBo.getSourceId(), "视频id不能为空");
        RRException.isNotEmpty(uploadBo.getSourceImagesAddress(), "上传的图片地址不能为空");
        RRException.isNotEmpty(uploadBo.getSourceImagesType(), "图片来源不能为空");
        RRException.isNotEmpty(uploadBo.getScreenshotCode(), "数据截图code不能为空");

        // 查询是否有重复的数据
        DataScreenshotEntity one = dataScreenshotService.getOne(new LambdaQueryWrapper<DataScreenshotEntity>()
                .eq(DataScreenshotEntity::getScreenshotCode, uploadBo.getScreenshotCode())
                .eq(DataScreenshotEntity::getSourceType, uploadBo.getSourceType())
                .eq(DataScreenshotEntity::getSourceId, uploadBo.getSourceId())
                .eq(DataScreenshotEntity::getUserId, uploadBo.getUserId())
                .eq(DataScreenshotEntity::getTenantId, uploadBo.getTenantId())
                .last("limit 1")
        );
        // 设置状态
        if (ObjectUtil.isNotEmpty(one)) {
            uploadBo.setScreenshotStatus(1);
            uploadBo.setAiContent(null);
            uploadBo.setId(one.getId());
            dataScreenshotService.update(new LambdaUpdateWrapper<DataScreenshotEntity>()
                    .eq(DataScreenshotEntity::getId, one.getId())
                    .set(DataScreenshotEntity::getSourceImagesAddress, uploadBo.getSourceImagesAddress())
                    .set(DataScreenshotEntity::getScreenshotStatus, uploadBo.getScreenshotStatus())
                    .set(DataScreenshotEntity::getAiContent, uploadBo.getAiContent())
                    .set(DataScreenshotEntity::getUpdateDate, new Date())
            );
            update(uploadBo);
        }else{
            uploadBo.setScreenshotStatus(1);
            DataScreenshotInfoVo save = save(uploadBo);
            uploadBo.setId(save.getId());
        }
    }

    @Override
    public List<DataScreenshotInfoVo> getByIds(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) RRException.create("ids不能为空");
        List<DataScreenshotEntity> list = dataScreenshotService.listByIds(ids);
        return BeanUtil.copyToList(list, DataScreenshotInfoVo.class);
    }

    @Override
    public DataScreenshotInfoVo getByCode(String batchNumber, String videoId, String screenshotCode, Long userId) {

        DataScreenshotEntity screenshot = dataScreenshotService.getOne(new LambdaQueryWrapper<DataScreenshotEntity>()
                .eq(DataScreenshotEntity::getSourceType, batchNumber)
                .eq(DataScreenshotEntity::getSourceId, videoId)
                .eq(DataScreenshotEntity::getScreenshotCode, screenshotCode)
                .eq(DataScreenshotEntity::getUserId, userId)
                .last("limit 1")
        );

        if (ObjectUtil.isNotEmpty(screenshot)){
            return BeanUtil.copyProperties(screenshot, DataScreenshotInfoVo.class);
        }
        return null;
    }

    /**
     * 判断是否存在视频截图
     * @param bo
     * @return
     */
    @Override
    public boolean existVideoDataScreenshot(DataScreenshotListBo bo) {
        LambdaQueryWrapper<DataScreenshotEntity> wrapper = getWrapper(bo)
                .eq(DataScreenshotEntity::getSourceType, bo.getSourceType())
                .eq(DataScreenshotEntity::getSourceId, bo.getSourceId())
                .eq(DataScreenshotEntity::getUserId, bo.getUserId())
                .eq(DataScreenshotEntity::getTenantId, bo.getTenantId())
                .last("limit 1")
                ;
        return ObjectUtil.isNotEmpty(dataScreenshotService.getOne(wrapper));
    }

    /**
     * 更新状态
     * @param ids
     * @param status
     */
    @Override
    public void updateStatusByIds(List<Long> ids, int status) {
        dataScreenshotService.update(new LambdaUpdateWrapper<DataScreenshotEntity>()
                .in(DataScreenshotEntity::getId, ids)
                .set(DataScreenshotEntity::getScreenshotStatus, status)
        );
    }
}

