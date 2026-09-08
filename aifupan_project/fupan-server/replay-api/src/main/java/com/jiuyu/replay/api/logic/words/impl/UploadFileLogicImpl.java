package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.api.logic.words.UploadFileLogic;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserDetailsVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.bll.AiAnalysisRecordBll;
import com.jiuyu.replay.words.bll.SensitiveWordsBll;
import com.jiuyu.replay.words.bll.UploadFileAnalysisRecordBll;
import com.jiuyu.replay.words.bll.UploadFileBll;
import com.jiuyu.replay.words.bo.UploadFileAnalysisBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisListBo;
import com.jiuyu.replay.words.bo.UploadFileBo;
import com.jiuyu.replay.words.bo.file.ClientFileListBo;
import com.jiuyu.replay.words.bo.file.UpdateFileAnalysisStatusBo;
import com.jiuyu.replay.words.bo.file.UpdateFileTradeBo;
import com.jiuyu.replay.words.bo.file.UploadFileInfoBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;
import com.jiuyu.replay.words.vo.UploadFileVO;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Service
public class UploadFileLogicImpl implements UploadFileLogic {

    @Resource
    private UploadFileBll uploadFileBll;

    @Resource
    private UserBll userBll;
    @Resource
    private UploadFileAnalysisRecordBll uploadFileAnalysisRecordBll;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private AiAnalysisRecordBll aiAnalysisRecordBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private OrderBll orderBll;


