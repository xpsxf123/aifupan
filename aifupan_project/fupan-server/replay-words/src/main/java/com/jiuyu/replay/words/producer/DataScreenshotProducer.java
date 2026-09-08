package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotListBo;
import com.jiuyu.replay.words.vo.DataScreenshotInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotListVo;

import java.util.List;


/**
 * 数据截图记录
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
public interface DataScreenshotProducer {


    /**
     * 数据截图记录列表
     * @param dataScreenshotListBo 数据截图记录列表查询参数
     * @return
     */
    PageUtils<DataScreenshotListVo> queryPage(DataScreenshotListBo dataScreenshotListBo);

    /**
     * 数据截图记录列表
     * @param dataScreenshotListBo
     * @return
     */
    List<DataScreenshotListVo> list(DataScreenshotListBo dataScreenshotListBo);

    /**
    * 数据截图记录信息
    * @param id 数据截图记录id
    * @return
    */
    DataScreenshotInfoVo info(Long id);

    /**
     * 新增数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
     DataScreenshotInfoVo save(DataScreenshotBo dataScreenshotBo);

    /**
     * 修改数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    void update(DataScreenshotBo dataScreenshotBo);

    /**
     * 删除数据截图记录
     * @param id 数据截图记录id
     * @return
     */
    void deleteById(Long id);

    /**
     * 数据截图上传
     *
     * @param uploadBo
     */
    void screenshotUpload(DataScreenshotBo uploadBo);

    /**
     * 根据ids批量查询数据截图记录
     * @param ids
     * @return
     */
    List<DataScreenshotInfoVo> getByIds(List<Long> ids);

    /**
     * 根据code查询数据截图记录
     * @param batchNumber
     * @param videoId
     * @param screenshotCode
     * @param userId
     * @return
     */
    DataScreenshotInfoVo getByCode(String batchNumber, String videoId, String screenshotCode, Long userId);

    /**
     * 查询视频是否存在数据截图
     * @param bo
     * @return
     */
    boolean existVideoDataScreenshot(DataScreenshotListBo bo);

    /**
     * 更新数据截图记录状态
     * @param ids
     * @param status
     */
    void updateStatusByIds(List<Long> ids, int status);
}

