package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.CruxTypeBo;
import com.jiuyu.replay.words.bo.CruxTypeListBo;
import com.jiuyu.replay.words.entity.CruxTypeEntity;
import com.jiuyu.replay.words.entity.SensitiveWordsEntity;
import com.jiuyu.replay.words.producer.CruxTypeProducer;
import com.jiuyu.replay.words.repository.service.CruxTypeService;
import com.jiuyu.replay.words.repository.service.SensitiveWordsService;
import com.jiuyu.replay.generic.vo.words.CruxTypeInfoVo;
import com.jiuyu.replay.words.vo.CruxTypeListVo;
import com.jiuyu.replay.words.vo.CruxTypeTreeVo;
import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 关键词类型
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Service
public class CruxTypeProducerImpl implements CruxTypeProducer {

    @Resource
    private CruxTypeService cruxTypeService;
    @Resource
    private SensitiveWordsService sensitiveWordsService;


    @Override
    public PageUtils<CruxTypeListVo> queryPage(CruxTypeListBo cruxTypeListBo) {
        QueryWrapper<CruxTypeEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(cruxTypeListBo.getKeyword())){
            wrapper.like("name", cruxTypeListBo.getKeyword());
        }
        wrapper.orderByAsc("sort");

        IPage<CruxTypeEntity> iPage = cruxTypeService.page(new Query<CruxTypeEntity>().getPage(cruxTypeListBo.getPage(), cruxTypeListBo.getLimit()), wrapper);

        PageUtils<CruxTypeListVo> pageUtils = new PageUtils<>(cruxTypeListBo.getPage(), cruxTypeListBo.getLimit(), iPage);

        List<CruxTypeEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<CruxTypeListVo> vos = records.stream().map(item -> {
                CruxTypeListVo cruxTypeVo = new CruxTypeListVo();
                BeanUtils.copyProperties(item, cruxTypeVo);
                return cruxTypeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public CruxTypeInfoVo info(Long id) {

        CruxTypeEntity cruxTypeEntity = cruxTypeService.getById(id);
        if(cruxTypeEntity != null) {
            CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
            BeanUtils.copyProperties(cruxTypeEntity, cruxTypeInfoVo);
            return cruxTypeInfoVo;
        }

        return null;
    }

    /**
     * 新增关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
     public CruxTypeInfoVo save(CruxTypeBo cruxTypeBo) {

         CruxTypeEntity cruxTypeEntity = new CruxTypeEntity();
         BeanUtils.copyProperties(cruxTypeBo, cruxTypeEntity);
         cruxTypeEntity.setId(SnowflakeManager.nextValue());
         cruxTypeEntity.setCreateDate(new Date());
         cruxTypeEntity.setUpdateDate(new Date());

         cruxTypeService.save(cruxTypeEntity);

         CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
         BeanUtils.copyProperties(cruxTypeEntity, cruxTypeInfoVo);

         return cruxTypeInfoVo;
     }

    /**
     * 修改关键词类型
     * @param cruxTypeBo 关键词类型对象
     * @return
     */
    public void update(CruxTypeBo cruxTypeBo) {

        CruxTypeEntity cruxTypeEntity = new CruxTypeEntity();
        BeanUtils.copyProperties(cruxTypeBo, cruxTypeEntity);
        cruxTypeEntity.setUpdateDate(new Date());

        cruxTypeService.updateById(cruxTypeEntity);
    }

    /**
     * 删除关键词类型
     * @param id 关键词类型id
     * @return
     */
    public void deleteById(Long id) {

        cruxTypeService.removeById(id);
    }

    @Override
    public List<CruxTypeVo> getShowCompassList() {

        List<CruxTypeEntity> cruxTypeEntities = cruxTypeService.list(
                new QueryWrapper<CruxTypeEntity>().eq("is_show_compass", 1).orderByAsc("sort"));

        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
            List<CruxTypeVo> cruxTypeVoList = cruxTypeEntities.stream().map(item -> {
                CruxTypeVo cruxTypeVo = new CruxTypeVo();
                BeanUtils.copyProperties(item, cruxTypeVo);
                return cruxTypeVo;
            }).collect(Collectors.toList());

            return cruxTypeVoList;
        }

        return null;
    }

    @Override
    public List<CruxTypeTreeVo> listTree(Integer childrenNotNull) {

        // 获取所有关键词类型
        List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list(new QueryWrapper<CruxTypeEntity>().orderByAsc("sort"));
        // 获取所有关键词(用于统计每个关键词类型的关键词数量)
        QueryWrapper<SensitiveWordsEntity> wordsWrapper = new QueryWrapper<>();
        wordsWrapper.eq("words_type", 1);
        wordsWrapper.select("id", "crux_type_id");
        List<SensitiveWordsEntity> cruxWordsEntities = this.sensitiveWordsService.list(wordsWrapper);

        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
            List<CruxTypeTreeVo> cruxTypeTreeVoList = cruxTypeEntities.stream().filter((item) -> item.getParentId() == 0)
                    .map(item -> {
                        CruxTypeTreeVo cruxTypeTreeVo = new CruxTypeTreeVo();
                        BeanUtils.copyProperties(item, cruxTypeTreeVo);
                        // 封装子菜单
                        cruxTypeTreeVo.setChildren(getChildrenMenu(cruxTypeTreeVo, cruxTypeEntities, cruxWordsEntities, childrenNotNull));
                        // 封装关键词数量
                        cruxTypeTreeVo.setCruxNum(0);
                        if (cruxWordsEntities != null && cruxWordsEntities.size() > 0) {
                            for (SensitiveWordsEntity cruxWord : cruxWordsEntities) {
                                if (cruxTypeTreeVo.getId().equals(cruxWord.getCruxTypeId())) {
                                    cruxTypeTreeVo.setCruxNum(cruxTypeTreeVo.getCruxNum() + 1);
                                }
                            }
                        }
                        // 封装父级id数组
                        cruxTypeTreeVo.setParentIdArr(getParentIdArr(cruxTypeTreeVo, cruxTypeEntities));
                        // 设置id层级数组
                        List<Long> idArr = new LinkedList<>(cruxTypeTreeVo.getParentIdArr());
                        idArr.add(cruxTypeTreeVo.getId());
                        cruxTypeTreeVo.setIdArr(idArr);

                        return cruxTypeTreeVo;
                    }).toList();

            return cruxTypeTreeVoList;
        }

        return null;
    }

