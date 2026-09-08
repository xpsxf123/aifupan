package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotListBo;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.producer.DataScreenshotConfigProducer;
import com.jiuyu.replay.words.producer.DataScreenshotProducer;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 数据截图记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Component
public class DataScreenshotBll {

    @Resource
    private DataScreenshotProducer dataScreenshotProducer;
    @Resource
    private DataScreenshotConfigProducer dataScreenshotConfigProducer;
    @Resource
    private ImgOssUtils imgOssUtils;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;


    /**
     * 数据截图记录列表
     * @param dataScreenshotListBo 数据截图记录列表查询参数
     * @return
     */
    public R<PageUtils<DataScreenshotListVo>> queryPage(DataScreenshotListBo dataScreenshotListBo) {

        return R.ok("获取成功", dataScreenshotProducer.queryPage(dataScreenshotListBo));
    }

    /**
    * 数据截图记录信息
    * @param id 数据截图记录id
    * @return
    */
    public R<DataScreenshotInfoVo> info(Long id) {

        DataScreenshotInfoVo dataScreenshotInfoVo = dataScreenshotProducer.info(id);

        if (ObjectUtil.isNotEmpty(dataScreenshotInfoVo)){
            // 设置图片地址
            dataScreenshotInfoVo.setSourceImagesAddress(getImgUrl(dataScreenshotInfoVo.getSourceImagesAddress(), dataScreenshotInfoVo.getSourceImagesType()));
        }
        return R.ok("获取成功", dataScreenshotInfoVo);
    }

    /**
     * 新增数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    public R<String> save(DataScreenshotBo dataScreenshotBo) {

        DataScreenshotInfoVo dataScreenshotInfoVo = dataScreenshotProducer.save(dataScreenshotBo);
        return R.ok("添加成功");
    }

    /**
     * 修改数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    public R<String> update(DataScreenshotBo dataScreenshotBo) {

        dataScreenshotProducer.update(dataScreenshotBo);
        return R.ok("修改成功");
    }

    /**
     * 修改数据截图记录状态
     * @param ids       id
     * @param status    状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败
     * @return
     */
    public R<String> updateStatusByIds(List<Long> ids, int status){
        dataScreenshotProducer.updateStatusByIds(ids, status);
        return R.ok("修改成功");
    }

