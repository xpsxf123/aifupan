# dialog_drawer_recipe

## 本技能用途

- 统一弹窗与抽屉的实现方式，减少各页面自己直接使用 ElementUI 导致的样式/滚动/受控问题差异。
- 输出最小可用的 Dialog/Drawer 使用骨架与常见交互范式。

## 适用场景

- 弹窗：编辑表单、提示确认、内容展示（需要滚动容器）
- 抽屉：配置面板、侧滑详情、批量操作面板

## 输入

- 展示形态：弹窗 / 抽屉
- 是否受控：v-model / ref 命令式
- 内容类型：表单 / 表格 / 富文本 / 组合组件

## 输出

- 组件引用方式与模板骨架
- 事件与数据流约定（关闭/确认/重置）

## 推荐用法

### Dialog（受控）

组件：`src/components/dialog/index.vue`

```vue
<Dialog :visible.sync="visible" title="标题" width="800px">
  <div>内容</div>
  <template #footer>
    <el-button @click="visible = false">取消</el-button>
    <el-button type="primary" @click="handleOk">确定</el-button>
  </template>
</Dialog>
```

### Drawer（命令式）

组件：`src/components/drawer/index.vue`

```vue
<Drawer ref="drawer" title="配置" width="520px">
  <div>内容</div>
</Drawer>
```

```js
this.$refs.drawer.show({ id: row.id }, '配置')
```

## 常见坑位

- 关闭时重置表单：优先在 `before-close/closed` 时清理本地 data
- 弹窗内容滚动：优先使用项目封装的 Dialog（内置 scrollbar），避免各页面重复处理

