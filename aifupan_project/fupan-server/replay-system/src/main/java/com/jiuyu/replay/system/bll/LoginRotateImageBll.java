package com.jiuyu.replay.system.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.system.bo.LoginRotateImageBo;
import com.jiuyu.replay.system.bo.LoginRotateImageListBo;
import com.jiuyu.replay.system.producer.LoginRotateImageProducer;
import com.jiuyu.replay.system.vo.LoginRotateImageInfoVo;
import com.jiuyu.replay.system.vo.LoginRotateImageListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Component
public class LoginRotateImageBll {

    @Resource
    private LoginRotateImageProducer loginRotateImageProducer;


    /**
     * 客户端登录页轮播图列表
     * @param loginRotateImageListBo 客户端登录页轮播图列表查询参数
     * @return
     */
    public R<PageUtils<LoginRotateImageListVo>> queryPage(LoginRotateImageListBo loginRotateImageListBo) {

        return R.ok("获取成功", loginRotateImageProducer.queryPage(loginRotateImageListBo));
    }

    public R<List<LoginRotateImageListVo>> noPage() {

        return R.ok("获取成功", loginRotateImageProducer.noPage());
    }



    /**
    * 客户端登录页轮播图信息
    * @param id 客户端登录页轮播图id
    * @return
    */
    public R<LoginRotateImageInfoVo> info(Long id) {

        LoginRotateImageInfoVo loginRotateImageInfoVo = loginRotateImageProducer.info(id);
        return R.ok("获取成功", loginRotateImageInfoVo);
    }

    /**
     * 新增客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
    public R<String> save(LoginRotateImageBo loginRotateImageBo) {

        LoginRotateImageInfoVo loginRotateImageInfoVo = loginRotateImageProducer.save(loginRotateImageBo);
        return R.ok("添加成功");
    }

    /**
     * 修改客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
    public R<String> update(LoginRotateImageBo loginRotateImageBo) {

        loginRotateImageProducer.update(loginRotateImageBo);
        return R.ok("修改成功");
    }

    /**
     * 删除客户端登录页轮播图
     * @param id 客户端登录页轮播图id
     * @return
     */
    public R<String> delete(Long id) {

        loginRotateImageProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

