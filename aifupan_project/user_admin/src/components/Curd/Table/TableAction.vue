<template>
  <div class="table-action">
    <!-- View -->
    <el-button
      v-if="resolveBuiltInVisible('view')"
      type="primary"
      link
      :size="actionButtonSize"
      @click="handleCommand('view', row)"
    >
      {{ viewText || '查看' }}
    </el-button>

    <!-- Edit -->
    <el-button
      v-if="resolveBuiltInVisible('edit')"
      type="primary"
      link
      :size="actionButtonSize"
      @click="handleCommand('edit', row)"
    >
      {{ editText || '编辑' }}
    </el-button>

    <!-- Delete -->
    <el-popconfirm v-if="resolveBuiltInVisible('del')" title="确定删除吗？" @confirm="handleCommand('del', row)">
      <template #reference>
        <el-button type="danger" link :size="actionButtonSize">
          {{ delText || '删除' }}
        </el-button>
      </template>
    </el-popconfirm>

    <!-- Custom Actions -->
    <template v-for="(action, index) in customActions" :key="index">
      <span v-if="shouldShow(action, row)" class="action-btn-wrapper">
        <el-popconfirm
          v-if="action.confirm"
          :title="action.confirm.title || '确定执行此操作？'"
          :confirm-button-text="action.confirm.confirmButtonText || '确定'"
          :cancel-button-text="action.confirm.cancelButtonText || '取消'"
          @confirm="handleCommand(action.command, row)"
        >
          <template #reference>
            <el-button
              :type="action.type || 'primary'"
              :link="action.link !== false"
              :size="action.size || actionButtonSize"
            >
              {{ action.label }}
            </el-button>
          </template>
        </el-popconfirm>
        <el-button
          v-else
          :type="action.type || 'primary'"
          :link="action.link !== false"
          :size="action.size || actionButtonSize"
          @click="handleCommand(action.command, row)"
        >
          {{ action.label }}
        </el-button>
      </span>
    </template>
  </div>
</template>

<script setup>
  import { computed } from 'vue'
  import { usePermissionStore } from '@/auth/store'

  const props = defineProps({
    row: {
      type: Object,
      required: true
    },
    actions: {
      type: Object,
      default: () => ({ view: true, edit: true, del: true })
    },
    customActions: {
      type: Array,
      default: () => []
    },
    viewText: {
      type: String,
      default: '查看'
    },
    editText: {
      type: String,
      default: '编辑'
    },
    delText: {
      type: String,
      default: '删除'
    },
    actionButtonSize: {
      type: String,
      default: 'default'
    },
    listPermissionCode: {
      type: String,
      default: ''
    }
  })

  const emit = defineEmits(['command'])

  const permissionStore = usePermissionStore()
  const cleanListCode = computed(() => String(props.listPermissionCode || '').trim())

  const getActionCodeByOp = (op) => {
    const listCode = cleanListCode.value
    if (!listCode) return ''
    return permissionStore.getActionPermissionCode(listCode, op)
  }

  const canByAnyCode = (code) => {
    if (!code) return true
    return permissionStore.hasPermission(code)
  }

  const resolveBuiltInVisible = (actionKey) => {
    const v = props.actions?.[actionKey]
    if (!v) return false

    if (typeof v === 'function') {
      return v(props.row)
    }

    if (typeof v === 'string' || Array.isArray(v)) {
      return permissionStore.hasPermission(v)
    }

    if (v !== true) return Boolean(v)

    const opMap = { edit: 'update', del: 'delete' }
    const op = opMap[actionKey]
    if (!op) return true

    const code = getActionCodeByOp(op)
    if (!code) return !cleanListCode.value
    return canByAnyCode(code)
  }

  const handleCommand = (type, row) => {
    emit('command', { type, row })
  }

  const shouldShow = (action, row) => {
    if (typeof action.show === 'function') {
      return action.show(row)
    }

    if (action.show === false) return false

    const permissionCode = action.permissionCode || action.permission
    if (typeof permissionCode === 'string' || Array.isArray(permissionCode)) {
      return permissionStore.hasPermission(permissionCode)
    }

    const op = action.permissionOp || action.op
    if (op) {
      const code = getActionCodeByOp(op)
      if (!code) return !cleanListCode.value
      return canByAnyCode(code)
    }

    return true
  }
</script>

<style scoped>
  .table-action {
    display: flex;
    gap: 10px; /* Reduced gap from 8px */
    flex-wrap: wrap;
    align-items: center; /* Ensure vertical alignment */
    justify-content: center;
  }

  /* Reset Element Plus button margins to rely on flex gap */
  .table-action :deep(.el-button + .el-button) {
    margin-left: 0;
  }
  .table-action :deep(.el-button) {
    margin-left: 0;
    margin-right: 0;
  }

  .action-btn-wrapper {
    display: inline-flex; /* Use inline-flex to play nice with parent flex */
  }
</style>
