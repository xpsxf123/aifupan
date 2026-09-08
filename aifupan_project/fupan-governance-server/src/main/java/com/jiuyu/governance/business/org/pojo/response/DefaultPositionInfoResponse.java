package com.jiuyu.governance.business.org.pojo.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 默认岗位信息响应
 *
 * @author HeHui
 * @date 2026-04-02 14:04
 */
@Getter
@Setter
public class DefaultPositionInfoResponse {

    /**
     * 岗位 - 主播 ID
     */
    private Long anchorId;

    /**
     * 岗位 - 主播名称
     */
    private String anchorName;

    /**
     * 岗位 - 副播 ID
     */
    private Long subAnchorId;

    /**
     * 岗位 - 副播名称
     */
    private String subAnchorName;

    /**
     * 岗位 - 运营 ID
     */
    private Long operationId;

    /**
     * 岗位 - 运营名称
     */
    private String operationName;

    /**
     * 岗位 - 中控 ID
     */
    private Long controlId;

    /**
     * 岗位 - 中控名称
     */
    private String controlName;

    /**
     * 岗位 - 投手 ID
     */
    private Long touCherId;

    /**
     * 岗位 - 投手名称
     */
    private String touCherName;

    /**
     * 岗位 - 剪辑 ID
     */
    private Long editorId;

    /**
     * 岗位 - 剪辑名称
     */
    private String editorName;

    /**
     * 岗位 - 嘉宾 ID
     */
    private Long guestId;

    /**
     * 岗位 - 嘉宾名称
     */
    private String guestName;

    /**
     * 岗位 - 合规专员 ID
     */
    private Long complianceSpecialistId;

    /**
     * 岗位 - 合规专员名称
     */
    private String complianceSpecialistName;

    /**
     * 岗位 - 合规经理 ID
     */
    private Long complianceManagerId;

    /**
     * 岗位 - 合规经理名称
     */
    private String complianceManagerName;

    /**
     * 岗位 - 合规负责人 ID
     */
    private Long complianceLeaderId;

    /**
     * 岗位 - 合规负责人名称
     */
    private String complianceLeaderName;
}
