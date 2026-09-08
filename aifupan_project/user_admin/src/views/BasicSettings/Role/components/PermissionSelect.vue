<template>
  <div class="permission-select" :style="{ minHeight: typeof height === 'number' ? height + 'px' : height }">
    <el-tree
      ref="treeRef"
      :data="options"
      show-checkbox
      :check-strictly="true"
      node-key="id"
      :props="defaultProps"
      class="permissions-menu"
      @check="handleCheck"
    />
  </div>
</template>

<script setup>
  /**
   * @file PermissionSelect.vue
   * @description 权限选择组件，基于 el-tree 封装
   */
  import { ref, watch, nextTick } from 'vue'

  /**
   * @description 组件入参
   * @property {Array} modelValue - 已选中的节点 id 列表
   * @property {Array} options - 树形数据（id/label/children）
   * @property {string|number} height - 容器高度
   */
  const props = defineProps({
    modelValue: {
      type: Array,
      default: () => []
    },
    options: {
      type: Array,
      default: () => []
    },
    height: {
      type: [String, Number],
      default: '350px'
    }
  })

  const emit = defineEmits(['update:modelValue'])

  const treeRef = ref(null)
  const defaultProps = {
    children: 'children',
    label: 'label'
  }

  let lastEmittedStr = ''
  let syncing = false

  const emitCheckedKeys = () => {
    const keys = treeRef.value ? treeRef.value.getCheckedKeys(false) : []
    const uniq = Array.from(new Set(keys || []))
    lastEmittedStr = JSON.stringify(uniq)
    emit('update:modelValue', uniq)
  }

  const syncChildrenChecked = (nodes, checked) => {
    if (!treeRef.value || !Array.isArray(nodes) || !nodes.length) return
    nodes.forEach((item) => {
      if (!item) return
      if (Object.prototype.hasOwnProperty.call(item, 'id')) {
        treeRef.value.setChecked(item.id, checked, false)
      }
      const isTypeOneNode = Number(item.type) === 1
      const shouldSkipAutoCheckChildren = checked && isTypeOneNode
      if (item.children && !shouldSkipAutoCheckChildren) {
        syncChildrenChecked(item.children, checked)
      }
    })
  }

  const hasCheckedDirectChild = (node) => {
    const children = Array.isArray(node?.childNodes) ? node.childNodes : []
    return children.some((child) => child && child.checked)
  }

  const hasTypeOneDirectChild = (node) => {
    const children = Array.isArray(node?.childNodes) ? node.childNodes : []
    return children.some((child) => child && Number(child?.data?.type) === 1)
  }

  const syncParentChecked = (node) => {
    if (!treeRef.value || !node) return
    const parent = node.parent
    if (!parent || parent.level <= 0) return
    const checkedByChildren = hasCheckedDirectChild(parent)
    // 仅限父子关系：父节点只受直接子节点的 type=1 保护
    const keepCheckedByType = !checkedByChildren && hasTypeOneDirectChild(parent)
    const shouldChecked = checkedByChildren || keepCheckedByType
    if (parent.checked !== shouldChecked) {
      treeRef.value.setChecked(parent.data?.id, shouldChecked, false)
    }
  }

  const handleCheck = (data) => {
    if (!treeRef.value) return
    if (syncing) return

    const node = treeRef.value.getNode(data?.id)
    const checked = Boolean(node?.checked)
    if (data && Array.isArray(data.children) && data.children.length) {
      syncing = true
      try {
        const shouldSyncChildren = !(checked && Number(data.type) === 1)
        if (shouldSyncChildren) {
          syncChildrenChecked(data.children, checked)
        }
        syncParentChecked(node)
      } finally {
        syncing = false
      }
    } else {
      syncing = true
      try {
        syncParentChecked(node)
      } finally {
        syncing = false
      }
    }

    emitCheckedKeys()
  }

  /**
   * @description 外部 v-model 或 options 变化时，同步到 el-tree 的勾选态
   */
  watch(
    () => [props.modelValue, props.options],
    ([val, options]) => {
      const valStr = JSON.stringify(val || [])
      // 如果变化是由内部点击触发的（即与上次 emit 的值一致），则跳过，避免 el-tree 状态被破坏引发死循环和全选
      if (valStr === lastEmittedStr) return

      nextTick(() => {
        if (treeRef.value && options && options.length > 0) {
          syncing = true
          try {
            treeRef.value.setCheckedKeys(val || [])
          } finally {
            syncing = false
          }
        }
      })
    },
    { deep: true, immediate: true }
  )
</script>

<style scoped>
  .permission-select {
    border-radius: 4px;
    width: 100%;
    overflow-y: auto;
  }
  .permissions-menu {
    background-color: #f7f7f7;
  }
</style>
