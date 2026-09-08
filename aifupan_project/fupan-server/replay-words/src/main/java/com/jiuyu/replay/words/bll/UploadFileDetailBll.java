package com.jiuyu.replay.words.bll;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.third.AiFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailBo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailListBo;
import com.jiuyu.replay.words.producer.AnchorVideoDetailProducer;
import com.jiuyu.replay.words.producer.UploadFileDetailProducer;
import com.jiuyu.replay.words.vo.AnalysisResultAllVo;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailListVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailVo;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Component
public class UploadFileDetailBll {

    @Resource
    private AnchorVideoDetailProducer anchorVideoDetailProducer;
    @Resource
    private UploadFileDetailProducer uploadFileDetailProducer;
    @Resource
    private AiFeign aiFeign;


    /**
     * 文件的详情列表
     * @param uploadFileDetailListBo 文件的详情列表查询参数
     * @return
     */
    public R<PageUtils<UploadFileDetailListVo>> queryPage(UploadFileDetailListBo uploadFileDetailListBo) {

        return R.ok("获取成功", uploadFileDetailProducer.queryPage(uploadFileDetailListBo));
    }

    /**
    * 文件的详情信息
    * @param id 文件的详情id
    * @return
    */
    public R<UploadFileDetailInfoVo> info(Long id) {

        UploadFileDetailInfoVo uploadFileDetailInfoVo = uploadFileDetailProducer.info(id);
        return R.ok("获取成功", uploadFileDetailInfoVo);
    }

    /**
     * 文件的详情信息
     *
     * @param fileId 文件的详情id
     * @return 上传文件的详情
     */
    public UploadFileDetailInfoVo infoByFileId(String fileId) {
        UploadFileDetailBo bo = new UploadFileDetailBo();
        bo.setFileId(fileId);
        return uploadFileDetailProducer.getAndSave(bo);
    }

    /**
     * 新增文件的详情
     * @param uploadFileDetailBo 文件的详情对象
     * @return
     */
    public R<String> save(UploadFileDetailBo uploadFileDetailBo) {

        UploadFileDetailInfoVo uploadFileDetailInfoVo = uploadFileDetailProducer.save(uploadFileDetailBo);
        return R.ok("添加成功");
    }

    /**
     * 修改文件的详情
     * @param uploadFileDetailBo 文件的详情对象
     * @return
     */
    public R<String> update(UploadFileDetailBo uploadFileDetailBo) {

        uploadFileDetailProducer.update(uploadFileDetailBo);
        return R.ok("修改成功");
    }

    /**
     * 删除文件的详情
     * @param id 文件的详情id
     * @return
     */
    public R<String> delete(Long id) {

        uploadFileDetailProducer.deleteById(id);
        return R.ok("删除成功");
    }

}

