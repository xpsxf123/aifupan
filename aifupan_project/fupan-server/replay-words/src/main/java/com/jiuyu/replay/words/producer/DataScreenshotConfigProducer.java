package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;

import java.util.List;


/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
public interface DataScreenshotConfigProducer {


    /**
     * 数据截图配置列表
     * @param dataScreenshotConfigListBo 数据截图配置列表查询参数
     * @return
     */
    PageUtils<DataScreenshotConfigListVo> queryPage(DataScreenshotConfigListBo dataScreenshotConfigListBo);

    /**
    * 数据截图配置信息
    * @param id 数据截图配置id
    * @return
    */
    DataScreenshotConfigInfoVo info(Long id);

    /**
     * 新增数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
     DataScreenshotConfigInfoVo save(DataScreenshotConfigBo dataScreenshotConfigBo);

    /**
     * 修改数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    void update(DataScreenshotConfigBo dataScreenshotConfigBo);

    /**
     * 删除数据截图配置
     * @param id 数据截图配置id
     * @return
     */
    void deleteById(Long id);

    /**
     * 查询所有数据截图配置
     * @return
     */
    List<DataScreenshotConfigInfoVo> listAllSort();

    /**
     * 根据screenshotCode查询数据截图配置
     * @param screenshotCode
     * @return
     */
    DataScreenshotConfigInfoVo getByCode(String screenshotCode);
}

