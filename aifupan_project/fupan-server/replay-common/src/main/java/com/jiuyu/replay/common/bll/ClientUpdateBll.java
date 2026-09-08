package com.jiuyu.replay.common.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.bo.*;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.entity.*;
import com.jiuyu.replay.common.redisOperate.ClientUpdateRedisOperate;
import com.jiuyu.replay.common.repository.service.*;
import com.jiuyu.replay.common.utils.ImageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.ClientUpdateNewestInfoVo;
import com.jiuyu.replay.common.vo.ClientUpdateRecordVo;
import com.jiuyu.replay.common.vo.ClientUpdateVo;
import com.jiuyu.replay.common.vo.FileVo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 客户端更新
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Component
@Slf4j
public class ClientUpdateBll {

    @Autowired
    private ClientUpdateService clientUpdateService;

    @Autowired
    private ClientUpdateRecordService clientUpdateRecordService;

    @Autowired
    private ClientFileService clientFileService;

    @Autowired
    private CommonProperties commonProperties;

    @Autowired
    private FileService fileService;

    @Autowired
    private ClientVersionService clientVersionService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ClientUpdateRedisOperate clientUpdateRedisOperate;

    @Autowired
    private UserGrayscaleService userGrayscaleService;

    /**
     * 新增客户端版本更新信息
     *
     * @param clientUpdateBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(ClientUpdateBo clientUpdateBo) {
        RRException.isNotEmpty(clientUpdateBo.getCosKey(), "文件的cosKey不能为空");
        RRException.isNotEmpty(clientUpdateBo.getVersionNum(), "版本号不能为空");
        RRException.isNotEmpty(clientUpdateBo.getVersion(), "版本值不能为空");
        RRException.isNotEmpty(clientUpdateBo.getIsFront(), "客户端类型不能为空");
        RRException.isNotEmpty(clientUpdateBo.getUpdateType(), "更新类型不能为空");
        RRException.isNotEmpty(clientUpdateBo.getUpdateInfo(), "更新描述不能为空");
        ClientUpdateEntity updateEntity = clientUpdateService.getOne(new QueryWrapper<ClientUpdateEntity>().lambda()
                .and(w -> w.eq(ClientUpdateEntity::getVersionNum, clientUpdateBo.getVersionNum()).or().eq(ClientUpdateEntity::getVersion, clientUpdateBo.getVersion()))
                .eq(ClientUpdateEntity::getIsFront, clientUpdateBo.getIsFront()));
        if (Objects.nonNull(updateEntity)) {
            return R.error(10003, "版本号已存在");
        }
        ClientUpdateEntity clientUpdateEntity = new ClientUpdateEntity();
        BeanUtils.copyProperties(clientUpdateBo, clientUpdateEntity);
        clientUpdateEntity.setId(SnowflakeManager.nextValue());
        clientUpdateEntity.setUpdateTime(new Date());
        clientUpdateEntity.setIsDeleted(0);
        clientUpdateService.save(clientUpdateEntity);
        clientUpdateRedisOperate.delete();
        return R.ok("添加成功");
    }

    /**
     * 计算文件的md5
     *
     * @param filePath 文件路径
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private static String getMD5Checksum(String filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] dataBytes = new byte[1024];
            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        }
        byte[] mdbytes = md.digest();

        // Convert the byte to hex format
        StringBuilder hexString = new StringBuilder();
        for (byte mdbyte : mdbytes) {
            String hex = Integer.toHexString(0xFF & mdbyte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 更新客户端版本更新信息
     *
     * @param clientUpdateBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(ClientUpdateBo clientUpdateBo) {

        RRException.isNotEmpty(clientUpdateBo.getCosKey(), "文件的cosKey不能为空");
        RRException.isNotEmpty(clientUpdateBo.getVersionNum(), "版本号不能为空");
        RRException.isNotEmpty(clientUpdateBo.getVersion(), "版本值不能为空");
        RRException.isNotEmpty(clientUpdateBo.getIsFront(), "客户端类型不能为空");
        RRException.isNotEmpty(clientUpdateBo.getUpdateType(), "更新类型不能为空");
        RRException.isNotEmpty(clientUpdateBo.getUpdateInfo(), "更新描述不能为空");

        Long id = clientUpdateBo.getId();
        ClientUpdateEntity clientUpdateEntity = clientUpdateService.getOne(new QueryWrapper<ClientUpdateEntity>().lambda()
                .and(w -> w.eq(ClientUpdateEntity::getVersionNum, clientUpdateBo.getVersionNum()).or().eq(ClientUpdateEntity::getVersion, clientUpdateBo.getVersion()))
                .eq(ClientUpdateEntity::getIsFront, clientUpdateBo.getIsFront())
                .ne(ClientUpdateEntity::getId, id)
        );
        if (Objects.nonNull(clientUpdateEntity) && !clientUpdateEntity.getId().equals(id)) {
            return R.error(10003, "版本号已存在");
        }
        ClientUpdateEntity clientUpdateEntityUpdate = clientUpdateService.getById(id);
        RRException.isNotEmpty(clientUpdateEntityUpdate, "修改失败,数据不存在");

        if (ObjectUtil.isNotEmpty(clientUpdateBo.getUpdateInfo())) {
            clientUpdateEntityUpdate.setUpdateInfo(clientUpdateBo.getUpdateInfo());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getVersionNum())) {
            clientUpdateEntityUpdate.setVersionNum(clientUpdateBo.getVersionNum());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getVersion())) {
            clientUpdateEntityUpdate.setVersion(clientUpdateBo.getVersion());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getCosKey())){
            clientUpdateEntityUpdate.setCosKey(clientUpdateBo.getCosKey());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getFileMd5())){
            clientUpdateEntityUpdate.setFileMd5(clientUpdateBo.getFileMd5());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getClientCosKey())
                && !ObjectUtil.equal(clientUpdateBo.getClientCosKey(), clientUpdateEntityUpdate.getClientCosKey())) {
            clientUpdateEntityUpdate.setClientCosKey(clientUpdateBo.getClientCosKey());
        }
        if (ObjectUtil.isNotEmpty(clientUpdateBo.getClientFilesPath())) {
            clientUpdateEntityUpdate.setClientFilesPath(clientUpdateBo.getClientFilesPath());
        }

        clientUpdateEntityUpdate.setUpdateType(clientUpdateBo.getUpdateType());
        clientUpdateEntityUpdate.setIsFront(clientUpdateBo.getIsFront());
        clientUpdateEntityUpdate.setIsPreserve(clientUpdateBo.getIsPreserve());
        clientUpdateService.updateById(clientUpdateEntityUpdate);

        clientUpdateRedisOperate.delete();
        return R.ok("编辑成功");
    }

    private void saveUpdateFile(ClientUpdateBo clientUpdateBo, Long id) {
        List<String> fileIds = clientUpdateBo.getFileIds();
        if (Objects.nonNull(fileIds) && fileIds.size() > 0) {
            List<FileEntity> list = fileService.list(new QueryWrapper<FileEntity>().lambda().in(FileEntity::getId, fileIds));
            if (Objects.nonNull(list) && list.size() > 0) {
                for (FileEntity fileEntity : list) {
                    fileEntity.setResourceId(id);
                }
                fileService.updateBatchById(list);
            }
        }
    }

    /**
     * 上传文件
     *
     * @param file
     * @param version
     * @return
     * @throws IOException
     */
    public R<Map<String, String>> upLoadFile(MultipartFile file, String version) {
        try {
            if (file.isEmpty()) {
                return R.error(10004, "上传失败，请选择文件");
            }
            if (StringUtils.isEmpty(file.getOriginalFilename()) || !file.getOriginalFilename().contains(".")) {
                return R.error(10005, "上传失败，请注意文件格式");
            }
            String oldFileName = file.getOriginalFilename();
            // 创建一个 Date 对象
            Date date = new Date(); // 当前时间
            // 定义日期格式
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            // 将 Date 转换为指定格式的字符串
            String formattedDate = formatter.format(date);
            String fileName = version + "-" + formattedDate + "-" + oldFileName;
            String clientFilePath = commonProperties.getClientFilePath();
            try {
                createDirectory(clientFilePath);
            } catch (Exception e) {
                return R.error(10006, "创建目录失败" + e.getMessage());
            }
            String saveFilePath = commonProperties.getClientFilePath() + fileName;
            ImageUtils.saveImgToDisk(file.getInputStream(), saveFilePath);
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileSize(file.getSize());
            fileEntity.setId(SnowflakeManager.nextValue());
            fileEntity.setFileName(fileName);
            String[] split = oldFileName.split("\\.");
            fileEntity.setFileType(split[split.length - 1]);
            fileEntity.setFileUrl(commonProperties.getClientFileUrl() + fileName);
            fileEntity.setCreateDate(new Date());
            fileEntity.setUpdateDate(new Date());
            fileService.save(fileEntity);
            Map<String, String> resultData = new HashMap<>();
            resultData.put("fileId", fileEntity.getId().toString());
            resultData.put("fileName", oldFileName);
            return R.ok("上传成功", resultData);
        } catch (IOException iex) {
            log.error("上传文件失败,失败信息" + iex.getMessage());
            return R.error(10007, "上传文件失败" + iex.getMessage());
        } catch (Exception ex) {
            log.error("上传文件失败,失败信息" + ex.getMessage());
            return R.error(10006, "上传文件失败" + ex.getMessage());
        }
    }

