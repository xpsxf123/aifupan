package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;


/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
public interface DataScreenshotConfigLogic {


    /**
     * 数据截图配置列表
     * @param dataScreenshotConfigListBo 数据截图配置列表查询参数
     * @return
     */
    R<PageUtils<DataScreenshotConfigListVo>> queryPage(DataScreenshotConfigListBo dataScreenshotConfigListBo);

    /**
    * 数据截图配置信息
    * @param id 数据截图配置id
    * @return
    */
    R<DataScreenshotConfigInfoVo> info(Long id);

    /**
     * 新增数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    R<String> save(DataScreenshotConfigBo dataScreenshotConfigBo);

    /**
     * 修改数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    R<String> update(DataScreenshotConfigBo dataScreenshotConfigBo);

    /**
     * 删除数据截图配置
     * @param id 数据截图配置id
     * @return
     */
    R<String> delete(Long id);


}

