<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增角色' : '编辑角色'"
      class="role-dialog"
      width="700px"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-position="left"
        label-width="100px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="角色名称" prop="name">
            <el-input
                v-model="dataForm.name"
                clearable
                placeholder="请输入角色名称"
            ></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="角色级别" prop="level">
            <el-select
                v-model="dataForm.level"
                clearable
                placeholder="请选择级别"
                style="width: 100%"
            >
              <el-option
                  v-for="item in ['A', 'B', 'C', 'D', 'E']"
                  :key="item"
                  :label="item + ' 级'"
                  :value="item"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider></el-divider>

      <el-form-item class="role-config" label="权限配置">
        <div class="tree-actions">
          <el-button size="small" type="primary" @click="handleCheckAll">
            <el-icon>
              <Check/>
            </el-icon>
            全选
          </el-button>
          <el-button size="small" type="info" @click="resetChecked">
            <el-icon>
              <Close/>
            </el-icon>
            清空
          </el-button>
        </div>
        <el-tree
            ref="menuTreeDomRef"
            :data="selfMenuTreeList"
            :props="defaultProps"
            :render-after-expand="false"
            accordion
            check-strictly
            class="permission-tree"
            highlight-current
            node-key="id"
            show-checkbox
            style="user-select: none"
            @check="checkMenu"
        >
          <template #default="{ data }">
            <span class="custom-tree-node">
              <span>{{ data.name }}</span>
              <span>{{ menuType[data.type] }}</span>
            </span>
          </template>
        </el-tree>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'
import api from '@/utils/request-api'

const visible = ref(false)
const dataFormRef = ref(null)
const menuTreeDomRef = ref(null)

const dataForm = reactive({
  id: 0,
  name: '',
  level: '',
  roleIdList: []
})

const defaultProps = reactive({
  label: 'name'
})

const selfMenuTreeList = ref([])
const checkedMenuIdList = ref([])
const menuList = ref([])

const dataRule = reactive({
  name: [{required: true, message: '角色名不能为空', trigger: 'blur'}]
})

const menuType = reactive({
  2: '📁 目录',
  1: '📌 功能',
  0: '📜 菜单'
})

const emit = defineEmits(['refreshDataList'])

const init = (id) => {
  dataForm.id = id || 0
  selfMenuTreeList.value = []
  checkedMenuIdList.value = []
  visible.value = true
  nextTick(() => {
    dataFormRef.value.resetFields()
    menuTreeDomRef.value.setCheckedKeys(checkedMenuIdList.value)
    getListTreeSelf()
  })
}

const checkMenu = (checkedNode, {checkedKeys}) => {
  const nodeId = checkedNode.id
  const isChecked = checkedKeys.includes(nodeId)

  if (isChecked) {
    const parentMenuId = findAncestorIds(selfMenuTreeList.value, nodeId)
    handleCheckChange(parentMenuId, true)
    const childMenuId = findAllChildIds(selfMenuTreeList.value, nodeId)
    handleCheckChange(childMenuId, true)
  } else {
    const childMenuId = findAllChildIds(selfMenuTreeList.value, nodeId)
    handleCheckChange(childMenuId, false)
  }
  nextTick(() => {
    checkedMenuIdList.value = menuTreeDomRef.value.getCheckedKeys()
  })
}

const getRoleInfo = () => {
  api.role.info({id: dataForm.id}).then((res) => {
    if (res && res.code === 0) {
      Object.assign(dataForm, res.data)
      if (res.data && res.data.menuList && res.data.menuList.length > 0) {
        checkedMenuIdList.value = res.data.menuList.map((item) => item.id)
        menuTreeDomRef.value.setCheckedKeys(checkedMenuIdList.value)
      }
    }
  })
}

const getListTreeSelf = () => {
  api.menu.listTreeSelf({}, {showLoading: true}).then((res) => {
    if (res && res.code === 0) {
      selfMenuTreeList.value = res.data.menuTreeList
      console.log('this.selfMenuTreeList', selfMenuTreeList.value)
      menuList.value = res.data.menuList
      if (dataForm.id) {
        getRoleInfo()
      }
    }
  })
}

const handleCheckAll = () => {
  const allMenuIds = menuList.value.map((item) => item.id)
  menuTreeDomRef.value.setCheckedKeys(allMenuIds)
  checkedMenuIdList.value = allMenuIds
}

const resetChecked = () => {
  menuTreeDomRef.value.setCheckedKeys([])
  checkedMenuIdList.value = []
}

const findAncestorIds = (treeData, targetId) => {
  const ancestorIds = []

  function findParent(nodeId, nodes, path) {
    for (const node of nodes) {
      if (node.id === nodeId) {
        return true
      }

      if (node.children && node.children.length > 0) {
        if (findParent(nodeId, node.children, path)) {
          path.push(node.id)
          return true
        }
      }
    }
    return false
  }

  findParent(targetId, treeData, ancestorIds)
  return ancestorIds.reverse()
}

const findAllChildIds = (treeData, targetId) => {
  const childIds = []

  function findNodeAndCollectChildren(nodes) {
    for (const node of nodes) {
      if (node.id === targetId) {
        collectChildren(node, childIds)
        return true
      }

      if (node.metaList && node.metaList.some((meta) => meta.id === targetId)) {
        return true
      }

      if (node.children && node.children.length > 0) {
        if (findNodeAndCollectChildren(node.children)) {
          return true
        }
      }
    }
    return false
  }

  function collectChildren(node, ids) {
    if (node.children && node.children.length > 0) {
      node.children.forEach((child) => {
        ids.push(child.id)
        collectChildren(child, ids)
      })
    }

    if (node.metaList && node.metaList.length > 0) {
      node.metaList.forEach((meta) => {
        ids.push(meta.id)
      })
    }
  }

  findNodeAndCollectChildren(treeData)
  return childIds
}

const handleCheckChange = (menuIds, checked) => {
  if (menuIds.length && menuIds.length > 0) {
    menuIds.forEach((item) => {
      menuTreeDomRef.value.setChecked(item, checked)
    })
  }
}

const dataFormSubmit = () => {
  dataFormRef.value.validate((valid) => {
    if (valid) {
      let requestDate = JSON.parse(JSON.stringify(dataForm))
      requestDate.roleIdList = checkedMenuIdList.value

      if (dataForm.id) {
        api.role.update(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          } else {
            ElMessage.error(res.msg)
          }
        })
      } else {
        requestDate.id = ''
        api.role.save(requestDate).then((res) => {
          if (res && res.code === 0) {
            visible.value = false
            emit('refreshDataList')
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          } else {
            ElMessage.error(res.msg)
          }
        })
      }
    }
  })
}

defineExpose({
  init
})
</script>

<style lang="scss" scoped>
.role-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 25px;
  }

  .el-divider {
    margin: 15px 0;
  }

  .tree-actions {
    margin-bottom: 10px;
  }

  .permission-tree {
    max-height: 500px;
    overflow-y: auto;
    padding: 10px;
    border: 1px solid #ebeef5;
    border-radius: 4px;

    .custom-tree-node {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 14px;
      padding-right: 8px;

      .tree-icon {
        color: #409eff;
        margin-left: 8px;
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    padding: 10px 20px 0;
    border-top: 1px solid #e8e8e8;
  }
}

.custom-tree-node {
  font-size: 17px !important;
}

:deep(.role-config .el-form-item__content) {
  flex-direction: column;
  align-items: stretch;
}
</style>