    /**
     * 获取父级id列表
     * @param cruxTypeVo 当前分类
     * @param cruxTypeEntities 所有分类
     * @return
     */
    private List<Long> getParentIdArr(CruxTypeVo cruxTypeVo, List<CruxTypeEntity> cruxTypeEntities) {


        LinkedList<Long> parentIdArr = new LinkedList<>();

        // 获取当前分类
        CruxTypeEntity currentCruxType = new CruxTypeEntity();
        for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
            if(cruxTypeVo.getId().equals(cruxTypeEntity.getId())) {
                currentCruxType = cruxTypeEntity;
            }
        }

        if(!currentCruxType.getParentId().equals(0L)) {
            // 父id队列
            Deque<Long> stack = new LinkedList<>();

            while (!currentCruxType.getParentId().equals(0L)) {
                for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
                    if(cruxTypeEntity.getId().equals(currentCruxType.getParentId())) {
                        stack.push(cruxTypeEntity.getId());
                        currentCruxType = cruxTypeEntity;
                    }
                }
            }

            // 将队列id弹出
            while (!stack.isEmpty()) {
                parentIdArr.add(stack.pop());
            }

        }

        return parentIdArr;
    }

    @Override
    public List<Long> getParentIdArr(Long id) {

        LinkedList<Long> parentIdArr = new LinkedList<>();

        // 获取当前分类
        CruxTypeEntity currentCruxType = this.cruxTypeService.getById(id);

        if(currentCruxType != null && !currentCruxType.getParentId().equals(0L)) {
            // 获取所有分类
            List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list();
            // 父id队列
            Deque<Long> stack = new LinkedList<>();

            while (!currentCruxType.getParentId().equals(0L)) {
                for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
                    if(cruxTypeEntity.getId().equals(currentCruxType.getParentId())) {
                        stack.push(cruxTypeEntity.getId());
                        currentCruxType = cruxTypeEntity;
                    }
                }
            }

            // 将队列id弹出
            while (!stack.isEmpty()) {
                parentIdArr.add(stack.pop());
            }

        }

        return parentIdArr;
    }

    @Override
    public List<Long> getParentIdContainerSelfArr(Long cruxTypeId) {
        List<Long> parentIdArr = this.getParentIdArr(cruxTypeId);
        if(!cruxTypeId.equals(0L)) {
            parentIdArr.add(cruxTypeId);
        }
        return parentIdArr;
    }

    @Override
    public List<CruxTypeInfoVo> listByParentId(Long parentId) {
        List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list(new QueryWrapper<CruxTypeEntity>().eq("parent_id", parentId));
        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
            List<CruxTypeInfoVo> cruxTypeInfoVos = cruxTypeEntities.stream().map(item -> {
                CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
                BeanUtils.copyProperties(item, cruxTypeInfoVo);
                return cruxTypeInfoVo;
            }).toList();
            return cruxTypeInfoVos;
        }
        return null;
    }

    @Override
    public List<CruxTypeInfoVo> listAll() {

        List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list(new QueryWrapper<CruxTypeEntity>().orderByAsc("sort"));
        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {

            List<CruxTypeInfoVo> vos = cruxTypeEntities.stream().map(item -> {
                CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
                BeanUtils.copyProperties(item, cruxTypeInfoVo);

                // 设置父级数组
                setCruxTypeParentArr(cruxTypeInfoVo, cruxTypeEntities);

                return cruxTypeInfoVo;
            }).collect(Collectors.toList());

            return vos;
        }

        return null;
    }

    @Override
    public List<CruxTypeInfoVo> listByLevel(int level) {
        List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list(new QueryWrapper<CruxTypeEntity>().eq("level", level));

        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
            List<CruxTypeInfoVo> cruxTypeInfoVos = cruxTypeEntities.stream().map(item -> {
                CruxTypeInfoVo cruxTypeInfoVo = new CruxTypeInfoVo();
                BeanUtils.copyProperties(item, cruxTypeInfoVo);
                return cruxTypeInfoVo;
            }).collect(Collectors.toList());

            return cruxTypeInfoVos;
        }

        return null;
    }

    @Override
    public List<Long> getChildrenIdAndSelfIdList(Long cruxTypeId) {

        List<Long> ids = new LinkedList<>();

        List<CruxTypeEntity> cruxTypeEntities = this.cruxTypeService.list();
        if(cruxTypeEntities != null && cruxTypeEntities.size() > 0) {
            ids.add(cruxTypeId);
            for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
                if(cruxTypeEntity.getParentId().equals(cruxTypeId)) {
                    ids.add(cruxTypeEntity.getId());
                    getChildrenRecursion(cruxTypeEntities, cruxTypeEntity.getId(), ids);
                }
            }
        }

        return ids;
    }

    /**
     * 递归封装分类下的所有层级的子分类id
     * @param cruxTypeEntities 所有分类列表
     * @param id 当前分类id
     * @param ids id结果集合
     */
    private void getChildrenRecursion(List<CruxTypeEntity> cruxTypeEntities, Long id, List<Long> ids) {
        for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
            if(cruxTypeEntity.getParentId().equals(id)) {
                ids.add(cruxTypeEntity.getId());
                getChildrenRecursion(cruxTypeEntities, cruxTypeEntity.getId(), ids);
            }
        }
    }

    /**
     * 设置关键词分类的父id数组、父名称数组等信息
     * @param cruxTypeInfoVo
     * @param cruxTypeEntities
     */
    private void setCruxTypeParentArr(CruxTypeInfoVo cruxTypeInfoVo, List<CruxTypeEntity> cruxTypeEntities) {
        List<Long> parentIdArr = new LinkedList<>();
        List<String> parentNameArr = new LinkedList<>();

        // 获取当前分类
        CruxTypeEntity currentCruxType = new CruxTypeEntity();
        for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
            if(cruxTypeInfoVo.getId().equals(cruxTypeEntity.getId())) {
                currentCruxType = cruxTypeEntity;
            }
        }

        if(!currentCruxType.getParentId().equals(0L)) {
            // 父队列
            Deque<CruxTypeEntity> stack = new LinkedList<>();

            while (!currentCruxType.getParentId().equals(0L)) {
                for (CruxTypeEntity cruxTypeEntity : cruxTypeEntities) {
                    if(cruxTypeEntity.getId().equals(currentCruxType.getParentId())) {
                        stack.push(cruxTypeEntity);
                        currentCruxType = cruxTypeEntity;
                    }
                }
            }

            // 将队列弹出
            while (!stack.isEmpty()) {
                CruxTypeEntity cruxTypeEntity = stack.pop();
                parentIdArr.add(cruxTypeEntity.getId());
                parentNameArr.add(cruxTypeEntity.getName());
            }

        }

        cruxTypeInfoVo.setParentIdArr(parentIdArr);
        List<Long> idArr = new LinkedList<>(cruxTypeInfoVo.getParentIdArr());
        idArr.add(cruxTypeInfoVo.getId());
        cruxTypeInfoVo.setIdArr(idArr);

        cruxTypeInfoVo.setParentNameArr(parentNameArr);
        List<String> nameArr = new LinkedList<>(cruxTypeInfoVo.getParentNameArr());
        nameArr.add(cruxTypeInfoVo.getName());
        cruxTypeInfoVo.setNameArr(nameArr);

    }

    /**
     * 返回当前关键词类型的子关键词类型
     * @param cruxTypeTreeVo 关键词类型
     * @param cruxTypeEntities 全部关键词类型列表
     * @param cruxWordsEntities 关键词列表
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    private List<CruxTypeTreeVo> getChildrenMenu(CruxTypeTreeVo cruxTypeTreeVo, List<CruxTypeEntity> cruxTypeEntities, List<SensitiveWordsEntity> cruxWordsEntities, Integer childrenNotNull) {

        List<CruxTypeTreeVo> childrenCruxTypeTreeVos = cruxTypeEntities.stream().filter(item -> item.getParentId().equals(cruxTypeTreeVo.getId())).map(item -> {
            CruxTypeTreeVo childrenCruxTypeTreeVo = new CruxTypeTreeVo();
            BeanUtils.copyProperties(item, childrenCruxTypeTreeVo);
            childrenCruxTypeTreeVo.setChildren(getChildrenMenu(childrenCruxTypeTreeVo, cruxTypeEntities, cruxWordsEntities, childrenNotNull));
            // 封装关键词和敏感词数量
            childrenCruxTypeTreeVo.setCruxNum(0);
            if (cruxWordsEntities != null && cruxWordsEntities.size() > 0) {
                for (SensitiveWordsEntity cruxWordsEntity : cruxWordsEntities) {
                    if (childrenCruxTypeTreeVo.getId().equals(cruxWordsEntity.getCruxTypeId())) {
                        childrenCruxTypeTreeVo.setCruxNum(childrenCruxTypeTreeVo.getCruxNum() + 1);
                    }
                }
            }
            // 封装父级id数组
            childrenCruxTypeTreeVo.setParentIdArr(getParentIdArr(childrenCruxTypeTreeVo, cruxTypeEntities));
            // 设置id层级数组
            List<Long> idArr = new LinkedList<>(childrenCruxTypeTreeVo.getParentIdArr());
            idArr.add(childrenCruxTypeTreeVo.getId());
            childrenCruxTypeTreeVo.setIdArr(idArr);

            return childrenCruxTypeTreeVo;
        }).toList();

        if(childrenCruxTypeTreeVos.size() == 0) {
            if(!StringUtils.isEmpty(childrenNotNull) && childrenNotNull == 1) {
                return childrenCruxTypeTreeVos;
            }else {
                return null;
            }

        }

        return childrenCruxTypeTreeVos;

    }


}

