<template>
    <div class="not-mark-word">
        <el-dialog 
            :title="'行业：' + dataInfo.tradeName" 
            v-model="dialogVisible" 
            width="50%"
            :close-on-click-modal="false"
            class="not-mark-word__dialog">

            <div v-if="dataInfo.notMarkWordList && dataInfo.notMarkWordList.length > 0" class="not-mark-word__content">
                <el-tag 
                    v-for="item in dataInfo.notMarkWordList"
                    :key="item.id"
                    class="not-mark-word__tag">
                    <span>{{ " " + item.sensitiveWord + " " }}</span>
                </el-tag>
            </div>
            
            <template #footer>
                <div class="not-mark-word__footer">
                    <el-button type="primary" @click="dialogVisible = false">确 定</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref } from 'vue'

const dialogVisible = ref(false)
const dataInfo = ref({
    tradeName: ""
})

const init = (data) => {
    dataInfo.value = data
    dialogVisible.value = true
}

defineExpose({
    init
})
</script>

<style lang="scss" scoped>
.not-mark-word {
    &__dialog {
        .el-dialog__body {
            padding: 20px;
        }
    }

    &__content {
        margin-bottom: 20px;
    }

    &__tag {
        margin-right: 20px;
        margin-bottom: 10px;
    }

    &__footer {
        text-align: right;
    }
}
</style>