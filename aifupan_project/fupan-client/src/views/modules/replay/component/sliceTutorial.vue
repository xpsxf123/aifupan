<template>
    <el-dialog
        :visible.sync="visible"
        class="slice-tutorial"
        width="484px"
        :close-on-press-escape="false"
        :destroy-on-close="true"
        :close-on-click-modal="false">
        <div class="content-container">
            <div class="header text-center">
                <div class="text-2xl">切片方式教程</div>
                <div class="text-sm" style="margin-top: 6px" v-if="isVideo">可以通过以下三种方式来进行切片</div>
            </div>
            <div class="content">
                <div class="flex items-center content-com content-one">
                    <div class="item-header" v-if="isVideo">方式一</div>
                    <div class="item-content">进入整场复盘详情页，点击 < 切片 >按钮，
                        通过选取视频时间/北京时间，来对整场复盘进行切片
                    </div>
                </div>
                <div class="flex items-center content-com content-two" v-if="isVideo">
                    <div class="item-header">方式二</div>
                    <div class="item-content">框选想要切片的分钟段落话术，点击鼠标右键，选择“切片”即可进行切片</div>
                </div>
                <div class="flex items-center content-com content-three" v-if="isVideo">
                    <div class="item-header">方式三</div>
                    <div class="item-content">进入数据曲线，通过曲线信息，对整场复盘进行切片</div>
                </div>
            </div>
            <el-button @click="handleClose" type="primary"
                       class="iKnow"
                       size="default"
                       :plain="false">
                我知道了
            </el-button>
        </div>
    </el-dialog>
</template>

<script>

export default {
    props: {
        isVideo: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            visible: false
        };
    },
    computed: {},
    watch: {},
    methods: {
        open() {
            this.visible = true
        },
        handleClose() {
            this.visible = false
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
.slice-tutorial {
    ::v-deep(.el-dialog__header) {
        display: none;
    }

    ::v-deep(.el-dialog__body) {
        padding: 40px 30px;
        background: url("~@/assets/imgs/slice-tutorial.png") no-repeat;
    }

    .content {
        .content-com {
            height: 74px;
            border-radius: 74px;
            padding: 16px;
            margin-top: 16px;
        }

        .item-header {
            width: 42px;
            height: 42px;
            border-radius: 50%;
            text-align: center;
            line-height: 42px;
            color: #fff;
            font-size: 12px;
            flex-shrink: 0;
        }

        .item-content {
            padding-left: 8px;
        }

        .content-one {
            background: linear-gradient(90deg, #F3F4FE 0%, #E4F5FF 100%);

            .item-header {
                background: linear-gradient(134deg, #47D6CA 0%, #17ABE7 100%);
            }
        }

        .content-two {
            background: linear-gradient(90deg, #ECFAF8 0%, #E4F4FF 100%);

            .item-header {
                background: linear-gradient(134deg, #47D6CA 0%, #17ABE7 100%);
            }
        }

        .content-three {
            background: linear-gradient(90deg, #FFFBEB 0%, #FFF1E4 100%);

            .item-header {
                background: linear-gradient(134deg, #FFDF87 0%, #FA7A2B 100%);
            }
        }
    }

    .iKnow {
        width: 100%;
        margin-top: 30px;
        height: 56px;
        background: linear-gradient(90deg, #08AFFD 0%, #444DFF 100%);
        border: none;
        border-radius: 56px;
    }
}
</style>
