<template>
  <el-col :span="field.span || 24" v-if="visible">
    <el-form-item
      :label="field.label"
      :prop="field.prop"
      :rules="computedRules"
      :class="{ 'is-required': field.required }"
    >
      <div class="field-content-wrapper">
        <!-- 1. Custom Slot -->
        <slot
          v-if="slotName"
          :name="slotName"
          :field="field"
          :model="modelValue"
          :state="state"
          :readonly="readonly"
          :disabled="disabled"
        />

        <!-- 2. Readonly Text View -->
        <div v-else-if="readonly" class="form-readonly-text">
          <div class="text-content">{{ displayValue }}</div>
          <!-- Action Button for Readonly (Inside capsule if needed, or outside) -->
          <!-- Assuming readonly text container is styled as capsule or similar -->
        </div>

        <!-- 3. Editable Controls -->
        <template v-else>
          <!-- Select -->
          <el-select
            v-if="field.type === 'select'"
            v-model="fieldValue"
            :disabled="disabled"
            :placeholder="field.placeholder"
            :clearable="field.clearable !== false"
            :multiple="field.multiple"
            style="width: 100%"
          >
            <el-option
              v-for="opt in normalizedOptions"
              :key="opt.key"
              :label="opt.label"
              :value="opt.key"
              :disabled="opt.disabled"
            />
          </el-select>

          <!-- Radio -->
          <el-radio-group v-else-if="field.type === 'radio'" v-model="fieldValue" :disabled="disabled">
            <el-radio v-for="opt in normalizedOptions" :key="opt.key" :value="opt.key" :disabled="opt.disabled">
              {{ opt.label }}
            </el-radio>
          </el-radio-group>

          <!-- Tree Select -->
          <el-tree-select
            v-else-if="field.type === 'tree-select' || field.type === 'treeSelect'"
            v-model="fieldValue"
            :data="rawOptions"
            :disabled="disabled"
            :placeholder="field.placeholder"
            :clearable="field.clearable !== false"
            :multiple="field.multiple"
            :node-key="field.nodeKey || 'value'"
            :props="field.props || { label: 'label', children: 'children' }"
            :check-strictly="field.checkStrictly"
            :filterable="field.filterable"
            style="width: 100%"
          />

          <!-- Checkbox -->
          <el-checkbox-group v-else-if="field.type === 'checkbox'" v-model="fieldValue" :disabled="disabled">
            <el-checkbox
              v-for="opt in normalizedOptions"
              :key="opt.key"
              :label="opt.label"
              :value="opt.key"
              :disabled="opt.disabled"
            />
          </el-checkbox-group>

          <!-- Date Picker -->
          <el-date-picker
            v-else-if="field.type === 'date' || field.type === 'datetime' || field.type === 'daterange'"
            v-model="fieldValue"
            :type="field.type"
            :disabled="disabled"
            :placeholder="field.placeholder"
            :value-format="field.valueFormat || 'YYYY-MM-DD'"
            style="width: 100%"
          />

          <!-- Switch -->
          <el-switch
            v-else-if="field.type === 'switch'"
            v-model="fieldValue"
            :disabled="disabled"
            :active-value="field.activeValue"
            :inactive-value="field.inactiveValue"
            :active-text="field.activeText"
            :inactive-text="field.inactiveText"
          />

          <!-- Input Number -->
          <el-input-number
            v-else-if="field.type === 'number'"
            v-model="fieldValue"
            :disabled="disabled"
            :min="field.min"
            :max="field.max"
            :step="field.step"
            :precision="field.precision"
            style="width: 100%"
          />

          <!-- Textarea -->
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="fieldValue"
            type="textarea"
            :disabled="disabled"
            :placeholder="field.placeholder"
            :rows="field.rows || 3"
            :maxlength="field.maxlength"
            show-word-limit
          />

          <!-- Default Input -->
          <el-input
            v-else
            v-model="fieldValue"
            :type="field.inputType || 'text'"
            :disabled="disabled"
            :placeholder="field.placeholder"
            :clearable="field.clearable !== false"
            :maxlength="field.maxlength"
          >
            <template v-if="field.append" #append>{{ field.append }}</template>
            <template v-if="field.prepend" #prepend>{{ field.prepend }}</template>
          </el-input>
        </template>

        <!-- Action Button (Overlay/Suffix) -->
      </div>
      <div v-if="visibleButtons.length > 0" class="action-button-wrapper">
        <template v-for="(btn, index) in visibleButtons" :key="index">
          <!-- With Popconfirm -->
          <el-popconfirm
            :width="160"
            v-if="btn.confirm"
            :title="btn.confirm.title || '您确定提交吗?'"
            :confirm-button-text="btn.confirm.confirmButtonText || '是'"
            :cancel-button-text="btn.confirm.cancelButtonText || '否'"
            @confirm="handleBtnClick(btn)"
          >
            <template #reference>
              <el-button link :type="btn.type || 'primary'">
                <el-icon v-if="btn.icon" class="el-icon--left">
                  <component :is="btn.icon" />
                </el-icon>
                {{ btn.text }}
              </el-button>
            </template>
          </el-popconfirm>

          <!-- Normal Button -->
          <el-button v-else link :type="btn.type || 'primary'" @click="handleBtnClick(btn)">
            <el-icon v-if="btn.icon" class="el-icon--left">
              <component :is="btn.icon" />
            </el-icon>
            {{ btn.text }}
          </el-button>
        </template>
      </div>
    </el-form-item>
  </el-col>
