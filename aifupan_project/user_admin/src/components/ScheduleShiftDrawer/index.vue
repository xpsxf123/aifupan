<template>
  <el-drawer
    v-model="visibleProxy"
    :size="size"
    direction="rtl"
    :show-close="false"
    :close-on-click-modal="closeOnClickModal"
    :header-class="headerClass"
    :body-class="bodyClass"
    :footer-class="footerClass"
    class="schedule-shift-drawer"
  >
    <template #header>
      <div class="role-drawer__header">
        <CloseSvg @click="visibleProxy = false" />
        <div class="role-drawer__title">{{ title }}</div>
      </div>
    </template>
    <ScheduleShiftForm
      ref="shiftFormRef"
      :mode="mode"
      :initial="initial"
      :duration-options="durationOptions"
      :rest-options="restOptions"
      :disable-pick-member="disablePickMember"
      @pick-member="(payload) => $emit('pick-member', payload)"
    />

    <template #footer>
      <div class="drawer-footer">
        <el-button class="status-btn" v-auth="confirmPermissionCode" type="primary" @click="$emit('confirm')">{{
          confirmText
        }}</el-button>
        <el-button class="status-btn" @click="handleCancel">{{ cancelText }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
  import { computed, ref } from 'vue'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import ScheduleShiftForm from '@/components/Schedule/components/ScheduleShiftForm.vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '添加排班'
    },
    confirmText: {
      type: String,
      default: '确定'
    },
    confirmPermissionCode: {
      type: [String, Array],
      default: ''
    },
    cancelText: {
      type: String,
      default: '取消'
    },
    size: {
      type: String,
      default: '902px'
    },
    headerClass: {
      type: String,
      default: 'common-drawer-header-style'
    },
    bodyClass: {
      type: String,
      default: 'common-drawer-body-style'
    },
    footerClass: {
      type: String,
      default: 'common-drawer-footer-style'
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String,
      default: 'add'
    },
    initial: {
      type: Object,
      default: () => ({})
    },
    durationOptions: {
      type: Array,
      default: () => []
    },
    restOptions: {
      type: Array,
      default: () => []
    },
    disablePickMember: {
      type: Boolean,
      default: false
    }
  })

  const emit = defineEmits(['update:modelValue', 'confirm', 'cancel', 'pick-member'])

  const shiftFormRef = ref(null)

  const visibleProxy = computed({
    get() {
      return props.modelValue
    },
    set(v) {
      emit('update:modelValue', v)
    }
  })

  const handleCancel = () => {
    visibleProxy.value = false
    emit('cancel')
  }

  const getSubmitData = () => {
    return shiftFormRef.value?.getSubmitData?.()
  }

  const setMember = (index, member) => {
    shiftFormRef.value?.setMember?.(index, member)
  }

  defineExpose({ getSubmitData, setMember })
</script>

<style scoped lang="scss">
  .role-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
    color: #151719;
  }

  .drawer-footer {
    display: flex;
    justify-content: flex-start;
    gap: 12px;
    .el-button {
      height: 34px;
      border-radius: 56px;
    }
    .status-btn {
      width: 84px;
    }
  }
</style>
