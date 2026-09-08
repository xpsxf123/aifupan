package com.jiuyu.replay.order.repository.dao;

import com.jiuyu.replay.order.entity.OrderDetailEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.order.vo.ClintGetDataVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 
 * 
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Mapper
public interface OrderDetailDao extends BaseMapper<OrderDetailEntity> {

    /**
     * 统计用户订单总数
     * @param userId
     * @return
     */
    @Select("SELECT " +
            " commodity_type_id, " +
            " sum(total_number) as total_number " +
            "FROM  " +
            " tb_order_detail od " +
            " LEFT JOIN tb_order o ON od.order_id = o.id " +
            "WHERE " +
            " od.`status` = 1  " +
            "AND o.`status` = 2 " +
            "and o.user_id = #{userId} " +
            "group by commodity_type_id")
    List<OrderDetailEntity> statisticsTotalNumberByUserId(@Param("userId") Long userId);


    @Select("<script>" +
            "SELECT" +
            "  ord.id," +
            "  o.id AS order_id," +
            "  CASE WHEN o.commodity_type = 1 THEN 1 ELSE 0 END AS commodityType," +
            "  ord.commodity_type_id, " +
            "  ord.commodity_type_code, " +
            "  ord.commodity_type_name, " +
            "  ord.commodity_type_unit, " +
            "  ord.commodity_type_reset, " +
            "  ord.total_number, " +
            "  ord.start_date as startTime," +
            "  ord.expiration_date as endTime," +
            "  0 as useNumber  " +
            "FROM" +
            "  `tb_order_detail` AS ord" +
            "  LEFT JOIN tb_order AS o ON o.id = ord.order_id " +
            "WHERE" +
            "  o.id IN " +
            "<foreach item='item' collection='orderIds' open='(' separator=',' close=')'>" +
            "    #{item}" +
            "</foreach>" +
            "</script>"
    )
    List<ClintGetDataVo> getDetailByOrderIds(@Param("orderIds") List<Long> orderIds);
}
