package com.jiuyu.replay.power.rse;

import com.jiuyu.replay.generic.vo.power.SubUserListVo;

import java.util.List;

public interface UserRse {

    /**
     * 根据用户id获取子账号列表
     * @param userId 用户id
     * @return
     */
    List<SubUserListVo> getSubUserListByUserId(Long userId);

    /**
     * 设置用户客户端版本
     *
     * @param userId        用户
     * @param clientVersion 版本
     */
    void saveClientVersion(Long userId, String clientVersion);

    /**
     * 获取客户端版本
     *
     * @param userId 用户
     * @return 客户端版本
     */
    String getClientVersion(Long userId);
}
