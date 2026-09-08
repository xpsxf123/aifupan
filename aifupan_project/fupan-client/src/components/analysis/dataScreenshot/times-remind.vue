<template>
    <div class="times-remind">
        <el-dialog
            title="提示"
            :visible.sync="dialogVisible"
            custom-class="times-remind-dialog">
            <div class="content">
                您当前的AI图片识别次数仅剩{{ imageInfo.imgIdentifyNum }}/{{ imageInfo.totalImgIdentifyNum }}，无法进行图片识别，是否马上扩容？
            </div>
            <div slot="footer" class="dialog-footer">
                <afp-button @click="dialogVisible=false" round size="small"
                           class="cancel">取 消
                </afp-button>
                <afp-button type="primary" @click="handleChange" round size="small"
                           class="confirm">
                    确 定
                </afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>

export default {
    components: {},
    props: {
        imageInfo: {
            type: Object,
            default: () => {}
        }
    },
    data () {
        return {
            dialogVisible: false,
        }
    },
    computed: {},
    watch: {},
    methods: {
        initStatus () {
            this.dialogVisible = true
        },
        handleChange () {
            this.dialogVisible = false
            this.$emit('handleChangeDialog')
        }
    },
    created () {

    },
    mounted () {

    },
    beforeCreate () { }, //生命周期 - 创建之前
    beforeMount () { }, //生命周期 - 挂载之前
    beforeUpdate () { }, //生命周期 - 更新之前
    updated () { }, //生命周期 - 更新之后
    beforeDestroy () { }, //生命周期 - 销毁之前
    destroyed () { }, //生命周期 - 销毁完成
    activated () { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.times-remind {
    ::v-deep(.times-remind-dialog) {
        height: 251px;
        width: 300px;
        background: url("~@/assets/imgs/wxts.png");
        background-repeat: no-repeat;
        background-size: contain;

        .el-dialog__body {
            padding: 30px 28px;
            margin-top: 18px;
        }

        .content {
            font-size: 13px;
            font-weight: 500;
        }

        .dialog-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 16px;

            .el-button {
                width: 100px;

                &:hover {
                    background-color: transparent;
                }
            }

            .cancel {
                border-color: #C97A50;
                color: #C97A50
            }

            .confirm {
                border-color: transparent;
                background: linear-gradient(93deg, #E89E73 0%, #D57646 100%);
                color: #fff
            }
        }
    }
}
</style>
