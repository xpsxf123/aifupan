package com.jiuyu.replay.common.utils;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;

public class ImageUtils {


    /**
     * 保存图片到本地磁盘
     * @param inputStream 图片流
     * @param savePath 保存地址
     * @throws Exception
     */
    public static void saveImgToDisk(InputStream inputStream, String savePath){

        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            OutputStream out = new FileOutputStream(savePath)) {

            // 转成byte数组
            byte[] bytesTemp = new byte[1024];
            while (inputStream.read(bytesTemp) != -1){
                bos.write(bytesTemp);
            }
            byte[] bytes = bos.toByteArray();

            // 保存文件
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * 改变图片尺寸
     *
     * @param imageStream
     * @return
     * @throws Exception
     */
    public static InputStream changeSize(InputStream imageStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        BufferedImage target = null;

        int maxWidth = 1334;
        int maxHeight = 750;

        try {
            BufferedImage bufferedImage = ImageIO.read(imageStream);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            // 如果图片的尺寸没有超过规定的尺寸，直接进入图片大小的处理
            if (width <= maxWidth && height <= maxHeight) {
                ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
                return compressSize(byteArrayOutputStream.toByteArray());
            }

            double widthScale = maxWidth * 1.0 / width;
            double heightScale = maxHeight * 1.0 / height;

            if (widthScale > heightScale) {
                widthScale = heightScale;
                maxWidth = (int) (widthScale * width);
            } else {
                heightScale = widthScale;
                maxHeight = (int) (heightScale * height);
            }
            int type = bufferedImage.getType();

            if (type == BufferedImage.TYPE_CUSTOM) {
                ColorModel colorModel = bufferedImage.getColorModel();
                WritableRaster writableRaster = colorModel.createCompatibleWritableRaster(maxWidth, maxHeight);
                boolean alphaPremultiplied = colorModel.isAlphaPremultiplied();
                target = new BufferedImage(colorModel, writableRaster, alphaPremultiplied, null);
            } else {
                target = new BufferedImage(maxWidth, maxHeight, type);
            }
            Graphics2D graphics2D = target.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.drawRenderedImage(bufferedImage, AffineTransform.getScaleInstance(widthScale, heightScale));
            graphics2D.dispose();

            ImageIO.write(target, "png", byteArrayOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return compressSize(byteArrayOutputStream.toByteArray());
    }

    /**
     *
     * 改变图片大小
     *
     * @param imageBytes
     * @return
     */
    public static InputStream compressSize(byte[] imageBytes) {
        long maxSize = 256000;

        // 如果图片的大小没有超过规定的大小，则不做处理
        if (imageBytes.length <= 0 || imageBytes.length <= maxSize) {
            return new ByteArrayInputStream(imageBytes);
        }

        double accuracy = getAccuracy(imageBytes.length / 1024);


        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(imageBytes.length);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes)) {

            Thumbnails.of(byteArrayInputStream)
                    .scale(accuracy)
                    .outputQuality(accuracy)
                    .toOutputStream(byteArrayOutputStream);

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }


    }

    /**
     *
     * 改变图片大小(mini)
     *
     * @param imageBytes
     * @return
     */
    public static InputStream compressSizeMini(byte[] imageBytes) {
        long maxSize = 20000;

        // 如果图片的大小没有超过规定的大小，则不做处理
        if (imageBytes.length <= 0 || imageBytes.length <= maxSize) {
            return new ByteArrayInputStream(imageBytes);
        }

        double accuracy = 0;
        int size = imageBytes.length / 1024;

        if (size < 20) {
            accuracy = 0.7;
        }else if (size < 30) {
            accuracy = 0.52;
        } else if (size < 40) {
            accuracy = 0.4;
        } else if (size < 50) {
            accuracy = 0.3;
        } else {
            accuracy = 0.28;
        }



        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(imageBytes.length);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes)) {


            Thumbnails.of(byteArrayInputStream)
                    .scale(accuracy)
                    .outputQuality(accuracy)
                    .toOutputStream(byteArrayOutputStream);

            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 改变图片大小不改变分辨率(效果不好)
     * @param file
     * @return
     * @throws IOException
     */
    public static File compressPictureByQality(File file) throws IOException {
        if(file.length() < 256000){
            // 不需要做压缩
            return file;
        }

        BufferedImage src = null;
        FileOutputStream out = null;
        ImageWriter imgWrier;
        ImageWriteParam imgWriteParams;
        // 指定写图片的方式为 jpg
        imgWrier = ImageIO.getImageWritersByFormatName("png").next();
        imgWriteParams = new javax.imageio.plugins.jpeg.JPEGImageWriteParam(
                null);
        // 要使用压缩，必须指定压缩方式为MODE_EXPLICIT
        imgWriteParams.setCompressionMode(imgWriteParams.MODE_EXPLICIT);
        // 这里指定压缩的程度，参数qality是取值0~1范围内，
        imgWriteParams.setCompressionQuality(getQality(file.length()));
        imgWriteParams.setProgressiveMode(imgWriteParams.MODE_DISABLED);
        ColorModel colorModel =ImageIO.read(file).getColorModel();// ColorModel.getRGBdefault();
        // 指定压缩时使用的色彩模式
        imgWriteParams.setDestinationType(new javax.imageio.ImageTypeSpecifier(
                colorModel, colorModel.createCompatibleSampleModel(16, 16)));

        if (!file.exists()) {
            throw new FileNotFoundException("Not Found Img File,文件不存在");
        } else {
            System.out.println("图片转换前大小"+file.length()+"字节");
            src = ImageIO.read(file);
            out = new FileOutputStream(file);

            imgWrier.reset();
            // 必须先指定 out值，才能调用write方法, ImageOutputStream可以通过任何
            // OutputStream构造
            imgWrier.setOutput(ImageIO.createImageOutputStream(out));
            // 调用write方法，就可以向输入流写图片
            imgWrier.write(null, new IIOImage(src, null, null),
                    imgWriteParams);
            out.flush();
            out.close();
            System.out.println("图片转换后大小"+file.length()+"字节");
            return file;
        }
    }

    /**
     *
     * 自动调节精度(经验数值)
     *
     * @param size
     * @return
     */
    private static double getAccuracy(long size) {
        double accuracy;
        if (size < 600) {
            accuracy = 0.7;
        }else if (size < 900) {
            accuracy = 0.52;
        } else if (size < 2047) {
            accuracy = 0.4;
        } else if (size < 3275) {
            accuracy = 0.3;
        } else {
            accuracy = 0.28;
        }
        return accuracy;
    }

    /**
     *
     * 自动调节压缩比(经验数值)
     *
     * @param size
     * @return
     */
    private static float getQality(long size) {
        size = size / 1000;
        float qality;
        if (size < 256) {
            qality = 0.86f;
        }else if (size < 400) {
            qality = 0.72f;
        }else if (size < 500) {
            qality = 0.58f;
        }else if (size < 600) {
            qality = 0.48f;
        }else if (size < 700) {
            qality = 0.41f;
        }else if (size < 800) {
            qality = 0.34f;
        }else if (size < 900) {
            qality = 0.30f;
        } else if (size < 1000) {
            qality = 0.28f;
        } else if (size < 2000) {
            qality = 0.2f;
        } else {
            qality = 0.1f;
        }
        return qality;
    }
}
