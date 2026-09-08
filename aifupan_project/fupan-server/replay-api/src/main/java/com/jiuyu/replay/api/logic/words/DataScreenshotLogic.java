package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.common.utils.CustomizeSseEmitter;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.DataScreenshotBo;
import com.jiuyu.replay.words.bo.DataScreenshotListBo;
import com.jiuyu.replay.words.bo.DataScreenshotUploadBo;
import com.jiuyu.replay.words.bo.ScreenshotAnalysisBo;
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
public interface DataScreenshotLogic {


    /**
     * 数据截图记录列表
     * @param dataScreenshotListBo 数据截图记录列表查询参数
     * @return
     */
    R<PageUtils<DataScreenshotListVo>> queryPage(DataScreenshotListBo dataScreenshotListBo);

    /**
    * 数据截图记录信息
    * @param id 数据截图记录id
    * @return
    */
    R<DataScreenshotInfoVo> info(Long id);

    /**
     * 新增数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    R<String> save(DataScreenshotBo dataScreenshotBo);

    /**
     * 修改数据截图记录
     * @param dataScreenshotBo 数据截图记录对象
     * @return
     */
    R<String> update(DataScreenshotBo dataScreenshotBo);

    /**
     * 删除数据截图记录
     * @param id 数据截图记录id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 数据截图上传
     * @param uploadBo
     * @return
     */
    R<DataScreenshotInfoVo> screenshotUpload(DataScreenshotUploadBo uploadBo);

    /**
     * 数据截图分析
     *
     * @param emitter
     * @param analysisBo
     */
    void screenshotAnalysis(CustomizeSseEmitter emitter, ScreenshotAnalysisBo analysisBo);

    /**
     * 数据截图列表
     *
     * @param batchNumber
     * @param videoId
     * @return
     */
    R<List<DataScreenshotListVo>> dataScreenshotList(Integer batchNumber, String videoId);

    /**
     * 根据batchNumber和videoId查询数据截图列表
     * @param sourceType
     * @param sourceId
     * @return
     */
    R<List<DataScreenshotListVo>> getExistDataScreenshotList(Integer sourceType, String sourceId);
}

