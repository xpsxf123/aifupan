<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    v-model="visible"
    :width="550"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataFormRef"
      label-width="140px"
    >
      <!-- <el-form-item label="所属父分类" prop="parentId">
                <el-cascader v-model="parentIdArr" style="width: 100%;" :options="cruxTypeTreeList"
                    :props="{ checkStrictly: true, value: 'id', label: 'name' }"
                    @change="parentCruxTypeChange"></el-cascader>
            </el-form-item> -->
      <el-form-item label="分类名称" prop="name">
        <el-input
          v-model="dataForm.name"
          placeholder="关键词分类名称"
        ></el-input>
      </el-form-item>
      <el-form-item label="分类描述" prop="remarks">
        <el-input v-model="dataForm.remarks" placeholder="分类描述"></el-input>
      </el-form-item>
      <el-form-item label="分类排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="分类排序"></el-input>
      </el-form-item>
      <el-form-item label="tab排序" prop="tabSort" v-if="dataForm.level == 2">
        <el-input v-model="dataForm.tabSort" placeholder="tab排序"></el-input>
      </el-form-item>
      <el-form-item
        label="罗盘展示"
        prop="isShowCompass"
        v-if="dataForm.level == 2"
      >
        <el-radio-group v-model="dataForm.isShowCompass">
          <el-radio :label="1">展示</el-radio>
          <el-radio :label="0">不展示</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="统计到关键词总数" prop="isCount">
        <el-radio-group v-model="dataForm.isCount">
          <el-radio :label="1">统计</el-radio>
          <el-radio :label="0">不统计</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import api from '@/utils/request-api'

const visible = ref(false)
const cruxTypeTreeList = ref([])
const cruxTypeList = ref([])
const parentIdArr = ref([])
const dataFormRef = ref(null)

const dataForm = reactive({
  id: 0,
  name: '',
  level: 1,
  sort: '',
  parentId: '',
  parentIdArr: [],
  isShowCompass: 0,
  isCount: 1,
  tabSort: '',
  remarks: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
})

const dataRule = reactive({
  name: [
    { required: true, message: '关键词分类名称不能为空', trigger: 'blur' },
  ],
  parentId: [{ required: true, message: '父分类不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  tabSort: [{ required: true, message: 'tab排序不能为空', trigger: 'blur' }],
  isShowCompass: [
    { required: true, message: '是否罗盘展示不能为空', trigger: 'blur' },
  ],
  isCount: [{ required: true, message: '是否统计不能为空', trigger: 'blur' }],
})

const emit = defineEmits(['refreshDataList'])

function parentCruxTypeChange(valueArr) {
  dataForm.parentId = valueArr[valueArr.length - 1]
  cruxTypeList.value.forEach((item) => {
    if (item.id == dataForm.parentId) {
      dataForm.level = item.level + 1
    }
  })
}

async function getCruxTypeTreeList() {
  cruxTypeTreeList.value = [
    {
      id: '0',
      name: '设置为一级分类',
      level: 0,
    },
  ]
  const res = await api.cruxtype.listTree({ childrenNotNull: 0 })
  if (res && res.code === 0 && res.data) {
    res.data.forEach((element) => {
      cruxTypeTreeList.value.push(element)
    })
    flattenTree()
  }
}

function flattenTree() {
  cruxTypeList.value = []
  cruxTypeTreeList.value.forEach((node) => {
    flattenNode(node)
  })
}

function flattenNode(node) {
  cruxTypeList.value.push(node)
  if (node.children && node.children.length > 0) {
    node.children.forEach((childrenNode) => {
      flattenNode(childrenNode)
    })
  }
}

async function init(id, parentId, parentIdArrIn, level) {
  await getCruxTypeTreeList()
  visible.value = true
  await nextTick()
  dataFormRef.value?.resetFields()

  parentIdArr.value = []
  dataForm.id = id || 0

  if (!dataForm.id) {
    parentIdArrIn.push(parentId + '')
    dataForm.level = level + 1
  } else {
    dataForm.level = level
  }

  parentIdArr.value = parentIdArrIn
  dataForm.parentId = parentId

  if (dataForm.id) {
    const data = await api.cruxtype.info({ id: dataForm.id })
    if (data && data.code === 0) {
      Object.assign(dataForm, data.data)
      if (dataForm.parentId != 0) {
        parentIdArr.value = dataForm.parentIdArr
      } else {
        parentIdArr.value = ['0']
      }
    }
  }
}

function dataFormSubmit() {
  dataFormRef.value?.validate(async (valid) => {
    if (!valid) return
    const requestData = JSON.parse(JSON.stringify(dataForm))
    if (dataForm.id) {
      const res = await api.cruxtype.update(requestData)
      if (res && res.code === 0) {
        emit('refreshDataList')
        ElMessage({
          message: res.msg,
          type: 'success',
        })
        visible.value = false
      }
    } else {
      requestData.id = ''
      const res = await api.cruxtype.save(requestData)
      if (res && res.code === 0) {
        emit('refreshDataList')
        ElMessage({
          message: res.msg,
          type: 'success',
        })
        visible.value = false
      }
    }
  })
}

defineExpose({
  init,
})
</script>
