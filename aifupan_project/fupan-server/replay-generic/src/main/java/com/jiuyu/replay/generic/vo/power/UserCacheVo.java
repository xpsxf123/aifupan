package com.jiuyu.replay.generic.vo.power;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserCacheVo extends UserVo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * token
     */
    private String token;
    /**
     * ip : 记录请求的ip
     */
    private String ip;
    /**
     * 拥有的角色id列表
     */
    private List<Long> roleIdList;
    /**
     * 拥有的菜单列表
     */
    private List<MenuVo> menuList;
    /**
     * 拥有的菜单列表（树形结构）
     */
    private List<MenuTreeVo> menuTreeList;
    /**
     * 请求的时间戳
     */
    private Long requestTime;
    /**
     * 在线状态 0：离线 1：在线
     */
    private Integer onlineStatus;

    /**
     * 代理商id
     */
    private Long agentId;
}
