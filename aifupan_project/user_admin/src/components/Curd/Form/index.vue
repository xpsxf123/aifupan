<template>
  <component
    :is="type === 'drawer' ? 'el-drawer' : 'el-dialog'"
    :class="['curd-form-dialog', { 'curd-form-dialog--grouped': hasGroupConfig }, customClass]"
    :title="title"
    :model-value="visible"
    :width="effectiveDialogWidth"
    :size="effectiveDialogWidth"
    :close-on-click-modal="false"
    @update:model-value="handleVisibleChange"
    @close="handleClose"
  >
    <div class="form-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :validate-on-rule-change="false"
        label-width="100px"
        :label-position="labelPosition"
        :disabled="mode === 'view'"
      >
        <el-row :gutter="20">
          <template v-for="item in currentConfig" :key="getItemKey(item)">
            <el-col v-if="isGroup(item)" :span="item.span || 24">
              <div v-if="shouldShowGroup(item)" class="curd-form__group">
                <div class="curd-form__group-title">{{ item.title }}</div>
                <el-row :gutter="20" class="curd-form__group-body">
                  <template v-for="child in resolveGroupChildren(item)" :key="getItemKey(child)">
                    <el-col :span="child.span || item.childSpan || 12" v-if="shouldShow(child)">
                      <slot v-if="child.slotRow" :name="child.slotName" :row="child" :form="formData" :mode="mode" />

                      <el-form-item
                        v-else
                        :label="child.label"
                        :prop="child.prop"
                        :label-width="child.labelWidth"
                        :class="child.class"
                      >
                        <slot v-if="child.slotName" :name="child.slotName" :row="child" :form="formData" :mode="mode" />

                        <div v-else-if="child.type === 'select'" class="curd-form__select-row">
                          <el-select
                            v-model="formData[child.prop]"
                            :placeholder="child.placeholder"
                            :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                            v-bind="child.opetion"
                            class="rounded"
                          >
                            <el-option
                              v-for="opt in normalizeOptions(child.options)"
                              :key="opt.key"
                              :label="opt.label"
                              :value="opt.key"
                            />
                          </el-select>
                          <el-button
                            v-if="shouldShowEmptyAction(child)"
                            class="curd-form__empty-action"
                            type="primary"
                            link
                            @click="handleEmptyAction(child)"
                          >
                            {{ child.emptyAction?.text || '去添加' }}
                          </el-button>
                        </div>

                        <el-cascader
                          v-else-if="child.type === 'cascader'"
                          v-model="formData[child.prop]"
                          :options="resolveRawOptions(child.options)"
                          :placeholder="child.placeholder"
                          :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
                          :props="
                            child.props || {
                              value: 'value',
                              label: 'label',
                              children: 'children'
                            }
                          "
                          :clearable="child.clearable !== false"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          v-bind="child.opetion"
                          class="rounded"
                        />

                        <el-radio-group
                          v-else-if="child.type === 'radio'"
                          v-model="formData[child.prop]"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          class="rounded"
                        >
                          <el-radio v-for="opt in normalizeOptions(child.options)" :key="opt.key" :label="opt.key">
                            {{ opt.label }}
                          </el-radio>
                        </el-radio-group>

                        <el-checkbox-group
                          v-else-if="child.type === 'checkbox'"
                          v-model="formData[child.prop]"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          class="rounded"
                        >
                          <el-checkbox v-for="opt in normalizeOptions(child.options)" :key="opt.key" :label="opt.key">
                            {{ opt.label }}
                          </el-checkbox>
                        </el-checkbox-group>

                        <el-date-picker
                          v-else-if="child.type === 'date'"
                          v-model="formData[child.prop]"
                          type="date"
                          value-format="YYYY-MM-DD"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          class="rounded"
                        />

                        <el-input
                          v-else-if="child.type === 'textarea'"
                          v-model="formData[child.prop]"
                          type="textarea"
                          :rows="child.rows || 3"
                          :placeholder="child.placeholder"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          class="rounded"
                        />

                        <el-input
                          v-else
                          v-model="formData[child.prop]"
                          :placeholder="child.placeholder"
                          :type="child.inputType || 'text'"
                          :style="{ width: child.width || '100%', maxWidth: child.width ? 'unset' : '350px' }"
                          class="rounded"
                        />
                      </el-form-item>
                    </el-col>
                  </template>
                </el-row>
              </div>
            </el-col>

            <el-col v-else-if="shouldShow(item)" :span="item.span || 24">
              <slot v-if="item.slotRow" :name="item.slotName" :row="item" :form="formData" :mode="mode" />

              <el-form-item
                v-else
                :label="item.label"
                :prop="item.prop"
                :label-width="item.labelWidth"
                :class="item.class"
              >
                <slot v-if="item.slotName" :name="item.slotName" :row="item" :form="formData" :mode="mode" />

                <div v-else-if="item.type === 'select'" class="curd-form__select-row">
                  <el-select
                    v-model="formData[item.prop]"
                    :placeholder="item.placeholder"
                    :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                    v-bind="item.opetion"
                    class="rounded"
                  >
                    <el-option
                      v-for="opt in normalizeOptions(item.options)"
                      :key="opt.key"
                      :label="opt.label"
                      :value="opt.key"
                    />
                  </el-select>
                  <el-button
                    v-if="shouldShowEmptyAction(item)"
                    class="curd-form__empty-action"
                    type="primary"
                    link
                    @click="handleEmptyAction(item)"
                  >
                    {{ item.emptyAction?.text || '去添加' }}
                  </el-button>
                </div>

                <el-cascader
                  v-else-if="item.type === 'cascader'"
                  v-model="formData[item.prop]"
                  :options="resolveRawOptions(item.options)"
                  :placeholder="item.placeholder"
                  :popper-options="{ modifiers: [{ name: 'flip', enabled: false }] }"
                  :props="
                    item.props || {
                      value: 'value',
                      label: 'label',
                      children: 'children'
                    }
                  "
                  :clearable="item.clearable !== false"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                  v-bind="item.opetion"
                  class="rounded"
                />

                <el-radio-group
                  v-else-if="item.type === 'radio'"
                  v-model="formData[item.prop]"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                  class="rounded"
                >
                  <el-radio v-for="opt in normalizeOptions(item.options)" :key="opt.key" :label="opt.key">
                    {{ opt.label }}
                  </el-radio>
                </el-radio-group>

                <el-checkbox-group
                  v-else-if="item.type === 'checkbox'"
                  v-model="formData[item.prop]"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                >
                  <el-checkbox v-for="opt in normalizeOptions(item.options)" :key="opt.key" :label="opt.key">
                    {{ opt.label }}
                  </el-checkbox>
                </el-checkbox-group>

                <el-date-picker
                  v-else-if="item.type === 'date'"
                  v-model="formData[item.prop]"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                  class="rounded"
                />

                <el-input
                  v-else-if="item.type === 'textarea'"
                  v-model="formData[item.prop]"
                  type="textarea"
                  :rows="item.rows || 3"
                  :placeholder="item.placeholder"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                  class="rounded"
                />

                <el-input
                  v-else
                  v-model="formData[item.prop]"
                  :placeholder="item.placeholder"
                  :type="item.inputType || 'text'"
                  :style="{ width: item.width || '100%', maxWidth: item.width ? 'unset' : '350px' }"
                  class="rounded"
                />
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleVisibleChange(false)">取消</el-button>
        <el-button v-if="mode !== 'view'" type="primary" @click="handleSubmit" :loading="loading"> 确定 </el-button>
      </span>
    </template>
  </component>
