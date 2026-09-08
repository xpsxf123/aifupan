package com.jiuyu.replay.generic.utils;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;

/**
 * @author ：lujie
 * @description：返回结果的工具类
 * @date ：2025/5/27 上午10:14
 */
public class ResultUtil {

    /**
     * 获取结果
     * @param r
     * @return
     * @param <T>
     */
    public static <T> T getResult(R<T> r){
        if (r == null) {
            throw new BusinessException(StatusCode.JWT_TOKEN_EXPIRED);
        }
        if (r.getCode() == 0){
            return r.getData();
        }else{
            return null;
        }
    }

    /**
     * 获取用户信息
     * 获取失败报错 @{@link StatusCode.JWT_TOKEN_EXPIRED}
     *
     * @param r 用户信息
     * @return 返回登录信息
     */
    public static UserCacheVo getUserResult(R<UserCacheVo> r) {
        if (r == null) {
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX);
        }
        if (r.getCode() == 0 && r.getData() != null) {
            return r.getData();
        } else {
            throw new BusinessException(StatusCode.JWT_TOKEN_EXPIRED);
        }
    }

}