    /**
     * 复盘上传文件
     * @param uploadFileBo
     * @return
     */
    @Override
    public R<PageUtils<UploadFileVO>> queryPage(UploadFileBo uploadFileBo) {
        boolean selectUserId = false;
        List<Long> userIdsByNickName = new ArrayList<>();
        List<Long> userIdsBySalesId = new ArrayList<>();
        //根据用户名称查询
        if(uploadFileBo.getUserName()!=null){
            selectUserId = true;
            UserListBo userListBo = new UserListBo();
            userListBo.setKeyword(uploadFileBo.getUserName());
            userIdsByNickName = userBll.selectByNameOrNick(userListBo);
            uploadFileBo.setUserIds(userIdsByNickName);
        }
        // 根据销售人员查询
        if (uploadFileBo.getUserSalesID() != null){
            selectUserId = true;
            userIdsBySalesId = userDetailsBll.getUserIdsBySalesId(uploadFileBo.getUserSalesID());
            if (userIdsBySalesId != null && userIdsBySalesId.size() > 0) {
                uploadFileBo.setUserIds(userIdsBySalesId);
            }
        }

        Collection<Long> finalUserIds = this.intersectionOrSingleSet(Arrays.asList(userIdsByNickName, userIdsBySalesId));
        if(selectUserId && finalUserIds.size() < 1) {
            uploadFileBo.setUserIds(new LinkedList<>());
            uploadFileBo.getUserIds().add(0L);
        }else if(finalUserIds.size() > 0) {
            uploadFileBo.setUserIds(new LinkedList<>());
            uploadFileBo.getUserIds().addAll(finalUserIds);
        }

        R<PageUtils<UploadFileVO>> pageUtilsR = uploadFileBll.queryPage(uploadFileBo);
        List<UploadFileVO> list = pageUtilsR.getData().getList();

        // 获取所有的AI分析关键词记录
        List<AiAnalysisRecordEntity> aiAnalysisRecordEntities =  aiAnalysisRecordBll.allList();
        Map<String, AiAnalysisRecordEntity> recordEntityMap = new HashMap<>();
        if(aiAnalysisRecordEntities != null && aiAnalysisRecordEntities.size() > 0) {
            recordEntityMap = aiAnalysisRecordEntities.stream().collect(Collectors.toMap(AiAnalysisRecordEntity::getUuid, a -> a,(existing, replacement) -> replacement));
        }

        if (list != null && !list.isEmpty()) {
            //填充用户名
            List<Long> userIds = list.stream().map(UploadFileVO::getUserId).distinct().toList();
            R<List<UserListVo>> listR = userBll.listByIds(userIds);
            if (listR.getData() != null && !listR.getData().isEmpty()) {
                Map<String, AiAnalysisRecordEntity> finalRecordEntityMap = recordEntityMap;

                // 获取所有用户详情信息
                List<UserDetailsVo> userDetailsEntityList = userDetailsBll.listByUserIds(userIds);
                Map<Long, UserDetailsVo> finalDetailsEntityMap = ObjectUtil.isEmpty(userDetailsEntityList) ? new HashMap<>() : userDetailsEntityList.stream()
                        .collect(Collectors.toMap(UserDetailsVo::getUserId, u -> u, (existing, replacement) -> replacement));

                // 获取所有销售人员
                List<SalesInfoVo> salesEntityList = ResultUtil.getResult(salesBll.listByIds(ObjectUtil.isNotEmpty(userDetailsEntityList) ? userDetailsEntityList.stream().map(UserDetailsVo::getSaleId).distinct().toList() : null));
                Map<Long, SalesInfoVo> finalSalesEntityMap = ObjectUtil.isEmpty(salesEntityList) ? new HashMap<>() : salesEntityList.stream()
                        .collect(Collectors.toMap(SalesInfoVo::getId, s -> s, (a, b) -> b));

                // 获取用户信息，以便查询用户版本
                List<Long> userOrderIds = new ArrayList<>();
                if (ObjectUtil.isNotEmpty(listR.getData())) {
                    userOrderIds = listR.getData().stream().map(item -> {
                        if (item.getParentId() != null && item.getParentId() != 0) {
                            return item.getParentId();
                        }
                        return item.getId();
                    }).toList();
                }
                // 查询用户版本信息
                Map<Long, OrderInfoVo> orderMap = orderBll.currentOrderByUserIds(userOrderIds)
                        .stream()
                        .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> o));
                Map<Long, Date> lastOrderDateMap = orderBll.userOrderExpireTimeByUserIds(userOrderIds);

                list.stream().map(item->{
                    List<UserListVo> list1 = listR.getData().stream().filter(vo -> vo.getId().equals(item.getUserId())).toList();
                    if (!list1.isEmpty()) {
                        UserListVo userListVo = list1.get(0);
                        item.setUserName(userListVo.getNickName());
                        // 填装版本名称和版本过期时间
                        Long currentUserId = userListVo.getParentId() != null && userListVo.getParentId() != 0 ? userListVo.getParentId() : userListVo.getId();
                        OrderInfoVo orderInfoVo = orderMap.get(currentUserId);
                        if (orderInfoVo != null) {
                            item.setPackageName(orderInfoVo.getCommodityName());
                        }

                        if (ObjectUtil.isNotEmpty(lastOrderDateMap)) {
                            item.setPackageExpiredTime(lastOrderDateMap.get(currentUserId));
                        }
                    }
                    // 填充每条记录进行AI分析后得到的关键词数量
                    AiAnalysisRecordEntity aiAnalysisRecordEntity = finalRecordEntityMap.get(item.getFileId());
                    if (aiAnalysisRecordEntity != null) {
                        item.setSensitiveWordTotal(aiAnalysisRecordEntity.getSensitiveWordTotal());
                        item.setSensitiveWordMark(aiAnalysisRecordEntity.getSensitiveWordMark());
                    }
                    // 填充每条记录的用户销售人员名称
                    UserDetailsVo userDetailsEntity = finalDetailsEntityMap.get(list1.get(0).getId());
                    if (userDetailsEntity != null) {
                        SalesInfoVo salesEntity = finalSalesEntityMap.get(userDetailsEntity.getSaleId());
                        if (salesEntity != null) {
                            item.setUserSales(salesEntity.getSalesName());
                        }
                    }
                    return item;
                }).toList();
            }
        }

        return pageUtilsR;
    }

    /**
     * 取联合查询出来的用户ID集合的交集
     * @param collections 所有集合
     * @return
     * @param <T>
     */
    public static <T> Collection<T> intersectionOrSingleSet(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Set<T>> filteredSets = new ArrayList<>();
        for (Collection<T> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                filteredSets.add(new HashSet<>(collection)); // 确保所有的集合都是 Set 类型
            }
        }

        int nonEmptyCount = filteredSets.size();
        if (nonEmptyCount == 0) {
            return Collections.emptyList(); // 所有集合都是 null 或空
        } else if (nonEmptyCount == 1) {
            // 如果只有一个非空集合，返回它的所有元素
            return filteredSets.get(0);
        } else {
            // 否则计算所有非空集合的交集
            Set<T> result = new HashSet<>(filteredSets.get(0));
            for (int i = 1; i < filteredSets.size(); i++) {
                result.retainAll(filteredSets.get(i));
            }
            return result;
        }
    }

    /**
     * 保存复盘上传文件信息
     * @param uploadFileBo
     * @return
     */
    @Override
    public R<String> saveUploadFile(UploadFileBo uploadFileBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        uploadFileBo.setUserId(user.getId());
        uploadFileBo.setTenantId(user.getActiveTenantId());
        return uploadFileBll.saveUploadFile(uploadFileBo);
    }

    /**
     * 保存文件分析内容
     * @param uploadFileAnalysisVo
     * @return
     */
    @Override
    public R<String> saveuploadFileAnalysis(List<UploadFileAnalysisVo> uploadFileAnalysisVo) {
        return uploadFileBll.saveuploadFileAnalysis(uploadFileAnalysisVo);
    }

    /**
     * 根据文件Id查询分析内容
     * @param fileId
     * @return
     */
    @Override
    public R<List<UploadFileAnalysisVo>> seletByFileId(String fileId) {
        return uploadFileBll.seletByFileId(fileId);
    }

    /**
     * 客户端获取复盘上传文件列表
     * @return
     */
    @Override
    public R<List<UploadFileVO>> seletList() {
        UserCacheVo user = GlobalObject.getLocalUser();
        return uploadFileBll.seletList(user.getId());
    }

    /**
     * 客户端获取复盘上传文件分析内容
     * @param uploadFileAnalysisBo
     * @return
     */
    @Override
    public R<List<UploadFileAnalysisVo>> seletContent(UploadFileAnalysisListBo uploadFileAnalysisBo) {

        // 根据文件id和行业id获取最后一条分析记录
        R<UploadFileAnalysisRecordInfoVo> recordInfoR = this.uploadFileAnalysisRecordBll.infoLastByFileIdAndTradeId(uploadFileAnalysisBo.getFileId(), uploadFileAnalysisBo.getTradeId());
        if(recordInfoR.getCode() == 0 && recordInfoR.getData() != null) {
            UploadFileAnalysisRecordInfoVo analysisRecordInfoVo = recordInfoR.getData();

            // 获取分析数据
            String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + analysisRecordInfoVo.getStoreFileName());
            if(!StringUtils.isEmpty(jsonDataStr)) {
                List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(jsonDataStr, SentenceMarkVo.class);

                List<UploadFileAnalysisVo> audioAnalysissVOList = sentenceMarkVos.stream().map(item -> {
                    UploadFileAnalysisVo audioAnalysissVo = new UploadFileAnalysisVo();
                    audioAnalysissVo.setId(0L);
                    audioAnalysissVo.setFileId(analysisRecordInfoVo.getFileId());
                    audioAnalysissVo.setUserId(analysisRecordInfoVo.getUserId());
                    audioAnalysissVo.setStatus(0);
                    audioAnalysissVo.setDataJson(JSON.toJSONString(item));
                    audioAnalysissVo.setTradeId(analysisRecordInfoVo.getTradeId() + "");
                    audioAnalysissVo.setParagraph(item.getCurrentSort());
                    return audioAnalysissVo;
                }).sorted(Comparator.comparingInt(UploadFileAnalysisVo::getParagraph)).collect(Collectors.toList());

                return R.ok(audioAnalysissVOList);
            }


        }

