<template>
  <el-dialog
      v-model="visible"
      :close-on-click-modal="false"
      :title="!dataForm.id ? '新增' : '修改'"
      :width="600"
  >
    <el-form
        :key="reRender"
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRule"
        label-width="100px"
    >
      <el-form-item label="按钮名称" prop="buttonName">
        <el-input
            v-model="dataForm.buttonName"
            placeholder="请输入按钮名称"
        ></el-input>
      </el-form-item>

      <el-form-item label="实际提示词" prop="problem">
        <el-input
            v-model="dataForm.problem"
            :rows="5"
            placeholder="实际提示词"
            type="textarea"
        ></el-input>
      </el-form-item>

      <el-form-item label="按钮类型" prop="buttonType">
        <el-radio-group v-model="dataForm.buttonType">
          <el-radio :label="1">违规</el-radio>
          <el-radio :label="0">运营</el-radio>
        </el-radio-group>
      </el-form-item>

      <!--            <el-form-item label="提示词范围" prop="scope">-->
      <!--              <el-radio-group v-model="dataForm.scope">-->
      <!--                <el-radio :label="1">段落</el-radio>-->
      <!--                <el-radio :label="0">全文</el-radio>-->
      <!--              </el-radio-group>-->
      <!--            </el-form-item>-->

      <el-form-item label="提示词方式" prop="scene">
        <el-radio-group v-model="dataForm.scene">
          <el-radio v-if="dataForm.buttonType === 1" :label="1"
          >弹框操作
          </el-radio
          >
          <el-radio :label="0">直接提示</el-radio>
        </el-radio-group>
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
        <el-button type="primary" @click="dataFormSubmit">确定</el-button>
      </span>
    </template>

    <rule-list
        v-if="ruleListVisible"
        ref="ruleListDialog"
        @reRender="reRender = !reRender"
    ></rule-list>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import api from '@/utils/request-api'
import RuleList from '../component/ruleList.vue'

const emit = defineEmits(['refreshDataList'])

const reRender = ref(false)
const ruleListVisible = ref(false)
const visible = ref(false)
const tradeTreeList = ref([]) // 行业列表
const oldCueWord = ref('')
const cascaderNum = ref(0)
const dataForm = reactive({
  id: 0,
  scope: 0,
  scene: 0,
  buttonType: 0,
  buttonName: '',
  problem: '',
  tradeId: '',
  sort: 0,
  createDate: '',
  updateDate: '',
  isDeleted: '',
  remarks: ''
})
const dataRule = reactive({
  buttonName: [{required: true, message: '提示词不能为空', trigger: 'blur'}],
  problem: [{required: true, message: '实际提示词不能为空', trigger: 'blur'}],
  tradeId: [{required: true, message: '行业不能为空', trigger: 'blur'}],
  buttonType: [{required: true, message: '类型不能为空', trigger: 'blur'}],
  sort: [{required: true, message: '排序不能为空', trigger: 'blur'}]
})

const dataFormRef = ref(null)
const ruleListDialog = ref(null)

const tradeChange = (value) => {
  if (value && value.length > 0) {
    dataForm.tradeId = value[value.length - 1]
  } else {
    dataForm.tradeId = ''
  }
}

const getTradeTreeList = async () => {
  try {
    tradeTreeList.value = []
    const res = await api.trade.listTree({})
    if (res && res.code === 0) {
      tradeTreeList.value = res.data
      cascaderNum.value++
    }
  } catch (error) {
    console.error('获取行业列表树形失败:', error)
  }
}

const init = async (id, tradeIdArr) => {
  await getTradeTreeList()
  dataForm.id = id || 0
  dataForm.tradeIdArr = []
  visible.value = true
  await nextTick(() => {
    dataFormRef.value.resetFields()
    dataForm.tradeIdArr = tradeIdArr
    if (tradeIdArr && tradeIdArr.length > 0) {
      dataForm.tradeId = tradeIdArr[tradeIdArr.length - 1]
    }
    if (dataForm.id) {
      api.aiCueButton.info({id: dataForm.id}).then((res) => {
        if (res && res.code === 0) {
          Object.assign(dataForm, res.data)
          //行业数据处理
          if (dataForm.tradeIdArr) {
            dataForm.tradeIdArr = JSON.parse(dataForm.tradeIdArr)
          }
        }
      })
    }
  })
}

const dataFormSubmit = async () => {
  const valid = await dataFormRef.value.validate()
  if (valid) {
    let requestDate = JSON.parse(JSON.stringify(dataForm))
    requestDate.tradeIdArr = JSON.stringify(requestDate.tradeIdArr)
    if (dataForm.id) {
      // 修改
      requestDate.oldCueWord = oldCueWord.value
      const res = await api.aiCueButton.update(requestDate)
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
      // 新增
      requestDate.id = ''
      const res = await api.aiCueButton.save(requestDate)
      if (res && res.code === 0) {
        if (res.data && res.data.length > 0) {
          ElMessage({
            message: JSON.stringify(res.data),
            type: 'success',
            duration: 60000,
            showClose: true
          })
        } else {
          ElMessage({
            message: '添加成功',
            type: 'success',
            duration: 1500,
            showClose: true
          })
        }
        visible.value = false
        emit('refreshDataList')
      } else {
        ElMessage.error(res.msg)
      }
    }
  }
}

watch(
    () => dataForm.cueType,
    (newVal) => {
      if (newVal === 0) {
        dataForm.scene = 0
      }
    }
)

defineExpose({
  init
})
</script>

<style scoped>
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
