<template>
    <el-dialog
        title="添加对比分析"
        :visible.sync="dialogVisible"
        width="678px"
        :close-on-press-escape="false"
        :close-on-click-modal="false"
        class="review-contrast">
        <el-dialog
            width="472px"
            title="温馨提示"
            :visible.sync="innerVisible"
            class="inner-review-contrast"
            append-to-body>
            <div class="inner-content">
                <div class="inner-item">
                    <span>1. </span>
                    <span>优化场次指是您觉得需要优化的场次</span>
                </div>
                <div class="inner-item">
                    <span>2. </span>
                    <span>参考场次指的是您觉得可以追赶的场次</span>
                </div>
                <div class="inner-item">
                    <span>3. </span>
                    <span>默认销售或场观高的定位为参考场次</span>
                </div>
                <div class="inner-item">
                    <span>4. </span>
                    <span>生成后，可在对比复盘里修改账号定位</span>
                </div>
                <div class="inner-item">
                    <span>5. </span>
                    <span>AI运营助手分析的时候，会根据定位不同生成 不同的分析报告！请谨慎定位！</span>
                </div>
            </div>
            <div slot="footer" class="dialog-footer w100 flex items-center justify-center">
                <afp-button size="default" @click="innerVisible = false">取消生成</afp-button>
                <afp-button type="primary" :plain="false" size="default" style="margin-left: 52px" @click="addContrast">
                    点我生成
                </afp-button>
            </div>
        </el-dialog>
        <div v-if="dialogVisible">
            <div class="labels">
                <div class="label">优化场次
                    <el-tooltip class="item" effect="dark" content="优化场次是您需要优化的场次" placement="top">
                        <i class="el-icon-question"
                           style="color: #ABAEB3;padding-inline:2px"></i>
                    </el-tooltip>
                </div>
                <div class="label text-y">参考场次
                    <el-tooltip class="item" effect="dark" content="参考场次是您觉得做得好的场次" placement="top">
                        <i class="el-icon-question text-y"
                           style="color: #ABAEB3;padding-inline:2px"></i>
                    </el-tooltip>
                </div>
            </div>
            <transition-group name="contrast" tag="div" class="cards">
                <div v-for="(item, index) in contrastList" :key="item.id" class="flex items-center">
                    <div class="contrast-item" :class="{'benchmark-item':index===1}">
                        <div class="flex items-center w100 item-child">
                            <img src="@/assets/imgs/video.png" class="compereImg" v-if="contrastType==='file'" alt="">
                            <img :src="item.anchorInfo?.anchorAvatar" v-else class="compereImg" alt=""/>
                            <div class="flex items-start flex-column"
                                 style="width:calc(100% - 40px);padding-left: 5px">
                                <div class="text-clamp1 w100">{{ item.videoName||item.fileName }}</div>
                                <div>{{ item.anchorInfo?.anchorName }}</div>
                            </div>
                            <template>
                                <img src="@/assets/imgs/you.png" v-if="index===0" class="upload-dui-you" alt=""/>
                                <img src="@/assets/imgs/2_5_8/can.png" v-if="index===1" class="upload-dui-you" alt=""/>
                            </template>
                        </div>

                        <div class="item-info" v-if="contrastType==='file'">
                            <div class="item-info">
                                <span class="font-bold">文件大小：</span>
                                <span class="gray-9 text-xs"> {{myUtils.retainDecimals(item.fileSize / 1024 / 1024) + 'M'}}</span>
                            </div>
                            <div class="item-info" v-if="item.fileType===0">
                                <span class="font-bold">视频时长：</span>
                                <span class="gray-9 text-xs">{{ getTimeStr(item?.fileDuration)}}</span>
                            </div>
                            <div class="item-info">
                                <span class="font-bold">上传时间：</span>
                                <span class="gray-9 text-xs">{{ item?.uploadTime}}</span>
                            </div>
                        </div>
                        <template v-else>
                            <div class="item-info">
                                <span class="font-bold">录制时间：</span>
                                <span class="gray-9 text-xs">{{ item.startTime }}</span>
                            </div>
                            <div class="item-info">
                                <span class="font-bold">{{ item.videoSliceType === 1 ? '切片' : '录制' }}时长：</span>
                                <span class="gray-9 text-xs">{{ item?.durationStr || item?.duration }}</span>
                            </div>
                            <div class="flex items-start item-info">
                                <span class="font-bold">核心数据：</span>
                                <Popover :item="item"/>
                            </div>
                        </template>
                    </div>
                </div>
            </transition-group>

            <el-tooltip class="item" effect="dark" content="交换定位" placement="bottom">
                <i v-if="contrastList.length >= 2"
                   @click="exchangeContrast"
                   class="el-icon-sort cursor-pointer exchange-icon text-2xl"></i>
            </el-tooltip>

            <div class="text-center">
                <afp-button type="primary" :plain="false" size="default" class="btn-contrast" @click="startContrast">
                    开始对比
                </afp-button>
            </div>
        </div>
    </el-dialog>
