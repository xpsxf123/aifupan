package com.jiuyu.replay.common.diff;


import com.jiuyu.replay.generic.vo.power.UserCacheVo;

import java.util.Map;

/**
 * 差异记录存储
 *
 * @author HeHui
 * @date 2025-06-23 11:50
 */
public interface DiffLogStorage {


    /**
     * 保存差异记录
     *
     * @param business    业务
     * @param sourceId    源id
     * @param beforeInfo  变更前信息
     * @param afterInfo   变更后信息
     * @param operateUser 操作人
     *
     * @return boolean
     */
    boolean save(Business business, long sourceId, Map<String, Object> beforeInfo, Map<String, Object> afterInfo, UserCacheVo operateUser);
}
