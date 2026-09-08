package com.jiuyu.governance.openfeign.replay.consts;

import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.pojo.bo.IdName;
import com.jiuyu.governance.openfeign.replay.response.AnchorVideoInfoResponse;
import com.jiuyu.governance.openfeign.replay.response.*;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;

/**
 * Replay HTTP 响应类型引用常量类
 * <p>
 * 集中管理所有 ParameterizedTypeReference 常量，避免重复定义，提高代码复用性
 *
 * @author HeHui
 * @date 2026-04-10
 */
public class ReplayApiResponseType {

    /**
     * ApiResponse&lt;Long&gt; 类型引用
     * <p>
     * 用于返回 Long 类型的 API 响应，如账户 ID、用户 ID 等
     */
    public static final ParameterizedTypeReference<ApiResponse<Long>> LONG_TYPE = new ParameterizedTypeReference<ApiResponse<Long>>() {
    };

    /**
     * ApiResponse&lt;Void&gt; 类型引用
     * <p>
     * 用于返回空数据的 API 响应，如解绑、更新等操作
     */
    public static final ParameterizedTypeReference<ApiResponse<Void>> VOID_TYPE = new ParameterizedTypeReference<ApiResponse<Void>>() {
    };

    /**
     * ApiResponse&lt;ReplayAccountResponse&gt; 类型引用
     * <p>
     * 用于返回账户基本信息的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<ReplayAccountResponse>> ACCOUNT_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<ReplayAccountResponse>>() {
    };

    /**
     * ApiResponse&lt;ReplayAccountDetailResponse&gt; 类型引用
     * <p>
     * 用于返回账户详细信息的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<ReplayAccountDetailResponse>> ACCOUNT_DETAIL_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<ReplayAccountDetailResponse>>() {
    };

    /**
     * ApiResponse&lt;List&lt;ReplayAccountDetailResponse&gt;&gt; 类型引用
     * <p>
     * 用于返回账户详细列表的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<List<ReplayAccountDetailResponse>>> LIST_ACCOUNT_DETAIL_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<List<ReplayAccountDetailResponse>>>() {
    };

    /**
     * ApiResponse&lt;ReplayUserDetailsInfo&gt; 类型引用
     * <p>
     * 用于返回用户详细信息（通过手机号查询）的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<ReplayUserDetailsInfo>> USER_DETAILS_INFO_TYPE = new ParameterizedTypeReference<ApiResponse<ReplayUserDetailsInfo>>() {
    };

    /**
     * ApiResponse&lt;SmsResponse&gt; 类型引用
     * <p>
     * 用于返回短信发送结果的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<SmsResponse>> SMS_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<SmsResponse>>() {
    };

    /**
     * ApiResponse&lt;List&lt;TradeSimpleTreeResponse&gt;&gt; 类型引用
     * <p>
     * 用于返回行业树结构列表的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<List<TradeSimpleTreeResponse>>> TRADE_SIMPLE_TREE_LIST_TYPE = new ParameterizedTypeReference<ApiResponse<List<TradeSimpleTreeResponse>>>() {
    };

    /**
     * ApiResponse&lt;List&lt;IdName&gt;&gt; 类型引用
     * <p>
     * 用于返回 ID 和名称映射列表的 API 响应，如行业名称列表
     */
    public static final ParameterizedTypeReference<ApiResponse<List<IdName>>> ID_NAME_LIST_TYPE = new ParameterizedTypeReference<ApiResponse<List<IdName>>>() {
    };


    /**
     * ApiResponse&lt;TenantPropertyQuotaResponse&gt; 类型引用
     * <p>
     * 用于返回租户资产额度信息的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<TenantPropertyQuotaResponse>> QUOTA_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<TenantPropertyQuotaResponse>>() {
    };

    /**
     * ApiResponse&lt;SystemKvResponse&gt; 类型引用
     * <p>
     * 用于返回系统键值对信息的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<SystemKvResponse>> SYSTEM_KV_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<SystemKvResponse>>() {
    };


    /**
     * ApiResponse&lt;List&lt;TenantAnchorInfoResponse&gt;&gt; 类型引用
     * <p>
     * 用于返回租户主播信息列表的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<List<TenantAnchorInfoResponse>>> TENANT_ANCHOR_INFO_LIST_RESPONSE_TYPE = new ParameterizedTypeReference<ApiResponse<List<TenantAnchorInfoResponse>>>() {
    };

    /**
     * ApiResponse&lt;AnchorVideoInfoResponse&gt; 类型引用
     * <p>
     * 用于返回视频信息的 API 响应
     */
    public static final ParameterizedTypeReference<ApiResponse<AnchorVideoInfoResponse>> ANCHOR_VIDEO_INFO_TYPE = new ParameterizedTypeReference<>() {
    };

    /**
     * 视频列表响应类型（游标分页）
     */
    public static final ParameterizedTypeReference<ApiResponse<ReplayCursorPage<AnchorVideoListItem>>> VIDEO_LIST_RESPONSE = new ParameterizedTypeReference<>() {
    };

    /**
     * 私有构造函数，防止实例化
     */
    private ReplayApiResponseType() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
