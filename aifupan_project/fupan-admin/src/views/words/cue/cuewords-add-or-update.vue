<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      class="cuewords-dialog"
  >
    <el-form
        :key="reRender"
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="100px"
    >
      <el-form-item label="提示词概要" prop="outline">
        <el-input v-model="dataForm.outline" placeholder="请输入提示词"></el-input>
      </el-form-item>
      <el-form-item label="提示词" prop="cueWord">
        <el-input
            v-model="dataForm.cueWord"
            placeholder="请输入提示词"
        ></el-input>
      </el-form-item>

      <el-form-item label="实际提示词" prop="problem">
        <span>行业：#{trade} &nbsp;&nbsp; 违规原因：#{reason} &nbsp;&nbsp; 平台：#{platform}</span>
        <el-input
            v-model="dataForm.problem"
            :rows="5"
            placeholder="实际提示词"
            type="textarea"
        ></el-input>
      </el-form-item>

      <el-form-item label="行业" prop="tradeId">
        <el-cascader
            :key="cascaderNum"
            v-model="dataForm.tradeId"
            :options="tradeTreeList"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            clearable
            filterable
            placeholder="请选择"
            style="width: 100%"
            @change="tradeChange"
        >
        </el-cascader>
      </el-form-item>

      <el-form-item label="提示词类型" prop="cueType">
        <el-radio-group v-model="dataForm.cueType" @change="changeCueType">
          <el-radio
              v-for="item in wordDict.cueType"
              :key="item.value"
              :label="item.value"
          >{{ item.label }}
          </el-radio
          >
        </el-radio-group>
      </el-form-item>

      <el-form-item label="提示词场景" prop="applyTo">
        <template v-for="item in wordDict.applyTo" :key="item.value">
          <el-radio-group
              v-if="applyToHas(item.value)"
              v-model="dataForm.applyTo"
          >
            <el-radio :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </template>
      </el-form-item>

      <el-form-item label="提示词范围" prop="scope">
        <template v-for="item in wordDict.cueScope" :key="item.value">
          <el-radio-group
              v-if="cueScopeHas(item.value)"
              v-model="dataForm.scope"
          >
            <el-radio :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </template>
      </el-form-item>

      <el-form-item label="提示词方式" prop="scene">
        <template v-for="item in wordDict.cueScene" :key="item.value">
          <el-radio-group
              v-if="cueSceneHas(item.value)"
              v-model="dataForm.scene"
          >
            <el-radio :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </template>
      </el-form-item>

      <el-form-item label="排序" prop="sort">
        <el-input
            v-model="dataForm.sort"
            placeholder="排序"
            type="number"
        ></el-input>
      </el-form-item>

      <el-form-item label="描述" prop="remarks">
        <el-input
            v-model="dataForm.remarks"
            placeholder="请输入描述"
        ></el-input>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
      </span>
    </template>

    <rule-list
        v-if="ruleListVisible"
        ref="ruleListDialogRef"
        @reRender="reRender = !reRender"
    ></rule-list>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { useDict } from '@/hooks/useDict'
import api from '@/utils/request-api'
import RuleList from '../component/ruleList.vue'

const {wordDict} = useDict()

const reRender = ref(false)
const ruleListVisible = ref(false)
const visible = ref(false)
const tradeTreeList = ref([])
const oldCueWord = ref('')
const cascaderNum = ref(0)

const dataFormRef = ref(null)
const tradeList = ref([])
const ruleListDialogRef = ref(null)

const dataForm = reactive({
  id: 0,
  outline: '',
  scope: 0,
  applyTo: 0,
  scene: 0,
  cueType: 0,
  cueWord: '',
  problem: '',
  tradeId: '',
  sort: 0,
  createDate: '',
  updateDate: '',
  isDeleted: '',
  remarks: ''
})

