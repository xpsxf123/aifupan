<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="550"
      @close="handleClose"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="120px"
    >
      <el-form-item label="行业名称" prop="name">
        <el-input v-model="dataForm.name" placeholder="行业名称"></el-input>
      </el-form-item>
      <el-form-item label="行业描述" prop="remarks">
        <el-input v-model="dataForm.remarks" placeholder="行业描述"></el-input>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model="dataForm.sort" placeholder="排序"></el-input>
      </el-form-item>

      <div v-if="!dataForm.id">
        <el-form-item label="默认通用模型" prop="defaultGeneralModelId">
          <el-select
              v-model="dataForm.defaultGeneralModelId"
              clearable
              placeholder="请选择默认通用模型"
              style="width: 100%"
          >
            <el-option
                v-for="item in dataModelList"
                :key="item.modelId"
                :label="item.modelName"
                :value="item.modelId"
            >
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="行业模型">
          <div>
            <el-button
                v-if="dataForm.tradeModelId === 0"
                size="small"
                type="primary"
                @click="tradeModelAdd"
            >{{ !tradeModelVisible ? '添加' : '关闭' }}
            </el-button>
            <el-button
                v-if="dataForm.tradeModelId !== 0"
                size="small"
                type="danger"
                @click="deleteTradeModel(dataForm.id, dataForm.tradeModelId)"
            >删除
            </el-button>
          </div>
        </el-form-item>
        <div v-if="tradeModelVisible || dataForm.tradeModelId !== 0">
          <el-form-item label="模型名称" prop="modelName">
            <el-input
                v-model="dataForm.modelName"
                placeholder="行业模型名称"
                style="width: 250px"
            ></el-input>
          </el-form-item>
          <el-form-item
              v-for="(item, index) in dataForm.cruxTypeScaleBos"
              :key="index"
              :label="item.cruxTypeName"
              :prop="'cruxTypeScaleBos.' + index + '.scale'"
              :rules="[{ validator: validateSumNotExceed, trigger: 'blur' }]"
          >
            <el-input
                v-model="item.scale"
                placeholder="输入关键词占比"
                style="width: 250px"
            >
              <template #suffix>
                %
              </template>
            </el-input>

          </el-form-item>
        </div>
      </div>
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

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const dataFormRef = ref(null)
const dataForm = reactive({
  id: 0,
  name: '',
  createDate: '',
  updateDate: '',
  isDeleted: '',
  remarks: '',
  parentId: 0,
  sort: '',
  defaultGeneralModelId: '',
  modelName: '',
  cruxTypeScaleBos: [],
  tradeModelId: 0
})

const dataRule = {
  name: [{required: true, message: '行业名称不能为空', trigger: 'blur'}],
  sort: [{required: true, message: '排序不能为空', trigger: 'blur'}],
  modelName: [
    {required: true, message: '行业模型名称不能为空', trigger: 'blur'}
  ],
  defaultGeneralModelId: [
    {required: true, message: '默认通用模型不能为空', trigger: 'blur'}
  ]
}

