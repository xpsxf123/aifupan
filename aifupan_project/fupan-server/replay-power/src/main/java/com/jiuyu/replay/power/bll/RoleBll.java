package com.jiuyu.replay.power.bll;

import com.jiuyu.replay.power.bo.RoleInfoBo;
import com.jiuyu.replay.power.bo.RoleListBo;
import com.jiuyu.replay.power.constant.Constant;
import com.jiuyu.replay.power.producer.MenuProducer;
import com.jiuyu.replay.power.producer.RoleProducer;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.MenuVo;
import com.jiuyu.replay.power.vo.RoleInfoVo;
import com.jiuyu.replay.power.vo.RoleVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RoleBll {

    @Resource
    private RoleProducer roleProducer;
    @Resource
    private MenuProducer menuProducer;

    /**
     * 角色列表
     * @param roleListBo 菜单列表查询参数
     * @return
     */
    public R<PageUtils<RoleVo>> queryPage(RoleListBo roleListBo) {

        return R.ok(this.roleProducer.queryPage(roleListBo));
    }

    /**
     * 信息
     * @param id 角色id
     * @return
     */
    public R<RoleInfoVo> info(Long id) {



        RoleVo info = this.roleProducer.info(id);
        if(info != null) {
            RoleInfoVo roleInfoVo = new RoleInfoVo();

            BeanUtils.copyProperties(info, roleInfoVo);

            // 封装角色的菜单
            List<MenuVo> menuEntities = menuProducer.listByRoleId(id);
            roleInfoVo.setMenuList(menuEntities);
            roleInfoVo.setMenuTreeList(menuProducer.packageMenuTree(menuEntities, false));

            return R.ok(roleInfoVo);

        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "角色不存在");
    }

    /**
     * 保存
     * @param role 数据对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(RoleInfoBo role) {
        // 保存角色
        RoleInfoVo roleInfoVo = this.roleProducer.save(role);

        // 保存角色跟菜单的关联
        this.menuProducer.saveRoleMenu(roleInfoVo.getId(), role.getRoleIdList());

        return R.ok("添加成功");

    }

    /**
     * 修改
     * @param role 数据对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> modify(RoleInfoBo role) {

        // 修改角色
        this.roleProducer.modify(role);

        // 删除原关联
        this.menuProducer.deleteByRoleId(role.getId());
        // 保存角色跟菜单的关联
        this.menuProducer.saveRoleMenu(role.getId(), role.getRoleIdList());

        return R.ok("修改成功");

    }

    /**
     * 删除
     * @param id 角色id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> delete(Long id) {

        this.roleProducer.deleteById(id);

        this.menuProducer.deleteByRoleId(id);

        return R.ok("删除成功");

    }
}
