package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.words.bo.AnchorVideoBo;
import com.jiuyu.replay.words.bo.video.HistoryBatchNumberListBo;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AnchorVideoDao extends BaseMapper<AnchorVideoEntity> {

    @Select("SELECT COUNT(*) FROM tb_anchor_video WHERE sec_uid = #{secUid} and is_deleted = 0")
    int countBySecUid(String secUid);


    /**
     * 获取历史批次列表
     *
     * @param bo
     * @return
     */
    List<AnchorVideoEntity> historyBatchNumberList(HistoryBatchNumberListBo bo);

    /**
     * 分页接口
     *
     * @param page
     * @param anchorVideoBo
     * @return
     */
    IPage<AnchorVideoVO> pageList(@Param("page") Page<AnchorVideoVO> page, @Param("bo") AnchorVideoBo anchorVideoBo);

    /**
     * Data Hub：按 租户×业务账号 聚合成功分析记录（analysis_status=2）
     *
     * @param tenantIds 租户id列表
     * @param startDate 窗口起（含，按 analysis_time，可空）
     * @param endDate   窗口止（不含，按 analysis_time，可空）
     *
     * @return 每个 租户×sec_uid 一行的分析聚合
     */
    List<com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo> selectDataHubAnalysisStats(
            @Param("tenantIds") java.util.Collection<Long> tenantIds,
            @Param("startDate") java.util.Date startDate,
            @Param("endDate") java.util.Date endDate);
}
