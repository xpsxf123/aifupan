<template>
  <my-dialog v-model="dialog" :title="title" :width="width" close-on-click-modal :footer="footer" max-height @close="close" append-to-body>
    <el-row>
      <el-form :model="dataForm" label-suffix=":" label-width="120px" label-position="right" inline>
        <div v-if="!dataList || dataList?.length === 0" style="width: 100%;">
          <el-empty :image-size="200"></el-empty>
        </div>
        <div class="my-all" v-for="(item, index) in dataList" :key="`token-item-${index}`">
          <div class="block">
            <el-col :span="24">
              <div class="content">
                <el-col :span="24">
                  <el-form-item label="requestId">{{item.requestId}}</el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="模型名称">{{item.modelName}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="总token数量">{{item.totalTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="输入token数量">{{item.promptTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="输出token数量">{{item.completionTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="图片token数量">{{item.imageTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="音频token数量">{{item.audioTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="视频token数量">{{item.videoTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="上下文缓存token数量" label-width="160px">{{item.cachedTokens}}</el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="输出思维链内容token数量" label-width="180px">{{item.reasoningTokens}}</el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="备注">{{item.remarks}}</el-form-item>
                </el-col>
              </div>
            </el-col>
            <el-col :span="24">
              <el-divider/>
            </el-col>
          </div>
        </div>
      </el-form>
    </el-row>
  </my-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import myDialog from "@/components/commonComponent/myDialog.vue"
import api from '@/utils/request-api'

const emit = defineEmits(['close'])

const dialog = ref(false)
const title = ref('token使用详情')
const width = ref('800px')
const footer = ref(false)
const appendToBody = ref(false)
const dataForm = reactive({})
const dataList = ref([])

const init = async (detailsId) => {
  const res = await api.userproperty.aiTokenUseRecordByDetailId({propertyDetailsId: detailsId})
  if (res && res.code === 0) {
    dataList.value = res.data
  }
  dialog.value = true
}

const close = () => {
  dialog.value = false
  emit('close')
}

defineExpose({
  init
})
</script>

<style scoped lang="scss">

.my-all {
  height: 100%;
  padding: 10px;

  .block {
    height: 100%;
    padding-bottom: 20px;

    .title {
      font-size: 16px;
      font-weight: 600;
      color: #333333;
    }

    .content {
      padding: 0 10px 20px 10px;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 0;
}

.m-t-20 {
  margin-top: 20px;
}

</style>