</template>

<script setup>
  /**
   * @file Form Component for Curd Module
   * @description Provides a dynamic form dialog for Add/Edit/View operations.
   * Updated to support Drawer mode and slotRow.
   */
  import { ref, computed, watch, nextTick, unref } from 'vue'
  import { normalizeKeyLabelOptions } from '@/utils/options'

  const props = defineProps({
    visible: {
      type: Boolean,
      default: false
    },
    mode: {
      type: String, // 'add', 'edit', 'view'
      default: 'add'
    },
    type: {
      type: String, // 'dialog' | 'drawer'
      default: 'drawer'
    },
    /**
     * Form Configuration.
     * Can be an array (common for all modes) or an object { add: [], edit: [], view: [] }
     */
    formConfig: {
      type: [Array, Object],
      default: () => []
    },
    modelValue: {
      type: Object,
      default: () => ({})
    },
    loading: {
      type: Boolean,
      default: false
    },
    width: {
      type: [String, Number],
      default: '500px' // Changed default from '50%' to '500px' as per user request (400-500)
    },
    labelPosition: {
      type: String,
      default: 'right',
      validator: (val) => ['left', 'right', 'top'].includes(val)
    },
    customClass: {
      type: String,
      default: ''
    }
  })

  const emit = defineEmits(['update:visible', 'update:modelValue', 'submit', 'close', 'empty-action'])

  const formRef = ref(null)

  // Local copy of form data
  const formData = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val)
  })

  const title = computed(() => {
    const map = {
      add: '新增',
      edit: '编辑',
      view: '查看'
    }
    return map[props.mode] || '操作'
  })

  // Get the config for the current mode
  const currentConfig = computed(() => {
    if (Array.isArray(props.formConfig)) {
      return props.formConfig
    }
    return props.formConfig[props.mode] || []
  })

  const hasGroupConfig = computed(() => {
    return Array.isArray(currentConfig.value) && currentConfig.value.some((item) => isGroup(item))
  })

  // Generate rules based on config
  const rules = computed(() => {
    const r = {}
    const collect = (items) => {
      if (!Array.isArray(items)) return
      items.forEach((item) => {
        if (isGroup(item)) {
          collect(resolveGroupChildren(item))
          return
        }
        if (item && item.rules) {
          r[item.prop] = item.rules
        }
      })
    }
    collect(currentConfig.value)
    return r
  })

  // Computed Dialog Width to handle numeric vs string inputs
  const computedDialogWidth = computed(() => {
    if (typeof props.width === 'number') {
      return `${props.width}px`
    }
    // Check if user passed percentage or px
    if (!props.width) return '500px' // Safe fallback
    return props.width
  })

  const computedGroupDrawerWidth = () => {
    const vw = window?.innerWidth || document?.documentElement?.clientWidth || 0
    if (!vw) return '600px'
    if (vw < 600) return '100%'
    const half = Math.round(vw * 0.5)
    if (half < 600) return '600px'
    return `${half}px`
  }

  const groupDrawerWidth = ref('')
  const effectiveDialogWidth = computed(() => {
    if (props.type === 'drawer' && hasGroupConfig.value) {
      return groupDrawerWidth.value || '600px'
    }
    return computedDialogWidth.value
  })

  const shouldShow = (item) => {
    if (typeof item.hidden === 'function') {
      return !item.hidden(formData.value, props.mode)
    }
    return !item.hidden
  }

  const isGroup = (item) => {
    return !!item && item.type === 'group' && Array.isArray(item.children)
  }

  const resolveGroupChildren = (group) => {
    return Array.isArray(group?.children) ? group.children : []
  }

  const shouldShowGroup = (group) => {
    if (typeof group.hidden === 'function') {
      return !group.hidden(formData.value, props.mode)
    }
    if (group.hidden) return false
    return resolveGroupChildren(group).some((child) => shouldShow(child))
  }

  const getItemKey = (item) => {
    return item?.prop || item?.slotName || item?.key || item?.title
  }

  const handleVisibleChange = (val) => {
    emit('update:visible', val)
  }

  const handleClose = () => {
    if (formRef.value) {
      formRef.value.resetFields()
    }
    emit('close')
  }

  const handleSubmit = async () => {
    if (!formRef.value) return
    await formRef.value.validate((valid) => {
      if (valid) {
        emit('submit', formData.value)
      }
    })
  }

  const resolveRawOptions = (options) => {
    let resolved = options
    if (options && typeof options === 'object') {
      if ('value' in options || options.__v_isRef) {
        resolved = options.value !== undefined ? options.value : unref(options)
      }
    }
    return Array.isArray(resolved) ? resolved : []
  }

  const normalizeOptions = (options) => normalizeKeyLabelOptions(options)

  const shouldShowEmptyAction = (item) => {
    if (!item || item.type !== 'select') return false
    if (props.mode === 'view') return false
    if (!item.emptyAction) return false
    const opts = normalizeOptions(item.options)
    return Array.isArray(opts) && opts.length === 0
  }

  const handleEmptyAction = (item) => {
    const formSnapshot = JSON.parse(JSON.stringify(formData.value || {}))
    emit('empty-action', {
      item,
      action: item?.emptyAction,
      mode: props.mode,
      formData: formSnapshot
    })
  }

  watch(
    () => props.visible,
    (val) => {
      if (!val) return
      if (props.type === 'drawer' && hasGroupConfig.value) {
        groupDrawerWidth.value = computedGroupDrawerWidth()
      } else {
        groupDrawerWidth.value = ''
      }
      nextTick(() => {
        if (formRef.value) {
          formRef.value.clearValidate()
        }
      })
    }
  )

  // Expose ref for external usage
  defineExpose({
    formRef
  })
</script>

<style scoped>
  .curd-form__select-row {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    flex: 1;
    min-width: 0;
  }

  .curd-form__select-row :deep(.el-select) {
    flex: 1;
    min-width: 0;
  }

  .curd-form__empty-action {
    flex-shrink: 0;
    padding: 0;
  }

  .curd-form__group {
    /* width: 100%; */
    padding: 16px;
    background: #f6f8fb;
    border-radius: 8px;
    overflow: hidden;
  }

  .curd-form__group-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2329;
    margin-bottom: 12px;
  }

  .curd-form__group-body {
    width: 100%;
    overflow-x: hidden;
  }

  .curd-form-dialog :deep(.el-drawer__body) {
    overflow-x: hidden;
  }

  .curd-form-dialog--grouped :deep(.el-row) {
    margin-left: 0 !important;
    margin-right: 0 !important;
  }

  .curd-form-dialog--grouped :deep(.el-drawer__body) {
    padding-bottom: 70px;
  }
</style>
