<template>
  <!-- <el-dialog :visible.sync="visible" width="1200px" :close-on-click-modal="false" custom-class="custom-dialog"> -->
  <div>
    <div class="dialog-top">
      <span>AI分析</span>
      <!-- <img src="@/assets/images/close.png" class="close" @click="close()"> -->
      <!-- <img src="@/assets/images/close.png" class="close"> -->
    </div>
    <div v-loading="loading" style="margin: 0px 64px 30px 64px">
      <!-- 回答区 -->
      <div class="conversation">
        <div v-for="(item, index) in anysisContentList" :key="index">
          <div v-if="index % 2 == 0" class="prompt-words-c">
            <span
                v-for="(prompt, promptIndex) in item"
                :key="promptIndex"
                class="prompt-words"
            >{{ prompt }}</span
            >
          </div>
          <div v-if="index % 2 != 0" class="answer">
            <!-- <div v-for="answer in item" style="margin-bottom: 6px;letter-spacing: 1px;">{{ answer }}<br></div> -->
            <!-- <div>{{ markdownText }}</div> -->
            <table border="1" cellpadding="10" cellspacing="0">
              <thead>
              <tr>
                <th v-for="(header, index) in headers(item)" :key="index">
                  {{ header }}
                </th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(row, rowIndex) in rows(item)" :key="rowIndex">
                <td v-for="(cell, cellIndex) in row" :key="cellIndex">
                  {{ cell }}
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- AI模型 -->
      <div class="ai-model">
        <span>请选择需要使用的模型</span>
        <div style="margin-top: 8px; display: flex; align-items: end">
          <div
              v-for="(model, index) in aiModelList"
              :key="model.id || index"
              :class="{
              'questions-prompt': true,
              'questions-tick': buttonIndex === index,
            }"
          >
            <div :class="useAiModelId == model.id ? 'model-check' : ''">
              <div
                  v-if="currentAnalysisStatus === 1"
                  @click="changeModel(model)"
              >
                {{ model.modelName }}
              </div>
            </div>

            <!--                      当前有正在分析的文本-->
            <div
                v-if="currentAnalysisStatus === 0"
                style="cursor: not-allowed"
                title="当前有其他模型正在分析"
            >
              {{ model.modelName }}
            </div>
          </div>
        </div>
      </div>

      <!-- 提问区 -->
      <div>
        <!-- 提示词按钮 -->
        <div style="display: flex; align-items: end">
          <div
              v-if="anysisContentList.length <= 0"
              class="start-questions"
              @click="startQuestions"
          >
            <img src="@/assets/images/Questions.png" width="16px"/>
            <span>开始提问</span>
          </div>
          <template
              v-for="(questions, index) in questionsButton"
              :key="questions.label || index"
          >
            <div
                v-if="questionsVisible || anysisContentList.length > 0"
                :class="{
                'questions-prompt': true,
                'questions-tick': buttonIndex === index,
              }"
            >
              <div
                  v-if="currentAnalysisStatus === 1"
                  @click="questionsPrompt(questions.value, index)"
              >
                {{ questions.label }}
              </div>
              <div
                  v-if="currentAnalysisStatus === 0"
                  style="cursor: not-allowed"
                  title="当前有其他文本正在分析"
              >
                {{ questions.label }}
              </div>
            </div>
          </template>
        </div>

        <!-- AI身份 -->
        <div class="ai-role">
          <span>赋予AI身份</span>
          <div style="margin-top: 8px">
            <el-input v-model="dataForm.aiModelRole"></el-input>
          </div>
        </div>

        <!-- 提示词输入 -->
        <div
            v-if="questionsVisible || anysisContentList.length > 0"
            class="input-prompt"
        >
          <el-input
              v-model="promptWordsAuto"
              clearable
              placeholder="请输入您想要解决的问题"
          ></el-input>
          <div v-if="currentAnalysisStatus === 1">
            <img
                v-if="promptWordsAuto == ''"
                src="@/assets/images/ai_tick.png"
                style="cursor: not-allowed"
                width="28px"
            />
            <img
                v-if="promptWordsAuto != ''"
                src="@/assets/images/ai_tick_up.png"
                style="cursor: pointer"
                width="28px"
                @click="inputPrompt"
            />
          </div>
          <div
              v-if="currentAnalysisStatus === 0"
              title="当前有其他文本正在分析"
          >
            <img
                src="@/assets/images/ai_tick.png"
                style="cursor: not-allowed"
                width="28px"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- </el-dialog> -->
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/request-api'

