package com.jiuyu.replay.third.shlianlu;

import lombok.Data;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/21 14:14
 */
@Data
public class LianLuConfigDto {

    /**
     * 模版参数
     */
    private List<String> templateParam;

    /**
     * 模版id
     */
    private Integer templateId;

}
