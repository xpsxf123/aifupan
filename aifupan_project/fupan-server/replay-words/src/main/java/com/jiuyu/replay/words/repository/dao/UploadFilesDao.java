package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.UploadFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
  *@author tisheng
  *@date 2024/9/5
  *@apinNote
 *
  */
@Mapper
public interface UploadFilesDao extends BaseMapper<UploadFileEntity> {

}
