package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.bo.SensitiveWordsBatchBo;
import com.jiuyu.replay.words.bo.SensitiveWordsBo;
import com.jiuyu.replay.words.bo.SensitiveWordsListBo;
import com.jiuyu.replay.words.vo.AiAnalysisRecordVo;
import com.jiuyu.replay.words.vo.SensitiveWordsInfoVo;
import com.jiuyu.replay.words.vo.SensitiveWordsListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsVo;

import java.util.List;


/**
 * 敏感词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
public interface SensitiveWordsProducer {


    /**
     * 敏感词列表
     * @param sensitiveWordsListBo 敏感词列表查询参数
     * @return
     */
    PageUtils<SensitiveWordsListVo> queryPage(SensitiveWordsListBo sensitiveWordsListBo);

    /**
    * 敏感词信息
    * @param id 敏感词id
    * @return
    */
    SensitiveWordsInfoVo info(Long id);

    /**
     * 新增敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @param existList 已存在的词语名称列表
     * @return
     */
     Long save(SensitiveWordsBo sensitiveWordsBo, List<String> existList);

    /**
     * 修改敏感词
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    void update(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 删除敏感词
     * @param id 敏感词id
     * @return
     */
    void deleteById(Long id);


    /**
     * 查是否已经存在，存在则返回对象
     * @param sensitiveWordsBo 敏感词对象
     * @return
     */
    SensitiveWordsVo exist(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 根据平台id和行业id获取系统和用户定义的所有敏感词
     * @param userId 用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId 行业id 1表示全行业
     * @param wordsType 词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)
     * @return
     */
    List<SensitiveWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType);

    /**
     * 查出重复的敏感词
     * @param sensitiveWordsBatchBo
     * @return
     */
    List<SensitiveWordsVo> listRepeat(SensitiveWordsBatchBo sensitiveWordsBatchBo);

    /**
     * 批量新增敏感词
     * @param sensitiveWordsBatchBo
     * @return
     */
    void saveBatch(SensitiveWordsBatchBo sensitiveWordsBatchBo);

    /**
     * 检查是否已经存在相似词，返回已存在的相似词列表
     * @param sensitiveWordsBo
     * @return
     */
    List<String> existSimilarList(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 保存词语
     * @param sensitiveWordsBo
     * @return
     */
    SensitiveWordsVo saveWord(SensitiveWordsBo sensitiveWordsBo);

    /**
     * 更新相似词
     * @param sensitiveWordsBo
     */
    String updateSimilar(SensitiveWordsBo sensitiveWordsBo, List<DictDataListVo> platformList);

    /**
     * 新增相似词
     * @param sensitiveWordsBo 页面传参
     * @param sensitiveWordsVo 敏感词信息
     * @param existList 已存在的相似词列表
     */
    void addSimilar(SensitiveWordsBo sensitiveWordsBo, SensitiveWordsVo sensitiveWordsVo, List<String> existList);

    /**
     * 根据词语id获取相似词列表
     * @param id 词语id
     * @return
     */
    List<SensitiveWordsInfoVo> listByParentId(Long id);

    /**
     * 将分析数据存储到本地
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param jsonStr 分析数据
     * @param recordId 记录id
     */
    String saveAnalysisDataToFile(String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId);

    /**
     * 将分析数据存储到本地2_0
     * @param version 版本号
     * @param uuid 视频/文件唯一标识
     * @param type 类型 0：视频 1：文件
     * @param tradeId 行业id
     * @param versionNum 版本号
     * @param jsonStr 分析数据
     * @param recordId 记录id
     */
    String saveAnalysisDataToFile2_0(String version, String uuid, int type, Long tradeId, int versionNum, String jsonStr, Long recordId);

    /**
     * 根据关键词分类id获取关键词列表
     * @param cruxTypeId 关键词分类id
     * @return
     */
    List<SensitiveWordsInfoVo> listByCruxTypeId(Long cruxTypeId);

    /**
     * 将AI分析的内容保存进文件
     * @param storePath 文件路径
     * @param jsonString 文件内容
     * @return
     */
    String AiContentFileName(String storePath,String jsonString);

    /**
     * 根据AI分析出来的关键词内容，去查询关键词是否存在于词库中
     * @param aiContentList Ai分析出来的内容
     * @return
     */
    List<String> aiAnswerAndSensitiveList(List<String> aiContentList);

    AiAnalysisRecordVo aiAnswerAndSensitiveTotal(List<String> aiContentList, Long tradeId);
}

