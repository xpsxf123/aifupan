package com.jiuyu.governance.business.performance.pojo.base;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 业绩核心指标基类（不含 ROI）
 * <p>
 * 统一管理 BO / Response / Export 层的10个核心业绩指标字段。
 * 新增业绩字段时只需在此基类添加一处，所有子类自动继承。
 * </p>
 * <p>
 * 子类若需要使用 Builder 模式，请用 {@code @SuperBuilder} 替代 {@code @Builder}。
 * </p>
 *
 * @author lj
 * @date 2026-04-02
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class BasePerformanceMetrics extends BasePerformanceEntity {
}
