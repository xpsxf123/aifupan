<template>
  <el-dialog
    v-bind="$attrs"
    :model-value="modelValue"
    @update:model-value="updateModelValue"
    class="common-dialog"
    :width="width"
    :close-on-click-modal="false"
    align-center
    destroy-on-close
  >
    <!-- Custom Header: Title Left, Close Right (Default behavior, but ensures style) -->
    <template #header="{ close, titleId, titleClass }">
      <div class="common-dialog-header">
        <span :id="titleId" :class="titleClass" class="dialog-title">{{ title }}</span>
        <!-- Close button is handled by Element Plus default header structure if we don't override it completely. 
             However, if we use #header slot, we replace the entire header content including the close button.
             So we must re-implement the close button if we use this slot.
        -->
        <button type="button" class="el-dialog__headerbtn" aria-label="Close" @click="close">
          <el-icon class="el-dialog__close"><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="common-dialog-content" :style="contentStyle">
      <slot />
    </div>

    <template #footer v-if="$slots.footer">
      <div class="common-dialog-footer">
        <slot name="footer" />
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
  import { computed } from 'vue'
  import { Close } from '@element-plus/icons-vue'

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    width: {
      type: [String, Number],
      default: '440px'
    }
  })

  const emit = defineEmits(['update:modelValue'])

  const updateModelValue = (val) => {
    emit('update:modelValue', val)
  }

  const contentStyle = computed(() => ({
    minHeight: props.minHeight
  }))
</script>

<style lang="scss"></style>