</template>

<script setup>
  /**
   * @file FieldItem.vue
   * @description SchemaForm 字段渲染器：根据 schema/state 渲染单个表单项并处理只读/禁用/显隐
   */
  import { computed, unref } from 'vue'
  import { normalizeKeyLabelOptions } from '@/utils/options'
  import { useFormState } from './useFormState'

  const props = defineProps({
    field: {
      type: Object,
      required: true
    },
    modelValue: {
      type: Object,
      required: true
    },
    state: {
      type: String,
      required: true
    },
    localState: {
      type: String,
      default: null
    }
  })

  const emit = defineEmits(['update:modelValue', 'action'])

  const fieldValue = computed({
    get() {
      return props.modelValue?.[props.field.prop]
    },
    set(val) {
      emit('update:modelValue', {
        ...props.modelValue,
        [props.field.prop]: val
      })
    }
  })

  const currentState = computed(() => props.state)
  const { resolveFieldState, resolveSlotName } = useFormState(props, currentState)

  // Resolve visibility and interactivity
  const fieldState = computed(() => resolveFieldState(props.field))
  const visible = computed(() => fieldState.value.visible)
  const readonly = computed(() => fieldState.value.readonly)
  const disabled = computed(() => fieldState.value.disabled)

  const slotName = computed(() => resolveSlotName(props.field))

  // Action Buttons Logic
  const normalizedActionButtons = computed(() => {
    const config = props.field.actionButton
    if (!config) return []
    return Array.isArray(config) ? config : [config]
  })

  const shouldShowButton = (btnConfig) => {
    if (!btnConfig.show) return true
    // Check if current state (or local state override) is in the allowed list
    const effectiveState = props.localState || currentState.value
    if (Array.isArray(btnConfig.show)) {
      return btnConfig.show.includes(effectiveState)
    }
    return btnConfig.show === effectiveState
  }

  const visibleButtons = computed(() => {
    return normalizedActionButtons.value.filter(shouldShowButton)
  })

  const handleBtnClick = (btnConfig) => {
    // 设置了自定义事件，则不执行监听回调。
    if (btnConfig.onClick) {
      btnConfig.onClick()
    } else {
      emit('action', { prop: props.field.prop, button: btnConfig })
    }
  }

  // Display Value for Readonly Mode
  const displayValue = computed(() => {
    const val = props.modelValue[props.field.prop]

    // 1. Handle Empty
    if (val === undefined || val === null || val === '') return '-'

    // 2. Handle Options (Select/Radio) mapping
    if (props.field.options) {
      // Handle Multiple Select
      if (Array.isArray(val)) {
        return val
          .map((v) => {
            const opt = normalizedOptions.value.find((o) => String(o.key) === String(v))
            return opt ? opt.label : v
          })
          .join(', ')
      }
      // Single Value
      const opt = normalizedOptions.value.find((o) => String(o.key) === String(val))
      return opt ? opt.label : val
    }

    // 3. Handle Boolean/Switch
    if (props.field.type === 'switch') {
      // If activeText is present, return that, else return value
      if (val === (props.field.activeValue ?? true)) return props.field.activeText || '是'
      return props.field.inactiveText || '否'
    }

    return val
  })

  // Rules
  const computedRules = computed(() => {
    // If hidden or readonly, maybe remove validation?
    // Usually readonly fields don't need validation, but sometimes we validate what's there.
    // For now, pass rules through if visible.
    return props.field.rules
  })

  const rawOptions = computed(() => {
    let opts = props.field.options
    if (opts && typeof opts === 'object') {
      if ('value' in opts || opts.__v_isRef) {
        opts = opts.value !== undefined ? opts.value : unref(opts)
      }
    }
    return Array.isArray(opts) ? opts : []
  })

  const normalizedOptions = computed(() => {
    return normalizeKeyLabelOptions(rawOptions.value)
  })
</script>

<style lang="scss" scoped>
  .field-content-wrapper {
    position: relative;
    width: 83%;
    display: flex;
    align-items: center;
  }

  .form-readonly-text {
    line-height: 32px;
    color: var(--el-text-color-regular);
    word-break: break-all;
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .action-button-wrapper {
    margin-left: 8px;
    // Ensure button fits in
  }

  // Adjust input padding to make room for button if it exists
  // This is a bit tricky as we don't know if button is visible via CSS only easily
  // But usually for this UI, the button is inside the capsule.
</style>
