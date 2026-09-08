package com.jiuyu.replay.words.api;

import com.jiuyu.replay.generic.feign.words.BlessBagFeign;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.words.bll.BlessBagBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description：
 * @author     ：lujie
 * @date       ：2025/6/11 下午6:36
 */
@Service
@AllArgsConstructor
public class BlessBagApi implements BlessBagFeign {

    private final BlessBagBll blessBagBll;

    @Override
    public List<BlessBagInfoVo> listByVideoId(String videoId) {
        return blessBagBll.listByVideoId(videoId);
    }
}
