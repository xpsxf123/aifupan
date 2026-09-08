package com.jiuyu.governance.business.room.pojo.response.schedule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 开放接口 - 主播排班查询响应（按 videoId 聚合）
 * <p>
 * 每个 videoId 一行：包含命中排班中「主播岗位」的人员（id + name）集合，
 * 以及该 videoId 命中的场次（排班）ID 集合。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Getter
@Setter
public class OpenAnchorScheduleResponse {

    /**
     * videoId
     */
    private String videoId;

    /**
     * 主播岗位人员集合（id + name）
     */
    private List<AnchorInfo> anchors;

    /**
     * 命中的场次（排班）ID 集合
     */
    private List<Long> scheduleIds;

    /**
     * 主播人员信息
     */
    @Getter
    @Setter
    public static class AnchorInfo {

        /**
         * 员工ID
         */
        private Long id;

        /**
         * 员工姓名
         */
        private String name;

        public AnchorInfo() {
        }

        public AnchorInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
