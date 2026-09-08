package com.jiuyu.replay.words.bll;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jiuyu.replay.common.constant.*;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.third.AiFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.bo.file.UploadFileDetailBo;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailListBo;
import com.jiuyu.replay.words.enums.ImportantBarrageStatusEnum;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import com.jiuyu.replay.words.vo.AnalysisResultAllVo;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import com.jiuyu.replay.words.vo.video.ImportantBarrageStatusVo;
import com.jiuyu.replay.words.vo.video.ToGeneratedVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * 视频的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-27 15:30:33
 */
@Component
@Slf4j
public class AnchorVideoDetailBll {

    @Resource
    private AnchorVideoDetailProducer anchorVideoDetailProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private UploadFileDetailProducer uploadFileDetailProducer;
    @Resource
    private CueWordsProducer cueWordsProducer;
    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private AiFeign aiFeign;
    @Resource
    private UserFeign userFeign;
    @Resource
    private ApplicationContext context;
    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private VideoContentProducer videoContentProducer;
    @Resource
    private AiModelFeign aiModelFeign;
    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
    @Resource
    private VideoSliceRse videoSliceRse;

    /**
     * 视频的详情列表
     *
     * @param anchorVideoDetailListBo 视频的详情列表查询参数
     * @return
     */
    public R<PageUtils<AnchorVideoDetailListVo>> queryPage(AnchorVideoDetailListBo anchorVideoDetailListBo) {

        return R.ok("获取成功", anchorVideoDetailProducer.queryPage(anchorVideoDetailListBo));
    }

    /**
     * 视频的详情信息
     *
     * @param id 视频的详情id
     * @return
     */
    public R<AnchorVideoDetailInfoVo> info(Long id) {

        AnchorVideoDetailInfoVo anchorVideoDetailInfoVo = anchorVideoDetailProducer.info(id);
        return R.ok("获取成功", anchorVideoDetailInfoVo);
    }

    /**
     * 获取视频的详情信息，没有就创建
     *
     * @param anchorVideoDetailBo
     * @return
     */
    public R<AnchorVideoDetailInfoVo> getAndSave(AnchorVideoDetailBo anchorVideoDetailBo) {
        return R.ok(anchorVideoDetailProducer.getAndSave(anchorVideoDetailBo));
    }

    /**
     * 修改视频的详情
     *
     * @param anchorVideoDetailBo 视频的详情对象
     * @return
     */
    public R<String> update(AnchorVideoDetailBo anchorVideoDetailBo) {

        anchorVideoDetailProducer.update(anchorVideoDetailBo);
        return R.ok("修改成功");
    }

    /**
     * 根据视频id更新
     *
     * @param anchorVideoDetailBo 值
     * @return 是否成功
     */
    public Boolean updateByVideoId(AnchorVideoDetailBo anchorVideoDetailBo) {
        return anchorVideoDetailProducer.updateByVideoId(anchorVideoDetailBo);
    }

    /**
     * 删除视频的详情
     *
     * @param id 视频的详情id
     * @return
     */
    public R<String> delete(Long id) {

        anchorVideoDetailProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 查询视频或文件的详情
     *
     * @param sourceId
     * @param type
     * @param sourceType
     * @return
     */
    private AnchorVideoFileAllVo setVideoOrFile(String sourceId, Integer type, Integer sourceType) {
        AnchorVideoFileAllVo result = new AnchorVideoFileAllVo();
        result.setSourceId(sourceId);
        result.setSourceType(sourceType);
        result.setType(type);
        result.setContentStatus(WordsEnum.contentStatus.PENDING.getCode());
        if (WordsEnum.sourceType.VIDEO.getCode() == sourceType) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            RRException.isNotEmpty(video, "查询视频失败");
            result.setAnchorVideo(video);
            AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
            detailBo.setVideoId(sourceId);
            AnchorVideoDetailInfoVo andSave = anchorVideoDetailProducer.getAndSave(detailBo);
            if (andSave != null) {
                result.setUserId(andSave.getUserId());
                result.setTenantId(andSave.getTenantId());
                result.setAnchorVideoDetail(andSave);
                result.setContentStatus(getContentStatus(type, andSave.getNatureContentStatus(), andSave.getOptimizeContentStatus()));
            }
        } else if (WordsEnum.sourceType.FILE.getCode() == sourceType) {
            UploadFileInfoVo uploadFile = uploadFileProducer.getByFileId(sourceId);
            RRException.isNotEmpty(uploadFile, "查询文件失败");
            result.setUploadFile(uploadFile);
            UploadFileDetailBo detailBo = new UploadFileDetailBo();
            detailBo.setFileId(sourceId);
            UploadFileDetailInfoVo andSave = uploadFileDetailProducer.getAndSave(detailBo);
            if (andSave != null) {
                result.setUserId(andSave.getUserId());
                result.setTenantId(andSave.getTenantId());
                result.setUploadFileDetail(andSave);
                result.setContentStatus(getContentStatus(type, andSave.getNatureContentStatus(), andSave.getOptimizeContentStatus()));
            }
        } else {
            RRException.create("暂不支持其他类型");
        }
        return result;
    }

    private Integer getContentStatus(Integer type, Integer natureContentStatus, Integer optimizeContentStatus) {
        if (type == WordsEnum.contentType.NATURE.getCode()) {
            return natureContentStatus;
        } else if (type == WordsEnum.contentType.OPTIMIZE.getCode()) {
            return optimizeContentStatus;
        } else {
            RRException.create("暂不支持其他类型");
        }
        return null;
    }

    private Long getContentTime(Integer type, Long natSetTime, Long optSetTime) {
        if (type == WordsEnum.contentType.NATURE.getCode()) {
            return natSetTime;
        } else if (type == WordsEnum.contentType.OPTIMIZE.getCode()) {
            return optSetTime;
        } else {
            RRException.create("暂不支持其他类型");
        }
        return 0L;
    }


    /**
     * 获取视频内容
     *
     * @param sourceId
     * @param type
     * @param sourceType
     * @return
     */
    public R<AnchorVideoFileAllVo> getVideoContent(String sourceId, Integer type, Integer sourceType) {
        RRException.isNotEmpty(sourceType, "sourceType不能为空");
        RRException.isNotEmpty(sourceId, "sourceId不能为空");
        RRException.isNotEmpty(type, "type不能为空");

        // 获取要返回的值
        AnchorVideoFileAllVo result = setVideoOrFile(sourceId, type, sourceType);

        // 状态==待生成 && 视频详情不存在 && 文件详情不存在
        if (result.getContentStatus() == WordsEnum.contentStatus.PENDING.getCode() && result.getAnchorVideoDetail() == null && result.getUploadFileDetail() == null) {
            return R.ok(result);
        }

        // 状态==生成中
        if (result.getContentStatus() == WordsEnum.contentStatus.GENERATING.getCode()) {
            // 检测原文分析的状态是否是生成中，是否超出设置的分析时间，如果超出就设置为生成失败
            handleStatusOne(result);
        }

        // 判断是生成完成就查询mongodb中的原文内容
        // 状态==生成成功
        if (result.getContentStatus() == WordsEnum.contentStatus.SUCCESS.getCode()) {
            // 查询对应的原文内容
            List<VideoContentVo> videoContentList = anchorVideoDetailProducer.selVideoContents(sourceId, type, sourceType, result.getUserId(), result.getTenantId());
            if (ObjectUtil.isNotEmpty(videoContentList)) {
                result.setVideoFileContentList(videoContentList);
            }
        }
        return R.ok(result);
    }


