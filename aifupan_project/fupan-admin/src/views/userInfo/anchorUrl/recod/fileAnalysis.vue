<template>
  <el-dialog 
    title="文件内容" 
    v-model="visible"
    :close-on-click-modal="false"
    class="file-analysis"
  >
    <el-table 
      :data="dataList" 
      border 
      stripe 
      size="default" 
      v-loading="dataListLoading"
      :element-loading-spinner="customSvg"
      header-row-class-name="my-header-row"
      style="width: 100%"
    >
      <el-table-column 
        prop="content" 
        header-align="center" 
        align="center" 
        label="内容"
      />
      <el-table-column 
        width="50"
        prop="currentSort" 
        header-align="center" 
        align="center" 
        label="第几段"
      />
    </el-table>

    <template #footer>
      <div class="file-analysis__footer">
        <el-button @click="visible = false">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>
  
<script setup>
import { ref, nextTick } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const visible = ref(false)
const dataList = ref([])
const dataListLoading = ref(false)

const init = async (fileId, tradeId) => {
  visible.value = true
  dataList.value = []
  
  if (fileId != null && tradeId != null) {
    await nextTick()
    const res = await api.AnchorVideo.selectAnalysisByFileId({ 
      fileId: fileId, 
      tradeId: tradeId 
    })
    if (res && res.code === 0) {
      dataList.value = res.data
    }
  }
}

defineExpose({
  init
})
</script>

<style lang="scss" scoped>
.file-analysis {
  &__footer {
    text-align: center;
  }
}

.my-header-row {
  background-color: #f5f7fa;
  
  th {
    background-color: #f5f7fa !important;
  }
}
</style>
  