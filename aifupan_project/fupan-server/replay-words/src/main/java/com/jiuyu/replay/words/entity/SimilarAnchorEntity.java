package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jiuyu.framework.util.EmptyUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.Count;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 相似主播信息表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-10-16 15:40:25
 */
@Data
@TableName("tb_similar_anchor")
@Slf4j
public class SimilarAnchorEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 相似度
	 */
	private String similarScore;
	/**
	 * 平均UV价值
	 */
	private String liveAverageUv;
	/**
	 * 粉丝数
	 */
	private String followerCount;
	/**
	 * 平均场观
	 */
	private String liveAverageUser;
	/**
	 * 场均销售额
	 */
	private String liveAverageAmount;
	/**
	 * 直播销售总额
	 */
	private String totalAmount;
	/**
	 * 销售指数
	 */
	private String liveTotalAmountCmmInd;
	/**
	 * 直播场次
	 */
	private Integer liveCount;
	/**
	 * 平均停留时长
	 */
	private String liveAverageOnline;
	/**
	 * 蝉妈妈唯一账号ID
	 */
	private String authorId;
	/**
	 * 主播抖音账号
	 */
	private String uniqueId;
	/**
	 * 主播secUid
	 */
	private String secUid;
	/**
	 * 采集时间
	 */
	private Date collectDate;
	/**
	 * 主播名称
	 */
	private String anchorName;
	/**
	 * 主播头像
	 */
	private String anchorAvatar;
	/**
	 * 账号热度（整数）
	 */
	private Integer accountHeat;
	/**
	 * 总销售额（数值，单位：元）- 解析total_amount字段得到的最低销售额数值
	 */
	private Long totalAmountNumeric;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;

	/**
	 * 相似主播收藏ID（tb_similar_collect主键，非表字段，仅用于查询返回）
	 */
	@TableField(exist = false)
	private Long similarCollectId;




	/**
	 * 计算总销售额 并给totalAmountNumeric赋值
	 */
	public void calcTotalAmountToValue() {
		if (EmptyUtil.isEmpty(this.getTotalAmount())) {
			return;
		}
		this.setTotalAmountNumeric(parseTotalAmountToLong(this.totalAmount));
	}


	/**
	 * 计算账号热度
	 */
	public void calcAccountHeat() {
		Integer accountHeat = calculateAccountHeatForSave(this);
        this.setAccountHeat(accountHeat);
    }

	/**
	 * 基础惩罚函数值 10000
	 */
	private static final BigDecimal BASE_PENALTY_VALUE = BigDecimal.valueOf(10000);
	/**
	 * 粉丝量惩罚函数值 100
	 */
	private static final BigDecimal FANS_PENALTY_VALUE = BigDecimal.valueOf(100);

	/**
	 * 粉丝量惩罚系数 10
	 */
	private static final BigDecimal FANS_PENALTY_FACTOR = BigDecimal.TEN;

	/**
	 * 直播场次惩罚函数值 28
	 */
	private static final BigDecimal LIVE_COUNT_PENALTY_VALUE = BigDecimal.valueOf(28);

	/**
	 * 直播场次惩罚系数 50
	 */
	private static final BigDecimal LIVE_COUNT_PENALTY_FACTOR = BigDecimal.valueOf(50);

	/**
	 * 平均场观惩罚系数 50
	 */
	private static final BigDecimal LIVE_AVERAGE_USER_FACTOR = BigDecimal.valueOf(50);

	/**
	 * 最低直播销售额惩罚系数 200
	 */
	private static final BigDecimal TOTAL_AMOUNT_FACTOR = BigDecimal.valueOf(200);

	/**
	 * 计算账号热度（用于回调数据入库）
	 * 热度计算公式：账号热度（取整）=（100-粉丝量/10000）*10+（直播场次-28）*50+（平均场观/10000）*50+（最低直播销售额/场次/10000）*200
	 * <p>
	 * 边界条件处理：
	 * 1. 如果热度计算参数中出现负数或"-"，当前参数按照0处理，只影响对应的计算项
	 * 2. 如果按0处理的参数是分母（场次），只有涉及该分母的计算项（场均销售额项）按0处理，其他项正常计算
	 * 3. 场次为0时，场均销售额项按0处理（避免除0异常），其他项正常计算
	 * 4. 直播销售总额支持范围格式：0、1w~2.5w、7500w~1亿、1亿+、-（取范围最低值，+号会被去掉）
	 * 5. 如果计算结果小于等于0，按0处理（不出现负数）
	 * 6. 所有异常情况记录警告日志
	 *
	 * @param entity 相似主播实体
	 *
	 * @return 账号热度（整数，>=0），计算失败或数据异常返回0
	 *
	 * @author RayChou
	 * @date 2025-10-28
	 */
	private static Integer calculateAccountHeatForSave(SimilarAnchorEntity entity) {
		String anchorName = entity.getAnchorName();

		try {
			// 1. 解析粉丝量（负数或"-"按0处理）
			BigDecimal followerCount = parseNumericValueWithZeroFallback(entity.getFollowerCount(), "粉丝量", anchorName);

			// 2. 解析直播场次（负数或null按0处理）
			Integer liveCountInt = entity.getLiveCount();
			if (liveCountInt == null || liveCountInt < 0) {
				log.warn("[热度计算] ⚠️ 直播场次异常(null或负数)，主播: {}, 场次: {}，按0处理", anchorName, liveCountInt);
				liveCountInt = 0;
			}

			BigDecimal liveCount = BigDecimal.valueOf(liveCountInt);

			// 3. 解析平均场观（负数或"-"按0处理）
			BigDecimal liveAverageUser = parseNumericValueWithZeroFallback(entity.getLiveAverageUser(), "平均场观", anchorName);

			// 4. 解析直播销售总额（支持范围格式，负数或"-"按0处理）
			BigDecimal totalAmount;
			if (entity.getTotalAmountNumeric() != null && entity.getTotalAmountNumeric() > 0) {
				totalAmount = BigDecimal.valueOf(entity.getTotalAmountNumeric());
			} else {
				totalAmount = parseTotalAmountWithZeroFallback(entity.getTotalAmount(), anchorName);
			}

			// 5. 计算场均销售额（场次为0时，该项按0处理，避免除0异常）
			BigDecimal averageAmountPerLive;
			if (liveCountInt == 0) {
				log.warn("[热度计算] ⚠️ 直播场次为0，主播: {}，场均销售额项按0处理（避免除0异常）", anchorName);
				averageAmountPerLive = BigDecimal.ZERO;
			} else {
				averageAmountPerLive = totalAmount.divide(liveCount, 2, RoundingMode.HALF_UP);
			}

			// 6. 热度计算公式各部分
			// (100 - 粉丝量/10000) * 10
			BigDecimal part1 = FANS_PENALTY_VALUE
				.subtract(followerCount.divide(BASE_PENALTY_VALUE, 2, RoundingMode.HALF_UP))
				.multiply(FANS_PENALTY_FACTOR);

			// (直播场次 - 28) * 50
			BigDecimal part2 = liveCount.subtract(LIVE_COUNT_PENALTY_VALUE)
				.multiply(LIVE_COUNT_PENALTY_FACTOR);

			// (平均场观/10000) * 50
			BigDecimal part3 = liveAverageUser.divide(BASE_PENALTY_VALUE, 2, RoundingMode.HALF_UP)
				.multiply(LIVE_AVERAGE_USER_FACTOR);

			// (场均销售额/10000) * 200
			BigDecimal part4 = averageAmountPerLive.divide(BASE_PENALTY_VALUE, 2, RoundingMode.HALF_UP)
				.multiply(TOTAL_AMOUNT_FACTOR);

			// 总热度（取整）
			BigDecimal totalHeat = part1.add(part2).add(part3).add(part4);
			int heatValue = totalHeat.setScale(0, RoundingMode.HALF_UP).intValue();

			// 如果热度小于等于0，按0处理（不出现负数）
			if (heatValue <= 0) {
				log.warn("[热度计算] ⚠️ 计算热度为负数或0，主播: {}, 原始热度: {}，按0处理", anchorName, heatValue);
				return 0;
			}

			log.debug("[热度计算] ✓ 主播: {}, 热度: {}, 粉丝: {}, 场次: {}, 平均场观: {}, 销售总额: {}",
				anchorName, heatValue, followerCount, liveCount, liveAverageUser, totalAmount);

			return heatValue;

		} catch (Exception e) {
			log.warn("[热度计算] ⚠️ 计算失败，主播: {}, 错误: {}，热度按0处理", anchorName, e.getMessage());
			return 0;
		}
	}




	/**
	 * 解析数值字段（负数或"-"按0处理）
	 *
	 * @param value      字段值
	 * @param fieldName  字段名称
	 * @param anchorName 主播名称
	 *
	 * @return BigDecimal值，异常、负数或"-"返回0
	 */
	private static BigDecimal parseNumericValueWithZeroFallback(String value, String fieldName, String anchorName) {
		if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
			log.warn("[热度计算] ⚠️ {}为空或'-'，主播: {}, {}值: {}，按0处理", fieldName, anchorName, fieldName, value);
			return BigDecimal.ZERO;
		}

		try {
			BigDecimal result = parseAmountValue(value.trim());
			if (result.compareTo(BigDecimal.ZERO) < 0) {
				log.warn("[热度计算] ⚠️ {}为负数，主播: {}, {}值: {}，按0处理", fieldName, anchorName, fieldName, value);
				return BigDecimal.ZERO;
			}
			return result;
		} catch (NumberFormatException e) {
			log.warn("[热度计算] ⚠️ {}格式错误，主播: {}, {}值: {}，按0处理", fieldName, anchorName, fieldName, value);
			return BigDecimal.ZERO;
		}
	}

	/**
	 * 解析直播销售总额（支持范围格式，取最低值，负数或"-"按0处理）
	 *
	 * @param value      销售总额字符串
	 * @param anchorName 主播名称
	 *
	 * @return BigDecimal值（范围格式取最低值），异常、负数或"-"返回0
	 */
	private static BigDecimal parseTotalAmountWithZeroFallback(String value, String anchorName) {
		Tuple2<BigDecimal, BigDecimal> tuple2 = parseAmountAnyValue(value, anchorName);
		if (tuple2.getT1().compareTo(tuple2.getT2()) == 0) {
			return tuple2.getT1();
		}
		return tuple2.getT1().min(tuple2.getT2());
	}


	/**
	 * 解析金额字段（支持范围格式，取最低值）
	 * 支持格式：
	 * - 0（返回0）
	 * - 1w~2.5w（取最低值：1w = 10000）
	 * - 7500w~1亿（取最低值：7500w = 75000000）
	 * - 2500~5000（取最低值：2500）
	 * - 1~25（取最低值：1）
	 * - 1亿+（去掉+号：1亿 = 100000000）
	 * - 1亿（返回100000000）
	 * - 7500w（返回75000000）
	 * - -（返回0）
	 * - 普通数字
	 * - 负数（返回0）
	 *
	 * @param value      销售总额字符串
	 * @param anchorName 主播名称
	 *
	 * @return {@link Tuple2 }<{@link BigDecimal }, {@link BigDecimal }> 范围值，左最小值、右最大值。 如果左右相等表示不是范围值
	 */
	public static Tuple2<BigDecimal, BigDecimal> parseAmountAnyValue(String value, String anchorName) {
		if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
			log.warn("[热度计算] ⚠️ 直播销售总额为空或'-'，主播: {}, 销售总额: {}，按0处理", anchorName, value);
			return Tuples.of(BigDecimal.ZERO, BigDecimal.ZERO);
		}

		try {
			String trimmedValue = value.trim();
			String splitStr = "~";
			if  (trimmedValue.contains("-")) {
				splitStr = "-";
			} else if (trimmedValue.contains("至")) {
				splitStr = "至";
			} else if (trimmedValue.contains("到")) {
				splitStr = "到";
			}
			// 处理范围格式：1w~2.5w、2500~5000、1~25（取最低值）
			if (trimmedValue.contains(splitStr)) {
				String[] parts = trimmedValue.split(splitStr);
				if (parts.length == 2) {
					// 取范围的最低值（最小值）
					BigDecimal min = parseAmountValue(parts[0].trim());
					BigDecimal max = parseAmountValue(parts[1].trim());

					if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.ZERO) < 0) {
						log.warn("[热度计算] ⚠️ 直播销售总额范围包含负数，主播: {}, 销售总额: {}，按0处理", anchorName, value);
						return Tuples.of(BigDecimal.ZERO, BigDecimal.ZERO);
					}

					// 使用最低值（最小值）
					BigDecimal minValue = min.min(max);
					log.debug("[热度计算] 解析销售总额范围，主播: {}, 原值: {}, 最低值: {} (min={}, max={})",
						anchorName, value, minValue, min, max);
					return Tuples.of(minValue, max);
				}
			}

			// 处理普通数字（可能带w）
			BigDecimal result = parseAmountValue(trimmedValue);
			if (result.compareTo(BigDecimal.ZERO) < 0) {
				log.warn("[热度计算] ⚠️ 直播销售总额为负数，主播: {}, 销售总额: {}，按0处理", anchorName, value);
				return Tuples.of(BigDecimal.ZERO, BigDecimal.ZERO);
			}
			return Tuples.of(result, result);

		} catch (Exception e) {
			log.warn("[热度计算] ⚠️ 直播销售总额格式错误，主播: {}, 销售总额: {}, 错误: {}，按0处理",
				anchorName, value, e.getMessage());
			return Tuples.of(BigDecimal.ZERO, BigDecimal.ZERO);
		}
	}

	/**
	 * 解析金额值（支持w、亿、+等单位）
	 * 支持格式：
	 * - 0（返回0）
	 * - 1w（返回10000）
	 * - 2.5w（返回25000）
	 * - 1亿（返回100000000）
	 * - 7500w（返回75000000）
	 * - 1亿+（去掉+号，返回100000000）
	 * - 普通数字（返回原值）
	 *
	 * @param value 金额字符串
	 *
	 * @return BigDecimal值
	 */
	private static BigDecimal parseAmountValue(String value) {
		String trimmedValue = value.trim();

		// 处理0
		if ("0".equals(trimmedValue)) {
			return BigDecimal.ZERO;
		}

		// 处理带+号的格式（例如：1亿+）
		if (trimmedValue.endsWith("+")) {
			trimmedValue = trimmedValue.substring(0, trimmedValue.length() - 1).trim();
		}

		// 处理亿单位（例如：1亿、1.5亿）
		if (trimmedValue.endsWith("亿")) {
			String numStr = trimmedValue.substring(0, trimmedValue.length() - 1);
			return new BigDecimal(numStr).multiply(BigDecimal.valueOf(100000000));
		}

		// 处理w单位（例如：1w、2.5w、7500w）
		if (trimmedValue.toLowerCase().endsWith("w")) {
			String numStr = trimmedValue.substring(0, trimmedValue.length() - 1);
			return new BigDecimal(numStr).multiply(BigDecimal.valueOf(10000));
		}
		if (trimmedValue.endsWith("万")) {
			String numStr = trimmedValue.substring(0, trimmedValue.length() - 1);
			return new BigDecimal(numStr).multiply(BigDecimal.valueOf(10000));
		}
		if (trimmedValue.toLowerCase().endsWith("k")) {
			String numStr = trimmedValue.substring(0, trimmedValue.length() - 1);
			return new BigDecimal(numStr).multiply(BigDecimal.valueOf(1000));
		}
		if (trimmedValue.endsWith("千")) {
			String numStr = trimmedValue.substring(0, trimmedValue.length() - 1);
			return new BigDecimal(numStr).multiply(BigDecimal.valueOf(1000));
		}

		// 普通数字
		return new BigDecimal(trimmedValue);
	}

	/**
	 * 解析总销售额字符串为Long类型数值（单位：元）
	 * 支持格式：
	 * - 0（返回0）
	 * - 1w~2.5w（取最低值：1w = 10000）
	 * - 7500w~1亿（取最低值：7500w = 75000000）
	 * - 2500~5000（取最低值：2500）
	 * - 1~25（取最低值：1）
	 * - 1亿+（去掉+号：1亿 = 100000000）
	 * - 1亿（返回100000000）
	 * - 7500w（返回75000000）
	 * - -（返回0）
	 * - 普通数字
	 * - 负数（返回0）
	 *
	 * @param totalAmount 总销售额字符串
	 *
	 * @return Long值（单位：元），解析失败或负数返回0
	 *
	 * @author RayChou
	 * @date 2025-11-12
	 */
	private static Long parseTotalAmountToLong(String totalAmount) {
		if (totalAmount == null || totalAmount.trim().isEmpty() || "-".equals(totalAmount.trim())) {
			return 0L;
		}

		try {
			// 复用现有的 parseTotalAmountWithZeroFallback 方法（返回 BigDecimal）
			BigDecimal result = parseTotalAmountWithZeroFallback(totalAmount, "");

			// 转换为 Long（四舍五入）
			return result.setScale(0, RoundingMode.HALF_UP).longValue();
		} catch (Exception e) {
			log.warn("[解析总销售额] ⚠️ 解析失败，totalAmount: {}, 错误: {}，返回0", totalAmount, e.getMessage());
			return 0L;
		}
	}

	/**
	 * 解析字符串为Long类型
	 *
	 * @param value 字符串值
	 *
	 * @return Long值，解析失败返回0
	 */
	public static Long parseLong(String value) {
		if (value == null || value.trim().isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}



	/**
	 * 验证额格式是否合法
	 * 支持格式：
	 * - 0
	 * - 普通数字：12345
	 * - 带单位：1w、2.5w、7500w、1亿、1.5亿、1亿+
	 * - 范围格式：1w~2.5w、2500~5000、7500w~1亿
	 * - 特殊值：-、空字符串
	 *
	 * @param totalAmount 直播销售总额字符串
	 * @return true表示格式合法，false表示格式非法
	 */
	public static boolean validateValueFormat(String totalAmount, String anchorName, String fieldName, boolean allowSplit) {
		if (totalAmount == null) {
			return true; // null视为合法（可选字段）
		}

		// 空字符串或特殊符号视为合法
		if (totalAmount.trim().isEmpty() || "-".equals(totalAmount.trim())) {
			return true;
		}
		if (totalAmount.equals("0")) {
			return true;
		}

		String[] rangeParts = {"~", "-", "至", "到"};
		String splitStr =  "";
		boolean isRangeFormat = false;
		if (allowSplit) {
			for (String rangePart : rangeParts) {
				if (totalAmount.contains(rangePart)) {
					isRangeFormat = true;
					splitStr = rangePart;
					break;
				}
			}
		}
		try {
			// 尝试解析数值，如果能成功解析则格式合法
			if (isRangeFormat) {
				// 检查是否为范围格式
				String trimmedValue = totalAmount.trim();
				String[] parts = trimmedValue.split(splitStr);
				if (parts.length == 2) {
					try {
						// 分别验证范围的两部分
						BigDecimal start = parseAmountValue(parts[0].trim());
						if (start.compareTo(BigDecimal.ZERO) == 0 && !parts[0].trim().equals("0")) {
							return false;
						}
						BigDecimal end = parseAmountValue(parts[1].trim());
                        return end.compareTo(BigDecimal.ZERO) != 0 || parts[1].trim().equals("0");
                    } catch (Exception ex) {
						log.warn("[验证格式] {} {}: {} 格式错误: {}", anchorName, fieldName, totalAmount,ex.getMessage());
						return false;
					}
				}
				return false;
			} else {
				BigDecimal value = parseAmountValue(totalAmount.trim());
                return value.compareTo(BigDecimal.ZERO) != 0 || totalAmount.trim().equals("0");
            }
		} catch (Exception e) {
			log.warn("[验证格式] {} {}: {} 错误: {}", anchorName, fieldName, totalAmount, e.getMessage());
			return false;
		}
	}

}
