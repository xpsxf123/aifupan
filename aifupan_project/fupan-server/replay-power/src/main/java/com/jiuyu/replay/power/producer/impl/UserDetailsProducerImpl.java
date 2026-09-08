package com.jiuyu.replay.power.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.diff.Business;
import com.jiuyu.replay.common.diff.DiffProcessor;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.feign.common.OperationLogFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.power.bo.CompanyBo;
import com.jiuyu.replay.power.bo.UserDetailsBo;
import com.jiuyu.replay.power.bo.UserDetailsListBo;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.bo.crm.CrmUserDetailsSyncBo;
import com.jiuyu.replay.power.entity.CompanyEntity;
import com.jiuyu.replay.power.entity.UserDetailsEntity;
import com.jiuyu.replay.power.producer.UserDetailsProducer;
import com.jiuyu.replay.power.repository.service.UserDetailsService;
import com.jiuyu.replay.power.repository.service.UserService;
import com.jiuyu.replay.power.repository.service.UserTokenService;
import com.jiuyu.replay.power.repository.service.impl.CompanyServiceImpl;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserDetailsInfoVo;
import com.jiuyu.replay.power.vo.UserDetailsListVo;
import com.jiuyu.replay.power.vo.UserDetailsVo;
import com.jiuyu.replay.power.vo.crm.CrmUserDetailsSyncVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 用户详情表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Service
@Slf4j
public class UserDetailsProducerImpl implements UserDetailsProducer {

    @Resource
    private UserDetailsService userDetailsService;
    @Resource
    private CompanyServiceImpl companyService;
    @Resource
    private UserService userService;
    @Resource
    private OperationLogFeign operationLogFeign;

    @Resource
    private DiffProcessor diffProcessor;
    @Resource
    private UserTokenService userTokenService;

    /**
     * 时区
     */
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");



    @Override
    public PageUtils<UserDetailsListVo> queryPage(UserDetailsListBo userDetailsListBo) {
        QueryWrapper<UserDetailsEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(userDetailsListBo.getKeyword())) {
            wrapper.like("name", userDetailsListBo.getKeyword());
        }

        IPage<UserDetailsEntity> iPage = userDetailsService.page(new Query<UserDetailsEntity>().getPage(userDetailsListBo.getPage(), userDetailsListBo.getLimit()), wrapper);

        PageUtils<UserDetailsListVo> pageUtils = new PageUtils<>(userDetailsListBo.getPage(), userDetailsListBo.getLimit(), iPage);

