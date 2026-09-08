package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.alibaba.ImgOssUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigBo;
import com.jiuyu.replay.words.bo.DataScreenshotConfigListBo;
import com.jiuyu.replay.words.producer.DataScreenshotConfigProducer;
import com.jiuyu.replay.words.vo.DataScreenshotConfigInfoVo;
import com.jiuyu.replay.words.vo.DataScreenshotConfigListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * 数据截图配置
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Component
public class DataScreenshotConfigBll {

    @Resource
    private DataScreenshotConfigProducer dataScreenshotConfigProducer;
    @Resource
    private ImgOssUtils imgOssUtils;


    /**
     * 数据截图配置列表
     * @param dataScreenshotConfigListBo 数据截图配置列表查询参数
     * @return
     */
    public R<PageUtils<DataScreenshotConfigListVo>> queryPage(DataScreenshotConfigListBo dataScreenshotConfigListBo) {
        PageUtils<DataScreenshotConfigListVo> list = dataScreenshotConfigProducer.queryPage(dataScreenshotConfigListBo);
        if (list != null && ObjectUtil.isNotEmpty(list.getList())){
            for (DataScreenshotConfigListVo listVo : list.getList()) {
                listVo.setExampleList(setExampleList(listVo.getExample()));
            }
        }
        return R.ok("获取成功", list);
    }

    /**
    * 数据截图配置信息
    * @param id 数据截图配置id
    * @return
    */
    public R<DataScreenshotConfigInfoVo> info(Long id) {

        DataScreenshotConfigInfoVo dataScreenshotConfigInfoVo = dataScreenshotConfigProducer.info(id);
        dataScreenshotConfigInfoVo.setExampleList(setExampleList(dataScreenshotConfigInfoVo.getExample()));
        return R.ok("获取成功", dataScreenshotConfigInfoVo);
    }

    /**
     * 设置示例图片列表
     * @param example
     */
    private List<FileShowVo> setExampleList(String example) {
        List<FileShowVo> exampleList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(example)){
            String[] split = example.split("_");
            if (ObjectUtil.isNotEmpty(split)){
                for (String s : split) {
                    FileShowVo e = new FileShowVo();
                    e.setUrl(imgOssUtils.getUrl(s));
                    e.setName(s);
                    exampleList.add(e);
                }
            }
        }
        return exampleList;
    }

    /**
     * 新增数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    public R<String> save(DataScreenshotConfigBo dataScreenshotConfigBo) {

        DataScreenshotConfigInfoVo dataScreenshotConfigInfoVo = dataScreenshotConfigProducer.save(dataScreenshotConfigBo);
        return R.ok("添加成功");
    }

    /**
     * 修改数据截图配置
     * @param dataScreenshotConfigBo 数据截图配置对象
     * @return
     */
    public R<String> update(DataScreenshotConfigBo dataScreenshotConfigBo) {

        dataScreenshotConfigProducer.update(dataScreenshotConfigBo);
        return R.ok("修改成功");
    }

    /**
     * 删除数据截图配置
     * @param id 数据截图配置id
     * @return
     */
    public R<String> delete(Long id) {

        dataScreenshotConfigProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

