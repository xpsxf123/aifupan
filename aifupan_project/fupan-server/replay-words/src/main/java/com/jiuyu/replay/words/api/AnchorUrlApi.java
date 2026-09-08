package com.jiuyu.replay.words.api;

import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.feign.words.AnchorUrlFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.bll.AnchorUrlBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/3 下午2:39
 */
@Component
@AllArgsConstructor
public class AnchorUrlApi implements AnchorUrlFeign {

    private final AnchorUrlBll anchorUrlBll;

    @Override
    public AnchorUrlUserVo getUserAnchorBySecUid(String secUid, Long userId, Long tenantId) {
        R<AnchorUrlUserVo> userAnchorBySecUid = anchorUrlBll.getUserAnchorBySecUid(secUid, userId, tenantId);

        if (userAnchorBySecUid.getCode() == 0 && userAnchorBySecUid.getData() != null){
            return userAnchorBySecUid.getData();
        }
        RRException.create(userAnchorBySecUid);
        return null;
    }

    @Override
    public void minusDataDiagnosisGenerateNum(String secUid, String videoId, Long userId, Long tenantId, int num) {
        anchorUrlBll.minusDataDiagnosisGenerateNum(secUid, videoId, userId, tenantId, num);
    }

    @Override
    public List<AnchorUrlInfoVo> listBySecUids(List<String> secUids) {
        R<List<AnchorUrlInfoVo>> result = anchorUrlBll.listLiveBySecUids(secUids);
        if (result.getCode() == 0 && result.getData() != null) {
            return result.getData();
        }
        return new ArrayList<>();
    }
}
