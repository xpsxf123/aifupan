<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :width="520"
      title="设置行业的模型"
  >
    <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="120px"
    >
      <el-form-item label="默认通用模型" prop="defaultGeneralModelId">
        <el-select
            v-model="dataForm.defaultGeneralModelId"
            clearable
            placeholder="请选择"
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
              v-if="dataForm.tradeModelId == 0"
              size="small"
              type="primary"
              @click="tradeModelAdd"
          >{{ !tradeModelVisible ? '添加' : '关闭' }}
          </el-button>
          <el-button
              v-if="dataForm.tradeModelId != 0"
              size="small"
              type="danger"
              @click="deleteTradeModel(dataForm.id, dataForm.tradeModelId)"
          >删除
          </el-button>
        </div>
      </el-form-item>
      <div v-if="tradeModelVisible">
        <el-form-item label="模型名称" prop="modelName">
          <el-input
              v-model="dataForm.modelName"
              placeholder="行业模型名称"
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
          >
            <template #suffix>%</template>
          </el-input>
        </el-form-item>
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
import api from '@/utils/request-api.js'

const visible = ref(false)
const dataFormRef = ref()
const tradeModelVisible = ref(false)
const dataModelList = ref([])
const cruxTypeList = ref([])

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
  tradeModelId: 0,
  cruxTypeScaleBos: []
})

const dataRule = reactive({
  modelName: [
    {required: true, message: '行业模型名称不能为空', trigger: 'blur'}
  ],
  defaultGeneralModelId: [
    {required: true, message: '默认通用模型不能为空', trigger: 'blur'}
  ]
})

const emit = defineEmits(['refreshDataList'])

const init = async (id, tradeModelId) => {
  dataForm.id = id || 0
  dataForm.tradeModelId = tradeModelId || 0
  tradeModelVisible.value = false
  visible.value = true
  cruxTypeList.value = []
  dataForm.modelName = ''
  getDataModelList()
  if (!dataForm.id || dataForm.tradeModelId == 0) {
    await getShowCompassList()
  }
  nextTick(() => {
    dataFormRef.value?.resetFields()
    if (dataForm.id) {
      api.trade.info({id: dataForm.id}).then((data) => {
        if (data && data.code == 0) {
          Object.assign(dataForm, data.data)

          if (dataForm.defaultGeneralModelId == '0') {
            dataForm.defaultGeneralModelId = ''
          }
          if (dataForm.tradeModelId != 0) {
            tradeModelVisible.value = true
            if (
                dataForm.cruxTypeScaleBos &&
                Array.isArray(dataForm.cruxTypeScaleBos)
            ) {
              dataForm.cruxTypeScaleBos.forEach((item) => {
                item.scale = Math.round(item.scale * 100)
              })
            }
          } else {
            dataForm.cruxTypeScaleBos = cruxTypeList.value || []
          }
        }
      })
    }
  })
}
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

const validateSumNotExceed = (rule, value, callback) => {
  console.log(value)
  if (
      typeof Number(value) != 'number' ||
      isNaN(value) ||
      value < 0 ||
      !Number.isInteger(Number(value)) ||
      value == ''
  ) {
    callback(new Error('输入的数值需要是正整数'))
  } else {
    callback()
  }
}

const tradeModelAdd = () => {
  tradeModelVisible.value = !tradeModelVisible.value
  if (tradeModelVisible.value) {
    dataForm.modelName = ''
    cruxTypeList.value = []
    getShowCompassList()
  }
}
const getDataModelList = () => {
  dataModelList.value = []
  api.dataModel.modelCruxTypeList({type: 0}).then((res) => {
    if (res && res.code == 0) {
      res.data.list.forEach((item) => {
        if (item.type == 0) {
          dataModelList.value.push({
            modelName: item.name,
            modelId: item.id
          })
        }
      })
    }
  })
}

const deleteTradeModel = (id, tradeModelId) => {
  ElMessageBox.confirm('确定要进行删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    api.trade
        .deleteTradeModel({tradeId: id, modelId: tradeModelId})
        .then((res) => {
          if (res && res.code == 0) {
            ElMessage({
              message: res.msg,
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
  })
}
const dataFormSubmit = () => {
  dataFormRef.value?.validate((valid) => {
    if (valid) {
      let sumScale = 0
      if (
          tradeModelVisible.value &&
          dataForm.cruxTypeScaleBos &&
          Array.isArray(dataForm.cruxTypeScaleBos)
      ) {
        dataForm.cruxTypeScaleBos.forEach((item) => {
          sumScale += Number(item.scale)
        })
      }
      const requestDate = JSON.parse(JSON.stringify(dataForm))

      if (sumScale == 100 || !tradeModelVisible.value) {
        if (
            requestDate.cruxTypeScaleBos &&
            Array.isArray(requestDate.cruxTypeScaleBos)
        ) {
          requestDate.cruxTypeScaleBos.forEach((value, index, arr) => {
            arr[index].scale = value.scale / 100
          })
        }
        if (requestDate.id) {
          api.trade.update(requestDate).then((res) => {
            if (res && res.code == 0) {
              ElMessage({
                message: res.msg,
                type: 'success'
              })
              visible.value = false
              emit('refreshDataList')
            }
          })
        } else {
          requestDate.id = ''
          api.trade.save(requestDate).then((res) => {
            if (res && res.code == 0) {
              ElMessage({
                message: res.msg,
                type: 'success',
                duration: 1500,
                onClose: () => {
                  visible.value = false
                  emit('refreshDataList')
                }
              })
            }
          })
        }
      } else {
        ElMessage.warning('数值不符合要求,数值占比相加需要等于100')
      }
    }
  })
}

defineExpose({
  init
})
</script>
