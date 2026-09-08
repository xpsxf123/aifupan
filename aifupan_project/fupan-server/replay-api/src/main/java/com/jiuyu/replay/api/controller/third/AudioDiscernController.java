package com.jiuyu.replay.api.controller.third;

import com.jiuyu.replay.api.bo.audio.AsrEngineLanguage;
import com.jiuyu.replay.api.logic.third.AudioDiscernLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.vo.TencentTempTokenVo;
import com.jiuyu.replay.common.vo.CheckSurplusVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.third.repository.service.AsrEngineConfigService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 语音识别
 */
@Slf4j
@RestController
@CrossOrigin
@RequestMapping("/replay/audio")
public class AudioDiscernController {

    @Resource
    private AudioDiscernLogic audioDiscernLogic;

    /**
     * ASR 引擎按租户 / 用户 / 语言优先级配置 Service。
     */
    @Resource
    private AsrEngineConfigService asrEngineConfigService;

    /**
     * 默认兜底引擎，未登录或未命中任何配置时返回。
     */
    private static final String DEFAULT_ASR_ENGINE = "sense-voice";

    /**
     * 查询是否还有调用语音识别接口的余量
     * @return
     */
    @GetMapping("/checkSurplus")
    @Operation(summary = "查询是否还有调用语音识别接口的余量")
    public R<CheckSurplusVo> checkSurplus() {

        return audioDiscernLogic.checkSurplus();
    }

    /**
     * 通知加回语音识别接口的余量
     * @param id 补回余量的id
     * @param secretId 补回余量的secretId
     * @return
     */
    @Operation(summary = "通知加回语音识别接口的余量")
    @GetMapping("/addSurplus")
    public R<String> addSurplus(@RequestParam Long id, @RequestParam String secretId) {

        return audioDiscernLogic.addSurplus(id, secretId);

    }

    /**
     * 获取一句话语音识别接口临时调用凭证
     * @param secretId 语音识别secretId
     * @return
     */
    @Operation(summary = "获取一句话语音识别接口临时调用凭证")
    @GetMapping("/getTempToken")
    public R<TencentTempTokenVo> getTempToken(@RequestParam(required = false) String secretId) throws TencentCloudSDKException {

        return audioDiscernLogic.getTempToken(secretId);
    }

    /**
     * 获取长音频识别接口临时调用凭证
     * @return
     */
    @Operation(summary = "获取长音频识别接口临时调用凭证")
    @GetMapping("/getRecTempToken")
    public R<TencentTempTokenVo> getRecTempToken() throws TencentCloudSDKException {

        return audioDiscernLogic.getRecTempToken();
    }

    /**
     * 记录QPS访问
     * @return
     */
    @GetMapping("/record")
    @Operation(summary = "记录QPS访问")
    public R<String> record() {

        audioDiscernLogic.record();

        return R.ok();
    }


    /**
     * 获取语音识别引擎列表。
     *
     * <p>未登录（{@code localUser == null}）时直接返回兜底 {@code sense-voice}；
     * 已登录时按 {@link AsrEngineConfigService#resolveEngines} 的优先级匹配
     * （userId+language &gt; userId &gt; tenant+language &gt; tenant），全部未命中再兜底。</p>
     *
     * @param language 请求体，可只携带语言编码
     * @return {@link R }<{@link List }<{@link String }>>
     */
    @PostMapping("/asr-engine")
    public R<List<String>> asrEngine(@RequestBody AsrEngineLanguage language) {
        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (localUser == null) {
            return R.ok(List.of(DEFAULT_ASR_ENGINE));
        }
        if (Objects.equals(language.getBusiness(), "short_video")) {
            return R.ok(List.of("tencent"));
        }
        List<String> engines = asrEngineConfigService.resolveEngines(
                localUser.getActiveTenantId(),
                localUser.getId(),
                language.getLanguage());
        if (engines == null || engines.isEmpty()) {
            // 16k_zh 16k_zh-PY 走sense-voice + 腾讯云兜底
            if (Objects.equals(language.getLanguage(), "16k_zh") || Objects.equals(language.getLanguage(), "16k_zh-PY")) {
                return R.ok(List.of(DEFAULT_ASR_ENGINE,"tencent"));
            }
            return R.ok(List.of("tencent"));
        }
        return R.ok(engines);
    }

}
