package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.CruxWordsBatchBo;
import com.jiuyu.replay.words.bo.CruxWordsBo;
import com.jiuyu.replay.words.bo.CruxWordsListBo;
import com.jiuyu.replay.words.bo.WordsBatchItemBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.producer.CruxWordsProducer;
import com.jiuyu.replay.words.vo.CruxWordsInfoVo;
import com.jiuyu.replay.words.vo.CruxWordsListVo;
import com.jiuyu.replay.words.vo.CruxWordsVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;


/**
 * 关键词
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Component
public class CruxWordsBll {

    @Resource
    private CruxWordsProducer cruxWordsProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private WordsProperties wordsProperties;


    /**
     * 关键词列表
     * @param cruxWordsListBo 关键词列表查询参数
     * @return
     */
    public R<PageUtils<CruxWordsListVo>> queryPage(CruxWordsListBo cruxWordsListBo) {

        return R.ok("获取成功", cruxWordsProducer.queryPage(cruxWordsListBo));
    }

    /**
    * 关键词信息
    * @param id 关键词id
    * @return
    */
    public R<CruxWordsInfoVo> info(Long id) {

        CruxWordsInfoVo cruxWordsInfoVo = cruxWordsProducer.info(id);
        return R.ok("获取成功", cruxWordsInfoVo);
    }

    /**
     * 新增关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    public R<String> save(CruxWordsBo cruxWordsBo) {

        if(StringUtils.isEmpty(cruxWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        // 检查是否已经存在
        boolean exist =  this.cruxWordsProducer.exist(cruxWordsBo);
        if(exist) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词已存在");
        }

        StringBuilder resultMsg = new StringBuilder("添加成功");

        // 检查是否已经存在，返回已存在的词语列表
        List<String> existList =  this.cruxWordsProducer.existList(cruxWordsBo);
        if(existList != null && existList.size() > 0) {
            resultMsg.append("，以下词语重复被忽略");
            for (String wordsName : existList) {
                resultMsg.append("--").append(wordsName);
            }
        }

        // 保存
        cruxWordsProducer.save(cruxWordsBo, existList);

        return R.ok(resultMsg.toString());
    }

    /**
     * 修改关键词
     * @param cruxWordsBo 关键词对象
     * @return
     */
    public R<String> update(CruxWordsBo cruxWordsBo) {

        if(StringUtils.isEmpty(cruxWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        if(!cruxWordsBo.getName().equals(cruxWordsBo.getOldName())) {
            // 检查是否已经存在
            boolean exist =  this.cruxWordsProducer.exist(cruxWordsBo);
            if(exist) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "关键词已存在");
            }
        }

        cruxWordsProducer.update(cruxWordsBo);
        return R.ok("修改成功");
    }

    /**
     * 删除关键词
     * @param id 关键词id
     * @return
     */
    public R<String> delete(Long id) {

        cruxWordsProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 根据平台id和行业id获取系统和用户定义的所有关键词
     * @param userId 用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId 行业id 1表示全行业
     * @return
     */
    public List<CruxWordsVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId) {

        List<CruxWordsVo> cruxWordsVoList = cruxWordsProducer.listByUidAndPidAndTid(userId, platformType, tradeId);

        return cruxWordsVoList;

    }

    /**
     * 批量新增关键词
     * @param cruxWordsBatchBo 关键词对象
     * @return
     */
    public R<String> saveBatch(CruxWordsBatchBo cruxWordsBatchBo) {

        List<WordsBatchItemBo> wordsList = cruxWordsBatchBo.getWordsList();

        if(wordsList == null || wordsList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "内容不能为空");
        }

        // 查出重复的关键词
        List<CruxWordsVo> repeatList = this.cruxWordsProducer.listRepeat(cruxWordsBatchBo);
        StringBuilder tip = new StringBuilder();
        if(repeatList != null && repeatList.size() > 0) {
            tip.append("，已忽略重复的关键词");
            for (CruxWordsVo repeatWordsVo : repeatList) {
                tip.append("--");
                tip.append(repeatWordsVo.getName());
                wordsList.removeIf(cruxWordsBatchItemBo -> repeatWordsVo.getName().equals(cruxWordsBatchItemBo.getWords()));
            }
        }

        this.cruxWordsProducer.saveBatch(cruxWordsBatchBo);

        return R.ok("批量添加成功" + tip);
    }
}

