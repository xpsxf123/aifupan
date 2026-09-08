<template>
  <my-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    @close="close"
    @submit="submit"
    class="appeal-dialog-custom"
  >
    <div class="container">
      <div class="content">
        <el-form :model="form" ref="myForm" :rules="rules">
          <el-form-item label="" prop="handleRemarks">
            <el-input
              v-model.trim="form.handleRemarks"
              type="textarea"
              :rows="4"
              placeholder="请输入处理备注"
              maxlength="500"
              show-word-limit
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </my-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import api from '@/utils/request-api'
import MyDialog from '@/components/commonComponent/myDialog.vue'

const emit = defineEmits(['close', 'submit'])

const dialogVisible = ref(false)
const title = ref('处理申诉')
const width = ref('40%')
const currentRow = ref({})
const myForm = ref(null)

const form = reactive({
  id: '',
  status: 1,
  handleRemarks: '',
})

const rules = reactive({
  reason: [{ required: false, message: '请输入申诉原因', trigger: 'blur' }],
})

const init = (row) => {
  currentRow.value = row
  dialogVisible.value = true
  form.handleRemarks = ''
  form.id = row.id
}

const close = () => {
  dialogVisible.value = false
  emit('close')
}

const submit = async () => {
  await ElMessageBox.confirm('确定处理申诉?', '提示', {})
  const res = await api.userVideoAppeal.handleAppeal(form)
  if (res?.code === 0) {
    ElMessage.success('处理申诉成功')
    emit('submit')
  }
}

defineExpose({
  init,
})
</script>

<style lang="scss">
.appeal-dialog-custom {
  min-width: 500px;
}
</style>
