<template>
  <el-drawer
    v-model="visibleProxy"
    header-class="common-drawer-header-style"
    body-class="common-drawer-body-style"
    footer-class="common-drawer-footer-style"
    direction="rtl"
    :size="size"
    :show-close="false"
    :close-on-click-modal="closeOnClickModal"
    class="schedule-attribute-drawer"
  >
    <template #header>
      <div class="role-drawer__header">
        <CloseSvg @click="visibleProxy = false" />
        <div class="role-drawer__title">{{ title }}</div>
      </div>
    </template>

    <div class="attribute-panel">
      <div class="attr-section">
        <div class="attr-title">基础信息</div>
        <div class="attr-card">
          <div class="attr-row">
            <div class="attr-label" :style="labelStyle">直播间名称</div>
            <el-input :model-value="roomName || '-'" disabled class="attr-input rounded" />
          </div>
        </div>
      </div>

      <div class="attr-section">
        <div class="attr-title"><span class="required">*</span>时间配置</div>
        <div class="attr-card">
          <div class="attr-row">
            <div class="attr-label">复制排班配置</div>
            <div v-if="copySourceName" class="attr-copy-pill">
              <span class="attr-copy-pill__text">{{ copySourceName }}</span>
              <div class="svg-container" @click.stop="$emit('clear-copy')">
                <SvgIcon name="round-close" :iconStyle="svgIconStyle" />
              </div>
            </div>
            <el-button v-else type="primary" link :icon="copyButtonIcon" @click="$emit('open-copy')">
              选择直播间
            </el-button>
          </div>

          <div class="attr-row">
            <div class="attr-label">每天轮班时间</div>
            <div class="every-time-picker">
              <el-time-picker
                v-model="planRangeProxy"
                is-range
                start-placeholder="开始"
                end-placeholder="结束"
                format="HH:mm"
                value-format="HH:mm"
                class="rounded"
                style="width: 100%"
              />
            </div>
          </div>

          <div class="attr-row">
            <div class="attr-label">班次时长</div>
            <div class="attr-option-line">
              <div v-for="i in shiftOptions" :key="`shift_${i}`" class="attr-copy-pill">
                <span class="attr-copy-pill__text">{{ i }}/分钟</span>
                <div class="svg-container" @click.stop="$emit('remove-shift-option', i)">
                  <SvgIcon name="round-close" :iconStyle="svgIconStyle" />
                </div>
              </div>
              <el-input
                v-model.number="shiftOptionInputProxy"
                :min="1"
                controls-position="right"
                class="attr-num rounded"
                placeholder="请填写"
              >
                <template #suffix>
                  <span>/分钟</span>
                </template>
              </el-input>
              <el-button type="primary" link @click="$emit('add-shift-option')" :icon="Plus"> 确定添加 </el-button>
            </div>
          </div>

          <div class="attr-row">
            <div class="attr-label">中场休息时长</div>
            <div class="attr-option-line">
              <div v-for="i in restOptions" :key="`rest_${i}`" class="attr-copy-pill">
                <span class="attr-copy-pill__text">{{ i }}/分钟</span>
                <div class="svg-container" @click.stop="$emit('remove-rest-option', i)">
                  <SvgIcon name="round-close" :iconStyle="svgIconStyle" />
                </div>
              </div>
              <el-input
                v-model.number="restOptionInputProxy"
                :min="0"
                controls-position="right"
                placeholder="请填写"
                class="attr-num rounded"
              >
                <template #suffix>
                  <span>/分钟</span>
                </template>
              </el-input>
              <el-button type="primary" link @click="$emit('add-rest-option')" :icon="Plus"> 确定添加 </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="attr-section">
        <div class="attr-title">展示配置</div>
        <div class="attr-card">
          <div class="attr-row show-options">
            <div class="other-label">直播岗位</div>
            <div class="attr-option-line">
              <el-checkbox-group v-model="selectedPositionIds" class="attr-position-group">
                <el-checkbox v-for="p in selectedPositionTags" :key="`pos_${p.id}`" :label="String(p.id)">
                  {{ p.name }}
                </el-checkbox>
              </el-checkbox-group>
              <el-button type="primary" link :icon="Plus" @click="$emit('open-position')"> 添加其他岗位 </el-button>
            </div>
          </div>
          <div class="attr-row show-options">
            <div class="other-label"> 时间轴单位</div>
            <el-radio-group v-model="timeAxisUnitProxy">
              <el-radio :value="120">120分钟</el-radio>
              <el-radio :value="60">60分钟</el-radio>
              <el-radio :value="30">30分钟</el-radio>
            </el-radio-group>
          </div>
        </div>
      </div>
    </div>

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
  import { computed, ref, watch } from 'vue'
  import CloseSvg from '@/components/CloseSvgIcon/index.vue'
  import { Plus } from '@element-plus/icons-vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: '排班配置'
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
      default: '760px'
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    roomName: {
      type: String,
      default: '-'
    },
    planRange: {
      type: Array,
      default: () => ['', '']
    },
    shiftOptions: {
      type: Array,
      default: () => []
    },
    restOptions: {
      type: Array,
      default: () => []
    },
    selectedPositionTags: {
      type: Array,
      default: () => []
    },
    timeAxisUnit: {
      type: Number,
      default: 60
    },
    shiftOptionInput: {
      type: Number,
      default: 120
    },
    restOptionInput: {
      type: Number,
      default: 0
    },
    copySourceName: {
      type: String,
      default: ''
    },
    copyButtonIcon: {
      type: [Object, Function],
      default: null
    },
    labelWidth: {
      type: Number,
      default: 70
    }
  })

  const emit = defineEmits([
    'update:modelValue',
    'update:planRange',
    'update:timeAxisUnit',
    'update:shiftOptionInput',
    'update:restOptionInput',
    'confirm',
    'cancel',
    'open-copy',
    'clear-copy',
    'add-shift-option',
    'remove-shift-option',
    'add-rest-option',
    'remove-rest-option',
    'open-position',
    'remove-position'
  ])

  const selectedPositionIds = ref([])
  const svgIconStyle = computed(() => {
    return {
      width: '15px',
      height: '15px'
    }
  })
  watch(
    () => props.selectedPositionTags,
    (list) => {
      selectedPositionIds.value = (Array.isArray(list) ? list : [])
        .map((i) => i?.id)
        .filter((x) => x !== undefined && x !== null && x !== '')
        .map((x) => String(x))
    },
    { immediate: true }
  )

  watch(selectedPositionIds, (next, prev) => {
    const prevList = Array.isArray(prev) ? prev : []
    const nextList = Array.isArray(next) ? next : []
    const removed = prevList.filter((id) => !nextList.includes(id))
    removed.forEach((id) => emit('remove-position', id))
  })

  const visibleProxy = computed({
    get() {
      return props.modelValue
    },
    set(v) {
      emit('update:modelValue', v)
    }
  })

  const planRangeProxy = computed({
    get() {
      return props.planRange
    },
    set(v) {
      emit('update:planRange', v)
    }
  })

  const timeAxisUnitProxy = computed({
    get() {
      return props.timeAxisUnit
    },
    set(v) {
      emit('update:timeAxisUnit', v)
    }
  })

  const shiftOptionInputProxy = computed({
    get() {
      return props.shiftOptionInput
    },
    set(v) {
      emit('update:shiftOptionInput', v)
    }
  })

  const restOptionInputProxy = computed({
    get() {
      return props.restOptionInput
    },
    set(v) {
      emit('update:restOptionInput', v)
    }
  })

  const labelStyle = computed(() => {
    return props.labelWidth ? { width: `${props.labelWidth}px` } : undefined
  })

  const handleCancel = () => {
    visibleProxy.value = false
    emit('cancel')
  }
