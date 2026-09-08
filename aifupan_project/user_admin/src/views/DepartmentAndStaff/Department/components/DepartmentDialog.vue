<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    :width="512"
    destroy-on-close
    class="common-dialog"
    @close="handleClose"
  >
    <div class="department-dialog__container">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="top">
        <el-form-item label="上级组织" prop="companyId">
          <el-select v-model="form.companyId" placeholder="请选择上级组织" style="width: 100%" class="rounded">
            <el-option v-for="opt in companyOptions" :key="opt.key" :label="opt.label" :value="opt.key" />
          </el-select>
        </el-form-item>

        <el-row class="department-dialog__row" :gutter="16">
          <el-col :span="12">
            <el-form-item label="部门名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入部门名称" class="rounded" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门排序" prop="sort">
              <el-input-number
                v-model="form.sort"
                :min="0"
                :precision="0"
                placeholder="请输入排序"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="管理者" prop="managerUserIds" class="department-dialog__manager">
          <ManagerSelect v-model="form.managerUserIds" v-model:users="form.managerUserInfos" />
        </el-form-item>

        <el-form-item class="department-dialog__actions">
          <el-button @click="handleClose">取消</el-button>
          <el-button
            v-auth="props.mode === 'edit' ? 'org:dept:manage:update' : 'org:dept:manage:add'"
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
  import ManagerSelect from '@/components/ManagerSelect/index.vue'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    row: { type: Object, default: () => ({}) },
    companyOptions: { type: Array, default: () => [] },
    api: { type: Object, required: true }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const formRef = ref(null)
  const submitting = ref(false)

  const form = reactive({
    id: '',
    companyId: '',
    name: '',
    sort: 0,
    managerUserIds: [],
    managerUserInfos: []
  })

  const rules = {
    companyId: [{ required: true, message: '请选择上级组织', trigger: 'change' }],
    name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
  }

  const dialogTitle = computed(() => (props.mode === 'edit' ? '编辑部门' : '新建部门'))

  const resetForm = () => {
    form.id = ''
    form.companyId = ''
    form.name = ''
    form.sort = 0
    form.managerUserIds = []
    form.managerUserInfos = []
  }

  const fillForm = (row) => {
    form.id = row?.id || ''
    form.companyId = row?.companyId || ''
    form.name = row?.name || ''
    form.sort = row?.sort || 0
    form.managerUserInfos = Array.isArray(row?.managerUserInfos) ? [...row.managerUserInfos] : []
    form.managerUserIds =
      Array.isArray(row?.managerUserIds) && row.managerUserIds.length
        ? [...row.managerUserIds]
        : form.managerUserInfos.map((u) => u.id)
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
        companyId: form.companyId,
        name: form.name,
        sort: form.sort || 0,
        managerUserIds: form.managerUserIds || []
      }

      if (props.mode === 'edit') {
        await props.api.edit({ ...payload, id: form.id })
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
  .department-dialog__container {
    width: 100%;
    display: flex;
    justify-content: center;
    .el-form {
      width: 392px;
    }
  }

  .department-dialog__actions {
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

  .department-dialog__manager {
    display: flex;
    align-items: flex-start;
    :deep(.el-form-item__label) {
      margin-bottom: 0;
      line-height: 2.3;
    }
  }
</style>
