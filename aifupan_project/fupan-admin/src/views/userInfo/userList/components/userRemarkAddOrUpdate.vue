<template>
  <div>
    <el-dialog
        v-model="visible"
        :title="dialogTitle"
        :width="550"
        append-to-body
        custom-class="my-dialog-class"
    >
      <div>
        <el-form
            ref="dataFormRef"
            :model="dataForm"
            :rules="dataRule"
            label-width="100px"
        >
          <el-form-item label="跟进时间" prop="followUpTime">
            <el-date-picker
                v-model="dataForm.followUpTime"
                placeholder="选择日期"
                style="width: 100%"
                type="date"
                value-format="YYYY-MM-DD HH:mm:ss"
            >
            </el-date-picker>
          </el-form-item>
          <el-form-item label="跟进类型" prop="followType">
            <el-select
                v-model="dataForm.followType"
                clearable
                placeholder="请选择"
                style="width: 100%"
            >
              <el-option
                  v-for="item in followTypeOptions"
                  :key="`follow-type-${item.value}`"
                  :label="item.label"
                  :value="item.value"
              >
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="下次跟进时间" prop="nextFolTime">
            <el-date-picker
                v-model="dataForm.nextFolTime"
                placeholder="选择日期"
                style="width: 100%"
                type="date"
                value-format="YYYY-MM-DD HH:mm:ss"
            >
            </el-date-picker>
          </el-form-item>
          <el-form-item label="跟进内容" prop="remark">
            <el-input
                v-model="dataForm.remark"
                :autosize="{
                minRows: 8,
              }"
                placeholder="请输入跟进内容"
                style="width: 100%; padding-top: 10px"
                type="textarea"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import dayjs from 'dayjs'
import api from '@/utils/request-api'

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const userId = ref(0)
const dataFormRef = ref()
const followTypeOptions = ref([])

const dataForm = reactive({
  id: null,
  userId: null,
  followUpTime: '',
  remark: '',
  followType: '',
  nextFolTime: ''
})

const validateNextFolTime = (rule, value, callback) => {
  if (value && dataForm.followUpTime) {
    const followUpTime = dayjs(dataForm.followUpTime)
    const nextFolTime = dayjs(value)

    if (nextFolTime.isBefore(followUpTime)) {
      callback(new Error('下次跟进时间不能小于跟进时间'))
    } else {
      callback()
    }
  } else {
    callback()
  }
}

const dataRule = {
  remark: [{required: true, message: '备注不能为空', trigger: 'blur'}],
  followType: [
    {required: true, message: '跟进类型不能为空', trigger: 'change'}
  ],
  nextFolTime: [
    {validator: validateNextFolTime, trigger: 'change'}
  ]
}

const dialogTitle = computed(() => {
  return dataForm.id ? '修改跟进内容' : '新增跟进内容'
})
const init = async (userIdParam, remarkId) => {
  dataForm.id = remarkId || 0
  dataForm.userId = userIdParam || 0
  visible.value = true

  await nextTick()
  dataFormRef.value.resetFields()

  if (dataForm.id) {
    console.log('dataForm.id', dataForm)

    const data = await api.userremark.info({id: dataForm.id})
    if (data && data.code === 0) {
      Object.assign(dataForm, data.data)
    }
  } else {
    dataForm.followUpTime = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }
}

const dataFormSubmit = () => {
  dataFormRef.value.validate(async (valid) => {
    if (valid) {
      const requestData = JSON.parse(JSON.stringify(dataForm))
      let res
      if (dataForm.id) {
        res = await api.userremark.update(requestData)
      } else {
        requestData.id = ''
        res = await api.userremark.save(requestData)
      }

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

const getFollowTypeOptions = async () => {
  const {
    code,
    data: {list}
  } = await api.userremark.listFollowType({
    isAll: 0,
    limit: -1,
    page: 1,
    typeLogo: 'user_follow_type'
  })

  if (code === 0 && list.length) {
    list.forEach((item) => {
      item.value = Number(item.value)
    })
    followTypeOptions.value = list
  }
}

onMounted(() => {
  getFollowTypeOptions()
})

defineExpose({
  init
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: end;
}

:deep(.my-dialog-class) {
  min-width: 450px;
}
</style>
