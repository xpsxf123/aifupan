<template>
  <div class="mod-config">
    <el-button type="primary" @click="addOrUpdateHandle(0, 0, 0)"
    >新增一级行业
    </el-button>
    <BaseTable
        :columns="columns"
        :loadingFlag="loadingFlag"
        :showPagination="false"
        :tableData="dataList"
        rowKey="id"
    >
      <template #operate="scope">
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="addOrUpdateHandle(0, 0, scope.row.id)"
        >添加子行业
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            type="primary"
            @click="addOrUpdateHandle(scope.row.id, 0, scope.row.parentId)"
        >修改
        </el-button>
        <!-- <el-button v-if="scope.row.children.length < 1" type="primary" link style="color: #409eff" size="small"
          @click="deleteHandle(scope.row.id)">删除</el-button> -->
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="setTradeModel(scope.row.id, scope.row.tradeModelId)"
        >设置模型
        </el-button>
      </template>
      <template #word="scope">
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toWordPage(scope.row.id, '/words/crux')"
        >关键词({{ scope.row.cruxNum }})
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toWordPage(scope.row.id, '/words/sensitive')"
        >敏感词({{ scope.row.sensitiveNum }})
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toCueWordPage(scope.row.id, '/words/cue', 1)"
        >运营全文提示词({{ scope.row.opeFullcueNum }})
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toCueWordPage(scope.row.id, '/words/cue', 2)"
        >运营段落提示词({{ scope.row.opeParCueNum }})
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toCueWordPage(scope.row.id, '/words/cue', 3)"
        >违规全文提示词({{ scope.row.vioFullCueNum }})
        </el-button>
        <el-button
            class="btn"
            link
            size="small"
            style="color: #409eff"
            type="primary"
            @click="toCueWordPage(scope.row.id, '/words/cue', 4)"
        >违规段落提示词({{ scope.row.vioParCueNum }})
        </el-button>
      </template>
    </BaseTable>
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update
        v-if="addOrUpdateVisible"
        ref="addOrUpdateRef"
        @refreshDataList="getDataList"
    ></add-or-update>
    <setTradeModelDialog
        v-if="tradeModelVisible"
        ref="setTradeModelRef"
        @refreshDataList="getDataList"
    >
    </setTradeModelDialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import AddOrUpdate from './trade-add-or-update.vue'
import setTradeModelDialog from './setTradeModelDialog.vue'
import BaseTable from '@/components/table/index.vue'
import { useSystemInfoStore } from '@/store'
import { storeToRefs } from 'pinia'
import api from '@/utils/request-api'

const {isMobile} = storeToRefs(useSystemInfoStore())
const router = useRouter()
const addOrUpdateRef = ref(null)
const setTradeModelRef = ref(null)
const loadingFlag = ref(false)
const dataList = ref([])
const tableKey = ref(0)
const addOrUpdateVisible = ref(false)
const tradeModelVisible = ref(false)

const columns = [
  {
    prop: 'name',
    label: '行业名',
    minWidth: '300'
  },
  {
    prop: 'remarks',
    label: '描述',
    minWidth: '250'
  },
  {
    prop: 'sort',
    label: '排序',
    width: '60'
  },
  {
    prop: 'createDate',
    label: '创建时间',
    width: 180
  },
  {
    label: '操作',
    width: '180',
    slotName: 'operate'
  },
  {
    label: '词语',
    minWidth: '600',
    slotName: 'word',
    fixed: isMobile.value ? false : 'right'
  }
]

// 设置行业模型
const setTradeModel = (tradeId, tradeModelId) => {
  tradeModelVisible.value = true
  nextTick(() => {
    setTradeModelRef.value.init(tradeId, Number(tradeModelId))
  })
}

// 查找节点路径
const findPathToNode = (node, targetId, path = []) => {
  if (!node) return null

  // 当前节点即为目标节点
  if (node.id === targetId) {
    return [...path, node.id] // 返回路径加上当前节点ID
  }

  // 如果当前节点有子节点，则递归搜索子节点
  if (node.children && node.children.length > 0) {
    for (let child of node.children) {
      let result = findPathToNode(child, targetId, [...path, node.id]) // 更新路径
      if (result) {
        return result // 找到了目标节点，返回结果
      }
    }
  }

  // 没有找到目标节点
  return null
}

// 根据三级ID获取所有层级ID
const getIdsByLevel3Id = (level3Id) => {
  for (let root of dataList.value) {
    let ids = findPathToNode(root, level3Id)
    if (ids !== null) {
      // 返回一级、二级和三级元素ID
      return [ids[0], ids[1], ids[2]]
    }
  }
  // 如果没有找到匹配项
  return null
}

// 跳转到关键词/敏感词列表页
const toWordPage = (tradeId, pageUrl) => {
  let arr = getIdsByLevel3Id(tradeId)
  let tradeArr = []
  arr.forEach((item) => {
    if (item) {
      tradeArr.push(item)
    }
  })
  router.push({path: pageUrl, query: {tradeArr}})
}

// 跳转到提示词列表页
const toCueWordPage = (tradeId, pageUrl, type) => {
  //提示词类型
  let cueType = 0
  //提示词范围
  let scope = 0
  if (2 === type) {
    scope = 1
  }
  if (3 === type) {
    cueType = 1
  }
  if (4 === type) {
    cueType = 1
    scope = 1
  }

  router.push({path: pageUrl, query: {tradeId, cueType, scope}})
}

// 获取数据列表
const getDataList = () => {
  loadingFlag.value = true
  dataList.value = []

  api.trade.listTree({childrenNotNull: 1}).then((res) => {
    if (res && res.code === 0) {
      dataList.value = res.data
      tableKey.value++
      loadingFlag.value = false
    }
  })
}

// 新增/修改行业
const addOrUpdateHandle = (id, tradeModelId, parentId) => {
  addOrUpdateVisible.value = true
  nextTick(() => {
    addOrUpdateRef.value.init(id, tradeModelId, parentId)
  })
}

// 删除行业
const deleteHandle = (id) => {
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.trade
        .delete({
          id
        })
        .then((res) => {
          if (res && res.code === 0) {
            ElMessage({
              message: res.msg,
              type: 'success',
              duration: 1500,
              onClose: () => {
                getDataList()
              }
            })
          }
        })
  })
}

// 页面加载时获取数据
onMounted(() => {
  getDataList()
})
</script>

<style lang="less" scoped>
.mod-config {
  padding: 15px;
}

:deep(.el-table__fixed-right) {
  height: 100% !important;
}

.btn {
  margin-left: 0;
}
</style>
