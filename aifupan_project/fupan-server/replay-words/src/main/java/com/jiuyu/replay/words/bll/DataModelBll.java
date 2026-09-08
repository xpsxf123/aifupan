package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.words.bo.DataModelBo;
import com.jiuyu.replay.words.bo.DataModelListBo;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;


/**
 * 罗盘数据模型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Component
public class DataModelBll {

    @Resource
    private DataModelProducer dataModelProducer;
    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;


    /**
     * 罗盘数据模型列表
     * @param dataModelListBo 罗盘数据模型列表查询参数
     * @return
     */
    public R<PageUtils<DataModelListVo>> queryPage(DataModelListBo dataModelListBo) {

        return R.ok("获取成功", dataModelProducer.queryPage(dataModelListBo));
    }

    /**
    * 罗盘数据模型信息
    * @param id 罗盘数据模型id
    * @return
    */
    public R<DataModelInfoVo> info(Long id) {

        DataModelInfoVo dataModelInfoVo = dataModelProducer.info(id);
        return R.ok("获取成功", dataModelInfoVo);
    }

    /**
     * 新增罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    public R<String> save(DataModelBo dataModelBo) {

        DataModelInfoVo dataModelInfoVo = dataModelProducer.save(dataModelBo);
        return R.ok("添加成功");
    }

    /**
     * 修改罗盘数据模型
     * @param dataModelBo 罗盘数据模型对象
     * @return
     */
    public R<String> update(DataModelBo dataModelBo) {

        dataModelProducer.update(dataModelBo);
        return R.ok("修改成功");
    }

    /**
     * 删除罗盘数据模型
     * @param id 罗盘数据模型id
     * @return
     */
    public R<String> delete(Long id) {

        dataModelProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public R<String> saveDataModel(DataModelSaveBo dataModelBo) {
        return dataModelProducer.saveDataModel(dataModelBo);
    }

    public R<PageUtils<DataModelSaveBo>> modelCruxTypeList(DataModelListBo dataModelBo) {
        return dataModelProducer.modelCruxTypeList(dataModelBo);
    }

    public R<String> updateDataModel(DataModelSaveBo dataModelBo) {
        return dataModelProducer.updateDataModel(dataModelBo);
    }

    public R<String> deleteDataModel(Long id) {

        List<TradeInfoVo> tradeInfoVos = this.tradeProducer.listByDefaultGeneralModelId(id);

        if(tradeInfoVos != null && tradeInfoVos.size() > 0) {
            List<String> tradeNameList = tradeInfoVos.stream().map(TradeVo::getName).toList();
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前模型有行业在使用，删除失败，分别是：" + String.join("，", tradeNameList));
        }

        return dataModelProducer.deleteDataModel(id);
    }

    public R<DataModelSaveBo> infoDataModel(Long id) {
        return dataModelProducer.infoDataModel(id);
    }


    /**
     * 根据视频id或对比id获取模型列表
     * @param uuid 视频id或对比id
     * @param type 类型 0：视频 1：对比 2：文件
     * @return
     */
    public R<List<DataModelInfoVo>> listTradeModel(String uuid, Integer type) {

        // 行业id
        List<Long> tradeIds = new LinkedList<>();

        if(type == 0) {
            // 视频
            AnchorVideoInfoVo anchorVideoInfoVo = this.anchorVideoProducer.getByVideoId(uuid);
            if(anchorVideoInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
            }
            tradeIds.add(anchorVideoInfoVo.getTradeId());
        }else if(type == 1) {
            // 对比
            SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(uuid);
            if(syncContrastInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息不存在");
            }

            if(!StringUtils.isEmpty(syncContrastInfoVo.getVideoOneId()) && !StringUtils.isEmpty(syncContrastInfoVo.getVideoTwoId())) {
                AnchorVideoInfoVo anchorVideoInfoVo1 = this.anchorVideoProducer.getByVideoId(syncContrastInfoVo.getVideoOneId());
                if(anchorVideoInfoVo1 == null) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息的视频信息1不存在");
                }
                tradeIds.add(anchorVideoInfoVo1.getTradeId());

                AnchorVideoInfoVo anchorVideoInfoVo2 = this.anchorVideoProducer.getByVideoId(syncContrastInfoVo.getVideoTwoId());
                if(anchorVideoInfoVo2 == null) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息的视频信息2不存在");
                }
                tradeIds.add(anchorVideoInfoVo2.getTradeId());

            }else {
                UploadFileInfoVo uploadFileInfoVo1 = this.uploadFileProducer.getByFileId(syncContrastInfoVo.getFileOneId());
                if(uploadFileInfoVo1 == null) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息的文件信息1不存在");
                }
                tradeIds.add(uploadFileInfoVo1.getTradeId());

                UploadFileInfoVo uploadFileInfoVo2 = this.uploadFileProducer.getByFileId(syncContrastInfoVo.getFileTwoId());
                if(uploadFileInfoVo2 == null) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比信息的文件信息2不存在");
                }
                tradeIds.add(uploadFileInfoVo2.getTradeId());
            }

        }else if(type == 2) {
            // 文件
            UploadFileInfoVo uploadFileInfoVo = this.uploadFileProducer.getByFileId(uuid);
            if(uploadFileInfoVo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件信息不存在");
            }
            tradeIds.add(uploadFileInfoVo.getTradeId());
        }

        List<Long> tradeModelIds = new LinkedList<>();

        // TODO 遍历查库，待优化
        for (Long tradeId : tradeIds) {
            TradeInfoVo tradeInfoVo = this.tradeProducer.info(tradeId);
            if(!tradeInfoVo.getTradeModelId().equals(0L)) {
                tradeModelIds.add(tradeInfoVo.getTradeModelId());
            }else {
                while (!tradeInfoVo.getParentId().equals(0L)) {
                    tradeInfoVo = this.tradeProducer.info(tradeInfoVo.getParentId());
                    if(!tradeInfoVo.getTradeModelId().equals(0L)) {
                        tradeModelIds.add(tradeInfoVo.getTradeModelId());
                        break;
                    }
                }
            }
        }

        // 获取模型信息
        List<DataModelInfoVo> dataModelVos = dataModelProducer.listByIdsAndGeneral(tradeModelIds);
        if(dataModelVos != null && dataModelVos.size() > 0) {

            if(tradeModelIds.size() > 0) {
                // 设置行业默认模型
                for (DataModelInfoVo dataModelVo : dataModelVos) {
                    for (int i = 0; i < tradeModelIds.size(); i++) {
                        Long tradeModelId = tradeModelIds.get(i);
                        if(dataModelVo.getId().equals(tradeModelId)) {
                            dataModelVo.setAlias("行业模型" + (i + 1));
                            if(i == 0) {
                                dataModelVo.setDefaultShow(1);
                            }
                        }
                    }
                }

            }else {
                if(tradeIds.isEmpty()) {
                    return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "行业信息不存在");
                }
                // 没有行业模型，判断第一个行业是否有默认通用模型
                TradeInfoVo tradeInfoVo = this.tradeProducer.info(tradeIds.get(0));
                if(tradeInfoVo != null) {
                    boolean existDefaultGeneralModel = false;

                    for (DataModelInfoVo dataModelVo : dataModelVos) {
                        if(dataModelVo.getId().equals(tradeInfoVo.getDefaultGeneralModelId())) {
                            dataModelVo.setDefaultShow(1);
                            existDefaultGeneralModel = true;
                            break;
                        }
                    }

                    if(!existDefaultGeneralModel) {
                        dataModelVos.get(0).setDefaultShow(1);
                    }
                }
            }

            return R.ok(dataModelVos);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "模型不存在");

    }
}

