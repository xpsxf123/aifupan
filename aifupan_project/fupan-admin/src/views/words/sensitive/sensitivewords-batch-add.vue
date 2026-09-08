<template>
  <el-dialog
    title="批量添加"
    :close-on-click-modal="false"
    v-model="visible"
    :width="600"
  >
    <el-form
      :model="dataFormData"
      :rules="dataRule"
      ref="dataForm"
      label-width="100px"
    >
      <el-form-item label="敏感词类型" prop="type">
        <el-select v-model="dataFormData.type" placeholder="请选择">
          <el-option
            v-for="item in typeList"
            :key="item.id"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <!-- <el-form-item label="平台类型" prop="platformTypeList">
                <el-checkbox-group v-model="dataForm.platformTypeList">
                    <el-checkbox v-for="item in platformList" :key="item.id" :label="item.value">{{ item.label
                        }}</el-checkbox>
                </el-checkbox-group>
            </el-form-item> -->
      <el-form-item label="平台类型" prop="platformType">
        <el-select v-model="dataFormData.platformType" placeholder="请选择">
          <el-option
            v-for="item in platformList"
            :key="item.id"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="行业" prop="tradeId">
        <el-cascader
          v-model="dataFormData.tradeIdArr"
          :options="tradeTreeList"
          :key="cascaderNum"
          :props="{ checkStrictly: true, value: 'id', label: 'name' }"
          clearable
          filterable
          @change="tradeChange"
          placeholder="请选择"
          style="width: 100%"
        >
        </el-cascader>
      </el-form-item>
      <el-form-item label="敏感词等级" prop="level">
        <el-select v-model="dataFormData.level" placeholder="请选择">
          <el-option
            v-for="item in LevelList"
            :key="item.id"
            :label="item.label"
            :value="item.value"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="备注" prop="groupStr">
        <el-input
          v-model="dataFormData.groupStr"
          placeholder="请输入备注"
        ></el-input>
      </el-form-item>
      <el-form-item label="描述" prop="remarks">
        <el-input
          v-model="dataFormData.remarks"
          placeholder="请输入描述"
        ></el-input>
      </el-form-item>
      <el-form-item label="敏感词" prop="words">
        <el-input
          type="textarea"
          :rows="16"
          v-model="dataFormData.words"
          placeholder="每行一个组，如：词1_词2_词3，第一个是敏感词，后面的是相似词"
        ></el-input>
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

const emit = defineEmits(['refreshDataList'])

const visible = ref(false)
const tradeTreeList = ref([]) // 行业列表
const platformList = ref([]) // 平台列表
const typeList = ref([]) // 类型列表
const LevelList = ref([]) // 等级列表
const cascaderNum = ref(0)
const dataForm = ref(null)

const dataFormData = reactive({
  id: 0,
  resourceType: 0,
  type: '',
  platformType: 1,
  tradeId: '',
  level: '',
  words: '',
  tradeIdArr: [],
  platformTypeList: [],
  groupStr: '',
  wordsType: 0,
  remarks: '',
})

const dataRule = reactive({
  type: [{ required: true, message: '敏感词类型不能为空', trigger: 'blur' }],
  platformType: [
    { required: true, message: '平台类型不能为空', trigger: 'blur' },
  ],
  platformTypeList: [
    { required: true, message: '平台类型不能为空', trigger: 'blur' },
  ],
  words: [{ required: true, message: '内容不能为空', trigger: 'blur' }],
  tradeId: [{ required: true, message: '行业不能为空', trigger: 'blur' }],
  level: [{ required: true, message: '敏感等级不能为空', trigger: 'blur' }],
})
// 选中行业回调
const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataFormData.tradeId = value[value.length - 1]
  } else {
    dataFormData.tradeId = ''
  }
}

// 获取警告等级列表
const getLevelList = () => {
  LevelList.value = []
  api.dictdata
    .list({ limit: -1, typeLogo: 'sensitive_words_level_type' })
    .then((res) => {
      if (res.code == 0) {
        LevelList.value = res.data.list
        LevelList.value.forEach((item) => {
          item.value = parseInt(item.value)
        })
      }
    })
}

// 获取行业列表树形
const getTradeTreeList = () => {
  tradeTreeList.value = []
  api.trade.listTree({}).then((res) => {
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  })
}

// 获取平台列表
const getPlatformList = () => {
  platformList.value = []
  api.dictdata
    .list({ limit: -1, typeLogo: 'words_platform_type' })
    .then((res) => {
      if (res.code == 0) {
        platformList.value = res.data.list
        platformList.value.forEach((item) => {
          item.value = parseInt(item.value)
        })
      }
    })
}

// 获取敏感词类型列表
const getTypeList = () => {
  typeList.value = []
  api.dictdata
    .list({ limit: -1, typeLogo: 'sensitive_words_type' })
    .then((res) => {
      if (res.code == 0) {
        typeList.value = res.data.list
        typeList.value.forEach((item) => {
          item.value = parseInt(item.value)
        })
      }
    })
}

const init = (tradeIdArr) => {
  getTradeTreeList()
  getTypeList()
  getPlatformList()
  getLevelList()
  dataFormData.platformTypeList = []
  visible.value = true
  nextTick(() => {
    dataForm.value.resetFields()
    dataFormData.tradeIdArr = tradeIdArr
    if (tradeIdArr && tradeIdArr.length > 0) {
      dataFormData.tradeId = tradeIdArr[tradeIdArr.length - 1]
    }
  })
}

const dataFormSubmit = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      let requestData = JSON.parse(JSON.stringify(dataFormData))
      requestData.platformTypeList = [requestData.platformType]

      requestData.words = requestData.words.replaceAll(' ', '')
      if (!requestData.words) {
        ElMessage.error('内容不能为空')
        return
      }
      requestData.wordsList = []
      // 分割行
      let rowArr = requestData.words.split('\n')
      if (!rowArr || rowArr.length < 1) {
        ElMessage.error('内容不能为空')
        return
      }
      rowArr.forEach((item) => {
        // 分割列
        let colArr = item.split('_')
        if (!colArr || colArr.length < 1) {
          ElMessage.error('内容格式异常')
          return
        }

        // 添加敏感词
        let obj = {
          words: colArr[0],
          similarWords: [],
        }

        // 添加相似词
        if (colArr.length > 1) {
          for (let i = 1; i < colArr.length; i++) {
            obj.similarWords.push(colArr[i])
          }
        }

        requestData.wordsList.push(obj)
      })
      requestData.tradeIdArr = JSON.stringify(requestData.tradeIdArr)

      api.sensitivewords.saveBatch(requestData).then((res) => {
        if (res && res.code === 0) {
          if (res.data && res.data.length > 0) {
            ElMessage({
              message: JSON.stringify(res.data),
              type: 'success',
              duration: 60000,
              showClose: true,
            })
          } else {
            ElMessage({
              message: '添加成功',
              type: 'success',
              duration: 1500,
              showClose: true,
            })
          }
          visible.value = false
          emit('refreshDataList')
        }
      })
    }
  })
}

defineExpose({
  init,
})
</script>

<style scoped>
.el-select {
  width: 100%;
}

.el-tag + .el-tag {
  margin-left: 10px;
}

.button-new-tag {
  margin-left: 10px;
  height: 32px;
  line-height: 30px;
  padding-top: 0;
  padding-bottom: 0;
}

.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
}
</style>
