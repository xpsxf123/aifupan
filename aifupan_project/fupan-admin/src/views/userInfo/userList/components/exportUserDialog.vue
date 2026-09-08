<template>
    <div>
        <el-dialog v-model="visible" width="460px" :close-on-click-modal="false">
            <div>请选择注册时间后导出用户信息</div>
        </el-dialog>
    </div>    
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)

const init = () => {
    visible.value = true
}

defineExpose({
    init
})
</script>

<style scoped>
:deep(.el-dialog__header) {
    padding: 0px;
}

:deep(.el-dialog__body) {
    padding: 0px;
}

:deep(.el-dialog__headerbtn) {
    /* display: none;  */
    padding: 0px;
    margin: 0px;
}
</style>