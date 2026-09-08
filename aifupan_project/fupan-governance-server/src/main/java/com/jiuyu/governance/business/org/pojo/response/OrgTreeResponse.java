package com.jiuyu.governance.business.org.pojo.response;

import com.jiuyu.governance.common.pojo.bo.LabelOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 组织架构响应
 *
 * @author HeHui
 * @date 2026-03-26 10:21
 */
@Getter
@Setter
public class OrgTreeResponse extends LabelOption {


    /**
     * 部门
     */
    private List<DeptOption> children;


    /**
     * 部门
     */
    @Getter
    @Setter
    public static class DeptOption extends LabelOption {

        /**
         * 小组
         */
        private List<LabelOption> children;
    }
}
