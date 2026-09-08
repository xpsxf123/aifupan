<template>
  <div class="page-tabs-container" :class="{ 'is-card': card }">
    <!-- Tab Header -->
    <div class="tabs-header">
      <div class="tabs-nav">
        <div
          v-for="(tab, index) in parsedTabs"
          :key="tab.value"
          class="tab-item"
          :class="{ 'is-active': activeTab === tab.value }"
          @click="handleTabClick(tab)"
        >
          <div class="tab-label-wrapper">
            <span class="tab-label">{{ tab.label }}</span>
            <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
          </div>
          <!-- Vertical Divider (except for the last item) -->
          <div v-if="index < parsedTabs.length - 1" class="tab-divider"></div>
        </div>
      </div>
      <slot name="attached-content"></slot>
    </div>

    <!-- Tab Content -->
    <div class="tabs-content">
      <template v-for="tab in parsedTabs" :key="tab.value">
        <div v-show="activeTab === tab.value" class="tab-pane">
          <slot :name="tab.value"></slot>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
  /**
   * @file src/components/PageTabs/index.vue
   * @description PageTabs 页面级标签页组件
   * @property {Array} tabs - 标签页配置数组，支持字符串数组 ['Tab1', 'Tab2'] 或对象数组 [{ label: 'Tab1', value: 'tab1', badge: 1 }]
   * @property {String|Number} modelValue - 当前激活的标签页 value
   * @event update:modelValue - 更新 modelValue
   * @event tab-click - 点击标签页时触发 (tab)
   */
  import { computed, ref, watch } from 'vue'

  const props = defineProps({
    modelValue: {
      type: [String, Number],
      default: undefined
    },
    tabs: {
      type: Array,
      default: () => [],
      required: true
    },
    card: {
      type: Boolean,
      default: true
    }
  })

  const emit = defineEmits(['update:modelValue', 'tab-click'])

  // Internal state for when v-model is not used
  const internalValue = ref('')

  // Computed active tab
  const activeTab = computed({
    get: () => (props.modelValue !== undefined ? props.modelValue : internalValue.value),
    set: (val) => {
      internalValue.value = val
      emit('update:modelValue', val)
    }
  })

  // Parse tabs input to standard format
  const parsedTabs = computed(() => {
    return props.tabs.map((item, index) => {
      if (typeof item === 'string') {
        // User requested: default name from 'a' start or 'tab1-tabN'
        // Using 'tab1', 'tab2'... as consistent naming
        return {
          label: item,
          value: `tab${index + 1}`
        }
      } else if (typeof item === 'object') {
        return {
          label: item.label,
          value: item.value || item.name || `tab${index + 1}`,
          badge: item.badge
        }
      }
      return { label: '', value: `tab${index + 1}` }
    })
  })

  // Initialize active tab if needed
  watch(
    () => parsedTabs.value,
    (tabs) => {
      if (tabs.length > 0 && !activeTab.value) {
        // If no active tab, select the first one
        // But check if modelValue was provided but empty? No, undefined check handles it.
        // If internalValue is empty, set it.
        if (!props.modelValue && !internalValue.value) {
          activeTab.value = tabs[0].value
        }
      }
    },
    { immediate: true }
  )

  const handleTabClick = (tab) => {
    activeTab.value = tab.value
    emit('tab-click', tab)
  }
</script>

<style scoped lang="scss">
  .page-tabs-container {
    width: 100%;
    display: flex;
    flex-direction: column;

    .tabs-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20px;
      background-color: #fff;
      border-bottom: 1px solid #f0f0f0;
    }
    .tabs-nav {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .tabs-content {
      background-color: #fff;
    }
    &.is-card {
      box-sizing: border-box;
      .tabs-header {
        padding-bottom: 16px;
        border-bottom: none;
        margin-bottom: 16px;
        border-radius: var(--border-radius-base);
      }
      .tabs-content {
        margin-top: 0;
        background: transparent;
      }
    }
  }

  .tab-item {
    position: relative;
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 0 20px;
    user-select: none;
    transition: all 0.3s;

    &:first-child {
      padding-left: 0;
    }

    .tab-label-wrapper {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .tab-label {
      font-size: 16px;
      color: #7a7c7f;
      transition: color 0.3s;
    }

    .tab-badge {
      background-color: var(--el-color-primary, #409eff);
      color: white;
      font-size: 12px;
      height: 18px;
      min-width: 18px;
      padding: 0 5px;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      line-height: 1;
    }

    &:hover {
      .tab-label {
        color: var(--el-color-primary, #409eff);
      }
    }

    &.is-active {
      .tab-label {
        color: var(--el-color-primary, #409eff);
      }
    }

    .tab-divider {
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 1px;
      height: 16px;
      background-color: #dcdfe6;
    }
  }

  .tabs-content {
    flex: 1;
    margin-top: 16px;
  }
</style>
