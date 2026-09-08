package com.jiuyu.governance.business.org.pojo.request;

import com.jiuyu.framework.shandard.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 岗位分页查询请求
 *
 * @author HeHui
 * @date 2026-03-23 11:07
 */
@Getter
@Setter
public class PositionPageQueryRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = -6356652775091966526L;
    /**
     * 岗位名称
     */
    private String name;

}