    private void handleStatusOne(AnchorVideoFileAllVo anchorVideoFile) {
        if (anchorVideoFile.getAnchorVideoDetail() == null && anchorVideoFile.getUploadFileDetail() == null) {
            return;
        }
        SystemKvInfoVo aiQuestionTimeOut = systemKvProducer.getByKey("ai_question_timeOut");
        int timeOut = 30;
        if (aiQuestionTimeOut != null) {
            timeOut = NumberUtil.parseInt(aiQuestionTimeOut.getKvValue(), timeOut);
        }
        long milliseconds = (long) timeOut * 60 * 1000;
        long nowMilliseconds = System.currentTimeMillis();

        if (anchorVideoFile.getSourceType() == WordsEnum.sourceType.VIDEO.getCode() && anchorVideoFile.getAnchorVideoDetail() != null) {
            AnchorVideoDetailInfoVo anchorVideoDetail = anchorVideoFile.getAnchorVideoDetail();
            Long contentTime = getContentTime(anchorVideoFile.getType(), anchorVideoDetail.getNatSetTime(), anchorVideoDetail.getOptSetTime());
            if (contentTime != null && nowMilliseconds - contentTime > milliseconds) {
                anchorVideoDetailProducer.updateContentStatus(anchorVideoDetail.getId(), anchorVideoFile.getType(), WordsEnum.contentStatus.FAILED.getCode());
            }
        } else if (anchorVideoFile.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            UploadFileDetailInfoVo uploadFileDetail = anchorVideoFile.getUploadFileDetail();
            Long contentTime = getContentTime(anchorVideoFile.getType(), uploadFileDetail.getNatSetTime(), uploadFileDetail.getOptSetTime());
            if (contentTime != null && nowMilliseconds - contentTime > milliseconds) {
                uploadFileDetailProducer.updateContentStatus(uploadFileDetail.getId(), anchorVideoFile.getType(), WordsEnum.contentStatus.FAILED.getCode());
            }
        }
    }

    private void handleStatusTxtOne(UploadFileDetailVo uploadFileDetailVo, Integer type, AnchorVideoFileAllVo videoFileAllVo) {
        SystemKvInfoVo aiQuestionTimeOut = systemKvProducer.getByKey("ai_question_timeOut");
        Integer timeOut = 15;
        if (aiQuestionTimeOut!=null){
            timeOut=Integer.valueOf(aiQuestionTimeOut.getKvValue());
        }
        long milliseconds = timeOut * 60 * 1000;
        long nowMilliseconds = System.currentTimeMillis();
        UploadFileDetailVo updateVo =  new UploadFileDetailVo();
        updateVo.setId(uploadFileDetailVo.getId());
        updateVo.setFileId(uploadFileDetailVo.getFileId());
        if (type==1&&uploadFileDetailVo.getNatureContentStatus()==1){
            if (uploadFileDetailVo.getNatSetTime()==null){
                //返回失败
                videoFileAllVo.setContentStatus(3);
                uploadFileDetailVo.setNatureContentStatus(3);
                uploadFileDetailVo.setUpdateDate(new Date());
                uploadFileDetailProducer.updateFileDetailById(updateVo);
                return ;
            }
            //超过指定时间
            if (nowMilliseconds-uploadFileDetailVo.getNatSetTime()>milliseconds){
                //返回失败
                videoFileAllVo.setContentStatus(3);
                uploadFileDetailVo.setNatureContentStatus(3);
                uploadFileDetailVo.setUpdateDate(new Date());
                uploadFileDetailProducer.updateFileDetailById(updateVo);
                return ;
            }

        }
        if (type==2&&uploadFileDetailVo.getOptimizeContentStatus()==1){
            if (uploadFileDetailVo.getOptSetTime()==null){
                //返回失败
                videoFileAllVo.setContentStatus(3);
                uploadFileDetailVo.setOptimizeContentStatus(3);
                uploadFileDetailVo.setUpdateDate(new Date());
                uploadFileDetailProducer.updateFileDetailById(updateVo);
                return ;
            }
            //超过指定时间
            if (nowMilliseconds-uploadFileDetailVo.getOptSetTime()>milliseconds){
                //返回失败
                videoFileAllVo.setContentStatus(3);
                uploadFileDetailVo.setOptimizeContentStatus(3);
                uploadFileDetailVo.setUpdateDate(new Date());
                uploadFileDetailProducer.updateFileDetailById(updateVo);
                return ;
            }
        }
    }


