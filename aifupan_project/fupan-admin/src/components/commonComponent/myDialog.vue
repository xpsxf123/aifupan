<template>
  <el-dialog
    v-model="dialogVisible"
    :append-to-body="appendToBody"
    :class="[customClass, { 'm-b-10': !footer }]"
    :close-on-click-modal="closeOnClickModal"
    :title="title"
    :width="width"
    @close="close"
    :fullscreen="fullscreen"
  >
    <div :class="`${maxHeight ? 'max' : ''}`" class="my-div">
      <slot />
    </div>
    <template v-if="footer" #footer>
      <el-button style="font-size: 16px" @click="close">取 消</el-button>
      <el-button style="font-size: 16px" type="primary" @click="submit"
        >确 定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  customClass: {
    type: String,
    default: '',
  },
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: '提示',
  },
  closeOnClickModal: {
    type: Boolean,
    default: false,
  },
  width: {
    type: String,
    default: '50%',
  },
  maxHeight: {
    type: Boolean,
    default: false,
  },
  footer: {
    type: Boolean,
    default: true,
  },
  appendToBody: {
    type: Boolean,
    default: false,
  },
  beforeSubmitFun: {
    type: Function,
    default: function () {
      return true
    },
  },
  fullscreen: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'submit', 'close'])

const dialogVisible = ref(false)

watch(
  () => props.modelValue,
  (newVal) => {
    dialogVisible.value = newVal
  },
  { immediate: true }
)

watch(dialogVisible, (newVal) => {
  emit('update:modelValue', newVal)
})

const handleClose = () => {
  // 处理关闭逻辑
}

const submit = () => {
  emit('submit')
}

const close = () => {
  emit('close')
}
</script>

<style lang="less" scoped>
.my-div {
  overflow-y: auto;
  max-height: 68vh;
}

.max {
  height: 68vh;
}

.m-b-10 {
  margin-bottom: 10px;
}

.el-dialog {
  top: 50% !important;
  transform: translateY(-50%) !important; /* 垂直居中 */
}

:deep(.el-dialog .el-dialog__header) {
  padding: 10px;
}

:deep(.el-dialog .el-dialog__body) {
  padding: 10px;
}

:deep(.el-dialog .el-dialog__footer) {
  padding: 0 15px 15px 15px;
}
</style>
