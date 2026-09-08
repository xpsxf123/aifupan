# core_table_page_builder

## 本技能用途

- 用项目现有的 `CoreTable` 组件快速搭建标准列表页骨架（搜索/表格/分页/操作列/插槽列渲染）。
- 输出最小可用的页面结构，并给出 column/searchConfig/menuConfig 的配置范式。

## 适用场景

- 任意“列表页 + 搜索 + 分页”的页面（最常见后台页面形态）

## 输入

- 接口函数：`getDataApi(formData)` 或页面自行请求返回 list/page
- 列定义：字段名、标题、宽度、是否使用 slot/formatter/option.render
- 搜索字段：prop/label/temp/options/默认值
- 操作列：按钮文案、权限、点击回调

## 输出

- 页面骨架（Vue SFC 模板）
- `column/searchConfig/menuConfig` 配置示例
- 常见坑位自检（缓存、自动请求、formatter 与 render 的选择）

## 组件与示例引用

- CoreTable：`src/components/coreTable/index.vue`
- 示例页：`src/views/modules/example/index.vue`

## 关键约定

- 列插槽名：等于 `column.prop`
- 简单文本：优先 `formatter(row)` 输出字符串
- 复杂结构：用 `option.render(row)` 输出 HTML 字符串（Column 内 v-html 渲染）

