package com.jiuyu.replay.api.logic.system.impl;

import com.jiuyu.replay.api.logic.system.LoginRotateImageLogic;
import com.jiuyu.replay.common.bll.FileBll;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.FileShowVo;
import com.jiuyu.replay.system.bll.LoginRotateImageBll;
import com.jiuyu.replay.system.bo.LoginRotateImageBo;
import com.jiuyu.replay.system.bo.LoginRotateImageListBo;
import com.jiuyu.replay.system.vo.LoginRotateImageInfoVo;
import com.jiuyu.replay.system.vo.LoginRotateImageListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 客户端登录页轮播图
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Service
public class LoginRotateImageLogicImpl implements LoginRotateImageLogic {

    @Resource
    private LoginRotateImageBll loginRotateImageBll;
    @Resource
    private FileBll fileBll;



    @Override
    public R<PageUtils<LoginRotateImageListVo>> queryPage(LoginRotateImageListBo loginRotateImageListBo) {
        R<PageUtils<LoginRotateImageListVo>> pageUtilsR = loginRotateImageBll.queryPage(loginRotateImageListBo);
        if (Objects.nonNull(pageUtilsR)&&pageUtilsR.getData().getList()!=null&&!pageUtilsR.getData().getList().isEmpty()){
            List<LoginRotateImageListVo> data = pageUtilsR.getData().getList();
            List<Long> fileIds = data.stream().map(LoginRotateImageListVo::getFileId).toList();
            List<FileShowVo> showVoList = fileBll.listByFileIds(fileIds);
            if (!showVoList.isEmpty()){
                Map<Long, FileShowVo> map = showVoList.stream().collect(Collectors
                        .toMap(FileShowVo::getId, vo -> vo, (o, n) -> n));
                for (LoginRotateImageListVo datum : data) {
                    datum.setName(map.get(datum.getFileId()).getName());
                    datum.setResourceId(map.get(datum.getFileId()).getResourceId());
                    datum.setUrl(map.get(datum.getFileId()).getUrl());
                }
            }
        }
        return pageUtilsR;
    }


    @Override
    public R<List<LoginRotateImageListVo>> noPage() {
        R<List<LoginRotateImageListVo>> listImages = loginRotateImageBll.noPage();
        if (Objects.nonNull(listImages)&&!listImages.getData().isEmpty()){
            List<LoginRotateImageListVo> data = listImages.getData();
            List<Long> fileIds = data.stream().map(LoginRotateImageListVo::getFileId).toList();
            List<FileShowVo> showVoList = fileBll.listByFileIds(fileIds);
            if (!showVoList.isEmpty()){
                Map<Long, FileShowVo> map = showVoList.stream().collect(Collectors
                        .toMap(FileShowVo::getId, vo -> vo, (o, n) -> n));
                for (LoginRotateImageListVo datum : data) {
                    datum.setName(map.get(datum.getFileId()).getName());
                    datum.setResourceId(map.get(datum.getFileId()).getResourceId());
                    datum.setUrl(map.get(datum.getFileId()).getUrl());
                }
            }
        }
        return listImages;
    }


    @Override
    public R<LoginRotateImageInfoVo> info(Long id) {
        R<LoginRotateImageInfoVo> data = loginRotateImageBll.info(id);
        if (data.getData()!=null){
            LoginRotateImageInfoVo vo =  data.getData();
            List<FileShowVo> showVoList = fileBll.listByFileIds(Collections.singletonList(vo.getFileId()));
            if (Objects.nonNull(showVoList)&&!showVoList.isEmpty()){
                vo.setName(showVoList.get(0).getName());
                vo.setUrl(showVoList.get(0).getUrl());
                vo.setResourceId(showVoList.get(0).getResourceId());
            }
        }
        return data;
    }

    @Override
    public R<String> save(LoginRotateImageBo loginRotateImageBo) {

        return loginRotateImageBll.save(loginRotateImageBo);
    }

    @Override
    public R<String> update(LoginRotateImageBo loginRotateImageBo) {

        return loginRotateImageBll.update(loginRotateImageBo);
    }

    @Override
    public R<String> delete(Long id) {
        R<LoginRotateImageInfoVo> info = loginRotateImageBll.info(id);
        if (Objects.nonNull(info.getData())){
            fileBll.delete(info.getData().getFileId());
        }
        return loginRotateImageBll.delete(id);
    }


}

