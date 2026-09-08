package com.jiuyu.replay.api.logic.power;

import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;

public interface RoleLogic {

    /**
     * 角色列表
     * @param roleListBo 菜单列表查询参数
     * @return
     */
    R<PageUtils<RoleVo>> queryPage(RoleListBo roleListBo);

    /**
     * 信息
     * @param id 角色id
     * @return
     */
    R<RoleInfoVo> info(Long id);

    /**
     * 保存
     * @param role 数据对象
     * @return
     */
    R<String> save(RoleInfoBo role);

    /**
     * 修改
     * @param role 数据对象
     * @return
     */
    R<String> modify(RoleInfoBo role);

    /**
     * 删除
     * @param id 角色id
     * @return
     */
    R<String> delete(Long id);
}
