package com.jiuyu.replay.system.bll;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.ai.AiContentCorrectionConfigVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.system.bo.DictDataBo;
import com.jiuyu.replay.system.bo.DictDataListBo;
import com.jiuyu.replay.system.producer.DictDataProducer;
import com.jiuyu.replay.system.vo.DictDataInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 字典
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Component
public class DictDataBll {

    @Resource
    private DictDataProducer dictDataProducer;

    /**
     * 根据字典类型标识和字典值查询字典信息
     * @param code
     * @param value
     * @return
     */
    public DictDataListVo dictDataByValue(String code, String value){
        RRException.isNotEmpty(code, "字典类型标识不能为空");
        RRException.isNotEmpty(value, "字典值不能为空");

        List<DictDataListVo> dictDataListVos = this.dictDataProducer.listByTypeLogo(code);
        if (dictDataListVos != null && !dictDataListVos.isEmpty()) {
            for (DictDataListVo dictDataListVo : dictDataListVos) {
                if(dictDataListVo.getValue().equals(value)) {
                    return dictDataListVo;
                }
            }
        }

        return null;
    }

    /**
     * 根据字典类型标识和字典值查询字典信息，label返回树形路径名称
     * @param code 字典类型标识
     * @param value 字典值
     * @return 字典信息，label格式如: 父级/子级/当前
     */
    public DictDataListVo dictDataTreeByValue(String code, String value) {
        RRException.isNotEmpty(code, "字典类型标识不能为空");
        RRException.isNotEmpty(value, "字典值不能为空");

        List<DictDataListVo> list = this.dictDataProducer.listByTypeLogo(code);
        if (ObjectUtil.isEmpty(list)) {
            return null;
        }

        return list.stream()
                .filter(vo -> value.equals(vo.getValue()))
                .findFirst()
                .map(vo -> {
                    vo.setLabel(buildTreePath(vo, list));
                    return vo;
                })
                .orElse(null);
    }

    /**
     * 迭代构建树形路径名称（避免递归栈溢出）
     * 使用Map预处理，时间复杂度从O(n²)优化为O(n)
     */
    private String buildTreePath(DictDataListVo vo, List<DictDataListVo> list) {
        // 预处理：构建id到节点的Map，O(n)
        Map<Long, DictDataListVo> idMap = list.stream()
                .collect(Collectors.toMap(DictDataListVo::getId, Function.identity(), (a, b) -> a));
        return buildTreePathWithMap(vo, idMap);
    }

