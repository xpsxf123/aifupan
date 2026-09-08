package com.jiuyu.replay.common.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.bo.ClientLogBo;
import com.jiuyu.replay.common.bo.clientlog.RequestBo;
import com.jiuyu.replay.common.entity.ClientLogEntity;
import com.jiuyu.replay.common.repository.service.ClientLogService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.vo.ClientLogVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class ClientLogBll {

    @Autowired
    private ClientLogService clientLogService;

    /**
     * 保存客户端日志
     * @param clientLogBo
     * @return
     */
    public R<String> save(ClientLogBo clientLogBo){
        try {
            ClientLogEntity clientLogEntity = new ClientLogEntity();
            BeanUtils.copyProperties(clientLogBo, clientLogEntity);
            clientLogEntity.setId(SnowflakeManager.nextValue());
            clientLogEntity.setExecuteTime(new Date());

            clientLogService.save(clientLogEntity);
            return R.ok("添加成功");
        }catch (Exception e)
        {
            return R.error(1000,"添加失败,失败信息"+e.getMessage());
        }

    }

    /**
     * 分页查询客户端日志
     * @param requestBo
     * @param pageIndex
     * @param pageSize
     * @return
     */
    public R<PageUtils<ClientLogVo>> queryPage(RequestBo requestBo, Integer pageIndex, Integer pageSize) {

        if(pageIndex == null||pageIndex<0)
        {
            pageIndex = 1;
        }
        if(pageSize == null||pageSize<0)
        {
            pageSize = 10;
        }
        IPage<ClientLogEntity> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<ClientLogEntity> lambda = getLambda(requestBo);
        lambda.orderBy(true,false,ClientLogEntity::getExecuteTime);
        PageUtils<ClientLogEntity> pageData = new PageUtils<>(clientLogService.page(page, lambda));
        List<ClientLogEntity> list = pageData.getList();
        if(Objects.isNull(list)||list.size()==0)
        {
            int totalPage = pageData.getTotalPage();
            if(pageIndex>totalPage)
            {
                pageIndex=totalPage;
                IPage<ClientLogEntity> pageRe = new Page<>(pageIndex, pageSize);
                pageData=  new PageUtils<>(clientLogService.page(pageRe, lambda));
                list = pageData.getList();
            }
        }
        List<ClientLogVo> resultList = new ArrayList<>();
        PageUtils<ClientLogVo> result = new PageUtils<>();
        for (ClientLogEntity item : list) {
            ClientLogVo clientLogVo = new ClientLogVo();
            BeanUtils.copyProperties(item, clientLogVo);
            resultList.add(clientLogVo);
        }
        result.setList(resultList);
        result.setCurrPage(pageData.getCurrPage());
        result.setTotalCount(pageData.getTotalCount());
        result.setTotalPage(pageData.getTotalPage());
        return R.ok(result);
    }


    public R<String> deleteBySearch(RequestBo requestBo){
        try {
            LambdaQueryWrapper<ClientLogEntity> lambda = getLambda(requestBo);
            List<ClientLogEntity> list = clientLogService.list(lambda);
            clientLogService.removeBatchByIds(list);
            return R.ok("删除成功");
        }catch (Exception e)
        {
            log.info("删除失败,失败信息：{}",e.getMessage());
            return R.error(1001,"删除失败");
        }
    }

    private LambdaQueryWrapper<ClientLogEntity> getLambda(RequestBo requestBo){
        QueryWrapper<ClientLogEntity> queryWrapper = new QueryWrapper<>();
        LambdaQueryWrapper<ClientLogEntity> lambda = queryWrapper.lambda();
        if (requestBo.getActionName() != null && !"".equals(requestBo.getActionName())) {
            lambda.like(ClientLogEntity::getActionName, requestBo.getActionName());
        }
        if ((requestBo.getStartTime()!=null&&!"".equals(requestBo.getStartTime()))&&(requestBo.getEndTime()!=null&&!"".equals(requestBo.getEndTime()))) {

            lambda.between(ClientLogEntity::getExecuteTime, requestBo.getStartTime(), requestBo.getEndTime());
        }
        if(Objects.nonNull(requestBo.getLogType())&&requestBo.getLogType()!=-1)
        {
            lambda.eq(ClientLogEntity::getLogType, requestBo.getLogType());
        }
        return lambda;
    }

    /**
     * 根据id删除日志
     * @param ids
     * @return
     */
    public R<String> deleteByIds(List<Long> ids)
    {
        try {
            if(clientLogService.removeBatchByIds(ids))
            {
                return R.ok("删除成功");
            }else
            {
                return R.error(1002,"删除失败,请检测输入的Id是否正确");
            }

        }catch (Exception e)
        {
            log.info("删除失败,失败信息：{}",e.getMessage());
            return R.error(1001,"删除失败");
        }
    }
}
