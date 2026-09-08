package com.jiuyu.replay.common.bll;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleAddsBo;
import com.jiuyu.replay.common.bo.userGrayscale.UserGrayscaleBo;
import com.jiuyu.replay.common.producer.UserGrayscaleProduct;
import com.jiuyu.replay.generic.dto.power.UserDto;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 18:20
 */
@Service
@AllArgsConstructor
public class UserGrayscaleBll {

    private final UserGrayscaleProduct userGrayscaleProduct;
    private final UserFeign userFeign;


    /**
     * 批量添加灰度用户
     * 解析输入的手机号列表，验证手机号有效性，查询用户信息，并批量添加到灰度名单中
     *
     * @param addsBo 灰度用户添加参数对象，包含版本ID和手机号列表
     * @return 返回添加结果信息，包含成功数量、失败数量及失败的手机号列表
     * @throws BusinessException 当没有识别到正确的手机号或用户不存在时抛出
     */
    public String adds(UserGrayscaleAddsBo addsBo) {
        // 解析手机号列表，支持逗号和换行符分隔，去除空白字符
        List<String> phoneListAll = Stream.of(addsBo.getPhones().split("[,\r\n]+"))
                .map(String::trim)
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .toList();

        // 校验是否识别到有效手机号
        if (ObjectUtil.isEmpty(phoneListAll)) {
            throw new BusinessException("没有识别到正确的手机号");
        }

        // 过滤出符合手机号格式的号码
        List<String> phoneList = phoneListAll.stream().filter(PhoneUtil::isPhone).toList();
        // 根据手机号查询用户信息
        List<UserDto> userDtos = ObjectUtil.isEmpty(phoneList) ? new ArrayList<>() : userFeign.listByPhones(phoneList);
        // 构建用户ID到用户信息的映射关系
        Map<Long, UserDto> userMap = userDtos.stream().collect(Collectors.toMap(UserDto::getId, Function.identity()));

        // 找出无法匹配到用户的手机号（失败的手机号）
        List<String> errorPhone = phoneListAll.stream()
                .filter(item -> userDtos.stream().noneMatch(val -> val.getPhone().equals(item)))
                .toList();

        // 获取所有用户ID
        List<Long> userIds = new ArrayList<>(userMap.keySet());
        // 构建灰度用户业务对象列表
        List<UserGrayscaleBo> bos = userIds.stream().map(userId -> {
            UserDto user = userMap.get(userId);
            if (user == null) {
                throw new BusinessException(StrUtil.format("用户不存在, userId:{}", userId));
            }
            UserGrayscaleBo bo = new UserGrayscaleBo();
            bo.setVersionId(addsBo.getVersionId());
            bo.setUserId(userId);
            bo.setPhone(user.getPhone());
            bo.setNickName(user.getNickName());
            return bo;
        }).toList();

        // 批量添加灰度用户
        userGrayscaleProduct.adds(bos);

        // 计算成功添加的数量
        int successCount = phoneListAll.size() - errorPhone.size();

        // 构建返回结果信息
        String format = StrUtil.format("添加成功, 去重后总数量：{}, 成功数量: {}, 失败数量: {}", phoneListAll.size(), successCount, errorPhone.size());
        if (ObjectUtil.isNotEmpty(errorPhone)) {
            format += StrUtil.format(", 失败手机号: \n{}", StrUtil.join("\n", errorPhone));
        }
        return format;
    }

    /**
     * 根据ID列表批量删除灰度用户
     *
     * @param ids 需要删除的灰度用户ID列表
     */
    public void deleteByIds(List<Long> ids) {
        userGrayscaleProduct.deleteByIds(ids);
    }
}
