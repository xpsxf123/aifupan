package com.jiuyu.replay.system.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.system.bo.LoginRotateImageBo;
import com.jiuyu.replay.system.bo.LoginRotateImageListBo;
import com.jiuyu.replay.system.vo.LoginRotateImageInfoVo;
import com.jiuyu.replay.system.vo.LoginRotateImageListVo;

import java.util.List;


/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
public interface LoginRotateImageProducer {


    /**
     * 客户端登录页轮播图列表
     * @param loginRotateImageListBo 客户端登录页轮播图列表查询参数
     * @return
     */
    PageUtils<LoginRotateImageListVo> queryPage(LoginRotateImageListBo loginRotateImageListBo);

    List<LoginRotateImageListVo> noPage();



    /**
    * 客户端登录页轮播图信息
    * @param id 客户端登录页轮播图id
    * @return
    */
    LoginRotateImageInfoVo info(Long id);

    /**
     * 新增客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
     LoginRotateImageInfoVo save(LoginRotateImageBo loginRotateImageBo);

    /**
     * 修改客户端登录页轮播图
     * @param loginRotateImageBo 客户端登录页轮播图对象
     * @return
     */
    void update(LoginRotateImageBo loginRotateImageBo);

    /**
     * 删除客户端登录页轮播图
     * @param id 客户端登录页轮播图id
     * @return
     */
    void deleteById(Long id);


}