    private void createDirectory(String directoryPath) throws Exception {
        Path path = Paths.get(directoryPath);
        // 检查文件夹是否存在
        if (!Files.exists(path)) {
            // 创建文件夹
            boolean created = Files.createDirectories(path).toFile().mkdirs();

            if (created) {
                System.out.println("Directory created successfully: " + path.toAbsolutePath());
            } else {
                System.out.println("Failed to create directory.");
            }
        } else {
            System.out.println("Directory already exists: " + path.toAbsolutePath());
        }
    }


    /**
     * 根据条件获取客户端版本更新列表
     *
     * @return
     */
    public R<PageUtils<ClientUpdateVo>> pageList(ClientUpdateListBo listBo) {
        IPage<ClientUpdateEntity> page = new Query<ClientUpdateEntity>().getPage(listBo.getPage(), listBo.getLimit(), null);
        QueryWrapper<ClientUpdateEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .like(ObjectUtil.isNotEmpty(listBo.getVersionNum()), ClientUpdateEntity::getVersionNum, listBo.getVersionNum())
                .eq(ObjectUtil.isNotEmpty(listBo.getIsFront()), ClientUpdateEntity::getIsFront, listBo.getIsFront())
                .eq(ObjectUtil.isNotEmpty(listBo.getParentId()), ClientUpdateEntity::getParentId, listBo.getParentId())
                .orderByAsc(ClientUpdateEntity::getIsFront)
                .orderByDesc(ClientUpdateEntity::getVersion)
        ;
        IPage<ClientUpdateEntity> pageData = clientUpdateService.page(page, queryWrapper);
        List<ClientUpdateEntity> records = pageData.getRecords();
        List<ClientUpdateVo> resultList = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(records)) {
            resultList = BeanUtil.copyToList(records, ClientUpdateVo.class);
        }
        return R.ok(new PageUtils<>(resultList, (int) pageData.getTotal(), (int) pageData.getSize(), (int) pageData.getCurrent()));
    }

    public R<String> delete(List<Long> ids) {
        try {
            if (clientUpdateService.removeBatchByIds(ids)) {
                clientUpdateRedisOperate.delete();
                return R.ok("删除成功");
            } else {
                return R.error(10010, "删除失败,请检测输入的Id是否正确");
            }
        } catch (Exception e) {
            log.info("删除失败,失败信息：{}", e.getMessage());
            return R.error(10009, "删除失败");
        }
    }

    public R<ClientUpdateVo> info(Long id) {
        try {
            ClientUpdateEntity clientUpdateEntity = clientUpdateService.getById(id);
            ClientUpdateVo clientUpdateVo = new ClientUpdateVo();
            BeanUtils.copyProperties(clientUpdateEntity, clientUpdateVo);
            FileVo e = new FileVo();
            e.setFileName(clientUpdateVo.getCosKey());
            clientUpdateVo.setFileList(List.of(e));

            FileVo e2 = new FileVo();
            e2.setFileName(clientUpdateVo.getClientCosKey());
            clientUpdateVo.setClientFileList(List.of(e2));
            return R.ok(clientUpdateVo);
        } catch (Exception e) {
            log.info("获取失败,失败信息：{}", e.getMessage());
            return R.error(10011, "获取失败");
        }
    }


    /**
     * 获取指定版本
     *
     * @param user    当前登录的用户
     * @param version 版本
     * @param isFront 类型 0：爱复盘软件补丁包 1：更新软件补丁包
     * @param status  客户端状态 0开发，1发布
     * @return 最新的版本信息
     */
    public R<ClientUpdateNewestInfoVo> getVersionByVersion(UserCacheVo user, String version, Integer isFront, Integer status) {
        try {
            if (Objects.isNull(version)) return R.error(10013, "版本号不能为空");
            // 获取最新的版本
            if (status == null) status = 1;

            List<ClientUpdateEntity> allUpdate = getAllUpdate();
            List<ClientUpdateEntity> changeUpdate = getChangeUpdateList(allUpdate, isFront);
            if (changeUpdate == null || changeUpdate.isEmpty()) {
                return R.error(10012, "没有可用版本");
            }

            ClientUpdateEntity lastClientUpdate = null;
            ClientUpdateEntity currentCurrent = null;
            if (ObjectUtil.equals(isFront, 0)) {
                // 根据版本号获取当前的版本
                currentCurrent = getByVersionNum(changeUpdate, isFront, version);
                if (currentCurrent == null) {
                    return R.error(10012, "没有可用版本");
                }
                if (currentCurrent.getVersion() > changeUpdate.get(0).getVersion()) {
                    return R.error(10012, "没有可用版本");
                }
                // 获取符合条件的升级列表
                List<ClientUpdateEntity> clientUpdateList = eligibleUpdatesList(changeUpdate, currentCurrent.getVersion());
                // 获取当前用户可更新的版本
                lastClientUpdate = getCurrentUserUpdateVersion(user, clientUpdateList, status);
            } else if (ObjectUtil.equals(isFront, 1)) {
                // 获取当前用户可更新的版本
                lastClientUpdate = getCurrentUserUpdateVersion(user, changeUpdate, status);
            }

            if (lastClientUpdate == null) return R.error(10012, "没有可用版本");

            ClientUpdateNewestInfoVo clientUpdateNewestInfoVo = new ClientUpdateNewestInfoVo();
            clientUpdateNewestInfoVo.setVersionNum(lastClientUpdate.getVersionNum());
            clientUpdateNewestInfoVo.setUpdateTime(lastClientUpdate.getUpdateTime().toString());
            clientUpdateNewestInfoVo.setIsFront(isFront);
            clientUpdateNewestInfoVo.setUpdateType(0);
            if (isFront == 0 && currentCurrent != null) {
                clientUpdateNewestInfoVo.setUpdateType(currentCurrent.getUpdateType());
            }

            clientUpdateNewestInfoVo.setVersion(lastClientUpdate.getVersion());
            clientUpdateNewestInfoVo.setFileMd5(lastClientUpdate.getFileMd5());
            clientUpdateNewestInfoVo.setUpdateInfo(lastClientUpdate.getUpdateInfo());
            clientUpdateNewestInfoVo.setCosKey(lastClientUpdate.getCosKey());
            clientUpdateNewestInfoVo.setClientCosKey(lastClientUpdate.getClientCosKey());
            clientUpdateNewestInfoVo.setClientFilesPath(lastClientUpdate.getClientFilesPath());
            return R.ok("获取成功", clientUpdateNewestInfoVo);
        } catch (Exception e) {
            return R.error(10012, "获取不到指定的版本信息");
        }
    }

    /**
     * 获取当前用户可更新的版本
     *
     * @param user             用户
     * @param clientUpdateList 版本列表
     * @param status          0:开发 1:发布
     * @return 可更新的版本
     */
    private ClientUpdateEntity getCurrentUserUpdateVersion(UserCacheVo user, List<ClientUpdateEntity> clientUpdateList, Integer status) {
        if (ObjectUtil.isEmpty(clientUpdateList)) {
            return null;
        }

        for (ClientUpdateEntity clientUpdateEntity : clientUpdateList) {
            if (canUpdate(user, clientUpdateEntity, status)) {
                return clientUpdateEntity;
            }
        }

        return null;
    }

    /**
     * 判断用户是否可以更新
     *
     * @param user               用户
     * @param clientUpdateEntity 版本
     * @param status             0:开发 1:发布
     * @return true: 可以更新
     */
    private boolean canUpdate(UserCacheVo user, ClientUpdateEntity clientUpdateEntity, Integer status) {
        // 版本已经发布 || (升级包不是爱复盘客户端的 && 状态要一样) || 用户的是开发模式
        if (ObjectUtil.equals(clientUpdateEntity.getStatus(), 1) || (clientUpdateEntity.getIsFront() != 0 && ObjectUtil.equals(clientUpdateEntity.getStatus(), status)) || status == 0) {
            return true;
        }

        if (user == null) {
            return false;
        }
        long count = userGrayscaleService.count(new LambdaQueryWrapper<UserGrayscaleEntity>()
                .eq(UserGrayscaleEntity::getUserId, user.getId())
                .eq(UserGrayscaleEntity::getVersionId, clientUpdateEntity.getId())
        );
        return count > 0;
    }

    public R<ClientUpdateNewestInfoVo> getPackageVersion(String version, String packageVersion) {
        try {
            if (Objects.isNull(version)) return R.error(10013, "版本号不能为空");
            ClientUpdateEntity one = clientUpdateService.getOne(new LambdaQueryWrapper<ClientUpdateEntity>()
                    .eq(ClientUpdateEntity::getVersionNum, version)
                    .last(" limit 1 ")
            );
            if (one == null) {
                return R.error(10012, "版本查询失败");
            }
            // 获取最新的版本
            ClientUpdateEntity lastClientUpdate = clientUpdateService.getOne(new QueryWrapper<ClientUpdateEntity>()
                    .lambda()
                    .eq(ClientUpdateEntity::getIsFront, 2)
                    .eq(ClientUpdateEntity::getParentId, one.getId())
                    .orderByDesc(ClientUpdateEntity::getVersion)
                    .last(" limit 1 "));
            if (lastClientUpdate == null) return R.error(10012, "没有可用的补丁包");

            if (ObjectUtil.equal(lastClientUpdate.getVersion().toString(), packageVersion)) {
                return R.error(10012, "已经是最新版本，无需更新");
            }
            double aDouble = NumberUtil.parseDouble(packageVersion);
            if (aDouble >= lastClientUpdate.getVersion()) {
                return R.error(10012, "已经是最新版本，无需更新");
            }
            ClientUpdateNewestInfoVo clientUpdateNewestInfoVo = new ClientUpdateNewestInfoVo();
            clientUpdateNewestInfoVo.setVersionNum(lastClientUpdate.getVersionNum());
            clientUpdateNewestInfoVo.setUpdateTime(lastClientUpdate.getUpdateTime().toString());
            clientUpdateNewestInfoVo.setIsFront(2);
            clientUpdateNewestInfoVo.setUpdateType(1);
            // 获取当前版本信息
            ClientUpdateEntity clientUpdateEntity = clientUpdateService.getOne(new QueryWrapper<ClientUpdateEntity>().lambda()
                    .eq(ClientUpdateEntity::getVersionNum, version)
                    .eq(ClientUpdateEntity::getIsFront, 2));
            if (clientUpdateEntity != null) {
                clientUpdateNewestInfoVo.setUpdateType(clientUpdateEntity.getUpdateType());
            }

            clientUpdateNewestInfoVo.setVersion(lastClientUpdate.getVersion());
            clientUpdateNewestInfoVo.setFileMd5(lastClientUpdate.getFileMd5());
            clientUpdateNewestInfoVo.setUpdateInfo(lastClientUpdate.getUpdateInfo());
            clientUpdateNewestInfoVo.setCosKey(lastClientUpdate.getCosKey());
            clientUpdateNewestInfoVo.setClientCosKey(lastClientUpdate.getClientCosKey());
            clientUpdateNewestInfoVo.setClientFilesPath(lastClientUpdate.getClientFilesPath());
//            QueryWrapper<FileEntity> fileWrapper = new QueryWrapper<>();
//            fileWrapper.eq("resource_id", lastClientUpdate.getId());
//            fileWrapper.orderByDesc("id");
//            fileWrapper.last(" limit 1 ");
//            FileEntity fileEntity = fileService.getOne(fileWrapper);
//
//            if (fileEntity != null) {
//                List<String> fileUrls = new ArrayList<>();
//                fileUrls.add(commonProperties.getClientFileUrl() + fileEntity.getFileName());
//                clientUpdateNewestInfoVo.setFileDownLoadUrls(fileUrls);
//            }

            return R.ok("获取成功", clientUpdateNewestInfoVo);
        } catch (Exception e) {
            return R.error(10012, "获取不到指定的补丁包信息");
        }
    }

    /**
     * 检测最新版本，获取最新版本的补丁包下载地址
     *
     * @param version     当前版本
     * @param publishTime 当前版本的发布时间
     * @return
     */
    public R<List<ClientUpdateNewestInfoVo>> checkNewest(String version, String publishTime) {
        List<ClientUpdateNewestInfoVo> result = new ArrayList<>();
        Object o = redisTemplate.opsForValue().get("version-update");
        if (Objects.nonNull(o)) {
            boolean isUpdate = false;
            List<ClientUpdateNewestInfoVo> listVo = (List<ClientUpdateNewestInfoVo>) o;
            for (ClientUpdateNewestInfoVo item : listVo) {
                if (item.getVersionNum().compareTo(version) > 0 && item.getUpdateType() == 0) {
                    isUpdate = true;
                    break;
                }
            }
            if (isUpdate) {
                return R.ok("获取最新版本成功", listVo);
            } else {
                return R.error(10012, "当前版本已经是最新版本");
            }
        }

        List<ClientUpdateEntity> list = clientUpdateService.list(new QueryWrapper<ClientUpdateEntity>().lambda().eq(ClientUpdateEntity::getIsFront, 0).orderByDesc(ClientUpdateEntity::getVersion));
        if (list != null && list.size() > 0) {
            try {
                //最新更新的客户端后端版本
                ClientUpdateEntity updateEntity = list.get(0);
                String versionNum = updateEntity.getVersionNum();
                Integer isFront = updateEntity.getIsFront();
                if (versionNum.compareTo(version) > 0) {
                    //说明存在后端版本更新
                    ClientUpdateNewestInfoVo clientUpdateNewestInfoVo = new ClientUpdateNewestInfoVo();
                    clientUpdateNewestInfoVo.setVersionNum(versionNum);
                    clientUpdateNewestInfoVo.setUpdateTime(updateEntity.getUpdateTime().toString());
                    clientUpdateNewestInfoVo.setIsFront(isFront);
                    clientUpdateNewestInfoVo.setUpdateType(updateEntity.getUpdateType());
                    Long id = updateEntity.getId();
                    List<FileEntity> fileEntityList = fileService.list(new QueryWrapper<FileEntity>().lambda().eq(FileEntity::getResourceId, id));
                    List<String> fileUrls = new ArrayList<>();
                    for (FileEntity item : fileEntityList) {
                        fileUrls.add(item.getFileUrl());
                    }
                    clientUpdateNewestInfoVo.setFileDownLoadUrls(fileUrls);
                    result.add(clientUpdateNewestInfoVo);
                }
            } catch (Exception ex) {
                log.error("获取后端最新版本失败,失败信息：{}", ex.getMessage());
                return R.error(10012, "获取后端最新版本失败");
            }
        }
        List<ClientUpdateEntity> listFront = clientUpdateService.list(new QueryWrapper<ClientUpdateEntity>().lambda().eq(ClientUpdateEntity::getIsFront, 1).orderByDesc(ClientUpdateEntity::getVersion));
        if (listFront != null && listFront.size() > 0) {
            try {
                ClientUpdateEntity updateEntity = listFront.get(0);
                String versionNum = updateEntity.getVersionNum();
                Integer isFront = updateEntity.getIsFront();
                if (versionNum.compareTo(version) > 0 || (versionNum.compareTo(version) == 0 && publishTime.compareTo(updateEntity.getUpdateTime().toString()) < 0)) {
                    //说明存在前端版本更新
                    ClientUpdateNewestInfoVo clientUpdateNewestInfoVo = new ClientUpdateNewestInfoVo();
                    clientUpdateNewestInfoVo.setVersionNum(versionNum);
                    clientUpdateNewestInfoVo.setUpdateTime(updateEntity.getUpdateTime().toString());
                    clientUpdateNewestInfoVo.setIsFront(isFront);
                    clientUpdateNewestInfoVo.setUpdateType(updateEntity.getUpdateType());
                    result.add(clientUpdateNewestInfoVo);
                }
            } catch (Exception ex) {
                log.error("获取前端最新版本失败,失败信息：{}", ex.getMessage());
                return R.error(10012, "获取前端最新版本失败");
            }
        }
        //强制更新
        List<ClientUpdateEntity> listForce = clientUpdateService.list(new QueryWrapper<ClientUpdateEntity>().lambda().eq(ClientUpdateEntity::getUpdateType, 1).eq(ClientUpdateEntity::getVersionNum, version).orderByDesc(ClientUpdateEntity::getVersion));
        if (listForce != null && listForce.size() > 0) {
            try {
                ClientUpdateEntity updateEntity = listForce.get(0);
                ClientUpdateNewestInfoVo clientUpdateNewestInfoVo = new ClientUpdateNewestInfoVo();
                clientUpdateNewestInfoVo.setVersionNum(version);
                clientUpdateNewestInfoVo.setUpdateTime(updateEntity.getUpdateTime().toString());
                clientUpdateNewestInfoVo.setIsFront(updateEntity.getIsFront());
                clientUpdateNewestInfoVo.setUpdateType(updateEntity.getUpdateType());
                clientUpdateNewestInfoVo.setIsFront(updateEntity.getIsFront());
                result.add(clientUpdateNewestInfoVo);
            } catch (Exception ex) {
                log.error("获取强制更新版本失败,失败信息：{}", ex.getMessage());
                return R.error(10012, "获取强制更新版本失败");
            }
        }
        if (result.size() > 0) {
            redisTemplate.opsForValue().set("version-update", result);
            return R.ok("获取最新版本成功", result);
        } else {
            return R.error(10012, "当前版本已经是最新版本");
        }
    }

    /**
     * 保存升级记录
     *
     * @param clientUpdateRecordBo
     * @return
     */
    public R<String> saveRecord(ClientUpdateRecordBo clientUpdateRecordBo) {
        try {
            ClientUpdateRecordEntity clientUpdateRecordEntity = new ClientUpdateRecordEntity();
            BeanUtils.copyProperties(clientUpdateRecordBo, clientUpdateRecordEntity);
            clientUpdateRecordEntity.setId(SnowflakeManager.nextValue());
            clientUpdateRecordEntity.setUpdateTime(new Date());
            String updateVersion = clientUpdateRecordBo.getUpdateVersion();
            ClientUpdateEntity one = clientUpdateService.getOne(new QueryWrapper<ClientUpdateEntity>().lambda().eq(ClientUpdateEntity::getVersionNum, updateVersion).eq(ClientUpdateEntity::getIsFront, clientUpdateRecordBo.getIsFront()));
            if (Objects.nonNull(one)) {
                clientUpdateRecordEntity.setUpdateId(one.getId());
            }
            clientUpdateRecordService.save(clientUpdateRecordEntity);
            return R.ok("保存升级记录成功");
        } catch (Exception ex) {
            log.error("保存升级记录失败,失败信息：{}", ex.getMessage());
            return R.error(10013, "保存升级记录失败");
        }
    }

    public R<PageUtils<ClientUpdateRecordVo>> recordPageList(SearchClientUpdateRecordBo searchClientUpdateRecordBo) {
        try {
            if (searchClientUpdateRecordBo.getPageIndex() == null || searchClientUpdateRecordBo.pageIndex < 1) {
                searchClientUpdateRecordBo.setPageIndex(1);
            }
            searchClientUpdateRecordBo.setPageSize(searchClientUpdateRecordBo.getPageSize() == null ? 10 : searchClientUpdateRecordBo.getPageSize());
            QueryWrapper<ClientUpdateRecordEntity> queryWrapper = new QueryWrapper<>();
            LambdaQueryWrapper<ClientUpdateRecordEntity> lambda = queryWrapper.lambda();
            if (!StringUtils.isEmpty(searchClientUpdateRecordBo.getClientUser())) {
                lambda.eq(ClientUpdateRecordEntity::getClientUser, searchClientUpdateRecordBo.getClientUser());
            }
            if (!StringUtils.isEmpty(searchClientUpdateRecordBo.getOldVersion())) {
                lambda.eq(ClientUpdateRecordEntity::getOldVersion, searchClientUpdateRecordBo.getOldVersion());
            }
            if (!StringUtils.isEmpty(searchClientUpdateRecordBo.getUpdateVersion())) {
                lambda.eq(ClientUpdateRecordEntity::getUpdateVersion, searchClientUpdateRecordBo.getUpdateVersion());
            }
            if (searchClientUpdateRecordBo.getUpdateType() != null && searchClientUpdateRecordBo.getUpdateType() > -1) {
                lambda.eq(ClientUpdateRecordEntity::getUpdateType, searchClientUpdateRecordBo.getUpdateType());
            }
            if (!StringUtils.isEmpty(searchClientUpdateRecordBo.getStartDate()) && !StringUtils.isEmpty(searchClientUpdateRecordBo.getEndDate())) {
                lambda.between(ClientUpdateRecordEntity::getUpdateTime, searchClientUpdateRecordBo.getStartDate(), searchClientUpdateRecordBo.getEndDate());
            }
            Page<ClientUpdateRecordEntity> pageEntity = clientUpdateRecordService.page(new Page<>(searchClientUpdateRecordBo.getPageIndex(), searchClientUpdateRecordBo.getPageSize()), queryWrapper);
            List<ClientUpdateRecordEntity> records = pageEntity.getRecords();
            List<ClientUpdateRecordVo> list = records.stream().map(item -> {
                ClientUpdateRecordVo clientUpdateRecordVo = new ClientUpdateRecordVo();
                BeanUtils.copyProperties(item, clientUpdateRecordVo);
                return clientUpdateRecordVo;
            }).collect(Collectors.toList());
            PageUtils<ClientUpdateRecordVo> pageUtils = new PageUtils<>();
            pageUtils.setList(list);
            pageUtils.setPageSize(Integer.parseInt(String.valueOf(pageEntity.getSize())));
            pageUtils.setTotalCount(Integer.parseInt(String.valueOf(pageEntity.getTotal())));
            pageUtils.setTotalPage(Integer.parseInt(String.valueOf(pageEntity.getPages())));
            return R.ok("查询成功", pageUtils);
        } catch (Exception ex) {
            log.error("获取升级记录失败,失败信息：{}", ex.getMessage());
            return R.error(10013, "获取升级记录失败");
        }
    }

    public byte[] downLoadFile(String fileName) {
        if (Objects.isNull(fileName)) {
            log.error("文件名称不能为空");
            return null;
        } else {
            return getFileByte(fileName);
        }
    }

    public byte[] getFileByte(String fileName) {
        FileInputStream inputStream = null;
        try {
            File file = new File(commonProperties.getClientFilePath() + fileName);
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, inputStream.available());
            return bytes;
        } catch (IOException e) {
            log.error("文件没找到:{}", commonProperties.getClientFilePath() + fileName);
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public R<String> setClientVersion(ClientVersionBo bo) {
        ClientVersionEntity version = clientVersionService.getOne(new LambdaQueryWrapper<ClientVersionEntity>()
                .eq(ClientVersionEntity::getClientCpuid, bo.getClientCpuid())
        );
        Date now = new Date();
        if (ObjectUtil.isNotEmpty(version)) {
            BeanUtil.copyProperties(bo, version, new CopyOptions().setIgnoreNullValue(true));
            version.setUpdateDate(now);
            clientVersionService.updateById(version);
        } else {
            ClientVersionEntity clientVersion = BeanUtil.copyProperties(bo, ClientVersionEntity.class);
            clientVersion.setCreateDate(now);
            clientVersion.setUpdateDate(now);
            clientVersion.setId(SnowflakeManager.nextValue());
            clientVersionService.save(clientVersion);
        }
        return R.ok("设置完成");
    }

    public R<String> updateStatus(Long id, Integer status) {
        clientUpdateService.update(new LambdaUpdateWrapper<ClientUpdateEntity>()
                .eq(ClientUpdateEntity::getId, id)
                .set(ClientUpdateEntity::getStatus, status)
        );
        clientUpdateRedisOperate.delete();
        return R.ok("修改完成");
    }

    /**
     * 根据版本号获取升级信息
     *
     * @param isFront    更新类型 0爱复盘主程序更新, 1更新程序更新
     * @param versionNum 版本号
     * @return 升级信息
     */
    public ClientUpdateEntity getByVersionNum(List<ClientUpdateEntity> allUpdate, Integer isFront, String versionNum) {
        if (ObjectUtil.isEmpty(allUpdate)) {
            return null;
        }
        return allUpdate.stream().filter(item -> {
            if (ObjectUtil.equals(isFront, 1) && ObjectUtil.equals(item.getIsFront(), 1)) {
                return true;
            }
            return item.getIsFront().equals(isFront) && item.getVersionNum().equals(versionNum);
        }).findFirst().orElse(null);
    }

    /**
     * 获取符合条件的升级列表
     *
     * @param gtVersion 大于的版本
     * @return 升级列表
     */
    public List<ClientUpdateEntity> eligibleUpdatesList(List<ClientUpdateEntity> allUpdate, Double gtVersion) {
        if (ObjectUtil.isEmpty(allUpdate)) {
            return List.of();
        }
        return allUpdate.stream().filter(item -> {
            boolean gtVersionFlag = true;
            if (gtVersion != null) {
                gtVersionFlag = item.getVersion() > gtVersion;
            }
            return gtVersionFlag;
        }).collect(Collectors.toList());
    }

    /**
     * 获取所有升级列表
     *
     * @return 升级列表
     */
    public List<ClientUpdateEntity> getAllUpdate() {
        List<ClientUpdateEntity> clientUpdateEntities = clientUpdateRedisOperate.get();
        if (clientUpdateEntities != null) {
            return clientUpdateEntities;
        }

        List<ClientUpdateEntity> list = clientUpdateService.list(new LambdaQueryWrapper<ClientUpdateEntity>()
                .orderByDesc(ClientUpdateEntity::getVersion)
        );

        clientUpdateRedisOperate.save(list);

        return list;
    }

    public List<ClientUpdateEntity> getChangeUpdateList(List<ClientUpdateEntity> allUpdate, Integer isFront) {
        return allUpdate.stream().filter(item -> item.getIsFront().equals(isFront)).toList();
    }
}

