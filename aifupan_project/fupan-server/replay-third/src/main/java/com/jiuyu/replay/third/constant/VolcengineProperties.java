package com.jiuyu.replay.third.constant;

import com.jiuyu.replay.generic.vo.third.VolcengineVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：lujie
 * @description：火山引擎配置
 * @date ：2025/2/20 下午8:06
 */
@Component
@Data
@ConfigurationProperties(prefix = "third.volcengine.ark")
public class VolcengineProperties extends VolcengineVo {

}
