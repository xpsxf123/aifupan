package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.common.aspect.lock.repeatsubmit.NoRepeatSubmit;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.words.bll.ScriptMonitorStandardScriptBll;
import com.jiuyu.replay.words.bo.script.ConfirmStandardScriptBo;
import com.jiuyu.replay.words.bo.script.GenerateStandardScriptBo;
import com.jiuyu.replay.words.vo.script.StandardScriptConfirmVo;
import com.jiuyu.replay.words.vo.script.StandardScriptDetailVo;
import com.jiuyu.replay.words.vo.script.StandardScriptVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 话术还原度标准稿接口（T21 生成 / T23 确认 / T24 查详情）。
 *
 * <p>与 {@link ScriptMonitorController} 同 prefix {@code replay/script-monitor}，
 * 独立类以保持职责分离（标准稿配置 vs 监控报告查询）。</p>
 *
 * <p>Controller 禁止接收外部 userId / tenantId 参数；全部从 JWT 取。</p>
 *
 * @author beta
 * @date 2026-06-11
 */
@RestController
@CrossOrigin
@RequestMapping("replay/script-monitor")
@Tag(name = "话术还原度标准稿")
@AllArgsConstructor
public class StandardScriptController {

    private final ScriptMonitorStandardScriptBll scriptMonitorStandardScriptBll;

    /**
     * T21 — 同步 AI 生成标准稿时间轴（不落库）。
     *
     * <p>调用 AI 生成，60s 超时；成功返回 timeAxisScript[]，失败抛 70008。
     * 加 {@code @NoRepeatSubmit} 防重复提交（AI 调用耗时，防多次触发）。</p>
     *
     * @param bo 生成请求入参（speechMode / speechSpeed / referenceScript 必填）
     * @return 生成结果，不含 standardScriptId
     */
    @PostMapping("/generateStandardScript")
    @Operation(summary = "生成标准稿（AI 同步调用，不落库）")
    @NoRepeatSubmit(key = "'generateStandardScript'")
    public R<StandardScriptVo> generateStandardScript(@Validated @RequestBody GenerateStandardScriptBo bo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return R.ok(scriptMonitorStandardScriptBll.generateStandardScript(bo, user.getId(), user.getActiveTenantId()));
    }

    /**
     * T23 — 确认标准稿落库（同一 (tenant, user, secuid) 永远只一行）。
     *
     * <p>按 (tenantId+userId+secUid) 联合定位：已存在则 updateById 内容字段，未存在则 INSERT 新稿（Snowflake）。
     * standardScriptId 跨 confirm 稳定（首次分配后不变），JSON 序列化为 String。</p>
     *
     * @param bo 确认请求入参（secUid / speechMode / speechSpeed / referenceScript / timeAxisScript 必填）
     * @return 确认结果（含 standardScriptId，UPDATE 路径返回既存 id）
     */
    @PostMapping("/confirmStandardScript")
    @Operation(summary = "确认标准稿落库")
    @NoRepeatSubmit(key = "'confirmStandardScript'")
    public R<StandardScriptConfirmVo> confirmStandardScript(@Validated @RequestBody ConfirmStandardScriptBo bo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return R.ok(scriptMonitorStandardScriptBll.confirmStandardScript(bo, user.getId(), user.getActiveTenantId()));
    }

    /**
     * T24 — 查询标准稿详情。
     *
     * <p>按 (tenantId+userId+secUid) 联合查询；无稿返回 hasScript=false + speechSpeed=280。</p>
     *
     * @param secUid 主播唯一标识
     * @return 标准稿详情
     */
    @GetMapping("/standardScriptDetail")
    @Operation(summary = "查询标准稿详情")
    public R<StandardScriptDetailVo> standardScriptDetail(@RequestParam String secUid) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return R.ok(scriptMonitorStandardScriptBll.standardScriptDetail(user.getActiveTenantId(), user.getId(), secUid));
    }
}
