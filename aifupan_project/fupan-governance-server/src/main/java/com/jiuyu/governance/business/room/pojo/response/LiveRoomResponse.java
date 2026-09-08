package com.jiuyu.governance.business.room.pojo.response;

import com.jiuyu.governance.business.rbac.pojo.response.EmployeeBaseInfo;
import com.jiuyu.governance.business.room.pojo.bo.RoomSchedulesRawBo;
import com.jiuyu.governance.business.room.pojo.constants.LivePlatformType;
import com.jiuyu.governance.plugins.webmvc.serializer.EnumDesc;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 直播间响应信息
 *
 * @author HeHui
 * @date 2026-03-25
 */
@Getter
@Setter
public class LiveRoomResponse {

    /**
     * 直播间主键ID
     */
    private Long id;

    /**
     * 直播平台类型 (0: 抖音, 1: 快手, 2: 视频号)
     */
    @EnumDesc(LivePlatformType.class)
    private Integer platform;

    /**
     * 主播平台账号 (如抖音号)
     */
    private String anchorNumber;

    /**
     * 主播在第三方平台的唯一标识
     */
    private String secUid;

    /**
     * 主播个人主页的URL链接
     */
    private String homeUrl;

    /**
     * 直播间外显的URL链接
     */
    private String liveUrl;

    /**
     * 主播名称/昵称
     */
    private String anchorName;

    /**
     * 主播头像的URL链接
     */
    private String anchorAvatar;

    /**
     * 主播在平台的首播日期
     */
    private LocalDate debutDate;

    /**
     * 关联的行业分类ID
     */
    private Long tradeId;

    /**
     * 所属子公司的唯一ID
     */
    private Long companyId;

    /**
     * 所属子公司的展示名称 (通过 Complete 工具类组装)
     */
    private String companyName;

    /**
     * 所属部门的唯一ID
     */
    private Long deptId;

    /**
     * 所属部门的展示名称 (通过 Complete 工具类组装)
     */
    private String deptName;

    /**
     * 所属小组的唯一ID
     */
    private Long teamId;

    /**
     * 所属小组的展示名称 (通过 Complete 工具类组装)
     */
    private String teamName;

    /**
     * 直播间账号状态 (0: 停用, 1: 正常)
     */
    private Integer accountStatus;

    /**
     * 记录创建的时间
     */
    private LocalDateTime createDate;

    /**
     * 记录最后一次更新的时间
     */
    private LocalDateTime updateDate;

    /**
     * 该直播间绑定的管理员基本信息列表
     */
    private List<EmployeeBaseInfo> managerUserInfos;


    /**
     * 该直播间当天的排班信息列表 - 请求参数 loadSchedule = true时才返回
     */
    private List<RoomSchedulesRawBo> thatDaySchedules;

    /**
     * 该直播间明天的排班信息列表 - 请求参数 loadSchedule = true时才返回
     */
    private List<RoomSchedulesRawBo> tomorrowSchedules;


    public Long getDeptId() {
        if (deptId != null && deptId <= 0L) {
            this.deptId = null;
        }
        return deptId;
    }

    public Long getTeamId() {
        if (teamId != null && teamId <= 0L) {
            this.teamId = null;
        }
        return teamId;
    }
}
