package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.words.vo.CruxWordsVo;

import java.util.List;


/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
public interface CruxWordsProducer {


    /**
     * 关键词列表
     * @param cruxWordsListBo 关键词列表查询参数
     * @return
     */
    PageUtils<CruxWordsListVo> queryPage(CruxWordsListBo cruxWordsListBo);

    /**
    * 关键词信息
    * @param id 关键词id
    * @return
    */
    CruxWordsInfoVo info(Long id);

    /**
     * 新增关键词
     * @param cruxWordsBo 关键词对象
     * @param existList 已存在的词语名称列表
     * @return
     */
     void save(CruxWordsBo cruxWordsBo, List<String> existList);

    /**
     * 修改关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    void update(CruxWordsBo cruxWordsBo);

    /**
     * 删除关键词
     * @param id 关键词id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据平台id和行业id获取系统和用户定义的所有关键词
     * @param userId 用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId 行业id 1表示全行业
     * @return
     */
    List<CruxWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId);

    /**
     * 根据关键词名称和行业查是否已经存在
     * @param cruxWordsBo 敏感词对象
     * @return
     */
    boolean exist(CruxWordsBo cruxWordsBo);

    /**
     * 批量新增关键词
     * @param cruxWordsBatchBo 关键词对象
     * @return
     */
    void saveBatch(CruxWordsBatchBo cruxWordsBatchBo);

    /**
     * 查出重复的关键词
     * @param cruxWordsBatchBo
     * @return
     */
    List<CruxWordsVo> listRepeat(CruxWordsBatchBo cruxWordsBatchBo);

    /**
     * 检查是否已经存在，返回已存在的词语列表
     * @param cruxWordsBo
     * @return
     */
    List<String> existList(CruxWordsBo cruxWordsBo);
}

