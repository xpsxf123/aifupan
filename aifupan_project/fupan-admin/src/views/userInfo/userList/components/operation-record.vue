<template>
  <div>
    <el-collapse v-model="activeNames" v-if="updateList.length > 0">
      <el-collapse-item
          v-for="(item, index) in updateList"
          :key="index"
          :name="item.index"
      >
        <template #title>
          <div class="header">
            <span style="margin-right: 20px">
          <i class="el-icon-user-solid"></i>
            修改人：{{ item.operatorName }}
          </span>

            　 <span>
          <i class="el-icon-timer"></i> 修改时间：{{ item.operationTime }}
          </span>
          </div>
        </template>
        <el-table
            empty-text="暂无修改"
            :data="item.tableData"
            style="width: 100%;margin: 12px 0; border-radius: 8px; overflow: hidden;"
            stripe
            size="small"
            highlight-current-row
            header-cell-class-name="diff-table-header"
            cell-class-name="diff-table-cell"
        >
          <el-table-column prop="label" label="字段名" width="160" align="center"/>
          <el-table-column prop="before" label="修改前">
            <template #default="{row}">
              <span class="before-cell">{{ row.before }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="after" label="修改后">
            <template #default="{row}">
              <span class="after-cell">{{ row.after }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>
    <el-empty v-else description="暂无修改记录" style="height: 200px" :image-size="80"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/request-api'

const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

const activeNames = ref([])
const updateList = ref([])
const fieldLabels = ref({
  status: '激活状态',
  channelName: '用户来源渠道',
  salesName: '跟进销售姓名',
  phones: '联系电话',
  tradeName: '行业名称'
})

const getDiffList = (item) => {
  const result = []
  const before = item.beforeObjData || {}
  const after = item.afterObjData || {}
  Object.keys(fieldLabels.value).forEach((key) => {
    if (key === 'status' && before[key] !== after[key]) {
      result.push({
        label: fieldLabels.value[key],
        before: before[key] === 0 ? '激活' : '未激活',
        after: after[key] === 0 ? '激活' : '未激活'
      })
      return
    }
    if (before[key] !== after[key]) {
      result.push({
        label: fieldLabels.value[key],
        before: before[key] || '',
        after: after[key] || ''
      })
    }
  })
  return result
}

const getUserOperation = async () => {
  const {code, data} = await api.userdetails.getUserOperations({
    userId: props.userId,
    businessType: 'USER_DETAILS'
  })
  if (code === 0 && data.list && data.list.length) {
    data.list.forEach(item => {
      item.tableData = getDiffList(item)
    })
    updateList.value = data.list.filter(item => {
      return item.tableData.length
    })
  }
}

onMounted(() => {
  getUserOperation()
})
</script>

<style scoped lang="less">
/* 表格整体样式 */
.el-table {
  border: 1px solid #ddd;
}

.diff-table-header {
  background-color: #f8f8f9 !important;
  color: #515a6e;
  font-weight: 600;
}

.diff-table-cell {
  padding: 8px 16px !important;
  font-size: 13px;
}

/* 表格边框和圆角 */
.el-table {
  border-collapse: separate;
  border-spacing: 0;
}

.el-table--border {
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.el-table--border th:first-child .cell {
  border-top-left-radius: 8px;
}

.el-table--border th:last-child .cell {
  border-top-right-radius: 8px;
}

/* 表头样式 */
.el-table thead {
  color: #606266;
}

.el-table thead th {
  background-color: #f5f7fa;
}

/* 行高亮效果 */
.el-table--enable-row-hover .el-table__body tr:hover > td {
  background-color: #f5f7fa;
}

.el-table__row.current-row > td {
  background-color: #f0f7ff !important;
}

/* 修改前/后列的特殊样式 */
.el-table .before-cell {
  background-color: #fff6f6;
  color: #f56c6c;
}

.el-table .after-cell {
  background-color: #f0f9eb;
  color: #67c23a;
}

/* 单元格内容样式 */
.el-table .cell {
  line-height: 1.5;
  word-break: break-word;
}
</style>