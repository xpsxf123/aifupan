<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    :width="512"
    destroy-on-close
    class="common-dialog"
    @close="handleClose"
  >
    <div class="subsidiary-dialog__container">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入公司名称" class="rounded" />
        </el-form-item>

        <el-form-item label="管理者" prop="managerUserIds" required class="subsidiary-dialog__manager">
          <ManagerSelect
            v-model="form.managerUserIds"
            v-model:users="form.managerUserInfos"
            @validate-manager-user-ids="handleManagerValidate"
          />
        </el-form-item>

        <el-form-item class="subsidiary-dialog__actions">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            v-auth="props.mode === 'edit' ? 'org:sub-company:manage:update' : 'org:sub-company:manage:add'"
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
  /**
   * @file SubsidiaryDialog.vue
   * @description 子公司新增/编辑弹窗
   */
  import { computed, reactive, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import apiModule from '@/http/api'
  import ManagerSelect from '@/components/ManagerSelect/index.vue'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    row: { type: Object, default: () => ({}) }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const formRef = ref(null)
  const submitting = ref(false)

  const form = reactive({
    id: '',
    name: '',
    managerUserIds: [],
    managerUserInfos: []
  })

  const rules = {
    name: [
      { required: true, message: '请输入公司名称', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    managerUserIds: [
      {
        required: true,
        trigger: 'change',
        validator: (rule, value, callback) => {
          const infos = Array.isArray(form.managerUserInfos) ? form.managerUserInfos : []
          const ids = Array.isArray(value) ? value : []
          if (infos.length || ids.length) {
            callback()
            return
          }
          callback(new Error('请选择管理者'))
        }
      }
    ]
  }

  const dialogTitle = computed(() => (props.mode === 'edit' ? '编辑子公司' : '添加子公司'))

  const resetForm = () => {
    form.id = ''
    form.name = ''
    form.managerUserIds = []
    form.managerUserInfos = []
  }

  const fillForm = (row) => {
    form.id = row?.id || ''
    form.name = row?.name || ''
    form.managerUserIds = row?.managerUserInfos?.map((u) => u.id) || []
    form.managerUserInfos = row?.managerUserInfos ? [...row.managerUserInfos] : []
  }

  const handleClose = () => {
    emit('update:modelValue', false)
  }

  const handleManagerValidate = async () => {
    if (!formRef.value) return
    await formRef.value
      .validateField('managerUserIds')
      .then(() => {
        formRef.value.clearValidate('managerUserIds')
      })
      .catch(() => {})
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
      const payload = {
        name: form.name,
        managerUserIds: form.managerUserIds || []
      }

      if (props.mode === 'edit') {
        payload.id = form.id
        await apiModule.subCompany.edit(payload)
      } else {
        await apiModule.subCompany.add(payload)
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
  .subsidiary-dialog__container {
    width: 100%;
    display: flex;
    justify-content: center;
    .el-form {
      width: 392px;
    }
  }

  .subsidiary-dialog__actions {
    :deep(.el-form-item__content) {
      display: flex;
      justify-content: flex-end;
    }
    .el-button {
      width: 84px;
      height: 34px;
      border-radius: 56px;
    }
  }

  .subsidiary-dialog__manager {
    display: flex;
    align-items: flex-start;
    :deep(.el-form-item__label) {
      margin-bottom: 0;
      line-height: 2.3;
    }
  }
</style>
