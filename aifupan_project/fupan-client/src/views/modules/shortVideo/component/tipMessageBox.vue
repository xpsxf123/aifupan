<template>
    <el-dialog
        title="友情提示"
        :visible.sync="dialogVisible"
        width="416px"
        :close-on-press-escape="false"
        :show-close="false"
        custom-class="tip-message"
        :close-on-click-modal="false">
        <div class="flex items-start">
            <i class="el-icon-warning" style="font-size: 20px;color: #FFCC32;"></i>
            <div style="padding-left: 3px" v-html="config.content"></div>
        </div>
        <div slot="footer" class="flex items-center justify-center">
            <afp-button @click="cancelEvent" size="default" v-if="config.showCancelButton">{{ config.cancelButtonText }}</afp-button>
            <div style="width: 40px" v-if="config.showCancelButton && config.showConfirmButton"></div>
            <afp-button type="primary" size="default" v-if="config.showConfirmButton" @click="confirmEvent">{{
                    config.confirmButtonText
                }}
            </afp-button>
        </div>
    </el-dialog>
</template>

<script>

export default {
    components: {},
    props: {},
    data() {
        return {
            dialogVisible: false,
            config: {}
        };
    },
    computed: {},
    watch: {},
    methods: {
        init() {
            return {
                content: '这是一个提示',
                showCancelButton: true,
                showConfirmButton: true,
                cancelButtonText: '取消',
                confirmButtonText: '确定',
            }
        },
        open(config) {
            this.config = {
                ...this.init(),
                ...config
            }
            this.dialogVisible = true;
        },
        confirmEvent() {
            this.$emit('confirmEvent')
            this.dialogVisible = false
        },
        cancelEvent() {
            this.$emit('cancelEvent')
            this.dialogVisible = false
        }
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss'>
.tip-message {
    .el-dialog__header {
        border-bottom: 1px rgba(227, 227, 229, 0.25) solid;
        padding: 16px;

        .el-dialog__title {
            font-weight: 500;
            font-size: 16px;
            color: #151719;
        }
    }

    .el-dialog__body {
        padding: 16px 16px 30px 16px;
    }

    .el-dialog__footer {
        padding-top: 0;
        padding-bottom: 40px;
    }
}
</style>