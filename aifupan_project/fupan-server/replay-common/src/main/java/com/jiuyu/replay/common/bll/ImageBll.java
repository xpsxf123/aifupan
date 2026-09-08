package com.jiuyu.replay.common.bll;

import com.jiuyu.replay.common.tencent.TencentCosUtils;
import com.jiuyu.replay.common.utils.ImageUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
public class ImageBll {

    /**
     * 上传图片到cos
     * @param cosSaveKey cos存储key
     * @param data 图片的字节数组
     * @return
     */
    public boolean uploadToCos(String cosSaveKey, byte[] data) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            return TencentCosUtils.putStreamObject(TencentCosUtils.getPublicCosBucketName(), byteArrayInputStream, cosSaveKey);
        }catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 保存图片
     * @param file 图片文件
     * @param savePath 保存地址
     * @param compressSizeType 0: 正常压缩  1：超级压缩 2：不压缩 默认0
     * @return
     */
    public boolean saveImg(MultipartFile file, String savePath, Integer compressSizeType) {

        InputStream inputStream = null;

        try {
            if(StringUtils.isEmpty(compressSizeType) || compressSizeType == 0){
                // 正常压缩
                inputStream = ImageUtils.compressSize(file.getBytes());

                if(inputStream != null){
                    ImageUtils.saveImgToDisk(inputStream, savePath);
                }

            }else if(compressSizeType == 1){
                // 超级压缩
                inputStream = ImageUtils.compressSizeMini(file.getBytes());

                if(inputStream != null){
                    ImageUtils.saveImgToDisk(inputStream, savePath);
                }
            }else if(compressSizeType == 2){
                // 不压缩
                inputStream = file.getInputStream();
                ImageUtils.saveImgToDisk(inputStream, savePath);
            }

            if(inputStream != null){
                inputStream.close();
            }

            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
