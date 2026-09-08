package com.jiuyu.replay.generic.bo.ai;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/20 下午6:33
 */
@Data
public class AiMessageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 系统角色
     */
    private List<Map<String, Object>> system;

    /**
     * 用户角色
     */
    private List<Map<String, Object>> user;

    /**
     * 助手角色
     */
    private List<Map<String, Object>> assistant;

    /**
     * 工具角色
     */
    private List<Map<String, Object>> tool;
}
