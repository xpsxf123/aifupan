<template>
  <div>
    <el-dialog
        v-model="visible"
        :close-on-click-modal="false"
        :title="dataForm.id == 0 ? '添加默认模型' : '修改默认模型'"
        :width="550"
    >
      <el-form
          ref="dataFormRef"
          :model="dataForm"
          :rules="dataRule"
          label-width="100px"
      >
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="dataForm.name" placeholder="模型名称"></el-input>
        </el-form-item>
        <div
            v-if="
            dataForm.cruxTypeScaleBos && dataForm.cruxTypeScaleBos.length > 0
          "
        >
          <el-form-item
              v-for="(item, index) in dataForm.cruxTypeScaleBos"
              :key="item.cruxTypeId || index"
              :label="item.cruxTypeName"
              :prop="'cruxTypeScaleBos.' + index + '.scale'"
              :rules="[{ required: true,validator: validateSumNotExceed, trigger: 'blur' }]"
          >
            <el-input
                v-model="item.scale"
                placeholder="输入关键词占比"
            >
              <template #suffix>
                <span>%</span>
              </template>
            </el-input>
          </el-form-item>
        </div>
      </el-form>
      <div style="display: flex; justify-content: center">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
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
  type: 0,
  tradeId: 0,
  cruxTypeScaleBos: []
})

const dataRule = {
  name: [{required: true, message: '请输入模型名称', trigger: 'blur'}]
}

/**
 * 自定义验证：关键词占比必须为非负整数
 * @param {Object} rule 表单规则
 * @param {string|number} value 当前输入值
 * @param {Function} callback 回调函数
 */
function validateSumNotExceed(rule, value, callback) {
  if (
      typeof Number(value) !== 'number' ||
      isNaN(value) ||
      Number(value) < 0 ||
      !Number.isInteger(Number(value)) ||
      value === ''
  ) {
    callback(new Error('输入的数值需要是正整数'))
  } else {
    callback()
  }
}

/**
 * 初始化弹窗
 * @param {number} id 模型ID，新增时为 0 或 undefined
 */
async function init(id) {
  dataForm.id = id || 0
  visible.value = true
  dataForm.cruxTypeScaleBos = []
  if (!dataForm.id) {
    await getShowCompassList()
  }
  await nextTick()
  dataFormRef.value?.resetFields()
  if (dataForm.id) {
    const res = await api.dataModel.infoDataModel({id: dataForm.id})
    if (res && res.code === 0) {
      const incoming = res.data || {}
      dataForm.name = incoming.name || ''
      dataForm.type = incoming.type ?? 0
      dataForm.tradeId = incoming.tradeId ?? 0
      dataForm.cruxTypeScaleBos = Array.isArray(incoming.cruxTypeScaleBos)
          ? incoming.cruxTypeScaleBos.map((it) => ({
            ...it,
            scale: Math.round((it.scale ?? 0) * 100)
          }))
          : []
    }
  }
}

/**
 * 获取关键词列表（行业/类型）
 */
async function getShowCompassList() {
  const res = await api.cruxtype.getShowCompassList()
  if (res && res.code === 0) {
    const list = res.data || []
    dataForm.cruxTypeScaleBos = list.map((item) => ({
      cruxTypeId: item.id,
      cruxTypeName: item.name,
      scale: ''
    }))
  } else {
    dataForm.cruxTypeScaleBos = []
  }
}

/**
 * 提交表单：校验并保存/更新模型
 */
function dataFormSubmit() {
  dataFormRef.value?.validate(async (valid) => {
    if (!valid) return
    let sumScale = 0
    dataForm.cruxTypeScaleBos.forEach((item) => {
      sumScale += Number(item.scale || 0)
    })

    if (sumScale !== 100) {
      ElMessage.warning('数值不符合要求,数值占比相加需要等于100')
      return
    }

    const requestData = JSON.parse(JSON.stringify(dataForm))
    requestData.cruxTypeScaleBos = requestData.cruxTypeScaleBos.map((it) => ({
      ...it,
      scale: Number(it.scale) / 100
    }))

    if (requestData.id) {
      const res = await api.dataModel.updateDataModel(requestData)
      if (res && res.code === 0) {
        emit('refreshDataList')
        ElMessage({
          message: '修改成功',
          type: 'success'
        })
        visible.value = false
      }
    } else {
      requestData.id = ''
      const res = await api.dataModel.saveDataModel(requestData)
      if (res && res.code === 0) {
        emit('refreshDataList')
        ElMessage({
          message: res.msg,
          type: 'success'
        })
        visible.value = false
      }
    }
  })
}

defineExpose({init})
</script>
