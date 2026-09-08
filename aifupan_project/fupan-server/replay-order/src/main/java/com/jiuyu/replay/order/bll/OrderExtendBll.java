package com.jiuyu.replay.order.bll;




import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.util.StringUtils;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.OrderExtendInfoVo;
import com.jiuyu.replay.generic.vo.order.OrderExtendListVo;
import com.jiuyu.replay.order.bo.OrderExtendBo;
import com.jiuyu.replay.order.bo.OrderExtendListBo;
import com.jiuyu.replay.order.producer.OrderExtendProducer;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Component
public class OrderExtendBll {

    @Resource
    private OrderExtendProducer orderExtendProducer;
    @Resource
    private FileProducer fileProducer;


    /**
     * 订单的扩展表列表
     * @param orderExtendListBo 订单的扩展表列表查询参数
     * @return
     */
    public R<PageUtils<OrderExtendListVo>> queryPage(OrderExtendListBo orderExtendListBo) {
        PageUtils<OrderExtendListVo> pageUtils = orderExtendProducer.queryPage(orderExtendListBo);

        //数据不为null、[] 进行填充
        if (ObjectUtils.isNotEmpty(pageUtils)&&ObjectUtils.isNotEmpty(pageUtils.getList())) {
            //订单付费数据
            List<OrderExtendListVo> list = pageUtils.getList();
            //填充数据
            List<OrderExtendListVo> vos = fillFileData(list,
                    OrderExtendListVo::getPayPictures,
                    OrderExtendListVo::setPayPictureList,
                    OrderExtendListVo::getPayPictureList,
                    OrderExtendListVo::setFileList);
            pageUtils.setList(vos);
        }
        return R.ok("获取成功", pageUtils);
    }

    /**
    * 订单的扩展表信息
    * @param id 订单的扩展表id
    * @return
    */
    public R<OrderExtendInfoVo> info(Long id) {

        OrderExtendInfoVo orderExtendInfoVo = orderExtendProducer.info(id);
        if (ObjectUtil.isEmpty(orderExtendInfoVo)){
            return R.ok("获取成功", orderExtendInfoVo);
        }
        //填充数据
        List<OrderExtendInfoVo> orderExtendInfoVos = fillFileData(
                Collections.singletonList(orderExtendInfoVo),
                OrderExtendInfoVo::getPayPictures,
                OrderExtendInfoVo::setPayPictureList,
                OrderExtendInfoVo::getPayPictureList,
                OrderExtendInfoVo::setFileList
        );
        if (ObjectUtil.isNotEmpty(orderExtendInfoVos)){
            return R.ok("获取成功", orderExtendInfoVos.get(0));
        }
        return R.ok("获取成功", orderExtendInfoVo);
    }




    /**
     * 公共方法：填充文件数据
     *
     * @param vos             VO对象集合
     * @param picturesGetter  获取支付图片字符串的方法
     * @param idListSetter    设置图片ID列表的方法
     * @param payPictureListGetter  获取支付图片的列表方法
     * @param fileListSetter  设置文件展示列表的方法
     * @param <T>             VO类型
     */
    private <T> List<T> fillFileData(List<T> vos,
                                  Function<T, String> picturesGetter,
                                  BiConsumer<T, List<Long>> idListSetter,
                                  Function<T, List<Long>> payPictureListGetter,
                                  BiConsumer<T, List<FileShowVo>> fileListSetter) {

        // 收集所有有效的图片ID
        Set<Long> fileIds = new HashSet<>();

        //遍历订单付费资料数据
        for (T orderExtend : vos) {
            //处理不为null的订单付费资料数据
            if (ObjectUtil.isNotEmpty(orderExtend)){
                //获取字符串","隔开的 ids
                String payPictures = picturesGetter.apply(orderExtend);
                if (StringUtils.isNotBlank(payPictures)) {
                    //分割后流式处理
                    List<Long> pictureIdList = Arrays.stream(payPictures.split(","))
                            //过滤null、空串、不为数字的数据
                            .filter(StringUtils::isNumeric)
                            //String 转 Long
                            .map(Long::valueOf)
                            .toList();

                    //存入orderExtend的payPictureList字段 （payPictureList内为干净fileId）
                    idListSetter.accept(orderExtend, pictureIdList);
                    fileIds.addAll(pictureIdList);
                }
            }
        }

        //收集不到fileIds 则不查file文件数据
        if (fileIds.isEmpty()) {
            return vos;
        }

        //查询图片文件数据
        List<FileShowVo> fileShowVos = fileProducer.listByFileIds(fileIds);

        //null、[] 则反
        if (ObjectUtil.isEmpty(fileShowVos)||fileShowVos.isEmpty()) {
            return vos;
        }

        //结果映射为map
        Map<Long, FileShowVo> fileMap = fileShowVos.stream()
                .collect(Collectors.toMap(FileShowVo::getId, Function.identity(), (oldValue, newValue) -> newValue));

        for (T orderExtend : vos) {
            if (ObjectUtil.isNotEmpty(orderExtend)){
                //获取付费资料文件的id列表
                List<Long> payPictureList = payPictureListGetter.apply(orderExtend);
                if (ObjectUtil.isNotEmpty(payPictureList)) {

                    //从fileMap获取对应的file数据
                    List<FileShowVo> fileList = payPictureList.stream()
                            .filter(Objects::nonNull)
                            .map(fileMap::get)
                            .filter(Objects::nonNull)
                            .toList();

                    //填充
                    fileListSetter.accept(orderExtend, fileList);
                }
            }
        }
        return vos;
    }




    /**
     * 新增订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    public R<String> save(OrderExtendBo orderExtendBo) {

        OrderExtendInfoVo orderExtendInfoVo = orderExtendProducer.save(orderExtendBo);
        return R.ok("添加成功");
    }

    /**
     * 修改订单的扩展表
     * @param orderExtendBo 订单的扩展表对象
     * @return
     */
    public R<String> update(OrderExtendBo orderExtendBo) {

        orderExtendProducer.update(orderExtendBo);
        return R.ok("修改成功");
    }

    /**
     * 删除订单的扩展表
     * @param id 订单的扩展表id
     * @return
     */
    public R<String> delete(Long id) {

        orderExtendProducer.deleteById(id);
        return R.ok("删除成功");
    }


    public OrderExtendInfoVo getByOrderId(Long orderId) {

        OrderExtendInfoVo orderExtendInfoVo = orderExtendProducer.getByOrderId(orderId);
        if (ObjectUtil.isNotEmpty(orderExtendInfoVo)) {
            //填充数据
            List<OrderExtendInfoVo> orderExtendInfoVos = fillFileData(
                    Collections.singletonList(orderExtendInfoVo),
                    OrderExtendInfoVo::getPayPictures,
                    OrderExtendInfoVo::setPayPictureList,
                    OrderExtendInfoVo::getPayPictureList,
                    OrderExtendInfoVo::setFileList
            );
            if (ObjectUtil.isNotEmpty(orderExtendInfoVos)){
                return orderExtendInfoVos.get(0);
            }
        }
        return orderExtendInfoVo;
    }
}

