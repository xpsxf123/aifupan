package com.jiuyu.replay.words.repository.dao;

import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.entity.ClientAiFavEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 运营/违规收藏列表
 * 
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Mapper
public interface ClientAiFavDao extends BaseMapper<ClientAiFavEntity> {

    /**
     * 判断是否存在已删除的数据
     * @param params
     * @return
     */
    @Select("select if(exists(select 1 from tb_client_ai_fav " +
            "where " +
            "tenant_id = #{p.tenantId} " +
            "and user_id = #{p.userId} " +
            "AND fav_type = #{p.favType} " +
            "and data_resource_type = #{p.dataResourceType} " +
            "and data_resource_uuid = #{p.dataResourceUuid} " +
            "and is_deleted = 1" +
            "), 1, 0) as num")
    int hasDelete(@Param("p") ClientAiFavBo params);
}
