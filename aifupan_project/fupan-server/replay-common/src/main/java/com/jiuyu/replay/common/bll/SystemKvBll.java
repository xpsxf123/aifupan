package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.common.bo.ImgConfigBo;
import com.jiuyu.replay.common.bo.SystemKvBo;
import com.jiuyu.replay.common.bo.SystemKvListBo;
import com.jiuyu.replay.common.producer.FileProducer;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.vo.ImgConfigVo;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.common.vo.SystemKvListVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;


/**
 * 系统配置的键值对
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Component
public class SystemKvBll {

    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private FileProducer fileProducer;


    /**
     * 系统配置的键值对列表
     * @param systemKvListBo 系统配置的键值对列表查询参数
     * @return
     */
    public R<PageUtils<SystemKvListVo>> queryPage(SystemKvListBo systemKvListBo) {

        return R.ok("获取成功", systemKvProducer.queryPage(systemKvListBo));
    }

    /**
    * 系统配置的键值对信息
    * @param id 系统配置的键值对id
    * @return
    */
    public R<SystemKvInfoVo> info(Long id) {

        SystemKvInfoVo systemKvInfoVo = systemKvProducer.info(id);
        return R.ok("获取成功", systemKvInfoVo);
    }

    /**
     * 新增系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    public R<String> save(SystemKvBo systemKvBo) {

        SystemKvInfoVo systemKvInfoVo = systemKvProducer.save(systemKvBo);
        return R.ok("添加成功");
    }

    /**
     * 修改系统配置的键值对
     * @param systemKvBo 系统配置的键值对对象
     * @return
     */
    public R<String> update(SystemKvBo systemKvBo) {

        systemKvProducer.update(systemKvBo);
        return R.ok("修改成功");
    }

    /**
     * 删除系统配置的键值对
     * @param id 系统配置的键值对id
     * @return
     */
    public R<String> delete(Long id) {

        systemKvProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据key获取系统配置的键值对
     * @param key code
     * @return 键值对
     */
    public R<SystemKvInfoVo> getByKey(String key){

        return R.ok("获取成功", systemKvProducer.getByKey(key));
    }

    /**
     * 根据key获取系统配置的键值对
     *
     * @param key code
     * @return 键值对
     */
    public String getValueByKey(String key, String defaultValue) {
        return systemKvProducer.getValueByKey(key, defaultValue);
    }

    /**
     * 根据key获取系统配置的键值对
     *
     * @param key code
     * @return 键值对
     */
    public Integer getValueByKey(String key, Integer defaultValue) {
        return systemKvProducer.getValueByKey(key, defaultValue);
    }

    /**
     * 根据key获取系统配置的键值对
     *
     * @param key code
     * @return 键值对
     */
    public Long getValueByKey(String key, Long defaultValue) {
        return systemKvProducer.getValueByKey(key, defaultValue);
    }

    /**
     * 根据key获取系统配置的键值对
     *
     * @param key code
     * @return 键值对
     */
    public SystemKvInfoVo getByKeyNotR(String key) {
        return systemKvProducer.getByKey(key);
    }


    /**
     * 修改图片配置
     * @param imgConfigBo 图片配置信息
     * @return
     */
    public R<String> updateImgConfig(ImgConfigBo imgConfigBo) {
        SystemKvInfoVo h5DefaultQrcodeImg = this.systemKvProducer.getByKey("h5_default_qrcode_img_id");
        if(h5DefaultQrcodeImg != null) {
            SystemKvBo systemKvBo = new SystemKvBo();
            BeanUtils.copyProperties(h5DefaultQrcodeImg, systemKvBo);
            systemKvBo.setKvValue(imgConfigBo.getH5ImgId().toString());
            systemKvProducer.update(systemKvBo);
        }else {
            SystemKvBo systemKvBo = new SystemKvBo();
            systemKvBo.setRemarks("H5兜底销售二维码图片文件id");
            systemKvBo.setKvKey("h5_default_qrcode_img_id");
            systemKvBo.setKvValue(imgConfigBo.getH5ImgId().toString());
            systemKvProducer.save(systemKvBo);
        }

        SystemKvInfoVo clientDefaultSaleQrcodeImg = this.systemKvProducer.getByKey("client_default_sale_qrcode_img_id");
        if(clientDefaultSaleQrcodeImg != null) {
            SystemKvBo systemKvBo = new SystemKvBo();
            BeanUtils.copyProperties(clientDefaultSaleQrcodeImg, systemKvBo);
            systemKvBo.setKvValue(imgConfigBo.getClientSaleImgId().toString());
            systemKvProducer.update(systemKvBo);
        }else {
            SystemKvBo systemKvBo = new SystemKvBo();
            systemKvBo.setRemarks("客户端销售销售二维码图片文件id");
            systemKvBo.setKvKey("client_default_sale_qrcode_img_id");
            systemKvBo.setKvValue(imgConfigBo.getClientSaleImgId().toString());
            systemKvProducer.save(systemKvBo);
        }

        SystemKvInfoVo pureRecordSaleQrcodeImg = this.systemKvProducer.getByKey("pure_record_sale_qrcode_img_id");
        if (pureRecordSaleQrcodeImg != null) {
            SystemKvBo systemKvBo = new SystemKvBo();
            BeanUtils.copyProperties(pureRecordSaleQrcodeImg, systemKvBo);
            systemKvBo.setKvValue(imgConfigBo.getPureRecordImgId().toString());
            systemKvProducer.update(systemKvBo);
        } else {
            SystemKvBo systemKvBo = new SystemKvBo();
            systemKvBo.setRemarks("纯录制版本客户端销售二维码图片文件id");
            systemKvBo.setKvKey("pure_record_sale_qrcode_img_id");
            systemKvBo.setKvValue(imgConfigBo.getPureRecordImgId().toString());
            systemKvProducer.save(systemKvBo);
        }

        return R.ok("修改成功");

    }

    /**
     * 获取图片配置
     * @return
     */
    public R<ImgConfigVo> getImgConfig() {

        ImgConfigVo imgConfigVo = new ImgConfigVo();
        SystemKvInfoVo h5DefaultQrcodeImg = this.systemKvProducer.getByKey("h5_default_qrcode_img_id");
        if(h5DefaultQrcodeImg != null) {
            FileShowVo fileShowVo = fileProducer.infoByFileId(Long.valueOf(h5DefaultQrcodeImg.getKvValue()));
            imgConfigVo.setH5ImgVo(fileShowVo);
        }
        SystemKvInfoVo clientDefaultSaleQrcodeImg = this.systemKvProducer.getByKey("client_default_sale_qrcode_img_id");
        if(clientDefaultSaleQrcodeImg != null) {
            FileShowVo fileShowVo = fileProducer.infoByFileId(Long.valueOf(clientDefaultSaleQrcodeImg.getKvValue()));
            imgConfigVo.setClientSaleImgVo(fileShowVo);
        }
        SystemKvInfoVo pureRecordSaleQrcodeImg = this.systemKvProducer.getByKey("pure_record_sale_qrcode_img_id");
        if (pureRecordSaleQrcodeImg != null) {
            FileShowVo fileShowVo = fileProducer.infoByFileId(Long.valueOf(pureRecordSaleQrcodeImg.getKvValue()));
            imgConfigVo.setPureRecordImgVo(fileShowVo);
        }

        return R.ok(imgConfigVo);
    }
}

