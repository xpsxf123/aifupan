<template>
  <div>
    <el-dialog
        v-model="wordRuleVisible"
        :before-close="handleClose"
        :close-on-click-modal="false"
        :title="title"
        :width="800"
        append-to-body
    >
      <div v-if="ruleList && ruleList.length > 0">
        <div class="wordRuleTitleContainer">
          <!-- <div class="wordRuleTitleItem1">类型</div> -->
          <div class="wordRuleTitleItem2">范围</div>
          <div class="wordRuleTitleItem3">匹配词</div>
          <div class="wordRuleTitleItem4">操作</div>
        </div>
        <div
            v-for="(rule, index) in ruleList"
            :key="`rule-${index}`"
            class="wordRuleContainer"
        >
          <!-- <div class="wordRuleTd1">
                        <el-select v-model="ruleList[index].blackOrWhite" placeholder="请选择">
                            <el-option v-for="(item, optionIndex) in ['黑名单', '白名单']" :key="`option-${optionIndex}`" :label="item" :value="optionIndex">
                            </el-option>
                        </el-select>
                    </div> -->
          <div class="wordRuleTd2">
            <el-input
                v-model="ruleList[index].rangeLength"
                placeholder="前后多少个字"
                type="number"
            ></el-input>
          </div>
          <div class="wordRuleTd3">
            <el-input
                v-model="ruleList[index].containerWords"
                placeholder="多个词用#隔开，匹配上任意一个词即为匹配成功"
            ></el-input>
          </div>
          <div class="wordRuleTd4">
            <span
                v-if="index == ruleList.length - 1"
                style="color: blue; cursor: pointer"
                @click="addRule"
            >添加</span
            >
            <span
                style="color: red; margin-left: 10px; cursor: pointer"
                @click="removeRule(index)"
            >删除</span
            >
          </div>
        </div>
      </div>
      <div v-else>
        <el-button size="small" type="primary" @click="addRule"
        >添加规则
        </el-button
        >
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleClose">取消</el-button>
          <el-button type="primary" @click="submit()">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineOptions({
  name: 'ReplayPcRuleList'
})

const emit = defineEmits(['reRender'])

const wordRuleVisible = ref(false)
const ruleList = ref([])
const oldRuleList = ref([])
const blackOrWhite = ref(0)
const title = ref('')
const wordName = ref('')

const handleClose = () => {
  ruleList.value.splice(0, ruleList.value.length)
  oldRuleList.value.forEach((item) => {
    ruleList.value.push(item)
  })
  wordRuleVisible.value = false
}

const submit = () => {
  let close = true
  if (ruleList.value && ruleList.value.length > 0) {
    ruleList.value.forEach((item) => {
      if (item.containerWords) {
        item.containerWords = item.containerWords.replace(/[ \t\n\r\f\v]+/g, '')
      }
      if (
          item.blackOrWhite === '' ||
          !item.rangeLength ||
          item.rangeLength < 1 ||
          !item.containerWords
      ) {
        close = false
      }
    })
  }
  if (close) {
    wordRuleVisible.value = false
    emit('reRender')
  } else {
    ElMessage.error('请正确填写完整数据')
  }
}

const removeRule = (index) => {
  ruleList.value.splice(index, 1)
}

const addRule = () => {
  if (!ruleList.value) {
    ruleList.value = []
  }
  ruleList.value.push({
    blackOrWhite: blackOrWhite.value,
    type: 2,
    rangeLength: '',
    containerWords: ''
  })
}

const init = (ruleListParam, blackOrWhiteParam, wordNameParam) => {
  ruleList.value = ruleListParam
  blackOrWhite.value = blackOrWhiteParam
  if (blackOrWhiteParam == 0) {
    title.value = '添加 [ ' + wordNameParam + ' ] 的黑名单规则'
  } else if (blackOrWhiteParam == 1) {
    title.value = '添加 [ ' + wordNameParam + ' ] 的白名单规则'
  } else if (blackOrWhiteParam == 2) {
    title.value = '添加 [ ' + wordNameParam + ' ] 的限定词规则'
  }

  oldRuleList.value = JSON.parse(JSON.stringify(ruleListParam))
  wordRuleVisible.value = true
}

defineExpose({
  init
})
</script>

<style scoped>
.wordRuleTd4 {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20%;
  box-sizing: border-box;
  height: 38px;
  border: 0.5px solid #ddd;
}

.wordRuleTd3 {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60%;
  box-sizing: border-box;
  height: 38px;
  border-left: 0.5px solid #ccc;
  border-bottom: 0.5px solid #ccc;
}

.wordRuleTd2 {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20%;
  box-sizing: border-box;
  height: 38px;
  border-left: 0.5px solid #ccc;
  border-bottom: 0.5px solid #ccc;
}

.wordRuleTd2, .wordRuleTd3 {
  :deep(.el-input__wrapper) {
    box-shadow: none !important;
  }

  :deep(.el-select__wrapper) {
    box-shadow: none !important;
  }
}


.wordRuleTd1 {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 15%;
  box-sizing: border-box;

  height: 38px;
}

.wordRuleTitleItem4 {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  height: 38px;
  width: 20%;
}

.wordRuleTitleItem3 {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
  height: 38px;
  width: 60%;
}

.wordRuleTitleItem2 {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
  height: 38px;
  width: 20%;
}

.wordRuleTitleItem1 {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(177, 250, 177);
  font-size: 16px;
  box-sizing: border-box;
  border-right: 0.5px solid #ccc;
  height: 38px;
  width: 15%;
}

.wordRuleContainer {
  display: flex;
  align-items: center;
  width: 100%;
}

.wordRuleTitleContainer {
  display: flex;
  align-items: center;
  width: 100%;
}
</style>