const route = useRoute()

const visible = ref(false)
const questionsVisible = ref(false)
const anysisContentList = ref([])
const aiModelList = ref([])
const useAiModelId = ref('0')
const questionsButton = ref([
  {
    mark: 0,
    label: '分析关键词',
    value:
        '尽可能多的总结这篇直播稿的原文关键词和出现次数，以及关键词前后阐述的观点，用表格形式展示，关键词前后的观点放在一个表格里'
  }
])
const buttonIndex = ref(-1)
const promptWordsAuto = ref('')
const dataForm = reactive({
  aiModelRole:
      '你是一个逻辑思维比较强的文案专家，你现在接到了一个任务，提取抖音直播脚本信息，这个直播稿是在抖音上直播的，请仔细阅读本直播稿和对应的在线变化情况 。如果在线人数上升，就说明留人效果好，在线人数下降就是留人效果不好 ，下一段的视频开始时间是上一段的视频结束时间\n' +
      '任务：我需要你告诉我，这场直播留人效果好和不好的原因',
  promptWords: '',
  uuid: '',
  analysisContent: '',
  type: 0,
  textType: 0,
  modelId: ''
})

const loading = ref(false)
const currentAnalysisStatus = ref(1)
const tradeName = ref('')
let timerStatius = null

const startTimer = () => {
  timerStatius = setInterval(analysisStatus, 3000)
}

const analysisStatus = async () => {
  const res = await api.aiModelAnalysis.analysisStatusByUuid({
    uuid: dataForm.uuid,
    modelId: dataForm.modelId
  })
  if (res && res.code === 0) {
    if (res.data) {
      loading.value = true
    } else {
      loading.value = false
    }
  }
}

const headers = (item) => {
  if (item.length === 0) return []
  return item[0]
      .trimStart()
      .replace(/^\|/, '')
      .trimEnd()
      .replace(/\|$/, '')
      .split('|')
}

const rows = (item) => {
  const currentText = []
  item.forEach((item) => {
    if (item != '') {
      currentText.push(item)
    }
  })
  return currentText.slice(1).map((row) =>
      row
          .trimStart()
          .replace(/^\|/, '')
          .trimEnd()
          .replace(/\|$/, '')
          .split('|')
          .map((item) => item.trim())
  )
}

const init = () => {
  let sessionData = ''
  let videoId = route.query.videoId || ''
  let fileId = route.query.fileId || ''
  let contrastId = route.query.contrastId || ''
  let aiAnalysisTradeId = route.query.aiAnalysisTradeId || 0

  if (videoId != '' && videoId != null) {
    dataForm.uuid = videoId
    sessionData = JSON.parse(sessionStorage.getItem(videoId))
  } else if (fileId != '' && fileId != null) {
    dataForm.uuid = fileId
    dataForm.type = 1
    sessionData = JSON.parse(sessionStorage.getItem(fileId))
  } else if (contrastId != '' && contrastId != null) {
    dataForm.uuid = contrastId
    sessionData = JSON.parse(sessionStorage.getItem(contrastId))
  }

  getTradeNameById(aiAnalysisTradeId)
  listAiButtonByTradeId(aiAnalysisTradeId)
  listAiModels()
  analysisStatus()
  startTimer()
  dataForm.aiModelRole = '你的身份是顶尖的直播运营操盘手'
  listAiAnalysis()
  textCeiling(sessionData.text)
  visible.value = true
}

const textCeiling = (text) => {
  let len = text.length
  let lenCeiling = 44000
  if (len <= lenCeiling) {
    dataForm.analysisContent = text
  } else {
    dataForm.analysisContent = text.slice(0, lenCeiling)
  }
}

const changeModel = (model) => {
  dataForm.modelId = model.id
  useAiModelId.value = model.id
  window.alert('切换成功')
}

