<template>
    <div class="dataEdit-container">
        <el-dialog title="数据概览" :visible.sync="!!currentItem.id" width="30%" :before-close="handleClose"
                   custom-class="dataEdit-dialog" :close-on-click-modal="false">
            <div v-if="isEdit">
                <el-input type="textarea" :autofocus="true" :rows="6" placeholder="请输入内容" v-model="textarea">
                </el-input>
            </div>
            <div class="text-container" v-else>
                <div v-html="textarea" style="white-space: pre-wrap;height:240px;overflow: auto"></div>
            </div>
            <span slot="footer" class="dialog-footer">
                <template v-if="isEdit">
                    <afp-button @click="handCancelText"  plain :disabled="!currentItem.isAuthenticated">取 消</afp-button>
                    <afp-button type="primary" @click="handleSave" 
                               :disabled="!currentItem.isAuthenticated">保 存</afp-button>
                </template>

                <afp-button v-else type="primary" @click="isEdit = !isEdit" 
                           :disabled="!currentItem.isAuthenticated">编 辑</afp-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
import { isEqual } from 'lodash'

export default {
    components: {},
    props: {
        currentItem: {
            type: Object,
            default: {}
        }
    },
    data () {
        return {
            isEdit: false,
            textarea: '流量数据：\n1.观看人次：一场直播总观看人数\n2.平均停留时长：时间时间\n3.人气峰值：直播中最高一次的在线人数\n4.平均在线人数：每分钟取到的在线人数\n销售数据：\n1.本场销售额：一场直播总观看人数\n2.销量：时间时间\n3.客单价：直播中最高一次的在线人数\n',
            cacheTextarea: '流量数据：\n1.观看人次：一场直播总观看人数\n2.平均停留时长：时间时间\n3.人气峰值：直播中最高一次的在线人数\n4.平均在线人数：每分钟取到的在线人数\n销售数据：\n1.本场销售额：一场直播总观看人数\n2.销量：时间时间\n3.客单价：直播中最高一次的在线人数\n',
        }
    },
    computed: {},
    watch: {
        currentItem (newVal, oldVal) {
            if (!isEqual(newVal, oldVal)) {
                this.textarea = newVal.aiContent
                this.cacheTextarea = newVal.aiContent
            }
        }
    },
    methods: {
        handleClose () {
            if (this.cacheTextarea === this.textarea) {
                this.handCancel()
                return
            }
            this.$confirm('是否需要保存后退出？', {
                title: '提示',
                confirmButtonText: '保存并退出',
                cancelButtonText: '直接退出',
                customClass: 'custom-confirm',
                closeOnClickModal: false,
                closeOnPressEscape: false,
                dangerouslyUseHTMLString: true, // 允许使用 HTML
                message: `
                    <div style="text-align:center">
                        <div style="margin-top:12px">
                            <svg v-else class="icon menuImg" aria-hidden="true" style='width:80px;height:80px'>
                                <use xlink:href="#icon-tixing"></use>
                            </svg>
                        </div>
                        <div style="padding:24px 0"> 是否需要保存后退出？</div>
                    </div>
                `
            }).then(() => {
                this.handleSave()
            }).catch(() => {
                this.handCancel()
            })
        },
        handCancelText () {
            this.isEdit = !this.isEdit
            this.textarea = this.cacheTextarea
        },
        handleSave () {
            this.$httpBack.v2300.updateScreenshot({
                id: this.currentItem.id,
                aiContent: this.textarea
            }).then(res => {
                if (res.code === 0) {
                    this.$message.success('保存成功')
                    this.handCancel()
                }
            })
        },
        handCancel () {
            this.isEdit = false
            this.$emit('handleChange', false)
        },
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
::v-deep(.dataEdit-dialog) {
    .el-dialog__header {
        padding: 10px 20px;
    }

    .el-textarea__inner {
        height: 230px !important;
    }

    .el-dialog__body {
        padding: 0 20px;
        height: 250px;
    }
}

.dataEdit-container {
    .text-container {
        font-weight: 500;
        font-size: 14px;
    }
}

</style>