const dataRule = reactive({
  outline: [{required: true, message: '提示词概要不能为空', trigger: 'blur'}],
  cueWord: [{required: true, message: '提示词不能为空', trigger: 'blur'}],
  problem: [{required: true, message: '实际提示词不能为空', trigger: 'blur'}],
  applyTo: [{required: true, message: '提示词场景不能为空', trigger: 'blur'}],
  tradeId: [{required: true, message: '行业不能为空', trigger: 'blur'}],
  cueType: [{required: true, message: '类型不能为空', trigger: 'blur'}],
  sort: [{required: true, message: '排序不能为空', trigger: 'blur'}]
})

const applyToHas = computed(() => {
  return (value) => {
    if (value === 0) {
      return true
    } else if (value === 1) {
      return [0, 3, 4].includes(dataForm.cueType)
    } else {
      return false
    }
  }
})

const cueScopeHas = computed(() => {
  return (value) => {
    if (value === 0) {
      return true
    } else if (value === 1) {
      return [0, 1, 2, 3, 4].includes(dataForm.cueType)
    } else {
      return false
    }
  }
})

const cueSceneHas = computed(() => {
  return (value) => {
    if (value === 0) {
      return true
    } else if (value === 1) {
      return [1].includes(dataForm.cueType)
    } else {
      return false
    }
  }
})
const changeCueType = () => {
  dataForm.applyTo = 0
  dataForm.scene = 0
  dataForm.scope = 0
}

const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataForm.tradeId = value[value.length - 1]
  } else {
    dataForm.tradeId = ''
  }
}

const getTradeTreeList = async () => {
  tradeTreeList.value = []
  try {
    const res = await api.trade.listTree({})
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  } catch (error) {
    console.error('获取行业列表失败:', error)
  }
}

const init = async (id, tradeIdArr) => {
  await getTradeTreeList()
  dataForm.id = id || 0
  dataForm.tradeIdArr = []
  visible.value = true

  await nextTick(() => {
    dataFormRef.value?.resetFields()
    dataForm.tradeIdArr = tradeIdArr
    if (tradeIdArr && tradeIdArr.length > 0) {
      dataForm.tradeId = tradeIdArr[tradeIdArr.length - 1]
    }
    if (dataForm.id) {
      api.cuewords.info({id: dataForm.id}).then((res) => {
        if (res && res.code === 0) {
          Object.assign(dataForm, res.data)
          if (dataForm.tradeIdArr) {
            dataForm.tradeIdArr = JSON.parse(dataForm.tradeIdArr)
          }
        }
      })
    }
  })
}

const emit = defineEmits(['refreshDataList'])

const dataFormSubmit = () => {
  dataFormRef.value?.validate(async (valid) => {
    if (valid) {
      let requestDate = JSON.parse(JSON.stringify(dataForm))
      requestDate.tradeIdArr = JSON.stringify(requestDate.tradeIdArr)
      if (dataForm.id) {
        requestDate.oldCueWord = oldCueWord.value
        const res = await api.cuewords.update(requestDate)
        if (res && res.code === 0) {
          if (res.msg && res.msg.length < 5) {
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          } else {
            ElMessage({
              message: res.msg,
              type: 'success'
            })
          }
          visible.value = false
          emit('refreshDataList')
        }
      } else {
        requestDate.id = ''
        const res = await api.cuewords.save(requestDate)
        if (res && res.code === 0) {
          emit('refreshDataList')
          if (res.data && res.data.length > 0) {
            ElMessage({
              message: JSON.stringify(res.data),
              type: 'success'
            })
          } else {
            ElMessage({
              message: '添加成功',
              type: 'success'
            })
          }
          visible.value = false
        }
      }
    }
  })
}

defineExpose({
  init
})
</script>

<style lang="scss" scoped>
.similarWordTd3 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTd2 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTd1 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  box-sizing: border-box;
  border: 0.5px solid #ccc;
}

.similarWordTrContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.el-radio {
  margin-right: 10px;
}

.similarWordTitleItem3 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
}

.similarWordTitleItem2 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1.5;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
}

.similarWordTitleItem1 {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
}

.similarWordTitleContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

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

<style>
.cuewords-dialog {
  min-width: 550px;
}
</style>
