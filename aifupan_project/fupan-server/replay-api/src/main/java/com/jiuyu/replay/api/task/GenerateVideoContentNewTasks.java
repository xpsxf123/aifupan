package com.jiuyu.replay.api.task;

import cn.hutool.core.util.ObjUtil;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自然/优化原文生成定时任务（新版：多线程分段并行）。
 *
 * <h3>与旧版的区别</h3>
 * 旧版 {@code GenerateVideoContentTasks} 使用单次 AI 调用生成全部原文，不分段、无重试。
 * 新版参考 C# 客户端 {@code VideoContentAuto.run} 的设计：
 * <ul>
 *   <li>分段生成：将原文按段落拆分，每段独立 AI 调用</li>
 *   <li>多线程并行：批次内所有分段通过线程池并行执行</li>
 *   <li>段级重试：每段最多重试 3 次</li>
 *   <li>格式纠正：每段 AI 生成后检查并纠正格式</li>
 *   <li>快速返回：定时器只负责拾取+提交，不等待 AI 结果</li>
 * </ul>
 *
 * <h3>XXL-Job 配置</h3>
 * JobHandler: generateVideoContentNew<br>
 * 参数：每次处理的条数上限（必填，建议 10~50）
 *
 * @author lujie
 * @date 2025/6/24
 */
@Component
@Slf4j
public class GenerateVideoContentNewTasks {

    @Resource
    private AnchorVideoLogic anchorVideoLogic;

    /**
     * 扫描视频和文件表中待生成自然/优化原文的记录，提交到线程池异步执行。
     */
    @XxlJob("generateVideoContent")
    public void generateVideoContentNew() {
        // 参数优先取方法入参，其次取 XXL-Job 任务配置中的参数
        String jobParam = XxlJobHelper.getJobParam();
        log.info("[定时任务] generateVideoContentNew 参数：{}", jobParam);
        try {
            Integer limit = Integer.valueOf(ObjUtil.defaultIfEmpty(jobParam, "10"));
            anchorVideoLogic.generateVideoContentNew(limit);
        } catch (NumberFormatException e) {
            log.error("[定时任务] generateVideoContentNew 参数格式错误：{}", jobParam, e);
        }
    }
}
