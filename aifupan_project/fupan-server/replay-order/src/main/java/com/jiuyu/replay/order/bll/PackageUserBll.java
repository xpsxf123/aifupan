package com.jiuyu.replay.order.bll;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.bo.PackageUserAddsBo;
import com.jiuyu.replay.order.bo.PackageUserListBo;
import com.jiuyu.replay.order.entity.PackageUserEntity;
import com.jiuyu.replay.order.producer.PackageUserProducer;
import com.jiuyu.replay.order.vo.PackageUserVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义版本用户业务逻辑层
 */
@Component
@AllArgsConstructor
@Slf4j
public class PackageUserBll {

    private final PackageUserProducer packageUserProducer;
    private final UserFeign userFeign;

    /**
     * 分页查询自定义版本用户
     */
    public R<PageUtils<PackageUserVo>> queryPage(PackageUserListBo listBo) {
        return R.ok("获取成功", packageUserProducer.queryPage(listBo));
    }

    /**
     * 批量添加自定义版本用户
     * 解析手机号列表，校验手机号格式，查询用户信息（userType=0），批量添加
     *
     * @param addsBo 添加参数
     * @return 添加结果
     */
    public String adds(PackageUserAddsBo addsBo) {
        // 解析手机号列表，支持分号、逗号和换行符分隔
        List<String> phoneListAll = Stream.of(addsBo.getPhones().split("[;,\r\n]+"))
                .map(String::trim)
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .toList();

        if (ObjectUtil.isEmpty(phoneListAll)) {
            throw new BusinessException("没有识别到正确的手机号");
        }

        // 过滤出符合手机号格式的号码
        List<String> phoneList = phoneListAll.stream().filter(PhoneUtil::isPhone).toList();

        if (ObjectUtil.isEmpty(phoneList)) {
            throw new BusinessException("没有识别到正确的手机号，请检查手机号格式");
        }

        // 根据手机号查询用户信息
        List<UserDto> userDtos = userFeign.listByPhones(phoneList);

        // 只保留 userType=0 的普通用户
        List<UserDto> validUsers = ObjectUtil.isEmpty(userDtos)
                ? new ArrayList<>()
                : userDtos.stream().filter(u -> u.getUserType() != null && u.getUserType() == 0).toList();

        // 构建用户映射
        Map<Long, UserDto> userMap = validUsers.stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity(), (a, b) -> a));

        // 找出无法匹配到用户的手机号（格式不对 + 未注册 + 非普通用户）
        List<String> errorPhone = phoneListAll.stream()
                .filter(item -> validUsers.stream().noneMatch(val -> val.getPhone().equals(item)))
                .toList();

        List<Long> userIds = new ArrayList<>(userMap.keySet());

        if (ObjectUtil.isEmpty(userIds)) {
            String msg = "没有匹配到符合条件的普通用户(userType=0)";
            if (ObjectUtil.isNotEmpty(errorPhone)) {
                msg += StrUtil.format("，失败手机号: \n{}", StrUtil.join("\n", errorPhone));
            }
            throw new BusinessException(msg);
        }

        // 查询已存在的记录，避免重复添加
        List<PackageUserVo> existList = packageUserProducer.listByPackageIdAndUserIds(addsBo.getPackageId(), userIds);
        Set<Long> existUserIds = existList.stream().map(PackageUserVo::getUserId).collect(Collectors.toSet());

        // 过滤掉已存在的用户
        List<Long> toAddUserIds = userIds.stream().filter(id -> !existUserIds.contains(id)).toList();

        if (ObjectUtil.isNotEmpty(toAddUserIds)) {
            Date now = new Date();
            List<PackageUserEntity> entities = toAddUserIds.stream().map(userId -> {
                UserDto user = userMap.get(userId);
                PackageUserEntity entity = new PackageUserEntity();
                entity.setId(SnowflakeManager.nextValue());
                entity.setPackageId(addsBo.getPackageId());
                entity.setUserId(userId);
                entity.setTenantId(user.getActiveTenantId() != null ? user.getActiveTenantId() : 0L);
                entity.setCreateDate(now);
                entity.setUpdateDate(now);
                entity.setIsDeleted(0);
                return entity;
            }).toList();

            packageUserProducer.saveBatch(entities);
        }

        int successCount = toAddUserIds.size();
        int existCount = existUserIds.size();

        StringBuilder result = new StringBuilder();
        result.append(StrUtil.format("添加成功, 去重后总数量：{}, 新增数量: {}", phoneListAll.size(), successCount));
        if (existCount > 0) {
            result.append(StrUtil.format(", 已存在数量: {}", existCount));
        }
        if (ObjectUtil.isNotEmpty(errorPhone)) {
            result.append(StrUtil.format(", 失败数量: {}, 失败手机号: \n{}", errorPhone.size(), StrUtil.join("\n", errorPhone)));
        }
        return result.toString();
    }

    /**
     * 根据ID列表批量删除
     */
    public void deleteByIds(List<Long> ids) {
        packageUserProducer.deleteByIds(ids);
    }
}
