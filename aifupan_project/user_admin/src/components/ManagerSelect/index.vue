<template>
  <div class="manager-select">
    <!-- Trigger Button & Tags -->
    <div class="manager-select-trigger">
      <el-button type="primary" link @click="openDrawer">选择管理员</el-button>
      <div v-if="selectedUsers.length > 0" class="selected-tags mt-10">
        <el-tag
          v-for="user in selectedUsers"
          :key="user.id"
          closable
          type="info"
          @close="removeUser(user.id)"
          class="mr-8 mb-8"
        >
          {{ user.name }}
        </el-tag>
      </div>
    </div>

    <!-- Drawer -->
    <el-drawer
      v-model="drawerVisible"
      size="1096px"
      class="manager-select-drawer"
      :show-close="false"
      header-class="manager-drawer-header"
      body-class="manager-drawer-body"
      destroy-on-close
      @close="handleDrawerClose"
    >
      <template #header>
        <div class="header-title">
          <CloseSvg @click="handleDrawerClose" />
          <div class="form-section__title">选择管理员</div>
        </div>
      </template>
      <div class="drawer-layout">
        <!-- Left: Tree & Search -->
        <div class="left-panel">
          <el-input
            v-model="searchName"
            placeholder="请输入名字"
            clearable
            prefix-icon="Search"
            @input="handleSearch"
            class="mb-16 rounded manager-select__search-input"
          />
          <el-tree
            :data="orgTreeData"
            :props="{ label: 'label', children: 'children' }"
            node-key="key"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          />
        </div>

        <!-- Right: Table & Pagination -->
        <div class="right-panel">
          <el-table
            ref="tableRef"
            :data="employeeList"
            v-loading="loading"
            row-key="id"
            @select="handleSelect"
            @select-all="handleSelectAll"
            :border="false"
            height="100%"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column label="姓名" min-width="120">
              <template #default="{ row }">
                <div class="flex-row flex-ai-center">
                  <div class="avtar-container" style="margin-right: 8px">
                    <el-avatar :size="24" :src="row.userAvatar || defaultImg" />
                  </div>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="companyName" label="分公司" min-width="150" show-overflow-tooltip />
            <el-table-column label="所属组织" min-width="150" show-overflow-tooltip>
              <template #default="{ row }"> {{ row.deptName }}{{ row.teamName ? `-${row.teamName}` : '' }} </template>
            </el-table-column>
            <el-table-column prop="roleName" label="所属角色" min-width="120" show-overflow-tooltip />
            <el-table-column prop="mobile" label="手机" min-width="120" />
          </el-table>
        </div>
      </div>
      <div class="footer-wrapper">
        <el-button type="primary" @click="confirmSelection" round class="custom-btn">确定</el-button>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="limit"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="fetchEmployees"
        />
      </div>
      <!--      <template #footer>
        <div class="flex-row flex-between flex-ai-center">
          <el-button type="primary" @click="confirmSelection" round>确定</el-button>
          <span class="text-color2 font-s12">已选 {{ tempSelection.length }} 人</span>
        </div>
      </template>-->
    </el-drawer>
  </div>
</template>

