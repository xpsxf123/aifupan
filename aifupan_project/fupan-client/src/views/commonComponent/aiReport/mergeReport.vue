<template>
    <div class="mergeReport">
        <el-dialog
            :visible.sync="mergeReportVisible"
            :before-close="closeMergeReport"
            :close-on-click-modal="false"
            :destroy-on-close="true"
            width="555px">
            <div class="header-title">合并AI诊断报告</div>
            <div class="mergeReport-content">
                <div v-for="(item,index) in currentSelectedObj" :key="index" class="list-item">
                    <template v-if="item.cueWordsList.length">
                        <div style="font-weight: 600"> {{ item.tagName }}：</div>
                        <div v-for="(_item,_index) in item.cueWordsList" :key="_index" class="list-item">
                            {{ _index + 1 }}.{{ _item.cueWord }}
                        </div>
                    </template>
                </div>
            </div>
            <div class="dialog-footer">
                <!--                <div>-->
                <!--                    <div style="font-size: 12px;padding: 3px">输出目录</div>-->
                <!--                    <el-select v-model="outputCatalogue" placeholder="请选择" >-->
                <!--                        <el-option-->
                <!--                            v-for="item in options"-->
                <!--                            :key="item.value"-->
                <!--                            :label="item.label"-->
                <!--                            :value="item.value">-->
                <!--                        </el-option>-->
                <!--                    </el-select>-->
                <!--                </div>-->
                <div style="font-size: 12px;padding: 3px">输出名称</div>
                <div class="outputName">
                    <el-input size="default" style="width: 350px;" placeholder="请输入内容" v-model="outputName"/>
                    <div>.pdf</div>
                </div>
            </div>
            <div class="progress">
                <el-progress define-back-color="#E7E6E8" :percentage="percentage"
                             style="flex: 1;font-size: 12px"></el-progress>
<!--                <afp-button type="text" style="padding: 0;font-size: 12px" @click="cancelMerge"-->
<!--                           :disabled="(percentage>=100)||!timer">取消-->
<!--                </afp-button>-->
            </div>
            <div class="tips_text" style="color: #A6A6A9" v-if="percentage<100">
                大约需要1分钟
            </div>
            <div class="tips_text" style="color: #73BB3B" v-else>
                pdf文件已生成
            </div>
            <span slot="footer" class="dialog-footer-btn">
                <afp-button type="primary" :plain="false" @click="handleMerge" :disabled="!!timer">开始合并</afp-button>
            </span>
        </el-dialog>
    </div>
</template>

<script>
export default {
    props: {
        currentSelectedObj: {
            type: Object,
            default: () => {
            }
        }
    },
    data() {
        return {
            mergeReportVisible: false,
            options: [{
                value: '选项1',
                label: '输出视频文件夹里面'
            }, {
                value: '选项2',
                label: '双皮奶'
            }],
            outputCatalogue: '',
            outputName: '',
            percentage: 0,
            timer: null
        }
    },
    computed: {},
    watch: {
    },
    methods: {
        changeMergeReportStatus(status,outputName) {
            this.outputName = outputName
            this.mergeReportVisible = status
        },
        closeMergeReport() {
            this.cancelMerge()
            this.changeMergeReportStatus(false)
        },
        cancelMerge() {
            if (this.timer) {
                clearInterval(this.timer)
                this.timer = null
            }
            this.percentage = 0
        },
        handleMerge() {
            this.$emit('mergeReportStatus', this.outputName)
            this.timer = setInterval(() => {
                this.percentage += Math.floor(Math.random() * 10)
                if (this.percentage >= 100) {
                    this.percentage = 100
                    clearInterval(this.timer)
                }
            }, 100)
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
<style lang='scss' scoped>
.mergeReport {
    ::v-deep(.el-dialog__body) {
        background: #F2F1F5;
    }

    ::v-deep(.el-dialog__footer) {
        background: #F2F1F5;
        padding: 0 12px 12px 12px;
    }

    .header-title {
        padding: 12px 0;
        font-size: 16px;
        font-weight: 600;
        color: #17191B;
    }

    .mergeReport-content {
        background: #fff;
        border-radius: 8px;
        padding: 8px;
        max-height: 280px;
        overflow: auto;

        .list-item {
            padding: 2px 0;
            font-size: 13px;
            color: #333;
        }
    }

    .dialog-footer {
        padding: 12px 0;

        .outputName {
            display: flex;
            align-items: center;
        }
    }

    .dialog-footer-btn {
        display: flex;
        justify-content: flex-end;
    }

    .progress {
        display: flex;
        align-items: center;

        ::v-deep(.el-progress__text) {
            font-size: 12px !important;
        }
    }

    .tips_text {
        font-size: 11px;
        text-align: right;
        padding-right: 42px;
    }

    ::v-deep(.el-dialog) {
        .el-dialog__header {
            padding: 0;
        }

        .el-dialog__body {
            padding: 12px;
        }
    }
}
</style>