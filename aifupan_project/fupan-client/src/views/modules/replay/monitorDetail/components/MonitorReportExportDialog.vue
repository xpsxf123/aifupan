<template>
    <dialog-box
        :visible.sync="dialogVisible"
        title="导出报告预览"
        class="share-window-dialog-box"
        :close-on-click-modal="false"
        width="900px"
    >
        <div>
            <shareImgModel ref="imgModel" sponsorship="">
                <template #title-after>
                    <div class="font-s12 flex-ji-c pd-t10">
                        <span class="pd-6 text-color3">直播间名称: <b class="text-colorc3">{{ anchorName }}</b></span>
                        <span class="pd-6 text-color3">录制开始时间:{{ startTime }}</span>
                        <span class="pd-6 text-color3">视频时长:{{ duration }}</span>
                    </div>
                </template>
                <div class="main-bg pd-l10 pd-r10 monitor-export-content">
                    <div v-if="isScriptRestoration">
                        <div
                            v-for="(item, index) in safeContentList"
                            :key="`scriptRestoration-${index}`"
                            :class="`scriptRestoration-${index + 1}`"
                            class="pd-t4 pd-b4"
                        >
                            <AiContentRenderer
                                :content="item"
                                render-mode="mdTag"
                                :option="{ forceStyle: true, upgradePlainTable: true }"
                                :data-block-scope="`monitor-report-export-${index}`"
                            />
                        </div>
                    </div>
                    <div v-else>
                        <ScriptQualityReportContent :content="content" />
                    </div>
                </div>
            </shareImgModel>
            <div class="flex-jc-sb pd-t10">
                <div style="width: 50%;">
                    <el-input v-model="localFileName" style="width: 100%;" size="mini" placeholder="请输入导出文件名称" class="pd-l10" />
                </div>
                <div class="share-window-action-group">
                    <el-dropdown trigger="hover" placement="bottom-end" :popper-append-to-body="true" @command="handlePdfExportCommand">
                        <afp-button>
                            <span>导出PDF</span>
                            <i class="el-icon-arrow-down el-icon--right"></i>
                        </afp-button>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="vector">
                                <el-tooltip
                                    effect="dark"
                                    placement="bottom"
                                    content="矢量导出：调用系统打印能力生成PDF，还原度高（约90%+），可编辑，文件相对更小，适合大多数场景。">
                                    <span>矢量导出</span>
                                </el-tooltip>
                            </el-dropdown-item>
                            <el-dropdown-item command="image">
                                <el-tooltip
                                    effect="dark"
                                    placement="bottom"
                                    content="图片导出：先生成长图再裁剪为PDF，还原度最高（接近100%），但可能文件更大且页数/长度受限。">
                                    <span>图片导出</span>
                                </el-tooltip>
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                    <el-dropdown trigger="hover" placement="bottom-end" :popper-append-to-body="true" @command="handleImgExportCommand">
                        <afp-button type="primary">
                            <span>导出图片</span>
                            <i class="el-icon-arrow-down el-icon--right"></i>
                        </afp-button>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="download">下载图片</el-dropdown-item>
                            <el-dropdown-item command="copy">复制图片</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </div>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import shareImgModel from '/src/components/shareImgModel/index.vue'
import aiCanvasImage from '@/mixins/aiCanvasImage'
import ScriptQualityReportContent from '@/components/aiMonitor/scriptQualityReportContent/index.vue'
import AiContentRenderer from '@/components/aiContentRenderer/index.vue'

export default {
    name: 'MonitorReportExportDialog',
    components: {
        DialogBox,
        shareImgModel,
        ScriptQualityReportContent,
        AiContentRenderer
    },
    mixins: [aiCanvasImage],
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        reportType: {
            type: String,
            default: 'scriptQuality'
        },
        sentenceMarkData: {
            type: Object,
            default: () => ({})
        },
        content: {
            type: [String, Array, Object],
            default: ''
        },
        contentList: {
            type: Array,
            default: () => []
        },
        fileName: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            localFileName: this.fileName || ''
        }
    },
    computed: {
        dialogVisible: {
            get() {
                return this.visible
            },
            set(val) {
                this.$emit('update:visible', val)
            }
        },
        isScriptRestoration() {
            return this.reportType === 'scriptRestoration'
        },
        safeContentList() {
            return Array.isArray(this.contentList) ? this.contentList : []
        },
        anchorName() {
            const anchorInfo = this.sentenceMarkData?.anchorInfo || {}
            return anchorInfo?.anchorName || anchorInfo?.AnchorName || this.sentenceMarkData?.anchorName || '-'
        },
        startTime() {
            return this.sentenceMarkData?.startTime
                || this.sentenceMarkData?.StartTime
                || this.sentenceMarkData?.videoInfo?.startTime
                || this.sentenceMarkData?.videoInfo?.StartTime
                || this.sentenceMarkData?.fileInfo?.UploadTime
                || '-'
        },
        duration() {
            return this.sentenceMarkData?.durationStr
                || this.sentenceMarkData?.DurationStr
                || this.sentenceMarkData?.videoInfo?.durationStr
                || this.sentenceMarkData?.videoInfo?.DurationStr
                || this.sentenceMarkData?.videoInfo?.Duration
                || this.sentenceMarkData?.fileInfo?.fileDuration
                || '-'
        }
    },
    watch: {
        visible(val) {
            if (!val) return
            this.$nextTick(() => {
                this.$refs?.imgModel?.addWatermark?.()
            })
        },
        fileName: {
            immediate: true,
            handler(val) {
                if (val) this.localFileName = val
            }
        }
    },
    methods: {
        handlePdfExportCommand(command) {
            if (command === 'vector') {
                this.createImg(this.$refs.imgModel.getHtml, 'htmlPDF', { fileName: this.localFileName })
                return
            }
            if (command === 'image') {
                this.createImg(this.$refs.imgModel.getHtml, 'pdf', { fileName: this.localFileName })
            }
        },
        handleImgExportCommand(command) {
            if (command === 'download') {
                this.createImg(this.$refs.imgModel.getHtml, 'download', { fileName: this.localFileName })
                return
            }
            if (command === 'copy') {
                this.createImg(this.$refs.imgModel.getHtml, 'copy')
            }
        }
    }
}
</script>

<style lang="scss">
.share-window-dialog-box {
    .monitor-export-content {
        min-height: 200px;
    }
}
</style>