    public void exportVideoContent(AnchorVideoFileAllVo anchorVideoFileAllVo, HttpServletResponse response, Integer sourceType) {
        if (ObjectUtil.isNull(anchorVideoFileAllVo)) {
            RRException.create("查询不到视频内容文本数据");
        }
        if (anchorVideoFileAllVo.getVideoFileContentList() == null || anchorVideoFileAllVo.getVideoFileContentList().isEmpty()) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }
        //视频
        if (sourceType == 0) {
            String videoName = "";
            if (ObjectUtil.isNotEmpty(anchorVideoFileAllVo.getAnchorVideo())) {
                AnchorVideoInfoVo anchorVideo = anchorVideoFileAllVo.getAnchorVideo();
                if (ObjectUtil.isNotEmpty(anchorVideo.getVideoName())) {
                    int endIndex = anchorVideo.getVideoName().lastIndexOf('_');
                    if (endIndex != -1) {
                        videoName = anchorVideo.getVideoName().substring(0, endIndex);
                    } else {
                        videoName = anchorVideo.getVideoName();
                    }
                }
            }
            String type = "自然原文";
            if (anchorVideoFileAllVo.getType() != null && anchorVideoFileAllVo.getType().equals(2)) {
                type = "优化原文";
            }
            toExport(response, videoName, type, anchorVideoFileAllVo.getVideoFileContentList());
        }
        //文件
        if (sourceType == 1) {
            String fileName = "";
            if (ObjectUtil.isNotEmpty(anchorVideoFileAllVo.getUploadFile())) {
                UploadFileInfoVo uploadFile = anchorVideoFileAllVo.getUploadFile();
                if (ObjectUtil.isNotEmpty(uploadFile.getFileName())) {
                    int endIndex = uploadFile.getFileName().lastIndexOf('_');
                    if (endIndex != -1) {
                        fileName = uploadFile.getFileName().substring(0, endIndex);
                    } else {
                        fileName = uploadFile.getFileName();
                    }
                }
            }
            String type = "自然原文";
            if (anchorVideoFileAllVo.getType() != null && anchorVideoFileAllVo.getType().equals(2)) {
                type = "优化原文";
            }
            toExport(response, fileName, type, anchorVideoFileAllVo.getVideoFileContentList());
        }
    }


    public void exportContent(AnalysisResultAllVo data, HttpServletResponse response, Integer sourceType) {
        if (ObjectUtil.isNull(data)) {
            RRException.create("查询不到内容文本数据");
        }
        if (ObjectUtil.isEmpty(data.getAnalysisList())) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }
        String type = "分钟段落";

        //视频的分钟段落
        if (sourceType == 0) {
            String videoName = "";
            if (ObjectUtil.isNotEmpty(data.getVideoInfo())) {
                AnchorVideoInfoVo videoInfo = data.getVideoInfo();
                if (ObjectUtil.isNotEmpty(videoInfo.getVideoName())) {
                    if (videoInfo.getVideoName().contains("_")){
                        videoName = videoInfo.getVideoName().substring(0, videoInfo.getVideoName().lastIndexOf('_'));
                    } else if (videoInfo.getVideoName().contains(".")) {
                        videoName = videoInfo.getVideoName().substring(0, videoInfo.getVideoName().lastIndexOf('.'));
                    } else {
                        videoName = videoInfo.getVideoName();
                    }

                }
            }
            toExportMin(response, videoName, type, data.getAnalysisList());
        }
        //文件的分钟段落
        if (sourceType == 1) {
            String fileName = "";
            if (ObjectUtil.isNotEmpty(data.getUploadFileInfoVo())) {
                UploadFileInfoVo uploadFileInfoVo = data.getUploadFileInfoVo();
                if (ObjectUtil.isNotEmpty(uploadFileInfoVo.getFileName())) {
                    if (uploadFileInfoVo.getFileName().contains("_")){
                        fileName = uploadFileInfoVo.getFileName().substring(0,uploadFileInfoVo.getFileName().lastIndexOf('_'));
                    } else if (uploadFileInfoVo.getFileName().contains(".")) {
                        fileName = uploadFileInfoVo.getFileName().substring(0, uploadFileInfoVo.getFileName().lastIndexOf("."));
                    } else {
                        fileName = uploadFileInfoVo.getFileName();
                    }
                }
            }
            toExportMin(response, fileName, type, data.getAnalysisList());
        }

    }


    /**
     * 导出分钟段落
     * @param response
     * @param videoName
     * @param type
     * @param audioaAlyses
     */
    private void toExportMin(HttpServletResponse response, String videoName, String type, List<OnlineAnalysisItemVo> audioaAlyses) {
        if (audioaAlyses == null || audioaAlyses.isEmpty()) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(videoName + (!ObjectUtil.isEmpty(videoName) ? "_" : "") + type + ".txt", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));

        try (OutputStream os = response.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            
            // 按段落号排序
            List<OnlineAnalysisItemVo> list = new ArrayList<>(audioaAlyses);
            list = list.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(OnlineAnalysisItemVo::getParagraph, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            
            // 写入内容
            for (OnlineAnalysisItemVo content : list) {
                if (content != null && StringUtil.isNotBlank(content.getDataJson())) {
                    JSONObject entries ;
                    try {
                        entries = JSONUtil.parseObj(content.getDataJson());
                    } catch (JSONException e) {
                        log.error("JSON转换错误，content: {}", content.getDataJson(), e);
                        // 跳过当前content，继续处理下一个
                        continue;
                    }
                    if (ObjectUtil.isNotEmpty(entries)){
                        String contentStr = entries.getStr("content");
                        if (contentStr != null) {
                            writer.write(cleanContent(contentStr));
                            writer.write(System.lineSeparator());
                        }
                    }
                }
            }
            writer.flush();
        } catch (IOException e) {
            log.error("导出分钟段落时出错:{}",videoName,e);
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(),"导出文本时出错");
        }
    }


    /**
     * 导出自然/优化原文
     * @param response
     * @param videoName
     * @param type
     * @param videoContentList
     */
    private void toExport(HttpServletResponse response, String videoName, String type, List<VideoContentVo> videoContentList) {
        if (videoContentList == null || videoContentList.isEmpty()) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(videoName + (!ObjectUtil.isEmpty(videoName) ? "_" : "") + type + ".txt", StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));

        try (OutputStream os = response.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            
            // 按段落号排序
            List<VideoContentVo> list = new ArrayList<>(videoContentList);
            list = list.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(VideoContentVo::getParagraph, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();

            
            // 写入内容
            for (VideoContentVo content : list) {
                if (content != null && StringUtil.isNotBlank(content.getContent()) && content.getContent().startsWith("[")){
                    List<String> strings ;
                    try {
                        strings = JSONUtil.toList(content.getContent(), String.class);
                    } catch (JSONException e) {
                        log.error("JSON转换错误，content: {}", content.getContent(), e);
                        // 跳过当前content，继续处理下一个
                        continue;
                    }
                    if (ObjectUtil.isNotEmpty(strings)){
                        for (String string : strings) {
                            if (string != null){
                                writer.write(cleanContent(string));
                                writer.write(System.lineSeparator());
                            }
                        }
                    }
                }
            }
            writer.flush();
        } catch (IOException e) {
            log.error("导出自然/优化原文时出错:{}",videoName,e);
            throw new BusinessException(StatusCode.OPERATION_EX.getCode(),"导出文本时出错");
        }
    }

    private static final Parser MD_PARSER;
    private static final HtmlRenderer MD_RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        MD_PARSER = Parser.builder(options).build();
        MD_RENDERER = HtmlRenderer.builder(options).build();
    }

    /**
     * 清洗内容：Markdown → HTML → 去标签，保留纯文本
     */
    private String cleanContent(String content) {
        if (content == null) {
            return null;
        }
        String html = MD_RENDERER.render(MD_PARSER.parse(content));
        return HtmlUtil.cleanHtmlTag(html);
    }


    //导出文本 这是自然原文或优化原文的
    public void exportVideoContentToTxt(String fileName, List<VideoContentVo> videoContentList) {

        // 1. 按段落号排序（确保顺序正确）
        if (videoContentList.isEmpty()) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }
        videoContentList.sort(Comparator.comparing(VideoContentVo::getParagraph, Comparator.nullsLast(Comparator.naturalOrder())));
        // 2. 写入 TXT 文件（使用 UTF-8 编码）
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {

            // 3. 遍历内容并写入
            for (VideoContentVo content : videoContentList) {
                List<String> strings = JSONUtil.toList(content.getContent(), String.class);
                for (String string : strings) {
                    writer.write(cleanContent(string));
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            RRException.create("内容写入TXT文件出错");
        }
    }

    //这是原文段落的
    private void exportVideoContentToTxt2(String fileName, List<OnlineAnalysisItemVo> audioaAlyses) {
        // 1. 按段落号排序（确保顺序正确）
        if (audioaAlyses.isEmpty()) {
            RRException.create(CustomizeConstant.NO_SPECIFIC_TEXT.getValue());
        }
        audioaAlyses.sort(Comparator.comparing(OnlineAnalysisItemVo::getParagraph, Comparator.nullsLast(Comparator.naturalOrder())));
        // 2. 写入 TXT 文件（使用 UTF-8 编码）
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8))) {

            // 3. 遍历内容并写入
            for (OnlineAnalysisItemVo content : audioaAlyses) {
                if (content != null && StringUtil.isNotBlank(content.getDataJson())) {
                    JSONObject entries = JSONUtil.parseObj(content.getDataJson());
                    // 格式：内容
                    writer.write(cleanContent(entries.getStr("content")));
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            RRException.create("内容写入TXT文件出错");
        }

    }


    /**
     * 自然/优化原文生成进行中
     * @param bo
     * @return
     */
    public R<String> generateVideoContentStatus(GenerateVideoContentBo bo) {
        UserCacheVo localUser = ResultUtil.getResult(userFeign.getLocalUser());
        bo.setUserId(localUser.getId());
        bo.setTenantId(localUser.getActiveTenantId());
        if (ObjectUtil.equals(bo.getSourceType(), WordsEnum.sourceType.VIDEO.getCode())) {
            AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
            detailBo.setVideoId(bo.getSourceId());
            anchorVideoDetailProducer.getAndSave(detailBo);
            toUpdateVideoDetail(bo);
        } else if (ObjectUtil.equals(bo.getSourceType(), WordsEnum.sourceType.FILE.getCode())) {
            UploadFileDetailBo detailBo = new UploadFileDetailBo();
            detailBo.setFileId(bo.getSourceId());
            uploadFileDetailProducer.getAndSave(detailBo);
            toUpdateFileDetail(bo, System.currentTimeMillis());
        } else {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "当前接口只支持视频或文件");
        }
        return R.ok("生成进行中");
    }


    private List<CueWordsListVo> toSelectTrade(String sourceId, Long tradeId, Integer type, Integer sourceType) {
        CueWordsListBo cueWordsListBo = new CueWordsListBo();
        cueWordsListBo.setSourceId(sourceId);
        cueWordsListBo.setApplyTo(0);
        cueWordsListBo.setSourceType(sourceType);
        //7自然  8优化
        cueWordsListBo.setCueType(type == 1 ? 7 : 8);
        List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeId(tradeId, Constant.GeneralEnum.GENERAL_NO.getCode());
        if (ObjectUtil.isNotEmpty(tradeVos) && ObjectUtil.isNotEmpty(tradeVos)) {
            cueWordsListBo.setTradeIds(tradeVos.stream().map(TradeInfoVo::getId).sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
        }
        PageUtils<CueWordsListVo> cueWordsListVoPageUtils = cueWordsProducer.queryPage(cueWordsListBo);
        List<CueWordsListVo> list = cueWordsListVoPageUtils.getList();
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list;
    }

    /**
     * 根据行业获取提示词（供定时器生成流程调用）。
     *
     * <h3>说明</h3>
     * 此方法是已有私有方法 {@link #toSelectTrade} 的公开包装。
     * 定时器流程需要复用提示词查询逻辑，但不改动原有的私有方法签名。
     *
     * <h3>返回数据格式</h3>
     * 每项 {@link CueWordsListVo} 包含：
     * <ul>
     *   <li>cueWord — 完整的 AI 提示词（含 system + problem + rule）</li>
     *   <li>rule — 规则说明</li>
     *   <li>problem — 问题描述</li>
     * </ul>
     *
     * @param sourceId   来源 id（videoId 或 fileId），用于查找对应分析数据中的行业
     * @param tradeId    行业 id
     * @param type       内容类型：1=自然原文, 2=优化原文
     * @param sourceType 来源类型：0=视频, 1=文件（用于区分提示词配置）
     * @return 提示词列表，未配置时返回空列表
     */
    public List<CueWordsListVo> getCueWordsByTrade(String sourceId, Long tradeId, Integer type, Integer sourceType) {
        return toSelectTrade(sourceId, tradeId, type, sourceType);
    }


    /**
     * 修改视频详情中原文的状态为生成中
     *
     * @param bo 修改条件
     * @return
     */
    private AnchorVideoDetailVo toUpdateVideoDetail(GenerateVideoContentBo bo) {
        return anchorVideoDetailProducer.updateContentStatusIng(bo);
    }

    private UploadFileDetailVo toUpdateFileDetail(GenerateVideoContentBo bo,Long setTime) {
        return uploadFileDetailProducer.getByFileId(bo,setTime);
    }


    /**
     * 获取当前用户待生产的自然、优化原文
     *
     * @param fun 用于获取原内容的函数
     * @return 待生产内容列表
     */
    public List<ToGeneratedVo> contentByToGenerated(BiFunction<Integer, String, AnalysisResultVo> fun) {
        List<ToGeneratedVo> result = getToGeneratedList();

        if (ObjectUtil.isEmpty(result)) {
            return List.of();
        }

        if (ObjectUtil.isNotEmpty(result)) {
            List<ToGeneratedVo> orGenerateQAContent = getOrGenerateQAContent(result, fun);
            if (orGenerateQAContent != null) {
                result = orGenerateQAContent;
            }
        }

        // 设置使用的模型
        int aiModel = getAiModel();
        for (ToGeneratedVo vo : result) {
            vo.setStatus(0);
            vo.setAiModel(aiModel);
        }


        return result;
    }

    private int getAiModel() {
        int aiModel = 0;
        SystemKvInfoVo aiQuestionGenModel = systemKvProducer.getByKey("ai_question_gen_model");
        if (aiQuestionGenModel != null) {
            aiModel = aiModelFeign.getAiModel(aiQuestionGenModel.getKvValue());
        }
        return aiModel;
    }

    /**
     * 根据视频ID获取待生产的自然、优化原文
     *
     * @param videoIds 视频ID列表
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 待生产内容列表
     */
    public List<ToGeneratedVo> getVideoByToGenerated(List<String> videoIds, Long userId, Long tenantId) {
        List<ToGeneratedVo> result = new ArrayList<>();
        // 查询视频的
        List<AnchorVideoDetailInfoVo> anchorVideoDetailList = anchorVideoDetailProducer.contentByToGenerated(videoIds, userId, tenantId);
        if (ObjectUtil.isNotEmpty(anchorVideoDetailList)) {
            for (AnchorVideoDetailInfoVo item : anchorVideoDetailList) {
                AnchorVideoInfoVo video = item.getVideo();
                if (video == null) {
                    continue; // 避免后续 NPE
                }
                if (ObjectUtil.equals(item.getNatureContentStatus(), 1)) {
                    addToGeneratedList(result, video, 1, item.getNatSetTime());
                }
                if (ObjectUtil.equals(item.getOptimizeContentStatus(), 1)) {
                    addToGeneratedList(result, video, 2, item.getOptSetTime());
                }
            }
        }
        return result;
    }

    /**
     * 将视频信息添加到待生产列表中
     *
     * @param list      待生产内容列表
     * @param video     视频信息
     * @param type      内容类型
     * @param startTime 开始时间
     */
    private void addToGeneratedList(List<ToGeneratedVo> list, AnchorVideoInfoVo video, int type, Long startTime) {
        ToGeneratedVo e = new ToGeneratedVo();
        e.setSourceId(video.getVideoId());
        e.setSourceType(0); // 公共字段统一设置
        e.setType(type);
        e.setStartDateTime(ObjectUtil.defaultIfNull(startTime, 0L));
        e.setAnchorVideo(video);
        list.add(e);
    }

    /**
     * 根据文件ID获取待生产的自然、优化原文
     *
     * @param fileIds  文件ID列表
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 待生产内容列表
     */
    public List<ToGeneratedVo> getFileByToGenerated(List<String> fileIds, Long userId, Long tenantId) {
        List<ToGeneratedVo> result = new ArrayList<>();
        List<UploadFileDetailInfoVo> uploadFileDetailInfoVos = uploadFileDetailProducer.contentByToGenerated(fileIds, userId, tenantId);
        if (ObjectUtil.isNotEmpty(uploadFileDetailInfoVos)) {
            for (UploadFileDetailInfoVo item : uploadFileDetailInfoVos) {
                if (item == null) {
                    continue; // 防止空指针异常
                }
                UploadFileInfoVo uploadFile = item.getUploadFile();
                if (uploadFile == null) {
                    continue;
                }
                if (ObjectUtil.equals(item.getNatureContentStatus(), 1)) {
                    addToResult(result, uploadFile, 1, item.getNatSetTime());
                }
                if (ObjectUtil.equals(item.getOptimizeContentStatus(), 1)) {
                    addToResult(result, uploadFile, 2, item.getOptSetTime());
                }
            }
        }
        return result;
    }

    /**
     * 将文件信息添加到待生产列表中
     *
     * @param result     待生产内容列表
     * @param uploadFile 文件信息
     * @param type       内容类型
     * @param startTime  开始时间
     */
    private void addToResult(List<ToGeneratedVo> result, UploadFileInfoVo uploadFile, int type, Long startTime) {
        ToGeneratedVo e = new ToGeneratedVo();
        e.setSourceId(uploadFile.getFileId());
        e.setSourceType(1);
        e.setType(type);
        e.setStartDateTime(startTime);
        e.setUploadFile(uploadFile);
        result.add(e);
    }

    /**
     * 获取当前用户待生成的自然、优化原文
     *
     * @return 待生成内容列表
     */
    public List<ToGeneratedVo> getToGeneratedList() {
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "获取用户信息失败");
        // 查询视频的
        List<ToGeneratedVo> result = new ArrayList<>(getVideoByToGenerated(null, user.getId(), user.getActiveTenantId()));
        // 查询文件的
        result.addAll(getFileByToGenerated(null, user.getId(), user.getActiveTenantId()));
        return result.stream().sorted(Comparator.comparing(ToGeneratedVo::getStartDateTime)).toList();
    }

    /**
     * 获取或生成AI问的提示词
     *
     * @param result 待处理的内容列表
     * @param fun    用于分析内容的函数
     * @return 处理后的内容列表
     */
    public List<ToGeneratedVo> getOrGenerateQAContent(List<ToGeneratedVo> result, BiFunction<Integer, String, AnalysisResultVo> fun) {
        if (ObjectUtil.isEmpty(result)) {
            return result;
        }
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "获取登录信息失败");

        // 查询在mongodb中是否有未分析完成的提示词，如果有就不新创建了
        setVideoContentList(result, user.getId(), user.getActiveTenantId());

        // 创建mongodb中没有查询到的记录
        // 1.获取分钟段落
        Map<String, AnalysisResultVo> analysisMap = analysisMapBySource(result, fun);

        // 组装并生成提示词
        for (ToGeneratedVo item : result) {
            if (ObjectUtil.isNull(item.getVideoContentList())) {
                AnalysisResultVo analysisResultVo = analysisMap.get(item.getSourceId());
                if (analysisResultVo != null && ObjectUtil.isNotEmpty(analysisResultVo.getSentenceMarkVos())) {
                    List<String> contentList = analysisResultVo.getSentenceMarkVos().stream()
                            .sorted(Comparator.comparing(SentenceMarkVo::getCurrentSort))
                            .map(this::formatParagraphWithTime)
                            .filter(ObjectUtil::isNotEmpty)
                            .toList();
                    Long tradeId = getTradeIdFromItem(item);
                    TradeInfoVo trade = tradeProducer.info(tradeId);
                    RRException.isNotEmpty(trade, "行业获取失败");
                    List<CueWordsListVo> selectTrade = toSelectTrade(item.getSourceId(), tradeId, item.getType(), item.getSourceType());
                    RRException.isNotEmpty(selectTrade, "没有配置提示词，请联系管理员配置");
                    CueWordsListVo cueWord = selectTrade.stream().findFirst()
                            .orElseThrow(() -> new RRException("提示词配置缺失"));
                    List<String> contentSection = aiFeign.getVideoContentSection(contentList, cueWord.getProblem(), Map.of("trade", trade.getName()));
                    if (ObjectUtil.isEmpty(contentSection)) {
                        log.error("生成提示词失败,contentList.size={}，problem={}，userId={}, tenantId={}", contentList.size(), selectTrade.get(0).getProblem(), user.getId(), user.getActiveTenantId());
                        RRException.create("生成提示词失败");
                    }
                    // 组装数据
                    List<VideoContentVo> videoContentList = getVideoContentVos(item, contentSection, user);
                    // 保存到mongodb中
                    videoContentList = videoContentProducer.saveAll(videoContentList);
                    item.setVideoContentList(videoContentList);
                }
            }
        }
        return result.stream()
                .filter(item -> ObjectUtil.isNotEmpty(item.getVideoContentList()))
                .toList();
    }

    /**
     * 查询在mongodb中是否有未分析完成的提示词
     *
     * @param result
     * @param userId
     * @param tenantId
     */
    private void setVideoContentList(List<ToGeneratedVo> result, Long userId, Long tenantId) {
        for (ToGeneratedVo item : result) {
            List<VideoContentVo> videoContentList = videoContentProducer.listBySourceIdAndStatus(item.getSourceId(), item.getType(), userId, tenantId, WordsEnum.contentGenerateStatus.NO_GENERATED.getCode());
            if (ObjectUtil.isNotEmpty(videoContentList)) {
                item.setVideoContentList(videoContentList);
            }
        }
    }

    private Map<String, AnalysisResultVo> analysisMapBySource(List<ToGeneratedVo> result, BiFunction<Integer, String, AnalysisResultVo> fun) {

        Map<String, AnalysisResultVo> analysisMap = new HashMap<>();
        for (ToGeneratedVo item : result) {
            if (!analysisMap.containsKey(item.getSourceId()) && ObjectUtil.isNull(item.getVideoContentList())) {
                AnalysisResultVo apply = fun.apply(item.getSourceType(), item.getSourceId());
                if (apply != null) {
                    analysisMap.put(item.getSourceId(), apply);
                }
            }
        }
        return analysisMap;
    }


    /**
     * 提取 tradeId 获取逻辑
     *
     * @param item
     * @return
     */
    private Long getTradeIdFromItem(ToGeneratedVo item) {
        if (item.getSourceType() == 0 && item.getAnchorVideo() != null) {
            return item.getAnchorVideo().getTradeId();
        } else if (item.getSourceType() == 1 && item.getUploadFile() != null) {
            return item.getUploadFile().getTradeId();
        }
        return CommonEnum.INIT_TRADE_ID.getCodeLong();
    }

    private static @NotNull ArrayList<VideoContentVo> getVideoContentVos(ToGeneratedVo item, List<String> contentSection, UserCacheVo user) {
        ArrayList<VideoContentVo> videoContentList = new ArrayList<>();
        for (int i = 0; i < contentSection.size(); i++) {
            String v = contentSection.get(i);
            VideoContentVo res = new VideoContentVo();
            res.setSourceId(item.getSourceId());
            res.setSourceType(item.getSourceType());
            res.setCueWord(v);
            res.setGenerateStatus(0);
            res.setParagraph(i);
            res.setType(item.getType());
            res.setUserId(user.getId());
            res.setTenantId(user.getActiveTenantId());
            res.setIsDeleted(0);
            videoContentList.add(res);
        }
        return videoContentList;
    }

    private String formatParagraphWithTime(SentenceMarkVo vo) {
        if (vo == null || ObjectUtil.isEmpty(vo.getContent())) {
            return null;
        }
        int paragraphNum = ObjectUtil.defaultIfNull(vo.getCurrentSort(), 1);
        StringBuilder sb = new StringBuilder();
        String timeRange = getTimeRange(vo);
        if (timeRange != null) {
            sb.append("段落").append(paragraphNum).append("开始时间：").append(timeRange).append("\n");
        }
        sb.append("段落").append(paragraphNum).append("内容：").append(vo.getContent());
        return sb.toString();
    }

    private String getTimeRange(SentenceMarkVo vo) {
        if (ObjectUtil.isEmpty(vo.getItems())) {
            return null;
        }
        List<WordListItemVo> items = vo.getItems();
        Long startMs = ObjectUtil.defaultIfNull(items.get(0).getStartTime(), 0L);
        return formatMsToDisplayTime(startMs);
    }

    public static String formatMsToDisplayTime(long ms) {
        if (ms < 0) {
            ms = 0;
        }
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("00:%02d:%02d", minutes, seconds);
    }

    /**
     * 从 OnlineAnalysisItemVo 的 dataJson 中解析段落内容并格式化为带时间信息的字符串。
     *
     * <p>用于定时器生成流程（AnchorVideoLogicImpl），dataJson 是 {@link SentenceMarkVo} 的 JSON 序列化。
     *
     * @param dataJson   OnlineAnalysisItemVo 的 dataJson（SentenceMarkVo JSON）
     * @param paragraph  段落号（从 1 开始）
     * @return 格式化的段落字符串，content 为空时返回 null
     */
    public static String formatParagraphFromDataJson(String dataJson, Integer paragraph) {
        if (ObjectUtil.isEmpty(dataJson)) {
            return null;
        }
        cn.hutool.json.JSONObject obj;
        try {
            obj = JSONUtil.parseObj(dataJson);
        } catch (Exception e) {
            return null;
        }
        String content = obj.getStr("content");
        if (ObjectUtil.isEmpty(content)) {
            return null;
        }
        int paragraphNum = paragraph != null ? paragraph : 1;
        StringBuilder sb = new StringBuilder();
        cn.hutool.json.JSONArray items = obj.getJSONArray("items");
        if (items != null && !items.isEmpty()) {
            long startMs = items.getJSONObject(0).getLong("startTime", 0L);
            sb.append("段落").append(paragraphNum).append("开始时间：").append(formatMsToDisplayTime(startMs)).append("\n");
        }
        sb.append("段落").append(paragraphNum).append("内容：").append(content);
        return sb.toString();
    }

    /**
     * 生成视频内容的提示词
     *
     * @param bo  生成内容的请求参数
     * @param fun 用于分析内容的函数
     * @return 生成的内容
     */
    @Transactional
    public ToGeneratedVo generateOntQAContent(GenerateVideoContentBo bo, BiFunction<Integer, String, AnalysisResultVo> fun) {
        RRException.isNotEmpty(bo.getSourceId(), "来源id不能为空");
        RRException.isNotEmpty(bo.getSourceType(), "来源类型不能为空");
        RRException.isNotEmpty(bo.getType(), "内容类型不能为空");
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        // 修改状态
        boolean flag = updateContentStatus(bo);
        if (!flag) {
            return null;
        }

        List<ToGeneratedVo> result = new ArrayList<>();
        if (bo.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            result = getVideoByToGenerated(Collections.singletonList(bo.getSourceId()), user.getId(), user.getActiveTenantId());
        } else if (bo.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            result = getFileByToGenerated(Collections.singletonList(bo.getSourceId()), user.getId(), user.getActiveTenantId());
        }

        if (ObjectUtil.isNotEmpty(result)) {

            List<ToGeneratedVo> orGenerateQAContent = getOrGenerateQAContent(result, fun);
            if (ObjectUtil.isNotEmpty(orGenerateQAContent)) {
                ToGeneratedVo toGeneratedVo = orGenerateQAContent.stream()
                        .filter(item -> ObjectUtil.equals(bo.getType(), item.getType()))
                        .findFirst().orElse(null);
                if (toGeneratedVo != null) {
                    toGeneratedVo.setAiModel(getAiModel());
                    toGeneratedVo.setStatus(0);
                    return toGeneratedVo;
                }
            }
        }

        return null;
    }

    /**
     * 修改状态为生成中
     * <p>只有在待生成的时候才会修改状态</p>
     *
     * @param bo
     * @return
     */
    private boolean updateContentStatus(GenerateVideoContentBo bo) {
        if (bo.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(bo.getSourceId());
            // 判断视频是否分析成功
            if (video == null) {
                throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "视频不存在");
            }

            if (!ObjectUtil.equals(AnchorVideoEnums.analysisStatus.ANALYSIS_COMPLETE.getCode(), video.getAnalysisStatus())) {
                throw new BusinessException(StatusCode.OPERATION_EX.getCode(), "视频未分析完成，请稍后再试");
            }

            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(video.getVideoId(), video.getTradeId());
            if (videoAnalysisRecordInfoVo == null || ObjectUtil.isEmpty(videoAnalysisRecordInfoVo.getStoreFileOssKey())) {
                throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "段落数据不存在，不能发起");
            }

            return updateVideoContentStatus(bo);
        } else if (bo.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            // 文件的
            return updateFileContentStatus(bo);
        } else {
            RRException.create("来源类型不支持");
        }
        return false;
    }

    /**
     * 修改视频的自然、优化原文状态
     * <p>只有在待生成的时候才会修改状态</p>
     *
     * @param bo
     * @return
     */
    private boolean updateVideoContentStatus(GenerateVideoContentBo bo) {
        Integer natureContentStatus = null;
        Integer optimizeContentStatus = null;
        Integer natureSourceType = null;
        Integer optimizeSourceType = null;
        if (bo.getType() == WordsEnum.contentType.NATURE.getCode()) {
            natureContentStatus = WordsEnum.contentStatus.GENERATING.getCode();
            natureSourceType = WordsEnum.contentSourceType.CLIENT.getCode();
        } else if (bo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
            optimizeContentStatus = WordsEnum.contentStatus.GENERATING.getCode();
            optimizeSourceType = WordsEnum.contentSourceType.CLIENT.getCode();
        } else {
            RRException.create("内容类型不支持");
        }
        // 视频的
        AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
        detailBo.setVideoId(bo.getSourceId());
        // 自然原文
        detailBo.setNatureContentStatus(natureContentStatus);
        detailBo.setNatureSourceType(natureSourceType);
        // 优化原文
        detailBo.setOptimizeContentStatus(optimizeContentStatus);
        detailBo.setOptimizeSourceType(optimizeSourceType);

        AnchorVideoDetailInfoVo andSave = anchorVideoDetailProducer.getAndSave(detailBo);
        if (andSave == null) {
            return false;
        }
        if (bo.getType() == WordsEnum.contentType.NATURE.getCode()) {
            if (andSave.getNatureContentStatus() != WordsEnum.contentStatus.GENERATING.getCode()) {
                AnchorVideoDetailBo videoDetailBo = new AnchorVideoDetailBo();
                videoDetailBo.setVideoId(bo.getSourceId());
                videoDetailBo.setNatureContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
                videoDetailBo.setNatureSourceType(WordsEnum.contentSourceType.CLIENT.getCode());
                return anchorVideoDetailProducer.updateContentStatus(videoDetailBo);
            }
        } else if (bo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()
                && (andSave.getOptimizeContentStatus() != WordsEnum.contentStatus.GENERATING.getCode())) {
            AnchorVideoDetailBo videoDetailBo = new AnchorVideoDetailBo();
            videoDetailBo.setVideoId(bo.getSourceId());
            videoDetailBo.setOptimizeContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
            videoDetailBo.setOptimizeSourceType(WordsEnum.contentSourceType.CLIENT.getCode());
            return anchorVideoDetailProducer.updateContentStatus(videoDetailBo);
        }
        return true;
    }

    /**
     * 修改文件的自然、优化原文状态
     *
     * @param bo
     * @return
     */
    private boolean updateFileContentStatus(GenerateVideoContentBo bo) {
        Integer natureContentStatus = null;
        Integer optimizeContentStatus = null;
        if (bo.getType() == WordsEnum.contentType.NATURE.getCode()) {
            natureContentStatus = WordsEnum.contentStatus.GENERATING.getCode();
        } else if (bo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
            optimizeContentStatus = WordsEnum.contentStatus.GENERATING.getCode();
        } else {
            RRException.create("内容类型不支持");
        }
        // 视频的
        UploadFileDetailBo detailBo = new UploadFileDetailBo();
        detailBo.setFileId(bo.getSourceId());
        detailBo.setNatureContentStatus(natureContentStatus);
        detailBo.setOptimizeContentStatus(optimizeContentStatus);
        UploadFileDetailInfoVo andSave = uploadFileDetailProducer.getAndSave(detailBo);
        if (andSave == null) {
            return false;
        }
        if (bo.getType() == WordsEnum.contentType.NATURE.getCode()) {
            if (andSave.getNatureContentStatus() == WordsEnum.contentStatus.PENDING.getCode()) {
                UploadFileDetailBo videoDetailBo = new UploadFileDetailBo();
                videoDetailBo.setFileId(bo.getSourceId());
                videoDetailBo.setNatureContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
                return uploadFileDetailProducer.updateContentStatus(videoDetailBo);
            }
        } else if (bo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
            if (andSave.getOptimizeContentStatus() == WordsEnum.contentStatus.PENDING.getCode()) {
                UploadFileDetailBo videoDetailBo = new UploadFileDetailBo();
                videoDetailBo.setFileId(bo.getSourceId());
                videoDetailBo.setOptimizeContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
                return uploadFileDetailProducer.updateContentStatus(videoDetailBo);
            }
        }
        return true;
    }

    /**
     * 保存视频内容
     *
     * @param contentVo 视频内容
     */
    public void saveVideoContent(VideoContentVo contentVo) {
        contentVo.setContent(videoContentProducer.formatContent(contentVo.getContent()));
        videoContentProducer.updateById(contentVo);
    }


    /**
     * 检查视频内容是否可以生成
     *
     * @param generatedVo 待生成的内容
     */
    @Transactional
    public void checkVideoContent(ToGeneratedVo generatedVo) {
        RRException.isNotEmpty(generatedVo.getSourceId(), "来源id不能为空");
        RRException.isNotEmpty(generatedVo.getSourceType(), "来源类型不能为空");
        RRException.isNotEmpty(generatedVo.getType(), "内容类型不能为空");
        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "获取用户失败");

        // 判断状态
        int natureContentStatus = WordsEnum.contentStatus.PENDING.getCode();
        int optimizeContentStatus = WordsEnum.contentStatus.PENDING.getCode();
        if (generatedVo.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            AnchorVideoDetailVo video = anchorVideoDetailProducer.getByVideo(user.getId(), user.getActiveTenantId(), generatedVo.getSourceId(), generatedVo.getType());
            if (video == null) {
                return;
            }
            natureContentStatus = video.getNatureContentStatus();
            optimizeContentStatus = video.getOptimizeContentStatus();
        } else if (generatedVo.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            UploadFileDetailVo video = uploadFileDetailProducer.getByFile(generatedVo.getSourceId());
            if (video == null) {
                return;
            }
            natureContentStatus = video.getNatureContentStatus();
            optimizeContentStatus = video.getOptimizeContentStatus();
        }
        // 类型是自然原文 && 原文生成状态不是生成中的
        if (generatedVo.getType() == WordsEnum.contentType.NATURE.getCode() && natureContentStatus != WordsEnum.contentStatus.GENERATING.getCode()) {
            return;
        }
        // 自然原文 && 原文生成状态不是生成中的
        if (generatedVo.getType() == WordsEnum.contentType.OPTIMIZE.getCode() && optimizeContentStatus != WordsEnum.contentStatus.GENERATING.getCode()) {
            return;
        }

        // 判断mongodb中是否全部分析完成
        if (videoContentProducer.checkContentAllComplete(generatedVo.getSourceId(), generatedVo.getType(), user.getId(), user.getActiveTenantId())) {
            // 更新状态
            ToGeneratedVo generatedVo1 = new ToGeneratedVo();
            generatedVo1.setSourceId(generatedVo.getSourceId());
            generatedVo1.setSourceType(generatedVo.getSourceType());
            generatedVo1.setType(generatedVo.getType());
            generatedVo1.setStatus(WordsEnum.contentStatus.SUCCESS.getCode());
            boolean flag = videoContentUpdateStatus(generatedVo1);
            if (!flag) {
                log.error("接口completeVideoContentGenerate中的videoContentUpdateStatus更新失败，generatedVo1={}", generatedVo1);
                throw new BusinessException(StatusCode.SQL_EX.getCode(), "更新状态失败");
            }
        }
    }

    /**
     * 修改自然、优化原文的状态
     *
     * @param generatedVo
     */
    public boolean videoContentUpdateStatus(ToGeneratedVo generatedVo) {
        // 更新状态
        if (generatedVo.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
            detailBo.setVideoId(generatedVo.getSourceId());
            if (generatedVo.getType() == WordsEnum.contentType.NATURE.getCode()) {
                detailBo.setNatureContentStatus(generatedVo.getStatus());
            } else if (generatedVo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
                detailBo.setOptimizeContentStatus(generatedVo.getStatus());
            }
            return anchorVideoDetailProducer.updateContentStatus(detailBo);
        } else if (generatedVo.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            UploadFileDetailBo detailBo = new UploadFileDetailBo();
            detailBo.setFileId(generatedVo.getSourceId());
            if (generatedVo.getType() == WordsEnum.contentType.NATURE.getCode()) {
                detailBo.setNatureContentStatus(generatedVo.getStatus());
            } else if (generatedVo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
                detailBo.setOptimizeContentStatus(generatedVo.getStatus());
            }
            return uploadFileDetailProducer.updateContentStatus(detailBo);
        }
        return true;
    }

    /**
     * 获取视频已分析完成的需要生成的自然/优化原文的数据
     * @param limit
     * @return
     */
    public List<AnchorVideoDetailVo> selectVideoDetail(Integer limit) {
        return anchorVideoDetailProducer.selectByQuery(limit);
    }

    /**
     * 获取视频详情
     *
     * @param videoId 视频id
     * @return 视频详情
     */
    public AnchorVideoDetailVo infoByVideoId(String videoId) {
        AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
        bo.setVideoId(videoId);
        return anchorVideoDetailProducer.getAndSave(bo);
    }

    /**
     * 获取重要弹幕状态
     *
     * @param videoId 视频
     * @return 值
     */
    public ImportantBarrageStatusVo getImportantBarrageStatus(String videoId) {
        AnchorVideoInfoVo video = anchorVideoProducer.getParentVideoByVideoId(videoId);
        if (video == null) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "视频未知");
        }
        videoId = video.getVideoId();
        ImportantBarrageStatusVo result = new ImportantBarrageStatusVo();
        AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
        bo.setVideoId(videoId);
        AnchorVideoDetailInfoVo videoDetail = anchorVideoDetailProducer.getAndSave(bo);
        if (videoDetail == null) {
            result.setImportantBarrageStatus(0);
            return result;
        }
        // 判断在生成中是否已经超过15分钟
        if (ObjectUtil.equals(videoDetail.getImportantBarrageStatus(), ImportantBarrageStatusEnum.IN_PROGRESS.getCode()) &&
                DateUtil.between(videoDetail.getImportantBarrageTime(), new Date(), DateUnit.MINUTE) >= 15) {
            videoDetail.setImportantBarrageStatus(ImportantBarrageStatusEnum.ACQUISITION_FAILED.getCode());
            videoDetail.setImportantBarrageError("获取中的状态超过15分钟，自动失败");
            AnchorVideoDetailBo update = new AnchorVideoDetailBo();
            update.setId(videoDetail.getId());
            update.setImportantBarrageStatus(videoDetail.getImportantBarrageStatus());
            update.setImportantBarrageError(videoDetail.getImportantBarrageError());
            anchorVideoDetailProducer.update(update);

        }

        result.setImportantBarrageStatus(videoDetail.getImportantBarrageStatus());
        result.setErrorReason(videoDetail.getImportantBarrageError());
        return result;
    }

    /**
     * 开始获取重要弹幕
     *
     * @param videoId 视频
     * @return 是否需要获取重要弹幕
     */
    public Boolean startImportantBarrage(String videoId) {

        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "视频未知");
        }
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        if (!ObjectUtil.equals(user.getActiveTenantId(), video.getTenantId())) {
            throw new BusinessException(StatusCode.DATA_MISMATCH_USER.getCode(), "当前登录的用户无权限操作");
        }

        AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
        bo.setVideoId(videoId);
        AnchorVideoDetailInfoVo videoDetails = anchorVideoDetailProducer.getAndSave(bo);
        if (videoDetails == null) {
            return false;
        }

        // 如果状态为完成、获取中就直接返回true
        if (ObjectUtil.equals(videoDetails.getImportantBarrageStatus(), ImportantBarrageStatusEnum.SUCCESS.getCode())
                || ObjectUtil.equals(videoDetails.getImportantBarrageStatus(), ImportantBarrageStatusEnum.IN_PROGRESS.getCode())) {
            return false;
        }

        // 修改状态为获取中
        AnchorVideoDetailBo update = new AnchorVideoDetailBo();
        update.setId(videoDetails.getId());
        update.setVideoId(videoId);
        update.setImportantBarrageStatus(ImportantBarrageStatusEnum.IN_PROGRESS.getCode());
        update.setImportantBarrageTime(new Date());
        update.setImportantBarrageError("");
        anchorVideoDetailProducer.update(update);
        return true;
    }

    public void initUpdateSuggestTrade(String videoId) {
        anchorVideoDetailProducer.initUpdateSuggestTrade(videoId);
    }
}

