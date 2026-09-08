<template>
    <el-dialog 
        :title="'录制分析内容'" 
        :close-on-click-modal="false" 
        v-model="visible"
    >
      <el-table 
          :data="dataList" 
          :element-loading-spinner="customSvg"
          border 
          header-row-class-name="my-header-row"
          size="default" 
          stripe 
          style="width: 100%" 
          v-loading="dataListLoading"
      >
        <el-table-column prop="content" header-align="center" align="center" label="内容">
        </el-table-column>
        <el-table-column width="50" prop="currentSort" header-align="center" align="center" label="第几段">
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
        </span>
      </template>
    </el-dialog>
</template>
  
  <script setup>
import { ref } from 'vue'
import { customSvg } from '@/utils/icon.js'
import api from '@/utils/request-api'

const visible = ref(false)
const dataList = ref([])
const dataListLoading = ref(false)
const dataForm = ref({})

const init = async (videoId, tradeId) => {
  visible.value = true
  dataList.value = []
  
  if (videoId != null && tradeId != null) {
    const res = await api.AnchorVideo.selectAnalysisByVideoId({
      videoId: videoId,
      tradeId: tradeId
    })
    if (res && res.code === 0) {
      dataList.value = res.data
      console.log(res.data)
    }
  }
}

const dataFormSubmit = async () => {
  // 这个方法在当前模板中未使用，保留以防需要
  // 可以根据实际需求进行实现
}

defineExpose({
  init
})
</script>
  