</script>

<style scoped lang="scss">
  .role-drawer__header {
    display: flex;
    width: 100%;
    gap: 13px;
    align-items: center;
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

  .attribute-panel {
    display: flex;
    flex-direction: column;
    gap: 36px;
    padding-right: 6px;
  }

  .attr-section {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .attr-title {
    font-size: 16px;
    font-weight: bold;
    color: #303133;
  }

  .required {
    color: #f56c6c;
    margin-right: 4px;
  }

  .attr-card {
    display: flex;
    flex-direction: column;
    row-gap: 24px;
    background: #f7f8fa;
    border-radius: 8px;
    padding: 24px;
  }

  .attr-row {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .show-options {
    gap: 20px;
    .other-label {
      color: #151719;
    }
  }

  .attr-label {
    width: 90px;
    text-align: end;
    flex-shrink: 0;
    color: #151719;
    font-size: 14px;
    text-wrap: nowrap;
  }

  .attr-input {
    width: 230px;

    :deep(.el-input__wrapper) {
      background-color: #dcdcdc;
      box-shadow: none;
    }

    :deep(.el-input__inner) {
      color: #151719 !important;
      -webkit-text-fill-color: #151719 !important;
    }
  }

  .every-time-picker {
    width: 230px;
  }

  .attr-option-line {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
  }

  .attr-tag {
    border-radius: 6px;
  }

  .attr-position-group {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }

  .attr-num {
    width: 120px;
  }

  .attr-hint {
    color: #909399;
    font-size: 12px;
  }

  .attr-copy-pill {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 10px;
    height: 30px;
    padding: 0 10px;
    border-radius: 2px;
    font-size: 14px;
    background-color: #dfeaf6;
    color: #484a4c;

    .svg-container {
      display: flex;
      align-items: center;
      position: absolute;
      top: -6px;
      right: -6px;
      cursor: pointer;
    }
  }

  .attr-copy-pill__text {
    line-height: 1;
  }
</style>
