package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.generic.bo.aiagent.AnchorListQueryBo;
import com.jiuyu.replay.generic.vo.aiagent.AnchorItemVo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface AnchorUrlUserDao extends BaseMapper<AnchorUrlUserEntity> {

    @Select("SELECT COUNT(*) FROM tb_anchor_url_user WHERE user_id = #{usrId} and is_deleted = 0")
    int countByUsrId(Long usrId);


    @Select("SELECT COUNT(*) FROM tb_anchor_url_user WHERE anchor_url_sec_uid = #{secUid} and is_deleted = 0")
    int countBySecUid(String secUid);


    /**
     * 搜索主播列表
     * @param queryBo
     * @return
     */
    List<AnchorItemVo> searchAnchorList(AnchorListQueryBo queryBo);

    /**
     * 获取当前租户已添加主播的行业ID
     *
     * @param tenantId 租户ID
     */
    List<Long> getTenantTradeIds(@Param("tenantId") long tenantId);

    /**
     * 根据 sec_uid 批量查询用户自选行业
     *
     * @param secUids 主播 sec_uid 集合
     */
    List<com.jiuyu.replay.words.vo.peer.AnchorUrlUserTradeVo> selectTradeBySecUids(@Param("secUids") Set<String> secUids);
}
