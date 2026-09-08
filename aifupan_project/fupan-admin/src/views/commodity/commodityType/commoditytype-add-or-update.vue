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
      label-width="160px"
    >
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="dataForm.name" placeholder="商品名称"></el-input>
      </el-form-item>
      <el-form-item label="商品类型code" prop="code">
        <el-input v-model="dataForm.code" placeholder="商品类型bean"></el-input>
      </el-form-item>
      <el-form-item label="商品类型单位" prop="unit">
        <el-input v-model="dataForm.unit" placeholder="商品类型单位"></el-input>
      </el-form-item>
      <el-form-item label="重置清零" prop="isReset">
        <el-select
          v-model="dataForm.isReset"
          placeholder="是否可以重置清零"
          style="width: 100%"
        >
          <el-option
            v-for="item in [
              { id: 0, name: '否' },
              { id: 1, name: '是' },
            ]"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item
        label="重置时间"
        prop="resetNum"
        v-if="dataForm.isReset == 1"
      >
        <el-input v-model="dataForm.resetNum" placeholder="重置时间"></el-input>
      </el-form-item>
      <el-form-item label="重置时间单位" prop="resetUnit">
        <el-select
          v-model="dataForm.resetUnit"
          placeholder="是否可以重置清零"
          style="width: 100%"
        >
          <el-option
            v-for="item in orderDict.timeUnit"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="子账号是否拥有" prop="subAccountHave">
        <el-select
          v-model="dataForm.subAccountHave"
          placeholder="当前类型子账号是否拥有"
          style="width: 100%"
        >
          <el-option
            v-for="item in [
              { id: 0, name: '否' },
              { id: 1, name: '是' },
            ]"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          >
          </el-option>
        </el-select>
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
import { useDict } from '@/hooks/useDict.js'
import api from '@/utils/request-api'

const emit = defineEmits(['refreshDataList'])

const { orderDict } = useDict()

const visible = ref(false)
const dataFormRef = ref()

const dataForm = reactive({
  id: null,
  name: null,
  code: null,
  unit: null,
  isReset: null,
  subAccountHave: null,
  resetNum: null,
  resetUnit: null,
})

const dataRule = reactive({
  name: [{ required: true, message: '商品名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '商品类型key不能为空', trigger: 'blur' }],
  unit: [{ required: true, message: '商品类型单位不能为空', trigger: 'blur' }],
})

const init = async (id) => {
  dataForm.id = id || 0
  visible.value = true
  await nextTick(() => {
    dataFormRef.value.resetFields()
    if (dataForm.id) {
      getInfo()
    }
  })
}

const getInfo = async () => {
  const res = await api.commoditytype.info(
    { id: dataForm.id },
    { showLoading: true }
  )
  if (res && res.code === 0) {
    Object.assign(dataForm, res.data)
  }
}

const dataFormSubmit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      let requestData = JSON.parse(JSON.stringify(dataForm))
      if (requestData.isReset != 1) {
        requestData.timeUnit = -1
        requestData.resetNum = -1
      }

      let res
      if (dataForm.id) {
        res = await api.commoditytype.update(requestData)
      } else {
        requestData.id = ''
        res = await api.commoditytype.save(requestData)
      }

      if (res && res.code === 0) {
        ElMessage.success(res.msg)
        visible.value = false
        emit('refreshDataList')
      } else {
        ElMessage.error(res.msg)
      }
    }
  })
}

defineExpose({
  init,
})
</script>