<script setup>
  /**
   * @file ManagerSelect.vue
   * @description 选择管理员组件
   */
  import { ref, watch, nextTick } from 'vue'
  import apiModule from '@/http/api'
  import defaultImg from '@/assets/images/funnel/defaultAvatar.png'

  const props = defineProps({
    modelValue: {
      type: Array,
      default: () => []
    },
    users: {
      type: Array,
      default: () => []
    }
  })

  const emit = defineEmits(['update:modelValue', 'update:users', 'change', 'validate-manager-user-ids'])

  // State for Tags outside
  const selectedUsers = ref([])

  // Sync from props
  watch(
    () => props.users,
    (val) => {
      selectedUsers.value = [...(val || [])]
    },
    { immediate: true, deep: true }
  )

  const removeUser = (id) => {
    const newUsers = selectedUsers.value.filter((u) => u.id !== id)
    const newIds = newUsers.map((u) => u.id)
    selectedUsers.value = newUsers
    emit('update:modelValue', newIds)
    emit('update:users', newUsers)
    emit('change', newIds, newUsers)
  }

  // Drawer State
  const drawerVisible = ref(false)
  const searchName = ref('')
  const orgTreeData = ref([])
  const currentNode = ref(null)

  // Table State
  const tableRef = ref(null)
  const employeeList = ref([])
  const loading = ref(false)
  const page = ref(1)
  const limit = ref(10)
  const total = ref(0)

  // Temporary selection in Drawer
  const tempSelection = ref([])

  const openDrawer = async () => {
    drawerVisible.value = true
    tempSelection.value = [...selectedUsers.value]
    searchName.value = ''
    currentNode.value = null
    page.value = 1
    await fetchOrgTree()
    await fetchEmployees()

    // Set initial table selection
    nextTick(() => {
      if (tableRef.value) {
        tableRef.value.clearSelection()
        employeeList.value.forEach((row) => {
          if (tempSelection.value.some((u) => u.id === row.id)) {
            tableRef.value.toggleRowSelection(row, true)
          }
        })
      }
    })
  }

  const mapTree = (nodes, level = 0) => {
    if (!nodes) return undefined
    return nodes.map((node) => {
      let type = 'company'
      if (level === 1) type = 'dept'
      if (level === 2) type = 'team'
      return {
        ...node,
        type,
        children: node.children && node.children.length ? mapTree(node.children, level + 1) : undefined
      }
    })
  }

  const fetchOrgTree = async () => {
    try {
      const res = await apiModule.org.tree()
      orgTreeData.value = mapTree(res.data) || []
    } catch (e) {
      console.error(e)
    }
  }

  const handleNodeClick = (data) => {
    currentNode.value = data
    page.value = 1
    fetchEmployees()
  }

  const handleSearch = () => {
    console.log('@@@@@@')

    page.value = 1
    fetchEmployees()
  }

  const fetchEmployees = async () => {
    loading.value = true
    try {
      const query = {
        page: page.value,
        limit: limit.value,
        name: searchName.value || undefined
      }

      if (currentNode.value) {
        const type = currentNode.value.type
        const id = currentNode.value.key
        if (type === 'company') query.companyIds = [id]
        if (type === 'dept') query.deptIds = [id]
        if (type === 'team') query.teamIds = [id]
      }

      const res = await apiModule.employee.list(query)
      employeeList.value = res.data?.list || []
      total.value = res.data?.totalCount || 0

      // Maintain selection state after data loads
      nextTick(() => {
        if (tableRef.value) {
          employeeList.value.forEach((row) => {
            if (tempSelection.value.some((u) => u.id === row.id)) {
              tableRef.value.toggleRowSelection(row, true)
            }
          })
        }
      })
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  const handleSelect = (selection, row) => {
    const isSelected = selection.some((item) => item.id === row.id)
    if (isSelected) {
      if (!tempSelection.value.some((item) => item.id === row.id)) {
        tempSelection.value.push(row)
      }
    } else {
      tempSelection.value = tempSelection.value.filter((item) => item.id !== row.id)
    }
  }

  const handleSelectAll = (selection) => {
    if (selection.length > 0) {
      // All current page rows are selected
      selection.forEach((row) => {
        if (!tempSelection.value.some((item) => item.id === row.id)) {
          tempSelection.value.push(row)
        }
      })
    } else {
      // All current page rows are deselected
      const currentIds = employeeList.value.map((row) => row.id)
      tempSelection.value = tempSelection.value.filter((item) => !currentIds.includes(item.id))
    }
  }

  const confirmSelection = () => {
    selectedUsers.value = [...tempSelection.value]
    const newIds = selectedUsers.value.map((u) => u.id)
    emit('update:modelValue', newIds)
    emit('update:users', selectedUsers.value)
    emit('change', newIds, selectedUsers.value)
    emit('validate-manager-user-ids')
    drawerVisible.value = false
  }

  const handleDrawerClose = () => {
    drawerVisible.value = false
    tempSelection.value = [...selectedUsers.value]
    emit('validate-manager-user-ids')
  }
</script>

<style scoped lang="scss">
  .avtar-container {
    display: flex;
    align-items: center;
  }

  .manager-select {
    padding-left: 20px;
    width: 100%;
  }
  .selected-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .drawer-layout {
    display: flex;
    height: calc(100% - 90px);
    overflow: hidden;
  }
  .left-panel {
    width: 240px;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #f7f7f7;
    overflow-y: hidden;
    padding: 0 24px 30px;
  }
  .left-panel .el-input {
    flex-shrink: 0;
  }
  .left-panel .el-tree {
    flex: 1;
    overflow-y: auto;
  }
  .right-panel {
    padding: 0 30px 0 25px;
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .footer-wrapper {
    display: flex;
    height: 90px;
    align-items: center;
    justify-content: space-between;
    padding: 0 30px 0 45px;
  }
  .custom-btn {
    width: 84px;
    height: 34px;
  }
  .manager-select__search-input {
    --el-input-hover-border-color: #dcdfe6;
    --el-input-focus-border-color: #dcdfe6;
  }
  .manager-select__search-input :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  }
  :deep(.el-drawer__body) {
    padding: 0;
    overflow: hidden;
  }
  /* 设置表头背景色为 #fbfbfb */
  :deep(.el-table th.el-table__cell) {
    background-color: #fbfbfb;
  }
</style>

<style lang="scss">
  .manager-drawer-header {
    height: 48px;
    padding: 0 30px;
    margin-bottom: 0;
    border-bottom: 1px solid #dcdcdc;
    .form-section__title {
      font-size: 16px;
      background-color: transparent;
      font-weight: bold;
      border: none;
      padding: 0;
    }
    .header-title {
      display: flex;
      width: 100%;
      gap: 13px;
    }
  }
  .manager-drawer-body {
    padding-top: 30px !important;
  }
</style>
