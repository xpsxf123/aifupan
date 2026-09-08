package com.jiuyu.replay.api.logic.third;

import com.jiuyu.replay.generic.bo.third.QueryDanMuBo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.video.VideoDanMuExportDetailVo;
import com.jiuyu.replay.third.bo.*;
import com.jiuyu.replay.third.vo.QueryDanMuVo;
import com.jiuyu.replay.third.vo.QueryOtherDanMuVo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 下午2:29
 */
public interface TableStoreLogic {

    /**
     * 上传弹幕数据
     * @param danMuData
     * @param file
     * @return
     */
    R<String> uploadDanMuData(UploadDanMuDataBo danMuData, MultipartFile file) throws Exception;

    /**
     * 查询弹幕数据
     * @param queryDanMuBo
     * @return
     */
    R<QueryDanMuVo> queryDanMuData(QueryDanMuBo queryDanMuBo);

    /**
     * 查询弹幕数据总条数
     *
     * @param queryDanMuBo 搜索条数
     * @return 总条数
     */
    R<VideoDanMuExportDetailVo> queryDanMuCount(QueryDanMuBo queryDanMuBo);

    /**
     * 查询其他场次弹幕数据
     *
     * @param danMu
     * @return
     */
    R<List<QueryOtherDanMuVo>> queryOtherDanMuData(QueryOtherDanMuBo danMu) throws Exception;

    /**
     * 判断是否存在弹幕
     * @param bo
     * @return
     */
    R<Boolean> existsBarrage(UploadLocalDanMuDataBo bo);

    /**
     * 导出弹幕数据
     *
     * @param response         响应流
     * @param queryDanMuExport 条件
     */
    void queryDanMuExport(HttpServletResponse response, QueryDanMuExport queryDanMuExport) throws IOException;
}