</template>


<script>

import Popover from "@/views/modules/dataAnalysis/component/common/popover.vue";
import myUtils from "@/utils/utils";

export default {
    components: {Popover},
    props: {},
    data() {
        return {
            dialogVisible: false,
            innerVisible: false,
            contrastType: '',
            contrastList: []
        }
    },
    computed: {
        myUtils(){
          return myUtils
        },
        getTimeStr() {
            return (duration) => {
                return myUtils.toformatTimeChinse(duration * 1000)
            }
        }
    },
    methods: {
        open(vals, type) {
            this.contrastType = type
            this.contrastList = vals
            this.dialogVisible = true
        },

        exchangeContrast() {
            const list = [...this.contrastList];
            [list[0], list[1]] = [list[1], list[0]]
            this.contrastList = list
            this.$emit('swapObj')
        },
        startContrast() {
            this.innerVisible = true
        },
        addContrast() {
            this.contrastList = []
            this.dialogVisible = false
            this.innerVisible = false
            this.$emit('contrastSubmit')
        }
    }
}
</script>

<style lang="scss" scoped>
.review-contrast {
    ::v-deep(.el-dialog__header) {
        background: #f4f9ff;
        font-size: 16px;
        height: 46px;
        padding: 12px 20px;
    }

    ::v-deep(.el-dialog__body) {
        padding: 16px;
    }

    ::v-deep(.el-dialog__headerbtn) {
        top: 14px;
    }

    .labels {
        display: grid;
        grid-template-columns: 1fr 1fr;
        margin-bottom: 8px;

        .label {
            text-align: center;
            font-weight: 500;
        }
    }


    .cards {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 54px;
    }

    .contrast-item {
        width: 296px;
        background: #E0EFFF;
        border-radius: 10px 10px 10px 10px;
        border: 1px solid #79C5FF;
        padding: 12px;

        .item-child {
            height: 66px;
            background: rgba(255, 255, 255, 0.7);
            border-radius: 8px 8px 8px 8px;
            position: relative;
            padding-inline: 8px;

            .upload-dui-you {
                position: absolute;
                bottom: 14px;
                left: 35px;
            }
        }

        .item-info {
            margin-top: 8px;
        }
    }

    .benchmark-item {
        border-color: #FF9B70;
        background-color: #FFF2E7;
    }

    .contrast-move {
        transition: transform 0.4s ease;
    }

    /* 可选：进入/离开 */
    .contrast-enter-active,
    .contrast-leave-active {
        transition: opacity 0.2s ease;
    }

    .contrast-enter,
    .contrast-leave-to {
        opacity: 0;
    }

    .exchange-icon {
        margin: 10px 10px 10px 0;
        cursor: pointer;
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%) rotate(-90deg);
        color: #444DFF;
    }

    .compereImg {
        width: 39px;
        height: 39px;
        border-radius: 50%;
    }

    .btn-contrast {
        width: 200px;
        margin: 32px 0 20px 0;
    }
}

.inner-review-contrast {
    ::v-deep(.el-dialog__header) {
        background: #f4f9ff;
        font-size: 16px;
        height: 46px;
        padding: 12px 20px;
    }

    ::v-deep(.el-dialog__headerbtn) {
        top: 14px;
    }

    ::v-deep(.el-dialog__body) {
        padding: 12px 80px;
    }

    .inner-content {
        .inner-item {
            margin-top: 16px;
            line-height: 24px;
            display: flex;
            align-items: start;

            > span {
                padding-right: 8px;
            }
        }
    }

    .dialog-footer {
        margin-top: 32px;
        margin-bottom: 20px;
        color: #484A4C;
    }
}
</style>
