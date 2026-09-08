<template>
  <el-drawer
    header-class="common-drawer-header-style"
    body-class="common-drawer-body-style"
    footer-class="common-drawer-footer-style"
    v-model="visibleProxy"
    size="600px"
    direction="rtl"
    :show-close="false"
    :close-on-click-modal="false"
    destroy-on-close
    class="live-room-drawer"
  >
    <template #header>
      <div class="live-room-drawer__header">
        <CloseSvg @click="visibleProxy = false" />
        <div class="live-room-drawer__title">{{ titleText }}</div>
      </div>
    </template>

    <div class="live-room-drawer__body">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="所属平台" prop="platform">
          <el-select v-model="form.platform" placeholder="请选择平台" class="w-100 rounded" :disabled="isEditMode">
            <el-option label="抖音" :value="0" />
<!--            <el-option label="快手" :value="1" />
            <el-option label="视频号" :value="2" />-->
          </el-select>
        </el-form-item>

        <el-form-item label="账号ID" prop="anchorNumber">
          <el-input
            v-model="form.anchorNumber"
            placeholder="请输入账号ID"
            maxlength="50"
            class="w-100 rounded"
            :disabled="isEditMode"
          />
        </el-form-item>

        <el-form-item label="行业选择" prop="tradeId">
          <el-cascader
            v-model="form.tradeId"
            :options="tradeTreeOptions"
            :props="tradeCascaderProps"
            :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
            placeholder="请选择行业"
            clearable
            style="width: 100%"
            class="rounded"
          />
        </el-form-item>

        <el-form-item label="首播日期" prop="debutDate">
          <el-date-picker
            v-model="form.debutDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择时间"
            class="rounded"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="所属组织" prop="orgPath">
          <el-cascader
            v-model="form.orgPath"
            :options="orgTreeOptions"
            :props="cascaderProps"
            placeholder="请选择所属组织"
            clearable
            filterable
            :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
            style="width: 100%"
            class="rounded"
          />
        </el-form-item>

        <el-form-item label="管理员" prop="managerUserIds" class="manager-user-ids-container">
          <ManagerSelect
            v-model="form.managerUserIds"
            v-model:users="form.managerUserInfos"
            @validate-manager-user-ids="validateManagerUserIds"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="live-room-drawer__footer">
        <el-button class="custom-btn status-btn" type="primary" :loading="submitting" @click="handleSubmit"
          >确定</el-button
        >
        <el-button class="custom-btn status-btn" @click="visibleProxy = false">取消</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, nextTick, reactive, ref, watch } from 'vue'
  import { ElMessage } from 'element-plus'
  import apiModule from '@/http/api'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import ManagerSelect from '@/components/ManagerSelect/index.vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String,
      default: 'add'
    },
    row: {
      type: Object,
      default: () => ({})
    },
    orgTreeOptions: {
      type: Array,
      default: () => []
    },
    tradeTreeOptions: {
      type: Array,
      default: () => []
    },
    cascaderProps: {
      type: Object,
      default: () => ({
        value: 'value',
        label: 'label',
        children: 'children',
        emitPath: true,
        checkStrictly: true,
        showPrefix: false
      })
    }
  })

  const emit = defineEmits(['update:modelValue', 'success'])

  const visibleProxy = computed({
    get() {
      return props.modelValue
    },
    set(val) {
      emit('update:modelValue', val)
    }
  })

  const titleText = computed(() => (props.mode === 'edit' ? '编辑直播间' : '添加直播间'))
  const isEditMode = computed(() => props.mode === 'edit')

  const tradeCascaderProps = {
    value: 'value',
    label: 'label',
    children: 'children',
    emitPath: false,
    checkStrictly: true
  }

  const formRef = ref(null)
  const submitting = ref(false)

  const form = reactive({
    id: '',
    platform: 0,
    anchorNumber: '',
    tradeId: '',
    debutDate: '',
    orgPath: [],
    managerUserIds: [],
    managerUserInfos: []
  })

  const rules = {
    platform: [{ required: true, message: '请选择平台', trigger: 'change' }],
    anchorNumber: [{ required: true, message: '请输入账号ID', trigger: 'blur' }],
    orgPath: [{ required: true, message: '请选择所属组织', trigger: 'change' }],
    managerUserIds: [{ type: 'array', required: true, message: '请选择管理员', trigger: 'change' }]
  }

  const normalizePlatform = (value) => {
    const n = Number(value)
    return Number.isFinite(n) ? n : 0
  }

  const resetForm = () => {
    const row = props.row || {}
    const safeOrgPath = Array.isArray(row.orgPath) ? row.orgPath : []
    const safeIds = Array.isArray(row.managerUserIds) ? row.managerUserIds : []
    const safeUsers = Array.isArray(row.managerUserInfos) ? row.managerUserInfos : []

    form.id = row.id || ''
    form.platform = normalizePlatform(row.platform)
    form.anchorNumber = row.anchorNumber || ''
    form.tradeId = row.tradeId ?? ''
    form.debutDate = row.debutDate || ''
    form.orgPath = safeOrgPath
    form.managerUserIds = safeIds
    form.managerUserInfos = safeUsers
  }

  const mapSubmitPayload = () => {
    const orgPath = Array.isArray(form.orgPath) ? form.orgPath : []
    return {
      id: form.id,
      platform: form.platform,
      anchorNumber: String(form.anchorNumber || '').trim(),
      tradeId: form.tradeId,
      debutDate: form.debutDate,
      companyId: orgPath[0] || '',
      deptId: orgPath[1] || '',
      teamId: orgPath[2] || '',
      managerUserIds: Array.isArray(form.managerUserIds) ? form.managerUserIds : []
    }
  }

  const validateManagerUserIds = () => {
    formRef.value?.validateField('managerUserIds')
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    const ok = await formRef.value.validate().catch(() => false)
    if (!ok) return
    if (submitting.value) return

    submitting.value = true
    try {
      const payload = mapSubmitPayload()
      if (props.mode === 'edit') {
        await apiModule.liveRoom.edit(payload)
        ElMessage.success('修改成功')
      } else {
        await apiModule.liveRoom.add(payload)
        ElMessage.success('新增成功')
      }
      emit('success')
      visibleProxy.value = false
    } catch (e) {
      void e
    } finally {
      submitting.value = false
    }
  }

  watch(
    () => props.modelValue,
    (val) => {
      if (!val) return
      resetForm()
      nextTick(() => {
        formRef.value?.clearValidate?.()
      })
    }
  )
</script>

<style scoped lang="scss">
  .live-room-drawer {
    &__header {
      display: flex;
      width: 100%;
      gap: 13px;
      align-items: center;
    }
  }

  .w-100 {
    width: 100%;
  }

  .custom-btn {
    height: 34px;
    border-radius: 50px;
  }
  .status-btn {
    width: 84px;
  }
  .manager-user-ids-container {
    display: flex;
  }
  :deep(.manager-user-ids-container .el-form-item__label) {
    line-height: 2.3;
    margin-bottom: 0;
  }
  :deep(.manager-user-ids-container .el-form-item__error) {
    left: 23px;
  }
</style>

<style lang="scss">
  .Live-room-drawer-header {
    padding: 11px 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
  }
</style>
