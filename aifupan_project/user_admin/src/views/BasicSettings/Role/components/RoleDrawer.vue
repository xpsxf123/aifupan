<template>
  <el-drawer
    header-class="common-drawer-header-style"
    body-class="common-drawer-body-style"
    footer-class="common-drawer-footer-style"
    :model-value="modelValue"
    :size="600"
    :show-close="false"
    destroy-on-close
    @close="handleClose"
  >
    <template #header>
      <div class="role-drawer__header">
        <CloseSvg @click="handleClose" />
        <div class="role-drawer__title">{{ dialogTitle }}</div>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" class="role-drawer__form" label-position="left">
      <p class="role-drawer__section-title">基本设置</p>
      <div class="role-drawer__section role-drawer__section--card">
        <div class="role-drawer__section-body">
          <el-form-item label="角色名称" prop="name" required label-width="80px" style="margin-bottom: 0">
            <el-input v-model="form.name" placeholder="请输入角色名称" class="rounded" />
          </el-form-item>
        </div>
      </div>

      <p class="role-drawer__section-title">权限配置</p>
      <div class="role-drawer__section role-drawer__section--card">
        <div class="role-drawer__section-body">
          <el-form-item prop="menuIds" style="margin-bottom: 0">
            <PermissionSelect v-auth="'sys:role:assign-menu'" v-model="form.menuIds" :options="permissionTreeData" />
          </el-form-item>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button
        v-auth="props.mode === 'edit' ? 'sys:role:update' : 'sys:role:add'"
        class="custom-btn status-btn"
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        确定添加
      </el-button>
      <el-button class="custom-btn status-btn" @click="handleClose">取消</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, reactive, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import PermissionSelect from './PermissionSelect.vue'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'

  const props = defineProps({
    modelValue: { type: Boolean, default: false },
    mode: { type: String, default: 'add' },
    row: { type: Object, default: () => ({}) },
    permissionTreeData: { type: Array, default: () => [] },
    api: { type: Object, required: true }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const formRef = ref(null)
  const submitting = ref(false)

  const form = reactive({
    id: '',
    name: '',
    menuIds: []
  })

  const rules = {
    name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
  }

  const dialogTitle = computed(() => (props.mode === 'edit' ? '编辑角色' : '新建角色'))

  const resetForm = () => {
    form.id = ''
    form.name = ''
    form.menuIds = []
  }

  const fillForm = (row) => {
    form.id = row?.id || ''
    form.name = row?.name || ''
    form.menuIds = Array.isArray(row?.menuIds) ? [...row.menuIds] : []
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
        name: form.name,
        menuIds: form.menuIds || []
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
      fillForm(props.row)
    }
  )
</script>

<style scoped lang="scss">
  .role-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
  }

  .role-drawer__title {
    font-size: 16px;
    color: #151719;
  }

  .role-drawer__form {
    padding-right: 10px;
  }

  .role-drawer__section-title {
    margin-bottom: 20px;
  }

  .role-drawer__section {
    border: 1px solid #ebeef5;
    border-radius: 10px;
    overflow: hidden;
    margin-bottom: 14px;
    background: #fff;
  }

  .role-drawer__section--card {
    padding: 24px;
    background-color: #f7f7f7;
    border: none;
  }

  .role-drawer__section-body {
    display: flex;
    flex-direction: column;
    row-gap: 20px;
  }

  :deep(.el-form-item__label) {
    color: #151719;
  }
</style>

<style lang="scss">
  .enter-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border: 1px solid #dcdcdc;
    .form-section__title {
      font-size: 16px;
      background-color: transparent;
      border: none;
      padding: 0;
    }
    .header-title {
      display: flex;
      width: 100%;
      gap: 13px;
    }
  }

  .custom-btn {
    width: 84px;
    height: 34px;
    border-radius: 50px;
  }
</style>