const questionsPrompt = async (value, mark, index) => {
  if (!dataForm.modelId) {
    ElMessage.error('请选择一个模型')
    return
  }
  dataForm.promptWords = value
  dataForm.outPutFormat = mark
  loading.value = true
  const res = await api.aiModelAnalysis.aiAnalysis(dataForm)
  if (res && res.code === 0) {
    listAiAnalysis()
  }
  buttonIndex.value = index
}

const listAiAnalysis = async () => {
  const res = await api.aiModelAnalysis.listAiAnalysisByUuid({
    uuid: dataForm.uuid
  })
  if (res && res.code === 0) {
    anysisContentList.value = res.data
  }
}

const inputPrompt = async () => {
  if (promptWordsAuto.value != '' && promptWordsAuto.value != null) {
    dataForm.promptWords = promptWordsAuto.value
    dataForm.outPutFormat = 0
    loading.value = true
    const res = await api.aiModelAnalysis.aiAnalysis(dataForm)
    if (res && res.code === 0) {
      listAiAnalysis()
    }
  }
}

const getTradeNameById = async (aiAnalysisTradeId) => {
  if (aiAnalysisTradeId != 0) {
    const res = await api.trade.info({id: aiAnalysisTradeId})
    if (res && res.code === 0) {
      tradeName.value = res.data.name
      questionsButton.value.splice(1, 0, {
        mark: 0,
        label: '分析行业关键词',
        value:
            '本场直播是在抖音上直播的，假如你是一个抖音直播脚本信息提取专家，你将仔细分析本场直播的话术脚本，找出其中的关键词和出现的次数。根据以下规则一步步执行：1. 仔细阅读直播脚本全2. 逐句分析，尽可能多的输出与' +
            tradeName.value +
            '行业有关的关键词和次数3.再给出关键词前后的观点，以及为哪些内容做铺垫；要求：1、用表格的形式展示，表头为：关键词、出现次数、关键词前观点，关键词后观点2、给出的观点不要太精简，也不要太复杂，每个观点，控制在150字以内'
      })
    }
  }
}

const listAiModels = async () => {
  const res = await api.aiModel.list({useType: 0})
  if (res && res.code === 0) {
    aiModelList.value = res.data.list
  }
}

const listAiButtonByTradeId = async (aiAnalysisTradeId) => {
  const res = await api.aiCueButton.list({tradeId: aiAnalysisTradeId})
  console.log(res)
  if (res && res.code === 0) {
    res.data.list.forEach((item) => {
      questionsButton.value.push({
        mark: 0,
        label: item.buttonName,
        value: item.problem
      })
    })
    console.log(questionsButton.value)
  }
}

const startQuestions = () => {
  questionsVisible.value = true
}

onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  if (timerStatius) {
    clearInterval(timerStatius)
  }
})
</script>

<style lang="scss" scoped>
.dialog-top {
  display: flex;
  justify-content: space-between;
  padding: 20px;
  color: #151719;
  font-size: 20px;
  font-weight: bold;
}