//        return uploadFileBll.seletContent(uploadFileAnalysisListBo);
        return R.error(5001, "数据不存在");
    }

    /**
     * 保存复盘上传文件分析记录及内容
     * @param uploadFileRecodBo
     * @return
     */
    @Override
    public R<String> saveFileAnalysis(List<UploadFileAnalysisBo> uploadFileRecodBo) {

        if(uploadFileRecodBo != null && uploadFileRecodBo.size() > 0) {
            return uploadFileBll.saveFileAnalysis(uploadFileRecodBo);
        }
        return R.ok();
    }

    /**
     * 修改复盘文件上传信息
     * @param uploadFileBo
     * @return
     */
    @Override
    public R<String> updateUploadFile(UploadFileBo uploadFileBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        uploadFileBo.setUserId(user.getId());
        uploadFileBo.setTenantId(user.getActiveTenantId());
        return uploadFileBll.updateUploadFile(uploadFileBo);
    }

    /**
     * 根据fileId删除
     * @param fileId
     * @return
     */
    @Override
    public void deleltByFileId(String fileId) {
         uploadFileBll.deleltByFileId(fileId);
    }

    /**
     * 根据客户端上传的List<FileId>删除
     * @param fileId
     * @return
     */
    @Override
    public R<String> removeByFileId(List<String> fileId) {
        return uploadFileBll.removeByFileId(fileId);

    }

    @Override
    public void clearAnalysis(String fileId) {

        uploadFileBll.clearAnalysis(fileId);
    }


    /**
     * 根据user_id查询文件上传分析
     * @param uploadFileBo
     * @return
     */
    @Override
    public R<PageUtils<UploadFileVO>> fileAnalysisByUserId(UploadFileBo uploadFileBo) {
        return uploadFileBll.fileAnalysisByUserId(uploadFileBo);
    }

    @Override
    public R<PageUtils<UploadFileInfoVo>> clientFileList(ClientFileListBo clientFileListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientFileListBo.setUserId(user.getId());
        clientFileListBo.setTenantId(user.getActiveTenantId());

        return uploadFileBll.clientFileList(clientFileListBo);
    }

    @Override
    public R<String> clientDeleteFile(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();
        // 子账号自能操作自己的
        Long userId = user.getUserType() == 2 ? user.getId() : null;

        return uploadFileBll.clientDeleteFile(ids, user.getActiveTenantId(), userId);
    }

    @Override
    public R<UploadFileInfoVo> clientGetFileByFileId(String fileId) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return uploadFileBll.clientGetFileByFileId(fileId, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<String> saveOrUpdateFile(UploadFileInfoBo uploadFileInfoBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        uploadFileInfoBo.setUserId(user.getId());
        uploadFileInfoBo.setTenantId(user.getActiveTenantId());

        return uploadFileBll.saveOrUpdateFile(uploadFileInfoBo);
    }

    @Override
    public R<String> updateFileAnalysisStatus(UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        updateFileAnalysisStatusBo.setUserId(user.getId());
        updateFileAnalysisStatusBo.setTenantId(user.getActiveTenantId());

        return uploadFileBll.updateFileAnalysisStatus(updateFileAnalysisStatusBo);
    }

    @Override
    public R<String> updateFileTrade(UpdateFileTradeBo updateFileTradeBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        updateFileTradeBo.setUserId(user.getId());
        updateFileTradeBo.setTenantId(user.getActiveTenantId());

        return uploadFileBll.updateFileTrade(updateFileTradeBo);
    }

    @Override
    public R<List<UploadFileInfoVo>> clientListFileByFileIds(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return uploadFileBll.clientListFileByFileIds(ids, user.getId(), user.getActiveTenantId());
    }


}
