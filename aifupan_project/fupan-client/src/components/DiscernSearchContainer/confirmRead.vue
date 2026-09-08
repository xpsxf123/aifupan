<template>
    <div class="confirm-read">
        <el-checkbox-group v-model="readSelect">
            <el-checkbox v-for="read in readList" :label="read.value" :key="read.value" :value="read.value">
                {{ read.name }}
            </el-checkbox>
        </el-checkbox-group>
        <afp-button
            type="primary"
            size="default"
            style="margin-left: 24px"
            :plain="false"
            :disabled="!reportId"
            @click="handleConfirm">
            已看完整个报告
        </afp-button>
    </div>
</template>

<script>

export default {
    props: {
        type: {
            type: String,
            default: 'interactionInspection'
        },
        reportId: {
            type: [Number, String, null],
            default: null
        }
    },
    data() {
        return {
            readList: [{
                name: '运营已知晓',
                value: 1
            }, {
                name: '主播已知晓',
                value: 2
            }, {
                name: '主管已知晓',
                value: 3
            }],
            readSelect: []
        }
    },
    methods: {
        handleConfirm() {
            const confirmRole = this.readSelect.length > 0 ? this.readSelect[0] : undefined;
            this.$emit('confirm', { type: this.type, confirmRole });
        }
    }
}
</script>

<style scoped lang="scss">
.confirm-read {
    display: flex;
    align-items: center;
    justify-content: end;
}
</style>
