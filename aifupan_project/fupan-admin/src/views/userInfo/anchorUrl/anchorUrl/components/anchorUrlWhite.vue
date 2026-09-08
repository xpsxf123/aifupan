<template>
  <el-dialog
      v-model="visible"
      :class="{ 'mobile-dialog-innner-custom': isMobile }"
      :close-on-click-modal="false"
      :fullscreen="isMobile"
      :width="800"
      title="白名单"
  >
    <div class="my-div" style="margin: 20px">
      <div class="none-white-user">
        <span class="none-white-user-text"
        >可以从用户列表中选择用户作为当前主播的所属用户</span
        >
        <el-form
            ref="dataFormRef"
            :inline="true"
            :model="dataForm"
            style="margin-top: 20px"
        >
          <el-form-item>
            <el-input
                v-model="dataForm.keyword"
                clearable
                placeholder="输入昵称或账号或手机号搜索"
                style="width: 300px"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button class="submit-button" icon="Search" plain type="primary" @click="search"
            >查询
            </el-button>
            <el-button
                class="submit-button"
                icon="Refresh"
                type="danger"
                @click="clearData"
            >清除查询条件
            </el-button>
          </el-form-item>
        </el-form>

        <el-table
            ref="tableRef"
            :data="tableUserList"
            :element-loading-spinner="customSvg"
            border
            header-row-class-name="my-header-row"
            size="default"
            stripe
            @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45"></el-table-column>
          <el-table-column
              align="center"
              header-align="center"
              label="昵称"
              prop="nickName"
          ></el-table-column>
          <el-table-column
              align="center"
              header-align="center"
              label="账号"
              prop="username"
          ></el-table-column>
          <el-table-column
              :width="130"
              align="center"
              header-align="center"
              label="手机号"
              prop="phone"
          ></el-table-column>
        </el-table>

        <el-pagination
            :current-page="pageIndex"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="totalCount"
            background
            layout="total, sizes, prev, pager,next,->, jumper"
            style="text-align: center; margin-top: 10px"
            @size-change="sizeChangeHandle"
            @current-change="currentChangeHandle"
        >
        </el-pagination>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submitData()">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import api from '@/utils/request-api'
import { customSvg } from '@/utils/icon.js'

const props = defineProps({
  isMobile: {
    type: Boolean,
    default: false
  }
})
const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const searchDataFormVisible = ref(true)

const whiteUserInfoList = ref([])
const removeList = ref([])

const saveAnchorInWhite = reactive({})
const removeAnchorInWhite = reactive({})

const anchorUserVisible = ref(false)
const addAnchorUserVisible = ref(true)

const selectedAddRows = ref([])

const dataForm = reactive({
  keyword: '',
  phone: ''
})

const tableUserList = ref([])
const pageIndex = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

const tableRef = ref(null)
const dataFormRef = ref(null)
const secUid = ref(0)

const init = (id) => {
  visible.value = true
  Object.assign(dataForm, {keyword: '', phone: ''})
  getUserList()
  secUid.value = id
  saveAnchorInWhite.secUid = id
  removeAnchorInWhite.secUid = id
  anchorUserVisible.value = false
  addAnchorUserVisible.value = true
}

const getUserList = () => {
  dataForm.page = pageIndex.value
  dataForm.limit = pageSize.value
  api.user.pageList(dataForm, {showLoading: true}).then((res) => {
    if (res && res.code === 0) {
      tableUserList.value = res.data.list
      anchorInUserList()
      totalCount.value = res.data.totalCount
    }
  })
}

const anchorInUserList = () => {
  api.anchorurl
      .selectUserByAnchorWhite({secUid: secUid.value})
      .then((res) => {
        if (res && res.code === 0) {
          whiteUserInfoList.value = res.data

          tableUserList.value.forEach((obj1) => {
            const match = whiteUserInfoList.value.find(
                (obj2) => obj2.id === obj1.id
            )
            if (match) {
              obj1.isSelected = true
            } else {
              obj1.isSelected = false
            }
          })
          initializeSelection()
        }
      })
}

const initializeSelection = () => {
  tableUserList.value.forEach((row) => {
    if (row.isSelected) {
      nextTick(() => {
        tableRef.value.toggleRowSelection(row, row.isSelected)
      })
    }
  })
}

const submitData = () => {
  const idList = selectedAddRows.value.map((row) => row.id)
  saveAnchorInWhite.userId = idList

  if (whiteUserInfoList.value != '') {
    const whiteArray = Object.keys(whiteUserInfoList.value[0])
    const convertedArray = selectedAddRows.value.map((objB) => {
      const newObj = {}
      whiteArray.forEach((property) => {
        if (objB.hasOwnProperty(property)) {
          newObj[property] = objB[property]
        }
      })
      return newObj
    })
    const map = new Map()
    convertedArray.forEach((obj) => {
      const key = JSON.stringify(obj)
      map.set(key, obj)
    })
    removeList.value = whiteUserInfoList.value.filter(
        (obj) => !map.has(JSON.stringify(obj))
    )
  }

  if (selectedAddRows.value.length > 0) {
    if (true) {
      api.anchorurl.saveAnchorUrlWhite(saveAnchorInWhite).then((res) => {
        if (res && res.code === 0) {
          ElMessage({
            message: '更改成功',
            type: 'success',
            duration: 1500,
            onClose: () => {
              visible.value = false
              emit('refreshDataList')
            }
          })
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
    if (removeList.value != '') {
      const idList = removeList.value.map((row) => row.id)
      removeAnchorInWhite.userId = idList
      ElMessageBox.confirm('有要去掉的白名单，是否继续', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        api.anchorurl.removeAnchorUrlWhite(removeAnchorInWhite).then((res) => {
          if (res && res.code == 0) {
            ElMessage({
              message: '更改成功',
              type: 'success',
              duration: 1500,
              onClose: () => {
                visible.value = false
                emit('refreshDataList')
              }
            })
          }
        })
      })
    }
  } else {
    visible.value = false
  }
}

const handleSelectionChange = (val) => {
  selectedAddRows.value = val
}

const search = () => {
  if (dataForm.keyword === null) {
    searchDataFormVisible.value = true
  } else {
    searchDataFormVisible.value = false
  }

  pageIndex.value = 1
  getUserList()
}

const sizeChangeHandle = (val) => {
  pageSize.value = val
  pageIndex.value = 1
  getUserList()
}

const currentChangeHandle = (val) => {
  pageIndex.value = val
  if (searchDataFormVisible.value) {
    Object.assign(dataForm, {keyword: '', phone: ''})
  }
  getUserList()
}

const clearData = () => {
  Object.assign(dataForm, {keyword: '', phone: ''})
  pageIndex.value = 1
  searchDataFormVisible.value = true
  getUserList()
}

defineExpose({
  init
})
</script>

<style scoped>
:deep(.el-dialog__body) {
  padding: 0px;
}

.none-white-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.none-white-user-text {
  font-size: 16px;
  color: #6f9f9f;
  font-weight: bold;
}

.have-white-user {
  color: #5c6f86;
  font-size: 20px;
  font-weight: bold;
}

.have-white-user-info {
  margin: 20px;
}

.have-white-user-info-text {
  margin-bottom: 10px;
  color: #5c6f86;
  font-size: 18px;
  font-weight: bold;
}

.anchor-in-user {
  font-size: 20px;
  font-weight: bold;
}
</style>
