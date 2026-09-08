package com.jiuyu.governance.business.room.pojo.response;

import lombok.Getter;
import lombok.Setter;

/**
 *  直播间组织架构响应
 * @author HeHui
 * @date 2026-05-11 11:58
 */
@Getter
@Setter
public class LiveRoomOrgResponse {

    /**
     * 直播间ID
     */
    private Long roomId;

    /**
     * 所属子公司的唯一ID
     */
    private Long companyId;



    /**
     * 所属部门的唯一ID
     */
    private Long deptId;



    /**
     * 所属小组的唯一ID
     */
    private Long teamId;
}
