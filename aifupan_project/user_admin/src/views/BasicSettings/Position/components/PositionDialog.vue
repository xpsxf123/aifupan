<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    :width="512"
    destroy-on-close
    class="common-dialog"
    @close="handleClose"
  >
    <div class="position-dialog__container">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-form-item label="岗位名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入岗位名称" class="rounded" />
        </el-form-item>

        <!--         <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :precision="0"
            placeholder="请输入排序"
            style="width: 100%"
          />
        </el-form-item> -->

        <el-form-item class="position-dialog__actions">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            v-auth="props.mode === 'edit' ? 'org:position:update' : 'org:position:add'"
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            确定
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
  import { computed, reactive, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    row: { type: Object, default: () => ({}) },
    api: { type: Object, required: true }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const formRef = ref(null)
  const submitting = ref(false)

  const form = reactive({
    id: '',
    name: '',
    sort: 0
  })

  const rules = {
    name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
    sort: [{ required: true, message: '请输入排序', trigger: 'change' }]
  }

  const dialogTitle = computed(() => (props.mode === 'edit' ? '编辑岗位' : '新建岗位'))

  const resetForm = () => {
    form.id = ''
    form.name = ''
    form.sort = 0
  }

  const fillForm = (row) => {
    form.id = row?.id || ''
    form.name = row?.name || ''
    form.sort = row?.sort ?? 0
  }

  const handleClose = () => {
    emit('update:modelValue', false)
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
      const payload = {
        id: form.id,
        name: form.name,
        sort: form.sort
      }

      if (props.mode === 'edit') {
        await props.api.edit(payload)
      } else {
        await props.api.add(payload)
      }

      ElMessage.success('保存成功')
      emit('success')
      handleClose()
    } catch (e) {
      console.error(e)
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => props.modelValue,
    (v) => {
      if (!v) return
      resetForm()
      if (props.mode === 'edit') fillForm(props.row)
    }
  )
</script>

<style scoped lang="scss">
  .position-dialog__container {
    width: 100%;
    display: flex;
    justify-content: center;
    .el-form {
      width: 392px;
    }
  }

  .position-dialog__actions {
    :deep(.el-form-item__content) {
      display: flex;
      justify-content: center;
    }
    .el-button {
      width: 84px;
      height: 34px;
      border-radius: 56px;
    }
  }
</style>