.close {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.dialog-top {
  display: flex;
  justify-content: space-between;
  padding: 20px;
  color: #151719;
  font-size: 20px;
  font-weight: bold;
}

.close {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.conversation {
  height: 590px;
  margin-bottom: 30px;
  overflow-y: auto;
}

.prompt-words-c {
  text-align: center;
  margin-bottom: 20px;
  background-color: #dedcf7;
  padding: 10px;
  border-radius: 8px;
  color: #3f3f3f;
  font-size: 18px;
}

.answer {
  margin-bottom: 30px;
  border-radius: 8px;
  /* border: 1px solid #D0D7DE; */
  /* padding: 16px; */
}

.conversation {
  height: 590px;
  margin-bottom: 30px;
  overflow-y: auto;
}

.prompt-words-c {
  text-align: center;
  margin-bottom: 20px;
  background-color: #dedcf7;
  padding: 10px;
  border-radius: 8px;
  color: #3f3f3f;
  font-size: 18px;
}

.answer {
  margin-bottom: 30px;
  border-radius: 8px;
  /* border: 1px solid #D0D7DE; */
  /* padding: 16px; */
}

.start-questions {
  display: flex;
  gap: 3px;
  color: #151719;
  font-size: 14px;
  padding: 20px;
  background-color: #fafbff;
  border-radius: 1px solid #ffffff;
  box-shadow: 1px 1px 5px 0px #e2e5ed;
  border-radius: 4px;
  width: 130px;
  margin-right: 16px;
  cursor: pointer;
}

.start-questions:hover {
  background-color: #ddd;
}

.start-questions {
  display: flex;
  gap: 3px;
  color: #151719;
  font-size: 14px;
  padding: 20px;
  background-color: #fafbff;
  border-radius: 1px solid #ffffff;
  box-shadow: 1px 1px 5px 0px #e2e5ed;
  border-radius: 4px;
  width: 130px;
  margin-right: 16px;
  cursor: pointer;
}

.start-questions:hover {
  background-color: #ddd;
}

.questions-prompt {
  padding: 6px 12px;
  margin-right: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
}

.questions-prompt:hover {
  color: #151719;
  background-color: #f2f2f2;
}

.questions-prompt {
  padding: 6px 12px;
  margin-right: 8px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
}

.questions-prompt:hover {
  color: #151719;
  background-color: #f2f2f2;
}

.questions-tick {
  color: #151719;
  background-color: #f2f2f2;
}

.questions-tick {
  color: #151719;
  background-color: #f2f2f2;
}

.ai-role {
  padding: 12px;
  color: #151719;
  font-size: 14px;
  font-weight: bold;
  border-radius: 12px;
  border: 1px solid #e1e6ea;
  margin-top: 20px;
}

.ai-model {
  padding: 12px;
  color: #151719;
  font-size: 14px;
  font-weight: bold;
  border-radius: 12px;
  border: 1px solid #e1e6ea;
  margin-bottom: 20px;
}

.model-check {
  background-color: #24c1c1;
}

.input-prompt {
  margin-top: 8px;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e1e6ea;
  display: flex;
  align-items: center;
  gap: 10px;
}

.input-prompt {
  margin-top: 8px;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e1e6ea;
  display: flex;
  align-items: center;
  gap: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  text-align: left;
  padding: 8px;
}

th,
td {
  text-align: left;
  padding: 8px;
}

th {
  background-color: #f2f2f2;
}

th {
  background-color: #f2f2f2;
}

body {
  margin: 0;
}

body {
  margin: 0;
}

:deep(.el-dialog__header) {
  padding: 20px 20px 0px 20px;
}

:deep(.el-dialog__header) {
  padding: 20px 20px 0px 20px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__headerbtn) {
  top: 20px;
}

:deep(.el-dialog__headerbtn) {
  top: 20px;
}

:deep(.el-input__inner) {
  border-radius: 20px;
  border: 1px solid #dcdfe6;
  color: #606266;
  display: inline-block;
  font-size: inherit;
  height: 40px;
  line-height: 40px;
  outline: none;
  padding: 0 15px;
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  width: 100%;
}

:deep(.el-input__inner) {
  border-radius: 20px;
  border: 1px solid #dcdfe6;
  color: #606266;
  display: inline-block;
  font-size: inherit;
  height: 40px;
  line-height: 40px;
  outline: none;
  padding: 0 15px;
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  width: 100%;
}

:deep(.custom-dialog) {
  .el-dialog {
    border-radius: 8px;
  }

  .el-dialog__header {
    border-bottom: 1px solid #e4e7ed;
  }
}

:deep(.custom-dialog) {
  .el-dialog {
    border-radius: 8px;
  }

  .el-dialog__header {
    border-bottom: 1px solid #e4e7ed;
  }
}

.conversation {
  height: 400px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  background-color: #f9f9f9;
}

:deep(.conversation::-webkit-scrollbar) {
  width: 8px;
}

:deep(.conversation::-webkit-scrollbar) {
  width: 8px;
}

:deep(.conversation::-webkit-scrollbar-thumb) {
  background-color: #c0c4cc;
  border-radius: 4px;
}

:deep(.conversation::-webkit-scrollbar-thumb) {
  background-color: #c0c4cc;
  border-radius: 4px;
}
</style>
