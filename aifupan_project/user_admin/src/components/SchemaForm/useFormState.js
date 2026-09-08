/**
 * @file useFormState.js
 * @description Hook to handle form field state resolution (visibility, readonly, disabled) based on current form state.
 */
import { computed } from 'vue'

/**
 * Resolve the state of a field (visible, readonly, disabled)
 * @param {Object} props - Component props
 * @param {String} currentState - Current form state (view, add, edit, custom...)
 * @returns {Object} Helper functions
 */
export function useFormState(props, currentState) {
  /**
   * Check if a value is in the target array or equals the target string
   * @param {String|Array} target
   * @param {String} value
   */
  const matches = (target, value) => {
    if (Array.isArray(target)) return target.includes(value)
    return target === value
  }

  /**
   * Resolve field configuration for the current state
   * @param {Object} field - Field configuration object
   * @returns {Object} { visible, readonly, disabled }
   */
  const resolveFieldState = (field) => {
    // 优先使用 localState 覆盖
    const state = props.localState || currentState.value

    // Default values
    let visible = true
    let readonly = state === 'view' // Default view mode is readonly
    let disabled = false

    // 1. Priority: stateConfig (Specific state configuration)
    // Example: stateConfig: { add: { visible: true }, edit: { readonly: true } }
    if (field.stateConfig && field.stateConfig[state]) {
      const config = field.stateConfig[state]
      if (config.visible !== undefined) visible = config.visible
      if (config.readonly !== undefined) readonly = config.readonly
      if (config.disabled !== undefined) disabled = config.disabled
      // If stateConfig is present for this state, we might stop here or let other props override?
      // Usually specific config overrides general props.
      return { visible, readonly, disabled }
    }

    // 2. General shortcuts
    // visible: ['add', 'view'] -> only visible in these states
    if (field.visible) {
      if (Array.isArray(field.visible)) {
        visible = field.visible.includes(state)
      } else if (typeof field.visible === 'function') {
        visible = field.visible(state)
      } else {
        visible = !!field.visible
      }
    }

    // hidden: ['edit'] -> hidden in edit
    if (field.hidden) {
      if (Array.isArray(field.hidden)) {
        if (field.hidden.includes(state)) visible = false
      } else if (typeof field.hidden === 'function') {
        if (field.hidden(state)) visible = false
      } else if (field.hidden === true) {
        visible = false
      }
    }

    // readonly: ['edit'] -> readonly in edit
    if (field.readonly !== undefined) {
      if (Array.isArray(field.readonly)) {
        if (field.readonly.includes(state)) readonly = true
      } else if (typeof field.readonly === 'boolean') {
        readonly = field.readonly
      }
    }

    // disabled: ['edit'] -> disabled in edit
    if (field.disabled) {
      if (Array.isArray(field.disabled)) {
        if (field.disabled.includes(state)) disabled = true
      } else if (typeof field.disabled === 'boolean') {
        disabled = field.disabled
      }
    }

    // Special User Requirements Logic (shortcuts for common patterns)
    // "Permanent Read-only": User might config this via `visible: ['view']` (already handled)

    // "Add Read-only" (Available in Add/View, Readonly in Edit):
    // This is a pattern. Users can implement it via:
    // visible: ['add', 'view', 'edit'], readonly: ['edit', 'view']

    return { visible, readonly, disabled }
  }

  /**
   * Resolve Slot Name
   * @param {Object} field
   */
  const resolveSlotName = (field) => {
    const state = currentState.value
    if (!field.slot) return null

    // slot: true -> always use prop name
    if (field.slot === true) return field.prop

    // slot: ['add', 'view'] -> use 'add-prop', 'view-prop'
    if (Array.isArray(field.slot)) {
      if (field.slot.includes(state) || field.slot.includes('*')) {
        return `${state}-${field.prop}`
      }
      return null // Slot not active for this state
    }

    // slot: 'customName'
    if (typeof field.slot === 'string') return field.slot

    return null
  }

  return {
    resolveFieldState,
    resolveSlotName
  }
}
