package com.jiuyu.governance.business.org.service;


import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.org.pojo.entity.Position;
import com.jiuyu.governance.business.org.pojo.request.PositionAddRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.PositionUpdateRequest;
import com.jiuyu.governance.business.org.pojo.response.DefaultPositionInfoResponse;
import com.jiuyu.governance.business.org.pojo.response.PositionResponse;
import com.jiuyu.governance.business.rbac.pojo.constants.DefaultPosition;
import com.jiuyu.governance.common.pojo.bo.LabelOption;

import java.util.List;
import java.util.Map;

/**
 * 岗位服务
 *
 * @author HeHui
 * @date 2026-03-23 10:18
 */
public interface PositionService {

    /**
     * 新增岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> addPosition(PositionAddRequest request, AccessUser accessUser);

    /**
     * 修改岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> updatePosition(PositionUpdateRequest request, AccessUser accessUser);

    /**
     * 删除岗位
     *
     * @param id         ID
     * @param accessUser 访问用户
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    ApiResponse<Void> deletePosition(long id, AccessUser accessUser);

    /**
     * 分页查询岗位
     *
     * @param request    请求
     * @param accessUser 访问用户
     *
     * @return {@link PageData }<{@link PositionResponse }>
     */
    PageData<PositionResponse> pageQueryPosition(PositionPageQueryRequest request, AccessUser accessUser);

    /**
     * 获取岗位名称映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    Map<Long, String> getPositionNameMap(List<Long> ids);

    /**
     * 获取岗位编码映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    Map<Long, String> getPositionCodeMap(List<Long> ids);


    /**
     * 获取岗位选项
     *
     * @param ids ids
     *
     * @return {@link List }<{@link LabelOption }>
     */
    List<LabelOption> getPositionOptions(List<Long> ids);

    /**
     * 列出选项
     *
     * @param keyword  关键词
     * @param limit    限制
     * @param tenantId 租户ID
     *
     * @return {@link List }<{@link LabelOption }>
     */
    List<LabelOption> listOptions(String keyword, int limit, long tenantId);


    /**
     * 获取默认位置id
     *
     * @param defaultPosition 默认位置
     * @param tenantId        租户ID
     *
     * @return {@link Long }
     */
    default Long getDefaultPositionId(DefaultPosition defaultPosition, long tenantId) {
        if (defaultPosition == null) {
            return null;
        }
        return this.getDefaultPositionMap(List.of(defaultPosition), tenantId).get(defaultPosition);
    }


    /**
     * 获取默认岗位映射
     *
     * @param defaultPositions 默认位置
     * @param tenantId         租户ID
     *
     * @return {@link Map }<{@link DefaultPosition }, {@link Long }>
     */
    Map<DefaultPosition, Long> getDefaultPositionMap(List<DefaultPosition> defaultPositions, long tenantId);


    /**
     * 获取默认岗位信息
     *
     * @param tenantId 租户ID
     *
     * @return {@link DefaultPositionInfoResponse }
     */
    DefaultPositionInfoResponse getDefaultPositionInfo(long tenantId);


    /**
     * 获取岗位映射
     *
     * @param ids ids
     *
     * @return {@link Map }<{@link Long }, {@link Position }>
     */
    Map<Long, Position> getPositionMap(List<Long> ids);
}