        List<UserDetailsEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<UserDetailsListVo> vos = records.stream().map(item -> {
                UserDetailsListVo userDetailsVo = new UserDetailsListVo();
                BeanUtils.copyProperties(item, userDetailsVo);
                return userDetailsVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UserDetailsInfoVo info(Long id) {

        UserDetailsEntity userDetailsEntity = userDetailsService.getById(id);
        if (userDetailsEntity != null) {
            UserDetailsInfoVo userDetailsInfoVo = new UserDetailsInfoVo();
            BeanUtils.copyProperties(userDetailsEntity, userDetailsInfoVo);
            return userDetailsInfoVo;
        }

        return null;
    }

    /**
     * 新增用户详情表
     * @param userDetailsBo 用户详情表对象
     * @return
     */
     public UserDetailsInfoVo save(UserDetailsBo userDetailsBo) {

         UserDetailsEntity userDetailsEntity = new UserDetailsEntity();
         BeanUtils.copyProperties(userDetailsBo, userDetailsEntity);
         userDetailsEntity.setId(SnowflakeManager.nextValue());
         userDetailsEntity.setCreateDate(new Date());
         userDetailsEntity.setUpdateDate(new Date());

         userDetailsService.save(userDetailsEntity);

         UserDetailsInfoVo userDetailsInfoVo = new UserDetailsInfoVo();
         BeanUtils.copyProperties(userDetailsEntity, userDetailsInfoVo);

         return userDetailsInfoVo;
     }

    /**
     * 修改用户详情表
     * @param userDetailsBo 用户详情表对象
     * @return
     */
    public void update(UserDetailsBo userDetailsBo) {

        UserDetailsEntity userDetailsEntity = new UserDetailsEntity();
        BeanUtils.copyProperties(userDetailsBo, userDetailsEntity);
        userDetailsEntity.setUpdateDate(new Date());

        userDetailsService.updateById(userDetailsEntity);
    }

    /**
     * 删除用户详情表
     * @param id 用户详情表id
     * @return
     */
    public void deleteById(Long id) {

        userDetailsService.removeById(id);
    }

    /**
     * 客户端保存或修改用户详情接口
     *
     * @param userDetailsBo
     */
    @Override
    public void saveUserDetails(UserDetailsBo userDetailsBo) {

        // 查看是否有数据
        UserDetailsEntity details = userDetailsService.getOne(new LambdaQueryWrapper<UserDetailsEntity>()
                .eq(UserDetailsEntity::getUserId, userDetailsBo.getUserId())
        );
        if (ObjectUtil.isNotEmpty(details)) userDetailsBo.setId(details.getId());

        if (ObjectUtil.isNotEmpty(userDetailsBo.getEmail())){
            String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            // 编译正则表达式
            Pattern pattern = Pattern.compile(emailRegex);
            if(!pattern.matcher(userDetailsBo.getEmail()).matches()) {
                RRException.create("请填写正确的邮箱格式");
            }
        }


        //[操作记录]-获取原用户详情信息
        UserDetailsEntity beforeUserById = getObjById(userDetailsBo.getUserId(), new UserDetailsEntity());
        //公司
        //[操作记录]-获取原公司信息
        Long companyId = null;
        if (beforeUserById!=null){
            companyId = beforeUserById.getCompanyId();
        }
        // 公司原信息
        CompanyEntity beforeObjById = getObjById(companyId, new CompanyEntity());
        if (beforeObjById==null){
            beforeObjById= new CompanyEntity();
        }
        if (beforeUserById==null){
            beforeUserById= new UserDetailsEntity();
        }
        //[操作记录]-获取修改后公司的信息
        CompanyEntity afterObjById =new CompanyEntity();

        if(ObjectUtil.isNotEmpty(userDetailsBo.getAnchorType()) && userDetailsBo.getAnchorType()==1){
            CompanyBo companyBo = userDetailsBo.getCompany();
            if (ObjectUtil.isNotEmpty(userDetailsBo.getCompany().getEmail())){
                String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
                // 编译正则表达式
                Pattern pattern = Pattern.compile(emailRegex);
                if(!pattern.matcher(companyBo.getEmail()).matches()) {
                    RRException.create("请填写正确的邮箱格式");
                }
            }
            CompanyEntity companyEntity = new CompanyEntity();
            BeanUtils.copyProperties(companyBo, companyEntity);
            companyEntity.setCreateDate(new Date());
            companyEntity.setUpdateDate(new Date());
            if (ObjectUtil.isNotEmpty(companyBo.getId())){
                companyService.updateById(companyEntity);
            }else {
                companyEntity.setId(SnowflakeManager.nextValue());
                companyService.save(companyEntity);
                userDetailsBo.setCompanyId(companyEntity.getId());
            }
            BeanUtil.copyProperties(companyEntity,afterObjById);
        }
        // 用户备注

        //[操作记录]-获取修改后用户详情信息
        UserDetailsEntity afterUserById = new UserDetailsEntity();

        // 插入或更新

        Date now = new Date();
        if (ObjectUtil.isEmpty(userDetailsBo.getId())){
            UserDetailsEntity entity = BeanUtil.copyProperties(userDetailsBo, UserDetailsEntity.class);
            entity.setId(SnowflakeManager.nextValue());
            entity.setCreateDate(now);
            entity.setUpdateDate(now);
            userDetailsService.save(entity);
            BeanUtil.copyProperties(entity,afterUserById);
        }else{
            UserDetailsEntity entity = BeanUtil.copyProperties(userDetailsBo, UserDetailsEntity.class);
            entity.setUpdateDate(now);
            userDetailsService.updateById(entity);
            BeanUtil.copyProperties(entity,afterUserById);
        }
        //[操作记录]-进行对比
        toCompareBeforeAndAfter(beforeUserById, afterUserById,beforeObjById,afterObjById,userDetailsBo);
    }





    /**
     *  1、如果 原用户详情 null  说明这是新增  则不进行比较  用户详情和公司是关联存在的 【不存在 用户有 公司null的情况】
     *  2、比较用户修改了 [关注字段]，获取修改后的关注字段
     *  3、比较公司修改了 [关注字段]，获取修改后的关注字段
     * @param beforeUserById 原用户详情
     * @param afterUserById  现用户详情
     * @param beforeObjById  原公司
     * @param afterObjById   现公司
     */
    private void toCompareBeforeAndAfter(UserDetailsEntity beforeUserById
            , UserDetailsEntity afterUserById, CompanyEntity beforeObjById
            , CompanyEntity afterObjById,UserDetailsBo userDetailsBo) {
        if (ObjectUtil.isEmpty(beforeUserById)){
            log.info("[用户详情]-新增用户详情-无需记录");
            return;
        }

        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (localUser!=null){
            Map<String, Object> allBeforeMap = new HashMap<>();
            Map<String, Object> allAfterMap = new HashMap<>();
            //获取得到差异的字段
            List<Map<String, Object>> mapList = diffProcessor.compareGetMap(Business.USER_DETAILS, afterUserById, beforeUserById);
            if (CollUtil.isNotEmpty(mapList)&&mapList.size()>= 2){
                allBeforeMap.putAll(mapList.get(0));
                allAfterMap.putAll(mapList.get(1));
            }
            //如果是公司类型-比较公司
            List<Map<String, Object>> mapcompList = diffProcessor.compareGetMap(Business.USER_DETAILS, afterObjById, beforeObjById);
            if (CollUtil.isNotEmpty(mapcompList)&&mapcompList.size()>=2){
                allBeforeMap.putAll(mapcompList.get(0));
                allAfterMap.putAll(mapcompList.get(1));
            }
            //[保存操作记录]
            operationLogFeign.saveOptLog(Business.USER_DETAILS.getDesc()
                    ,afterUserById.getId(),userDetailsBo.getUserId(), allBeforeMap, allAfterMap, localUser.getId(),localUser.getIp(),localUser.getNickName());
            return;
        }
        log.info("[用户详情]-无登录者-无需记录");
    }


    /**
     * 通过ID查询  用户详情/用户的公司详情
     * @param id
     * @return
     * @param <T> 不可为null
     */
    private <T> T getObjById(Long id,T t) {
        if (id!=null){
                if (t instanceof CompanyEntity){
                    CompanyEntity companyEntity =  companyService.getById(id);
                    if (ObjectUtil.isNotEmpty(companyEntity)){
                        BeanUtil.copyProperties(companyEntity, t);
                        return t;
                    }
                    log.info("[用户详情]-通过公司id：{}查公司：数据不存在",id);
                }else if (t instanceof UserDetailsEntity){
                    LambdaQueryWrapper<UserDetailsEntity> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(UserDetailsEntity::getUserId, id).last("limit 1");
                    UserDetailsEntity userDetailsEntity = userDetailsService.getOne(queryWrapper);
                    if (ObjectUtil.isNotEmpty(userDetailsEntity)){
                        BeanUtil.copyProperties(userDetailsEntity, t);
                        return t;
                    }
                    log.info("[用户详情]-通过用户id：{}查用户：数据不存在",id);
                }
        }
        return null;
    }



    @Override
    public UserDetailsInfoVo userDetailByUserId(Long userId) {
        UserDetailsEntity one = userDetailsService.getOne(new LambdaUpdateWrapper<UserDetailsEntity>()
                .eq(UserDetailsEntity::getUserId, userId)
        );
        if(one!=null){
            UserDetailsInfoVo userDetailsInfoVo = new UserDetailsInfoVo();
            BeanUtils.copyProperties(one, userDetailsInfoVo);
            return userDetailsInfoVo;
        }
        return null;
    }

    /**
     * 根据销售人员ID查询用户
     * @param salesId 销售人员ID
     * @return
     */
    @Override
    public List<Long> getUserIdsBySalesId(Long salesId) {
        List<UserDetailsEntity> userDetailsEntity = userDetailsService.list();
        List<Long> userIds = userDetailsEntity.stream().filter(item -> item.getSaleId().equals(salesId)).map(item ->{
            Long userId = 0L;
            userId = item.getUserId();

            return userId;
        }).toList();

        return userIds;
    }

    /**
     * 查询当前渠道是否有绑定用户
     * @param id 渠道ID
     * @return
     */
    @Override
    public Long getByChannelId(Long id) {
        List<UserDetailsEntity> detailsInfoList = userDetailsService.list(new QueryWrapper<UserDetailsEntity>().eq("channel_id", id));
        long count = detailsInfoList.stream().count();
        return count;
    }

    /**
     * 查询当销售人员是否有绑定的用户
     * @param id 销售人员ID
     * @return
     */
    @Override
    public Long getBySaleId(Long id) {
        return userDetailsService.lambdaQuery()
                .eq(UserDetailsEntity::getSaleId, id)
                .count();
    }

    /**
     * 根据微信名称查询用户
     * @param userWxName
     * @return
     */
    @Override
    public List<Long> getUserByWxName(String userWxName) {
        List<UserDetailsEntity> userDetaList = userDetailsService.list(new QueryWrapper<UserDetailsEntity>().like("wx_name",userWxName));
        List<Long> userIdsByWxName = new ArrayList<>();
        userDetaList.stream().forEach(item -> userIdsByWxName.add(item.getUserId()));
        return userIdsByWxName;
    }


    @Override
    public UserDetailsInfoVo getByUserId(Long userId) {

        UserDetailsEntity userDetailsEntity = this.userDetailsService.getOne(new QueryWrapper<UserDetailsEntity>().eq("user_id", userId));
        if(userDetailsEntity != null) {
            UserDetailsInfoVo userDetailsInfoVo = new UserDetailsInfoVo();
            BeanUtils.copyProperties(userDetailsEntity, userDetailsInfoVo);
            return userDetailsInfoVo;
        }

        return null;
    }



    /**
     * 根据查询条件查询用户详情
     * 支持：根据用户意向、客户类型、是否登录 查询用户详情
     * @param userListBo
     * @return
     */
    @Override
    public List<UserDetailsVo> selectByQuery(UserListBo userListBo) {
        List<UserDetailsEntity> list = userDetailsService.lambdaQuery()
                //筛选用户意向
                .eq(StringUtil.isNotBlank(userListBo.getUserAmbition()), UserDetailsEntity::getUserAmbition, userListBo.getUserAmbition())
                //筛选客户类型
                .eq(ObjectUtil.isNotNull(userListBo.getUserBelongType()), UserDetailsEntity::getUserBelongType, userListBo.getUserBelongType())
                //筛选是否登录
                .eq(ObjectUtil.isNotNull(userListBo.getIsLoggedIn()), UserDetailsEntity::getIsLoggedIn, userListBo.getIsLoggedIn())
                //查询 字段id、userId
                .select(UserDetailsEntity::getId, UserDetailsEntity::getUserId)
                .list();
        return BeanUtil.copyToList(list, UserDetailsVo.class);
    }



    @Override
    public UserDetailsInfoVo getDetailByUserId(Long userId) {
        UserDetailsEntity userDetailsEntity = userDetailsService
                .lambdaQuery()
                .eq(UserDetailsEntity::getUserId, userId)
                .last("limit 1")
                .one();
        return userDetailsEntity != null ? BeanUtil.copyProperties(userDetailsEntity, UserDetailsInfoVo.class) : null;
    }

    @Override
    public List<UserDetailsVo> selectByUserIds(List<Long> longList) {
        if (!ObjectUtil.isEmpty(longList)){
            List<UserDetailsEntity> list = userDetailsService.lambdaQuery().in(UserDetailsEntity::getUserId, longList).list();
            if (ObjectUtil.isNotEmpty(list)){
                return BeanUtil.copyToList(list, UserDetailsVo.class);
            }
        }
        return List.of();
    }

    @Override
    public CrmUserDetailsSyncVo saveCrmProfileFields(CrmUserDetailsSyncBo bo) {
        UserDetailsEntity entity = userDetailsService.lambdaQuery()
                .eq(UserDetailsEntity::getUserId, bo.getUserId())
                .last("limit 1")
                .one();

        Date now = new Date();
        if (entity == null) {
            entity = new UserDetailsEntity();
            entity.setId(SnowflakeManager.nextValue());
            entity.setUserId(bo.getUserId());
            entity.setCreateDate(now);
            entity.setIsDeleted(0);
        }

        entity.setUserAmbition(bo.getUserAmbition());
        entity.setUserBelongType(bo.getUserBelongType());
        entity.setUpdateDate(now);
        userDetailsService.saveOrUpdate(entity);

        CrmUserDetailsSyncVo result = new CrmUserDetailsSyncVo();
        result.setUserId(entity.getUserId());
        result.setUserAmbition(entity.getUserAmbition());
        result.setUserBelongType(entity.getUserBelongType());
        return result;
    }

    /**
     * 修改用户详情表登录状态
     *
     * @param id 用户ID
     * @return 更新结果
     */
    @Override
    public boolean updateLoggedInStatus(Long id) {
        if (id == null){
            log.info("用户详情[是否登录过]修改失败userId为null");
            return false;
        }
        LocalDateTime now = LocalDateTime.now(ZONE_SHANGHAI);
        boolean update = userDetailsService.lambdaUpdate()
                // 根据用户ID修改
                .eq(UserDetailsEntity::getUserId, id)
                // 修改为已登录
                .set(UserDetailsEntity::getIsLoggedIn, 1)
                // 修改者 （当前登陆者）
                .set(UserDetailsEntity::getUpdateId, id)
                // 修改时间
                .set(UserDetailsEntity::getUpdateDate, now)
                .update();
        return update;
    }

    @Override
    public Long countClientDetailBySaleId(Long salesId) {
        return userDetailsService.countClientDetailBySaleId(salesId);
    }
}
