package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.CueWordsInfoVo;
import com.jiuyu.replay.generic.vo.words.CueWordsListVo;
import com.jiuyu.replay.generic.vo.words.CustomizeCueWordsResponse;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.words.producer.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Component
public class CueWordsBll {

    @Resource
    private CueWordsProducer cueWordsProducer;
    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Autowired
    private BasicSettingsProducer basicSettingsProducer;

    /**
     * 提示词列表
     * @param cueWordsListBo 提示词列表查询参数
     * @return
     */
    public R<PageUtils<CueWordsListVo>> queryPage(CueWordsListBo cueWordsListBo) {

        return R.ok("获取成功", cueWordsProducer.queryPage(cueWordsListBo));
    }

    /**
    * 提示词信息
    * @param id 提示词id
    * @return
    */
    public R<CueWordsInfoVo> info(Long id) {

        CueWordsInfoVo cueWordsInfoVo = cueWordsProducer.info(id);
        return R.ok("获取成功", cueWordsInfoVo);
    }

    /**
     * 新增提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    public R<String> save(CueWordsBo cueWordsBo) {

        CueWordsInfoVo cueWordsInfoVo = cueWordsProducer.save(cueWordsBo);
        return R.ok("添加成功");
    }

    /**
     * 修改提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    public R<String> update(CueWordsBo cueWordsBo) {

        cueWordsProducer.update(cueWordsBo);
        return R.ok("修改成功");
    }

    /**
     * 删除提示词
     * @param id 提示词id
     * @return
     */
    public R<String> delete(Long id) {

        cueWordsProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据行业id获取提示词-如果当前行业不存在提示词则查询父行业
     * @param tradeId
     * @return
     */
    public R<CueWordsInfoVo> importantCueWordsByTradeId(Long tradeId) {
        List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeId(tradeId, Constant.GeneralEnum.GENERAL_YES.getCode());

        return R.ok("获取成功", cueWordsProducer.importantCueWordsByTradeId(tradeVos));
    }

    /**
     * 获取cueWords
     * @param ids
     * @return
     */
    public List<CueWordsInfoVo> listByIds(List<Long> ids) {
        return cueWordsProducer.listByIds(ids);
    }

    /**
     * 分页获取提示词
     *
     * @param pageBo 查询参数
     * @return 提示词列表
     */
    public PageUtils<CueWordsListVo> pageCueWords(CueWordsPageBo pageBo) {
        return cueWordsProducer.pageCueWords(pageBo);
    }

    /**
     * 租户定制提示词列表
     *
     * @param newCueWordsPageBo cueWordsPageBo
     *
     * @return {@link CustomizeCueWordsResponse }
     */
    public CustomizeCueWordsResponse customizeCueWordsList(CueWordsPageBo newCueWordsPageBo) {
        return cueWordsProducer.customizeCueWordsList(newCueWordsPageBo);
    }
}

