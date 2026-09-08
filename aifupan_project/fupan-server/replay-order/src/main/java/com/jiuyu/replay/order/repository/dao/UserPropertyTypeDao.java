package com.jiuyu.replay.order.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.entity.UserPropertyTypeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户资产类型总明细
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Mapper
public interface UserPropertyTypeDao extends BaseMapper<UserPropertyTypeEntity> {

    @Select("SELECT " +
            " commodity_type_id, " +
            " sum( use_number ) AS useQuantity, " +
            " sum( total_number ) AS totalQuantity  " +
            "FROM " +
            " tb_type_surplus " +
            "WHERE " +
            " property_id = #{propertyId}  " +
            " AND time_status = 0 " +
            " AND use_status in (0, 1) " +
            "GROUP BY " +
            " commodity_type_id")
    List<UserPropertyTypeEntity> statisticsProperty(@Param("propertyId") Long propertyId);


    @Update("UPDATE tb_user_property_type ut  " +
            "SET ut.use_quantity = ifnull(( " +
            " SELECT " +
            "  sum( t.use_number )  " +
            " FROM " +
            "  tb_type_surplus t  " +
            " WHERE " +
            "  t.use_status IN ( 0, 1 )  " +
            "  AND t.time_status = 0  " +
//            "  AND #{now} BETWEEN t.start_time  " +
//            "  AND t.end_time  " +
            "  AND t.commodity_type_id = ut.commodity_type_id  " +
            "  AND t.property_id = ut.property_id " +
            " ), 0) " +
            "WHERE " +
            " id = #{id}")
    @Deprecated
    void updateCurrent(@Param("id") Long id,@Param("now") String now);

    @Select("select pt.* from tb_user_property_type pt inner join tb_user_property p on pt.property_id = p.id " +
            "where " +
            "p.is_use = 1 and p.is_deleted = 0 and pt.is_deleted = 0")
    List<UserPropertyTypeEntity> listByIsUse();


    @Update("UPDATE tb_user_property_type m " +
            "JOIN tb_user_property t ON m.property_id = t.id " +
            "JOIN tb_user_property t2 ON t.parent_id = t2.id " +
            "JOIN tb_user_property_type t3 ON t2.id = t3.property_id AND m.commodity_type_code = t3.commodity_type_code " +
            "SET m.parent_id = ifnull(t3.id, 0) " +
            "WHERE m.commodity_type_code = #{commodityTypeCode}")
    void setPropertyParentId(String commodityTypeCode);

    @Update("update tb_user_property_type set parent_id = 0 where commodity_type_code = #{commodityTypeCode}")
    void setPropertyParentIdToNull(String commodityTypeCode);

    /**
     * 清除用户的私有资产
     *
     * @param typeEntities 用户id > 资产id
     */
    void clearUserPrivateProperty(List<UserPropertyTypeEntity> typeEntities);

    /**
     * 获取用户剩余资产
     *
     * @param userIds       用户ID
     * @param commodityCode 资产类型
     *
     * @return {@link List }<{@link UserPropertyTypeEntity }> 汇总后的数据
     */
    List<UserPropertyTypeEntity> getUserPropertyRemainingMap(@Param("userIds") Collection<Long> userIds, @Param("commodityCode") String commodityCode);
}
