package com.jiuyu.replay.common.utils;

import cn.hutool.core.lang.tree.Tree;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：liwj
 * &#064;description：树的stirng和map
 * @date ：2025/9/17 11:45
 */
public class TreePrinterWithMap {

    @Data
    public static class TradeTemp {
        private long tradeId;
        private String tradeName;
    }


    /**
     * 结果包装类
     *
     * @param treeText      树形文本
     * @param tradeList 编号 -> 名称 映射
     */
    public record TreePrintResult(String treeText, Map<String, TradeTemp> tradeList) {

        @Override
        public String treeText() {
            return treeText;
        }

        @Override
        public Map<String, TradeTemp> tradeList() {
            return tradeList;
        }
    }

    /**
     * 将 List<Tree<Long>> 转成树形文本 + 编号映射
     */
    public static TreePrintResult printTreeWithMap(List<Tree<Long>> treeList) {
        StringBuilder sb = new StringBuilder();
        // 保持插入顺序
        Map<String, TradeTemp> map = new LinkedHashMap<>();

        for (int i = 0; i < treeList.size(); i++) {
            Tree<Long> tree = treeList.get(i);
            String prefix = (i + 1) + "";
            printTreeRecursive(tree, prefix, 0, sb, map);
        }

        return new TreePrintResult(sb.toString(), map);
    }

    /**
     * 递归打印并填充 map
     */
    private static void printTreeRecursive(
            Tree<Long> node,
            String numbering,
            int depth,
            StringBuilder sb,
            Map<String, TradeTemp> map) {

        String name = node.getName() != null ? node.getName().toString() : "Unnamed";

        // ✅ 写入文本
        String indent = "    ".repeat(depth);
        sb.append(indent).append(numbering).append(" ").append(name).append("\n");

        // ✅ 写入映射
        TradeTemp value = new TradeTemp();
        value.setTradeId(node.getId());
        value.setTradeName(node.getName() != null ? node.getName().toString() : "Unnamed");
        map.put(numbering, value);

        // 递归子节点
        List<Tree<Long>> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                Tree<Long> child = children.get(i);
                String childNumbering = numbering + "." + (i + 1);
                printTreeRecursive(child, childNumbering, depth + 1, sb, map);
            }
        }
    }
}
