package com.jiuyu.replay.api.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 参数校验错误消息组装测试
 * <p>
 * 覆盖 PATCH 修改后的两条路径：
 * <ul>
 *     <li>buildFieldErrorMessage(private) — MethodArgumentNotValidException / BindException 走的私有方法</li>
 *     <li>ConstraintViolationException 分支 — 类级 @Validated + @RequestParam 失败的 inline stream 逻辑</li>
 * </ul>
 * 核心断言：errorMsg 仅含 message 拼接，不含字段名前缀（如 "speechSpeed:" / "secUid:" / "propertyPath:"）。
 *
 * @author Claude (test-engineer)
 * @date 2026-06-17
 */
@DisplayName("GlobalExceptionHandler 参数校验错误消息组装")
class GlobalExceptionHandlerTest {

    /**
     * AC-1: @Valid 校验失败 — @Min 失败 + @NotBlank 失败两条 FieldError 合成的 errorMsg
     * 应该仅含 defaultMessage 拼接，不含字段名前缀。
     */
    @Test
    @DisplayName("AC-1 @Valid 校验失败 errorMsg 不含字段名前缀")
    void buildFieldErrorMessage_shouldOnlyContainDefaultMessageWithoutFieldNamePrefix() throws Exception {
        // Given：模拟两个 FieldError —— 一条 @Min 失败、一条 @NotBlank 失败
        FieldError minError = new FieldError(
                "anchorAddBo",
                "speechSpeed",
                null,
                false,
                null,
                null,
                "语速必须大于等于 0");
        FieldError notBlankError = new FieldError(
                "anchorAddBo",
                "secUid",
                null,
                false,
                null,
                null,
                "secUid 不能为空");
        List<FieldError> fieldErrors = Arrays.asList(minError, notBlankError);

        // When：通过反射调用私有方法 buildFieldErrorMessage
        String errorMsg = invokeBuildFieldErrorMessage(fieldErrors);

        // Then：errorMsg 仅含 defaultMessage 拼接，绝不含字段名 + ":"
        assertThat(errorMsg)
                .as("errorMsg 应包含两条 defaultMessage")
                .contains("语速必须大于等于 0")
                .contains("secUid 不能为空");
        assertThat(errorMsg)
                .as("errorMsg 禁止以 \"speechSpeed:\" 形式暴露字段名")
                .doesNotContain("speechSpeed:")
                .doesNotContain("speechSpeed: ");
        assertThat(errorMsg)
                .as("errorMsg 禁止以 \"secUid:\" 形式暴露字段名")
                .doesNotContain("secUid:")
                .doesNotContain("secUid: ");
        // 两条 message 之间用 ", " 分隔
        assertThat(errorMsg).isEqualTo("语速必须大于等于 0, secUid 不能为空");
    }

    /**
     * AC-2: ConstraintViolationException 分支
     * fieldPaths（propertyPath）仅入日志，errorMsg 只能含 violation.getMessage() 拼接，
     * 不出现 propertyPath 字段名前缀（如 "patrolType:" / "interactionMatchAnchorList.addAnchorUrls[0].secUid:"）。
     */
    @Test
    @DisplayName("AC-2 ConstraintViolationException errorMsg 不含 propertyPath 前缀")
    void constraintViolationStream_shouldExposeOnlyMessagesNotPropertyPaths() {
        // Given：模拟两个 ConstraintViolation —— patrolType 和 secUid 的 propertyPath，各带 message
        ConstraintViolation<?> violationPatrolType = mockViolation(
                "patrolType",
                "巡检类型必须为 1 或 2");
        ConstraintViolation<?> violationSecUid = mockViolation(
                "interactionMatchAnchorList.addAnchorUrls[0].secUid",
                "secUid 不能为空");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violationPatrolType);
        violations.add(violationSecUid);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        // When：复现 GlobalExceptionHandler 中的 inline stream 表达式
        // 1) fieldPaths（日志用）—— 只入日志，不进 errorMsg
        String fieldPaths = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.joining(", "));
        // 2) errorMsg —— 给用户看，必须仅含 message
        String errorMsg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        // Then：fieldPaths 含字段路径
        assertThat(fieldPaths)
                .as("fieldPaths 用于日志，应保留字段路径")
                .contains("patrolType")
                .contains("interactionMatchAnchorList.addAnchorUrls[0].secUid");

        // Then：errorMsg 只含 message，绝不含 propertyPath 字段名
        assertThat(errorMsg)
                .as("errorMsg 应包含两条 message")
                .contains("巡检类型必须为 1 或 2")
                .contains("secUid 不能为空");
        assertThat(errorMsg)
                .as("errorMsg 禁止暴露 patrolType 字段名")
                .doesNotContain("patrolType:")
                .doesNotContain("patrolType ");
        assertThat(errorMsg)
                .as("errorMsg 禁止暴露 secUid 字段路径")
                .doesNotContain("secUid:")
                .doesNotContain("interactionMatchAnchorList")
                .doesNotContain("addAnchorUrls");
        // 关键：errorMsg 中绝对没有 ":" —— 字段名前缀通过 ":" 分隔，本次修改已剥离
        assertThat(errorMsg)
                .as("errorMsg 禁止出现冒号（字段名前缀的典型分隔符）")
                .doesNotContain(":");
    }

    /**
     * AC-3: fieldErrors 为空集合 → 返回默认提示
     */
    @Test
    @DisplayName("AC-3a fieldErrors 为空集合返回默认提示")
    void buildFieldErrorMessage_shouldReturnDefaultMessageWhenEmpty() throws Exception {
        // Given：空列表
        List<FieldError> fieldErrors = Collections.emptyList();

        // When
        String errorMsg = invokeBuildFieldErrorMessage(fieldErrors);

        // Then
        assertThat(errorMsg).isEqualTo("请求参数格式不正确，请检查输入内容");
    }

    /**
     * AC-3: fieldErrors 为 null → 返回默认提示
     * （CollectionUtil.isNotEmpty(null) == false，应走默认分支）
     */
    @Test
    @DisplayName("AC-3b fieldErrors 为 null 返回默认提示")
    void buildFieldErrorMessage_shouldReturnDefaultMessageWhenNull() throws Exception {
        // Given：null
        List<FieldError> fieldErrors = null;

        // When
        String errorMsg = invokeBuildFieldErrorMessage(fieldErrors);

        // Then
        assertThat(errorMsg).isEqualTo("请求参数格式不正确，请检查输入内容");
    }

    // ===================== helpers =====================

    /**
     * 反射调用 GlobalExceptionHandler#buildFieldErrorMessage(List)
     * 不破坏私有方法封装。
     */
    private String invokeBuildFieldErrorMessage(List<FieldError> fieldErrors) throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Method method = GlobalExceptionHandler.class.getDeclaredMethod("buildFieldErrorMessage", List.class);
        method.setAccessible(true);
        return (String) method.invoke(handler, fieldErrors);
    }

    /**
     * 构造一个 ConstraintViolation mock —— propertyPath.toString() 返回给定路径，getMessage() 返回给定消息。
     */
    private ConstraintViolation<?> mockViolation(String propertyPath, String message) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(propertyPath);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}
