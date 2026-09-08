package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientListBo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientInfoVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientVo;

import java.util.List;


/**
 * 客户端自定义词语
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
public interface SensitiveWordsClientProducer {


    /**
     * 客户端自定义词语列表
     * @param sensitiveWordsListBo 客户端自定义词语列表查询参数
     * @return
     */
    PageUtils<SensitiveWordsClientListVo> queryPage(SensitiveWordsClientListBo sensitiveWordsListBo);

    /**
    * 客户端自定义词语信息
    * @param id 客户端自定义词语id
    * @return
    */
    SensitiveWordsClientInfoVo info(Long id);

    /**
     * 修改客户端自定义词语
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    void update(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 删除客户端自定义词语
     * @param id 客户端自定义词语id
     * @return
     */
    void deleteById(Long id);


    /**
     * 查是否已经存在，存在则返回对象
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    SensitiveWordsClientVo exist(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 根据平台id和行业id获取系统和用户定义的所有客户端自定义词语
     * @param userId 用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId 行业id 1表示全行业
     * @param wordsType 词语类型 0：客户端自定义词语 1：关键词 2：白名单(只有客户自定义有)
     * @return
     */
    List<SensitiveWordsClientVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType);


    /**
     * 检查是否已经存在相似词，返回已存在的相似词列表
     * @param sensitiveWordsBo
     * @return
     */
    List<String> existSimilarList(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 保存词语
     * @param sensitiveWordsBo
     * @return
     */
    SensitiveWordsClientVo saveWord(SensitiveWordsClientBo sensitiveWordsBo);

    /**
     * 更新相似词
     * @param sensitiveWordsBo
     */
    String updateSimilar(SensitiveWordsClientBo sensitiveWordsBo, List<DictDataListVo> platformList);

    /**
     * 新增相似词
     * @param sensitiveWordsBo 页面传参
     * @param sensitiveWordsVo 客户端自定义词语信息
     * @param existList 已存在的相似词列表
     */
    void addSimilar(SensitiveWordsClientBo sensitiveWordsBo, SensitiveWordsClientVo sensitiveWordsVo, List<String> existList);

    /**
     * 根据词语id获取相似词列表
     * @param id 词语id
     * @return
     */
    List<SensitiveWordsClientInfoVo> listByParentId(Long id);
}

