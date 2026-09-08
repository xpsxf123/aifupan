<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :width="650" title="日志详情">
    <div class="detailsBox">
      <div class="row">
        <div class="coumn">
          <div>动作名称：</div>
          <div>{{ dataForm.actionName }}</div>
        </div>
        <div class="coumn">
          <div>日志类型：</div>
          <div>{{ dataForm.logType == 0 ? '正常日志' : '错误日志' }}</div>
        </div>
      </div>
      <div class="row">
        <div class="coumn">
          <div>客户端版本：</div>
          <div>{{ dataForm.clientVersion }}</div>
        </div>
        <div class="coumn">
          <div>执行用户：</div>
          <div>{{ dataForm.clientUser }}</div>
        </div>
      </div>
      <div class="row">
        <div class="coumn">
          <div>执行时间：</div>
          <div>{{ dataForm.executeTime }}</div>
        </div>
      </div>
      <div class="row">
        <div class="coumn" style="width: 98%;">
          <div>动作描述：</div>
          <div>{{ dataForm.actionInfo }}</div>
        </div>
      </div>
      <div class="row">
        <div class="coumn" style="width: 98%;">
          <div>错误信息：</div>
          <div class="error-msg">{{ dataForm.errorMsg }}</div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'

const visible = ref(false)
const dataForm = reactive({})

const init = (row) => {
  visible.value = true
  Object.assign(dataForm, row)
}

defineExpose({
  init
})
</script>

<style scoped>
.detailsBox {
  width: 100%;
  margin: 0;
  padding: 0;
  font-size: 14px;
}

.detailsBox .row {
  width: 100%;
  margin: 5px 0;

  display: flex;
  justify-content: flex-start;

}

.detailsBox .row .coumn {
  width: 48%;
  padding: 5px 0;
  display: flex;
  margin-left: 2%;
  justify-content: flex-start;
  border-bottom: 1px dashed #999;

  & div:first-child {
    white-space: nowrap;
  }
}

.detailsBox .row .coumn div:nth-child(1) {
  text-align: left;
  font-weight: bold;
  padding: 0;
  margin-right: 5px;
}

.detailsBox .row .coumn div:nth-child(2) {
  text-align: left;
}

.error-msg {
  white-space: normal; /* 正常换行 */
  word-break: break-all; /* 长单词也能换行 */

}
</style>
  