    /**
     * 批量构建树形路径名称，所有对象的label返回有层级的label
     * 使用Map预处理，避免重复构建Map，时间复杂度O(n)
     *
     * @param list 字典列表
     * @return 处理后的列表，每个对象的label为树形路径格式
     */
    public List<DictDataListVo> buildTreePathLabels(List<DictDataListVo> list) {
        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }
        // 预处理：构建id到节点的Map，只构建一次
        Map<Long, DictDataListVo> idMap = list.stream()
                .collect(Collectors.toMap(DictDataListVo::getId, Function.identity(), (a, b) -> a));
        list.forEach(vo -> vo.setLabel(buildTreePathWithMap(vo, idMap)));
        return list;
    }

    /**
     * 使用预处理的Map迭代构建树形路径名称
     */
    private String buildTreePathWithMap(DictDataListVo vo, Map<Long, DictDataListVo> idMap) {
        LinkedList<String> pathParts = new LinkedList<>();
        DictDataListVo current = vo;
        while (current != null) {
            pathParts.addFirst(current.getLabel());
            if (current.getParentId() == null || current.getParentId() == 0L) {
                break;
            }
            current = idMap.get(current.getParentId());
        }
        return String.join("/", pathParts);
    }

    /**
     * 根据字典标签查询字典信息
     *
     * @param code  字典类型标识
     * @param label 字典标签
     * @return 字典
     */
    public DictDataListVo dictDataByLabel(String code, String label) {
        RRException.isNotEmpty(code, "字典类型标识不能为空");
        RRException.isNotEmpty(label, "label值不能为空");

        List<DictDataListVo> dictDataListVos = this.dictDataProducer.listByTypeLogo(code);
        if (dictDataListVos != null && !dictDataListVos.isEmpty()) {
            for (DictDataListVo dictDataListVo : dictDataListVos) {
                if (ObjectUtil.equals(label, dictDataListVo.getLabel())) {
                    return dictDataListVo;
                }
            }
        }

        return null;
    }

    /**
     * 根据字典类型标识查询字典信息
     * @param code
     * @return
     */
    public List<DictDataListVo> dictDataListByCode(String code){
        RRException.isNotEmpty(code, "字典类型标识不能为空");

        List<DictDataListVo> dictDataListVos = this.dictDataProducer.listByTypeLogo(code);

        if (dictDataListVos != null && !dictDataListVos.isEmpty()) {
            return dictDataListVos;
        }
        return new ArrayList<>();
    }

    public List<DictDataListVo> dictDataTreeListByCode(String code, boolean isShowParentName) {
        RRException.isNotEmpty(code, "字典类型标识不能为空");

        List<DictDataListVo> tempList = this.dictDataProducer.listByTypeLogo(code);

        return buildTree(tempList, isShowParentName);
    }

    /**
     * 根据场景类型获取AI内容校正配置（模型code和提示词）。
     * 从字典 ai_content_correction 树形数据中查询：
     * 第一层节点 value 对应 sceneType，第二层子节点中 label=model_code 的 value 为模型code，
     * label=prompt_content 的 value 为提示词。
     *
     * @param sceneType 场景类型 0-AI问答助手纠正检查 1-AI问答助手纠正 2-自然/优化原文纠正检查 3-自然/优化原文纠正
     * @return 配置信息，未找到时返回 null
     */
    public AiContentCorrectionConfigVo getContentCorrectionConfig(Integer sceneType) {
        List<DictDataListVo> treeList = dictDataTreeListByCode("ai_content_correction", false);

        DictDataListVo sceneNode = treeList.stream()
                .filter(node -> String.valueOf(sceneType).equals(node.getValue()))
                .findFirst()
                .orElse(null);

        if (sceneNode == null || ObjectUtil.isEmpty(sceneNode.getChildren())) {
            return null;
        }

        AiContentCorrectionConfigVo configVo = new AiContentCorrectionConfigVo();
        for (DictDataListVo child : sceneNode.getChildren()) {
            if ("model_code".equals(child.getLabel())) {
                configVo.setModelCode(child.getValue());
            } else if ("prompt_content".equals(child.getLabel())) {
                configVo.setContentPrompt(child.getValue());
            }
        }

        return configVo;
    }

    /**
     * 字典列表
     * @param dictDataListBo 字典列表查询参数
     * @return
     */
    public R<PageUtils<DictDataListVo>> queryPage(DictDataListBo dictDataListBo) {

        PageUtils<DictDataListVo> queryPage = dictDataProducer.queryPage(dictDataListBo);

        return R.ok("获取成功", queryPage);
    }

    /**
     * 根据字典标识获取字典列表
     * @param typeLogo 字典标识
     * @return
     */
    public R<List<DictDataListVo>> listByTypeLogo(String typeLogo) {
        List<DictDataListVo> dictDataListVos = dictDataProducer.listByTypeLogo(typeLogo);
        return R.ok(dictDataListVos);
    }

    /**
    * 字典信息
    * @param id 字典id
    * @return
    */
    public R<DictDataInfoVo> info(Long id) {

        DictDataInfoVo dictDataInfoVo = dictDataProducer.info(id);
        return R.ok("获取成功", dictDataInfoVo);
    }

    /**
     * 新增字典
     * @param dictDataBo 字典对象
     * @return
     */
    public R<String> save(DictDataBo dictDataBo) {

        DictDataInfoVo dictDataInfoVo = dictDataProducer.save(dictDataBo);
        return R.ok("添加成功");
    }

    /**
     * 修改字典
     * @param dictDataBo 字典对象
     * @return
     */
    public R<String> update(DictDataBo dictDataBo) {

        dictDataProducer.update(dictDataBo);
        return R.ok("修改成功");
    }

    /**
     * 删除字典
     * @param id 字典id
     * @return
     */
    public R<String> delete(Long id) {

        dictDataProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据字典类型标识获取字典列表
     *
     * @param typeId 字典类型标识
     * @return 字典列表
     */
    public List<DictDataListVo> listDictDataTree(Long typeId) {
        List<DictDataListVo> tempList = dictDataProducer.listDictDataTree(typeId);

        return buildTree(tempList, false);
    }

    /**
     * 构建树形结构
     *
     * @param tempList 字典列表
     * @return 树形结构
     */
    private List<DictDataListVo> buildTree(List<DictDataListVo> tempList, boolean isShowParentName) {
        if (ObjectUtil.isEmpty(tempList)) {
            return List.of();
        }

        List<Tree<Long>> treeList = TreeUtil.build(tempList, 0L, (object, treeNode) -> {
            treeNode.setName(object.getLabel());
            treeNode.setId(object.getId());
            treeNode.setParentId(object.getParentId());
            treeNode.putExtra("value", object.getValue());
            treeNode.putExtra("sort", object.getSort());
            treeNode.putExtra("status", object.getStatus());
            treeNode.putExtra("typeId", object.getTypeId());
            treeNode.putExtra("typeName", object.getTypeName());
        });

        return ObjectUtil.isEmpty(treeList) ? List.of() : treeList.stream().map(item -> isShowParentName ? toVo(item, null) : toVo(item)).toList();
    }

    /**
     * 将树结构转换为字典列表对象（带父级名称拼接）
     *
     * @param tree       树结构
     * @param parentName 父级名称，用于拼接label
     * @return 字典列表对象
     */
    private DictDataListVo toVo(Tree<Long> tree, String parentName) {
        String label = (ObjectUtil.isNotEmpty(parentName) ? parentName + "/" : "") + tree.getName();
        DictDataListVo vo = buildVoFromTree(tree, label);
        vo.setChildren(ObjectUtil.isEmpty(tree.getChildren()) ? null : tree.getChildren().stream().map(item -> toVo(item, vo.getLabel())).toList());
        return vo;
    }

    /**
     * 将树结构转换为字典列表对象
     *
     * @param tree 树结构
     * @return 字典列表对象
     */
    private DictDataListVo toVo(Tree<Long> tree) {
        DictDataListVo vo = buildVoFromTree(tree, (String) tree.getName());
        vo.setChildren(ObjectUtil.isEmpty(tree.getChildren()) ? null : tree.getChildren().stream().map(this::toVo).toList());
        return vo;
    }

    /**
     * 从树节点构建VO基础属性（提取公共逻辑）
     *
     * @param tree  树结构
     * @param label 标签名称
     * @return 字典列表对象
     */
    private DictDataListVo buildVoFromTree(Tree<Long> tree, String label) {
        DictDataListVo vo = new DictDataListVo();
        vo.setId(tree.getId());
        vo.setLabel(label);
        vo.setParentId(tree.getParentId());
        vo.setValue((String) tree.getOrDefault("value", null));
        vo.setSort((Integer) tree.getOrDefault("sort", null));
        vo.setStatus((Integer) tree.getOrDefault("status", null));
        vo.setTypeId((Long) tree.getOrDefault("typeId", null));
        vo.setTypeName((String) tree.getOrDefault("typeName", null));
        return vo;
    }

    /**
     * 根据字典id列表查询字典信息
     *
     * @param ids 字典id列表
     * @return 字典信息列表
     */
    public List<DictDataListVo> dictDataListByIds(List<Long> ids) {
        return dictDataProducer.dictDataListByIds(ids);
    }

    public Map<String, List<DictDataListVo>> dictDataListByCodes(String codes) {
        RRException.isNotEmpty(codes, "字典类型标识不能为空");

        String[] split = codes.split(",");
        if (ObjectUtil.isEmpty(split)) {
            return new HashMap<>();
        }

        HashMap<String, List<DictDataListVo>> res = new HashMap<>();
        for (String code : split) {
            List<DictDataListVo> dictDataListVos = this.dictDataProducer.listByTypeLogo(code);
            if (ObjectUtil.isNotEmpty(dictDataListVos)) {
                dictDataListVos = dictDataListVos.stream()
                        .filter(item -> ObjectUtil.isNotEmpty(item.getStatus()) && ObjectUtil.equals(item.getStatus(), 0))
                        .collect(Collectors.toList());
            }
            res.put(code, dictDataListVos);
        }
        return res;

    }
}

