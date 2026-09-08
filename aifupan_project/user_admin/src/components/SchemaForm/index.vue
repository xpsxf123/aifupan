<template>
  <div class="schema-form">
    <el-form
      ref="formRef"
      :model="modelValue"
      :label-width="options.labelWidth || '100px'"
      :label-position="options.labelPosition || 'right'"
      :disabled="globalDisabled"
      :validate-on-rule-change="false"
      class="schema-form__content"
    >
      <!-- Group Rendering -->
      <template v-if="isGrouped">
        <div v-for="(group, index) in schema" :key="group.title || index" class="schema-form__group">
          <div v-if="group.title" class="schema-form__group-title border-bottom pb-20 font-weight-500">
            {{ group.title }}
          </div>
          <el-row :gutter="options.gutter || 20">
            <template v-for="field in group.children" :key="field.prop">
              <FieldItem
                :field="field"
                :model-value="modelValue"
                :state="state"
                :local-state="localStates[field.prop]"
                @update:model-value="(val) => emit('update:modelValue', val)"
                @action="
                  (payload) =>
                    emit('action-click', {
                      ...payload,
                      actions: {
                        edit: () => handleAction(field.prop, 'edit'),
                        view: () => handleAction(field.prop, 'view')
                      }
                    })
                "
              >
                <!-- Pass through slots -->
                <template v-for="(_, name) in $slots" #[name]="slotData">
                  <slot :name="name" v-bind="slotData" />
                </template>
              </FieldItem>
            </template>
          </el-row>
        </div>
      </template>

      <!-- Flat Rendering -->
      <el-row v-else :gutter="options.gutter || 20">
        <template v-for="field in schema" :key="field.prop">
          <FieldItem
            :field="field"
            :model-value="modelValue"
            :state="state"
            :local-state="localStates[field.prop]"
            @update:model-value="(val) => emit('update:modelValue', val)"
            @action="
              (payload) =>
                emit('action-click', {
                  ...payload,
                  actions: {
                    edit: () => handleAction(field.prop, 'edit'),
                    view: () => handleAction(field.prop, 'view')
                  }
                })
            "
          >
            <!-- Pass through slots -->
            <template v-for="(_, name) in $slots" #[name]="slotData">
              <slot :name="name" v-bind="slotData" />
            </template>
          </FieldItem>
        </template>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
  /**
   * @file SchemaForm Component
   * @description A data-driven form component supporting complex states, grouping, and dynamic slots.
   */
  import { ref, computed, useSlots } from 'vue'
  import { useFormState } from './useFormState'
  import FieldItem from './FieldItem.vue'

  const props = defineProps({
    // Form Data Model
    modelValue: {
      type: Object,
      default: () => ({})
    },
    // Form Schema (Array of Fields or Groups)
    schema: {
      type: Array,
      required: true,
      default: () => []
    },
    // Current State: 'add', 'edit', 'view', or custom string
    state: {
      type: String,
      default: 'add'
    },
    // Global Options
    options: {
      type: Object,
      default: () => ({
        labelWidth: '100px',
        gutter: 20,
        labelPosition: 'right'
      })
    }
  })

  const emit = defineEmits(['update:modelValue', 'action-click'])

  const formRef = ref(null)

  // Local state overrides for individual fields
  const localStates = ref({})

  const handleAction = (prop, type) => {
    // type: 'edit' or 'view'
    localStates.value[prop] = type
  }

  // Computed State
  const currentState = computed(() => props.state)

  // Check if schema is grouped
  const isGrouped = computed(() => {
    return props.schema.some((item) => item.children && Array.isArray(item.children))
  })

  // Global Disabled
  const globalDisabled = computed(() => false)

  defineExpose({
    formRef
  })
</script>

<style lang="scss" scoped>
  .schema-form {
    &__group {
      border-radius: 4px;
      &-title {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 20px;
        padding-left: 10px;
        border-left: 4px solid var(--el-color-primary);
      }
    }
  }

  :deep(.is-readonly) {
    // Readonly styling
    .el-input__inner,
    .el-textarea__inner {
      box-shadow: none;
      background-color: transparent;
      padding-left: 0;
      color: var(--el-text-color-regular);
      cursor: default;
    }
    .el-input__wrapper {
      box-shadow: none !important;
      background-color: transparent !important;
    }
    .el-select .el-input__wrapper {
      box-shadow: none !important;
    }
    .el-input__suffix {
      display: none;
    }
  }
</style>
