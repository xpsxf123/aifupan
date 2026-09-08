package com.jiuyu.governance.business.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.governance.business.org.pojo.entity.Team;
import com.jiuyu.governance.business.org.pojo.request.TeamPageQueryRequest;
import com.jiuyu.governance.business.org.pojo.request.TeamSelectQueryRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小组
 *
 * @author hehui
 * @date 2026-03-18 17:23
 */
@Mapper
public interface TeamMapper extends BaseMapper<Team> {

    /**
     * 分页查询
     *
     * @param page 页
     * @param req 要求
     * @return {@link IPage }<{@link Team }>
     */
    IPage<Team> pageQueryTeam(@Param("page") IPage<Team> page, @Param("req") TeamPageQueryRequest req);


    /**
     * 列表查询
     *
     * @param req 要求
     * @return {@link List }<{@link Team }>
     */
    List<Team> listQueryTeam(@Param("req") TeamSelectQueryRequest req);
}
