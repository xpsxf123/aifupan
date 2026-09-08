package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.producer.DictDataProducer;
import com.jiuyu.replay.words.bo.LexiconWordBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientBo;
import com.jiuyu.replay.words.bo.SensitiveWordsClientListBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.CruxTypeProducer;
import com.jiuyu.replay.words.producer.LexiconWordProducer;
import com.jiuyu.replay.words.producer.SensitiveWordsClientProducer;
import com.jiuyu.replay.words.producer.WordRuleProducer;
import com.jiuyu.replay.words.vo.SensitiveWordsClientInfoVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientListVo;
import com.jiuyu.replay.words.vo.SensitiveWordsClientVo;
import com.jiuyu.replay.words.vo.WordRuleInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;


/**
 * 客户端自定义词语
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Service
public class SensitiveWordsClientBll {

    @Resource
    private SensitiveWordsClientProducer sensitiveWordsClientProducer;
    @Resource
    private DictDataProducer dictDataProducer;
    @Resource
    private WordRuleProducer wordRuleProducer;
    @Resource
    private LexiconWordProducer lexiconWordProducer;
    @Resource
    private CruxTypeProducer cruxTypeProducer;


    /**
     * 客户端自定义词语列表
     *
     * @param sensitiveWordsListBo 客户端自定义词语列表查询参数
     * @return
     */
    public R<PageUtils<SensitiveWordsClientListVo>> queryPage(SensitiveWordsClientListBo sensitiveWordsListBo) {

        if(!StringUtils.isEmpty(sensitiveWordsListBo.getCruxTypeId())) {
            List<Long> cruxTypeIds = this.cruxTypeProducer.getChildrenIdAndSelfIdList(sensitiveWordsListBo.getCruxTypeId());
            if(cruxTypeIds != null && cruxTypeIds.size() > 0) {
                sensitiveWordsListBo.setCruxTypeIds(cruxTypeIds);
            }
        }

        return R.ok("获取成功", sensitiveWordsClientProducer.queryPage(sensitiveWordsListBo));
    }

    /**
     * 客户端自定义词语信息
     *
     * @param id 客户端自定义词语id
     * @return
     */
    public R<SensitiveWordsClientInfoVo> info(Long id) {

        SensitiveWordsClientInfoVo sensitiveWordsInfoVo = sensitiveWordsClientProducer.info(id);

        if (sensitiveWordsInfoVo != null) {
            // 获取词语规则
            List<WordRuleInfoVo> ruleList = this.wordRuleProducer.listByWordId(sensitiveWordsInfoVo.getId());
            sensitiveWordsInfoVo.setRuleList(ruleList);
            // 获取相似词
            List<SensitiveWordsClientInfoVo> similarList = this.sensitiveWordsClientProducer.listByParentId(sensitiveWordsInfoVo.getId());
            sensitiveWordsInfoVo.setSimilarWordList(similarList);
            if(sensitiveWordsInfoVo.getWordsType() == 1) {
                // 获取关键词分类数组
                List<Long> cruxTypeIdArr = cruxTypeProducer.getParentIdContainerSelfArr(sensitiveWordsInfoVo.getCruxTypeId());
                sensitiveWordsInfoVo.setCruxTypeIdArr(cruxTypeIdArr);
            }
        }

        return R.ok("获取成功", sensitiveWordsInfoVo);
    }

    /**
     * 新增客户端自定义词语
     *
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<List<String>> save(SensitiveWordsClientBo sensitiveWordsBo) {

        if (StringUtils.isEmpty(sensitiveWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        List<String> resultMsgList = new LinkedList<>();

        // 查平台列表
        List<DictDataListVo> platformList = this.dictDataProducer.listByTypeLogo("words_platform_type");

        for (Integer platformType : sensitiveWordsBo.getPlatformTypeList()) {

            sensitiveWordsBo.setPlatformType(platformType);

            // 检查是否已经存在词语
            SensitiveWordsClientVo sensitiveWords = this.sensitiveWordsClientProducer.exist(sensitiveWordsBo);
            if (sensitiveWords != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语已存在，添加失败");
            }

            // 保存词语
            sensitiveWords = this.sensitiveWordsClientProducer.saveWord(sensitiveWordsBo);

            // 保存词语规则
            if (sensitiveWordsBo.getRuleList() != null && sensitiveWordsBo.getRuleList().size() > 0) {
                this.wordRuleProducer.saveBatchByWordId(sensitiveWords.getId(), sensitiveWordsBo.getRuleList());
            }

            // 检查是否已经存在相似词，返回已存在的相似词列表
            List<String> existList = this.sensitiveWordsClientProducer.existSimilarList(sensitiveWordsBo);
            if (existList != null && existList.size() > 0) {
                StringBuilder resultMsg = new StringBuilder();
                // resultMsg.append(platformList.get(platformType).getLabel());
                if (platformList != null && platformList.size() > 0) {
                    resultMsg.append(platformList.stream().filter(item -> item.getValue().equals(platformType + "")).toList().get(0).getLabel());
                }
//                resultMsg.append(platformList.get(0).getLabel());
                resultMsg.append("因重复而被忽略的相似词：");
                for (String wordsName : existList) {
                    resultMsg.append("-").append(wordsName);
                }
                resultMsgList.add(resultMsg.toString());
            }

            // 保存相似词
            this.sensitiveWordsClientProducer.addSimilar(sensitiveWordsBo, sensitiveWords, existList);

            // 如果是客户端添加的，跟词库增加关联
            if(sensitiveWordsBo.getResourceType() == 1) {
                LexiconWordBo lexiconWordBo = new LexiconWordBo();
                lexiconWordBo.setLexiconId(sensitiveWordsBo.getLexiconId());
                lexiconWordBo.setWordId(sensitiveWords.getId());
                lexiconWordBo.setWordsType(sensitiveWordsBo.getWordsType());
                this.lexiconWordProducer.save(lexiconWordBo);
            }

        }

        return R.ok(resultMsgList);
    }

    /**
     * 修改客户端自定义词语
     *
     * @param sensitiveWordsBo 客户端自定义词语对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(SensitiveWordsClientBo sensitiveWordsBo) {

        if (StringUtils.isEmpty(sensitiveWordsBo.getName())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "词语不能为空");
        }

        // 获取原词语
        SensitiveWordsClientVo sensitiveWordsVo = this.sensitiveWordsClientProducer.info(sensitiveWordsBo.getId());
        if (sensitiveWordsVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }
        sensitiveWordsBo.setUserId(sensitiveWordsVo.getUserId());

        if (!sensitiveWordsBo.getName().equals(sensitiveWordsBo.getOldName())) {
            // 检查是否已经存在
            SensitiveWordsClientVo exist = this.sensitiveWordsClientProducer.exist(sensitiveWordsBo);
            if (exist != null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), (sensitiveWordsBo.getWordsType() == 0 ? "客户端自定义词语" : "关键词") + "已存在，修改失败");
            }
        }

        // 更新词语
        this.sensitiveWordsClientProducer.update(sensitiveWordsBo);
        // 更新规则
        this.wordRuleProducer.updateBatchByWordId(sensitiveWordsBo.getId(), sensitiveWordsBo.getRuleList());

        // 查平台列表
        List<DictDataListVo> platformList = this.dictDataProducer.listByTypeLogo("words_platform_type");

        // 更新相似词
        String resultMsg = sensitiveWordsClientProducer.updateSimilar(sensitiveWordsBo, platformList);


        return R.ok(resultMsg);
    }

    /**
     * 删除词语
     *
     * @param id 客户端自定义词语id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {

        SensitiveWordsClientInfoVo wordsInfoVo = this.sensitiveWordsClientProducer.info(id);
        if(wordsInfoVo != null && wordsInfoVo.getResourceType() == 1) {
            // 客户自定义的词语，删除词语跟词库的关联关系
            this.lexiconWordProducer.deleteByWordId(wordsInfoVo.getId());
        }

        // 删除词语
        sensitiveWordsClientProducer.deleteById(id);

        return R.ok("删除成功");
    }

    /**
     * 根据平台id和行业id获取系统和用户定义的所有客户端自定义词语
     *
     * @param userId       用户id
     * @param platformType 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     * @param tradeId      行业id 1表示全行业
     * @param wordsType    词语类型 0：客户端自定义词语 1：关键词 2：白名单(只有客户自定义有)
     * @return
     */
    public List<SensitiveWordsClientVo> listByUidAndPidAndTid(Long userId, Integer platformType, Long tradeId, Integer wordsType) {

        // 获取客户端自定义词语
        List<SensitiveWordsClientVo> sensitiveWordsVos = sensitiveWordsClientProducer.listByUidAndPidAndTid(userId, platformType, tradeId, wordsType);

        // 设置客户端自定义词语的规则列表
        if (sensitiveWordsVos != null && sensitiveWordsVos.size() > 0) {
            List<Long> wordIds = sensitiveWordsVos.stream().map(SensitiveWordsClientVo::getId).toList();
            List<WordRuleInfoVo> wordRuleList = this.wordRuleProducer.listByWordIds(wordIds);

            for (SensitiveWordsClientVo sensitiveWordsVo : sensitiveWordsVos) {
                List<WordRuleInfoVo> ruleList = new LinkedList<>();
                if (wordRuleList != null && wordRuleList.size() > 0) {
                    for (WordRuleInfoVo wordRuleInfoVo : wordRuleList) {
                        if (wordRuleInfoVo.getWordId().equals(sensitiveWordsVo.getId())) {
                            ruleList.add(wordRuleInfoVo);
                        }
                    }
                }
                sensitiveWordsVo.setRuleList(ruleList);
            }
        }

        return sensitiveWordsVos;
    }

}

