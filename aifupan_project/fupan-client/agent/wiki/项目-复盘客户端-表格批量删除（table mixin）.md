# 项目-复盘客户端-表格批量删除（table mixin）

## 定义

本项目的列表页“批量删除”统一通过 `src/mixins/table.js` 复用：

- UI 入口：`CoreTable` 的批量删除按钮（依赖 `table-select` 开启选择列）
- 触发方式：`@deletes="deletes"`（直接复用 mixin 的 `deletes` 方法）
- 业务配置：页面实现 `deleteOpt(list)` 返回 `{ title, http, idKey, callback }`

## 适用范围

- 所有使用 `CoreTable` 的列表页，需要提供“批量删除”能力时
- 需要保证删除确认弹窗、id 收集、删除后刷新等行为一致时

## 标准用法（推荐）

### 1) CoreTable 侧

```vue
<CoreTable
  :table-select="enableBatchDelete"
  @deletes="deletes"
  row-key="videoId"
/>
```

说明：

- `:table-select="true"` 才会显示“批量删除”按钮与 selection 列
- 批量删除仅开发者模式出现时，把 `table-select` 绑定到开关即可

### 2) 页面脚本侧

```js
import table from '@/mixins/table'

export default {
  mixins: [table],
  computed: {
    enableBatchDelete () {
      return !!this.$store.getters.getMode
    }
  },
  methods: {
    deleteOpt (list = []) {
      return {
        title: '将删除选中的数据, 是否继续?',
        http: (ids) => this.$httpBack.xxx.batchDelete(ids, { load: false }),
        idKey: 'id',
        callback: () => {
          // 删除成功后的额外收尾（可选）
        }
      }
    }
  }
}
```

字段约定：

- `title`：确认弹窗文案（可选，不传会走默认文案）
- `http(ids)`：删除接口函数，参数为 id 数组，返回 `Promise<{ code: number }>`
- `idKey`：从行数据取 id 的 key（例如 `videoId` / `contrastId` / `id`）
- `callback`：删除成功后回调（可选）；表格刷新由 mixin 统一处理

## 权限/过滤（必须批量删除前过滤时）

当“选中列表”里存在不可删除项（例如星标、权限不足）时，需要保证最终提交给接口的 ids 只来自可删除项。

项目推荐做法：在 `deleteOpt(list)` 内对 `list` 原地过滤（保持 mixin 内部后续的 `list.map` 一致）。

示例：

```js
deleteOpt (list = []) {
  const rows = Array.isArray(list) ? list : []
  const canDelete = (row) => row && row.videoId && row.hasStar !== 1
  const deleteList = rows.filter(canDelete)

  if (Array.isArray(list)) {
    list.splice(0, list.length, ...deleteList)
  }

  if (!deleteList.length) {
    this.$message.warning('暂无可删除的数据')
    return { http: null }
  }

  return {
    title: `将批量删除 ${deleteList.length} 条记录，是否继续？`,
    http: (ids) => this.$httpBack.video.clientBatchDeleteCloudVideo(ids, { load: false }),
    idKey: 'videoId'
  }
}
```

## 相关链接

- `src/mixins/table.js`
- `src/components/coreTable/index.vue`