    /**
     * 删除数据截图记录
     * @param id 数据截图记录id
     * @return
     */
    public R<String> delete(Long id) {
        DataScreenshotInfoVo info = dataScreenshotProducer.info(id);
        RRException.isNotEmpty(info, "记录查询失败");
        try {
            if (info.getSourceImagesType()== 0){
                imgOssUtils.deleteObject(info.getSourceImagesAddress());
            }
        }catch (Exception e){

        }

        dataScreenshotProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 数据截图上传
     * @param uploadBo
     * @return
     */
    public R<DataScreenshotInfoVo> screenshotUpload(DataScreenshotBo uploadBo) {
        setUserIdAndTenantId(uploadBo);
        dataScreenshotProducer.screenshotUpload(uploadBo);
        R<DataScreenshotInfoVo> info = info(uploadBo.getId());
        info.setMsg("上传成功");
        return info;
    }

    /**
     * 根据id查询数据
     * @param ids
     * @return
     */
    public R<List<DataScreenshotInfoVo>> getByIds(List<Long> ids){
        return R.ok(dataScreenshotProducer.getByIds(ids));
    }

    /**
     * 根据视频查询数据截图列表
     * @param batchNumber
     * @param videoId
     * @return
     */
    public R<List<DataScreenshotListVo>> dataScreenshotList(Integer batchNumber, String videoId) {
        List<DataScreenshotConfigInfoVo> configInfoVoList = dataScreenshotConfigProducer.listAllSort();
        if (ObjectUtil.isNotEmpty(configInfoVoList)){
            DataScreenshotListBo page = new DataScreenshotListBo();
            page.setLimit(-1);
            page.setSourceType(batchNumber);
            page.setSourceId(videoId);
            R<PageUtils<DataScreenshotListVo>> pageUtilsR = queryPage(page);
            Map<String, DataScreenshotListVo> sourceMap;
            if (pageUtilsR.getCode() == 0 && pageUtilsR.getData() != null && ObjectUtil.isNotEmpty(pageUtilsR.getData().getList())){
                List<DataScreenshotListVo> sourceList = pageUtilsR.getData().getList();
                sourceMap = sourceList.stream().collect(Collectors.toMap(DataScreenshotListVo::getScreenshotCode, v -> v, (v1, v2) -> v2));
            }else {
                sourceMap = new HashMap<>();
            }
            List<DataScreenshotListVo> list = configInfoVoList.stream().map(config -> {
                DataScreenshotListVo item = new DataScreenshotListVo();
                DataScreenshotListVo dataScreenshotListVo = sourceMap.get(config.getScreenshotCode());
                if (ObjectUtil.isNotEmpty(dataScreenshotListVo)) {
                    BeanUtil.copyProperties(dataScreenshotListVo, item);
                    // 图片
                    item.setSourceImagesAddress(getImgUrl(item.getSourceImagesAddress(), item.getSourceImagesType()));
                } else {
                    item.setScreenshotStatus(0);
                }
                item.setTitle(config.getTitle());
                item.setScreenshotCode(config.getScreenshotCode());
                // 示例图片
                item.setExampleImgUrl(getImgUrl(config.getExample(), config.getSourceType()));
                return item;
            }).toList();
            return R.ok(list);
        }
        return R.ok(new ArrayList<>());
    }

    public String getImgUrl(String address, int sourceType){
        if (sourceType == 0){
            return imgOssUtils.getUrl(address);
        }else{
            RRException.create("未知图片来源");
        }
        return null;
    }

    /**
     * 根据batchNumber和videoId查询数据截图列表
     * @param sourceType
     * @param videoId
     * @return
     */
    public R<List<DataScreenshotListVo>> getExistDataScreenshotList(Integer sourceType, String videoId) {
        DataScreenshotListBo bo = new DataScreenshotListBo();
        bo.setSourceType(sourceType);
        bo.setSourceId(videoId);
        bo.setScreenshotStatus(3);
        List<DataScreenshotListVo> page = dataScreenshotProducer.list(bo);
        if (ObjectUtil.isNotEmpty(page)){
            List<DataScreenshotConfigInfoVo> configInfoVoList = dataScreenshotConfigProducer.listAllSort();
            if (ObjectUtil.isNotEmpty(configInfoVoList)){
                DataUtils.setFieldNameById(page, "screenshotCode", "title", configInfoVoList, "screenshotCode", "title");
            }
        }
        return R.ok(page);
    }

    /**
     * 判断是否存在视频截图
     * @param sourceType
     * @param sourceId
     * @return
     */
    public R<Boolean> existVideoDataScreenshot(Integer sourceType, String sourceId) {
        DataScreenshotListBo bo = new DataScreenshotListBo();
        bo.setSourceType(sourceType);
        bo.setSourceId(sourceId);
        setUserIdAndTenantId(bo);
        RRException.isNotEmpty(bo.getUserId(), "获取用户Id失败");
        RRException.isNotEmpty(bo.getTenantId(), "获取租户Id失败");
        return R.ok(dataScreenshotProducer.existVideoDataScreenshot(bo));
    }

    /**
     * 设置用户Id和租户Id
     * @param obj
     */
    public void setUserIdAndTenantId(Object obj){
        String sourceId = (String)ReflectUtil.getFieldValue(obj,  "sourceId");
        if (ObjectUtil.isEmpty(sourceId)) return;
        Integer sourceType = (Integer)ReflectUtil.getFieldValue(obj,  "sourceType");
        if (ObjectUtil.isEmpty(sourceType)) return;
        if (sourceType == 0){
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            if (ObjectUtil.isNotEmpty(video)){
                ReflectUtil.setFieldValue(obj,  "userId", video.getUserId());
                ReflectUtil.setFieldValue(obj,  "tenantId", video.getTenantId());
            }
        }else if (sourceType == 1){
            UploadFileInfoVo file = uploadFileProducer.getByFileId(sourceId);
            if (ObjectUtil.isNotEmpty(file)){
                ReflectUtil.setFieldValue(obj,  "userId", file.getUserId());
                ReflectUtil.setFieldValue(obj,  "tenantId", file.getTenantId());
            }
        }else{
            RRException.create("未知来源");
        }
    }
}