const dataModelList = ref([])
const tradeModelVisible = ref(false)
const cruxTypeList = ref([])
const init = async (id, tradeModelId, parentId) => {
  visible.value = true
  dataForm.id = id || 0
  dataForm.tradeModelId = tradeModelId
  dataForm.parentId = parentId
  tradeModelVisible.value = false
  cruxTypeList.value = []
  dataForm.modelName = ''
  dataForm.defaultGeneralModelId = ''
  if (!dataForm.id || dataForm.tradeModelId === 0) {
    await getShowCompassList()
  }

  nextTick(() => {
    dataFormRef.value.resetFields()
    getDataModelList()
    if (dataForm.id) {
      api.trade.info({id: dataForm.id}).then((data) => {
        if (data && data.code === 0) {
          Object.assign(dataForm, data.data)
          if (dataForm.defaultGeneralModelId === '0') {
            dataForm.defaultGeneralModelId = ''
          }

          if (dataForm.tradeModelId !== 0) {
            if (
                dataForm.cruxTypeScaleBos &&
                Array.isArray(dataForm.cruxTypeScaleBos)
            ) {
              dataForm.cruxTypeScaleBos.forEach((item) => {
                item.scale = Math.round(item.scale * 100)
              })
            }
          } else {
            dataForm.cruxTypeScaleBos = cruxTypeList.value
          }
        }
      })
    }
  })
}
// 获取关键词列表
const getShowCompassList = async () => {
  const res = await api.cruxtype.getShowCompassList()
  nextTick(() => {
    if (res && res.code === 0) {
      res.data.forEach((item) => {
        cruxTypeList.value.push({
          cruxTypeId: item.id,
          cruxTypeName: item.name,
          scale: ''
        })
      })
      dataForm.cruxTypeScaleBos = cruxTypeList.value
    }
  })
}
// 输入行业模型关键词的自定义验证规则
const validateSumNotExceed = (rule, value, callback) => {
  if (
      typeof Number(value) != 'number' ||
      isNaN(value) ||
      value < 0 ||
      !Number.isInteger(Number(value)) ||
      value === ''
  ) {
    callback(new Error('输入的数值需要是正整数'))
  } else {
    callback()
  }
}
// 控制是否显示行业模型输入框
const tradeModelAdd = () => {
  tradeModelVisible.value = !tradeModelVisible.value
  if (tradeModelVisible.value) {
    dataForm.modelName = ''
    cruxTypeList.value = []
    getShowCompassList()
  }
}
// 获取通用模型数据列表
const getDataModelList = () => {
  dataModelList.value = []

  api.dataModel.modelCruxTypeList({type: 0}).then((res) => {
    console.log('ddddddddddddddddd')
    if (res && res.code === 0) {
      res.data.list.forEach((item) => {
        if (item.type === 0) {

          dataModelList.value.push({
            modelName: item.name,
            modelId: item.id
          })
        }
      })
    }
  })
}
// 删除行业模型
const deleteTradeModel = (id, tradeModelId) => {
  ElMessageBox.confirm(`确定要进行删除吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.trade
        .deleteTradeModel({tradeId: id, modelId: tradeModelId})
        .then((res) => {
          if (res && res.code === 0) {
            ElMessage({
              message: res.msg,
              type: 'success'
            })
            visible.value = false
            emit('refreshDataList')
          } else {
            ElMessage.error(res.msg)
          }
        })
  })
}
const dataFormSubmit = () => {
  dataFormRef.value.validate((valid) => {
    if (valid) {
      // 是否有行业模型
      let sumScale = 0
      if (tradeModelVisible.value) {
        if (
            dataForm.cruxTypeScaleBos &&
            Array.isArray(dataForm.cruxTypeScaleBos)
        ) {
          dataForm.cruxTypeScaleBos.forEach((item) => {
            sumScale += Number(item.scale)
          })
        }
      }
      let requestDate = JSON.parse(JSON.stringify(dataForm))

      if (sumScale === 100 || !tradeModelVisible.value) {
        console.log(
            '@@requestDate.cruxTypeScaleBos',
            requestDate.cruxTypeScaleBos
        )
        if (
            requestDate.cruxTypeScaleBos &&
            Array.isArray(requestDate.cruxTypeScaleBos)
        ) {
          requestDate.cruxTypeScaleBos.forEach((value, index, arr) => {
            arr[index].scale = value.scale / 100
          })
        }
        if (requestDate.id) {
          // 修改
          api.trade.update(requestDate).then((res) => {
            if (res && res.code === 0) {
              ElMessage({
                message: res.msg,
                type: 'success'
              })
              visible.value = false
              emit('refreshDataList')
            }
          })
        } else {
          // 新增
          requestDate.id = ''
          api.trade.save(requestDate).then((res) => {
            if (res && res.code === 0) {
              ElMessage({
                message: res.msg,
                type: 'success'
              })
              visible.value = false
              emit('refreshDataList')
            }
          })
        }
      } else {
        ElMessage.warning('数值不符合要求,数值占比相加需要等于100')
      }
    }
  })
}
// 关闭弹窗的回调
const handleClose = () => {
  console.log('hello world')

  dataFormRef.value.resetFields()
}

// 暴露方法给父组件调用
defineExpose({
  init
})
</script>
