<template>
    <div class="copyText-content overflow_hidden overflow_auto_y">
        <el-input class="copyText-div-dom" type="textarea" :autosize="{ minRows: 10, maxRows: 10 }" :maxlength="20000"
            show-word-limit v-model="$attrs.value" @input="handlerInput"></el-input>
        <p v-if="!$attrs.value" class="div-empty-hint text-color3 text-center h100 flex-ji-c">
            请将复制的文本粘贴在此处<br>
            （单次文本分析最高支持2w字）
        </p>
    </div>
</template>

<script>

export default {
    components: {
    },
    props: {
    },
    data() {
        return {
        };
    },
    computed: {
    },
    watch: {},
    methods: {
        handlerInput(){
            this.$emit('input', this.$attrs.value);
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() { }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.copyText-content {
    margin-block: 6px;
    // height: 230px;
    position: relative;
    background: rgba(0, 119, 255, 0.05);
    border-radius: 4px 4px 4px 4px;
    border: 1px dashed var(--color-main);
}

.copyText-div-dom {
    position: relative;
    z-index: 1;
    background-color: transparent !important;
    min-height: 100%;
    border: none;

    ::v-deep(.el-textarea__inner) {
        background-color: transparent !important;
        border: none;
        min-height: 100%;
        resize: none;
        padding: 10px;
    }
}

.div-empty-hint {
    margin: 0;
    width: 100%;
    position: absolute;
    left: 0;
    top: 0;
    z-index: 0;
    background-color: transparent !important;
}
</style>