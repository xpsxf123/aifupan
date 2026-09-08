<template>
    <dialog-box :visible.sync="dialogVisible" title="AI诊断报告" v-if="dialogVisible"
                :class="{'ai-report-dialog-isReport':isReport,'share-window-dialog-box':true}"
                :close-on-click-modal="false" :destroy-on-close="true" :modal="!isReport" width="900px">
        <ShareImgModel ref="aiShareImgBox" :sponsorship="sponsorship">
            <template #title-after v-if="showHeaderInPdf">
                <div class="font-s12 flex-ji-c pd-t10">
                     <span class="pd-6 text-color3">
                         直播间名称: <b class="text-colorc3">{{ selectedRow.anchorInfo?.anchorName }}</b></span>
                    <span class="pd-6 text-color3">录制开始时间:{{ selectedRow.startTime }}</span>
                    <span class="pd-6 text-color3">视频时长:{{ selectedRow.durationStr }}</span>
                </div>
            </template>
            <div v-for="item in renderList" :key="item.name">
                <div class="ai-report-title"
                     style="font-size: 24px;padding: 24px;background: #fff;color:#000;font-weight: bold;text-align: center">
                    {{ item.label }}
                </div>
                <aiContent class="main-bg" :els="item.list" readonly type="assistant"></aiContent>
            </div>
        </ShareImgModel>
        <div class="footer">
            <afp-button size="medium" @click="exportImage" :loading="exportImageLoading">下载图片</afp-button>
        </div>
    </dialog-box>
</template>

<script>
import {isEmpty} from "lodash";
import aiCanvasImage from '@/mixins/aiCanvasImage'
import DialogBox from '/src/components/dialog/index'
import ShareImgModel from '/src/components/shareImgModel/index.vue'
import AiContent from '@/components/analysis/ai/common/aiContent.vue'

export default {
    name: 'DialogAiContent',
    components: {
        AiContent,
        ShareImgModel,
        DialogBox
    },
    mixins: [aiCanvasImage],
    props: {
        tabsList: {
            type: Array,
            default: () => {
                return []
            }
        },
        sponsorship: {
            type: [String, Array],
            default: ''
        },
        isReport: {
            type: Boolean,
            default: false
        },
        selectedRow: {
            type: Object,
            default: () => {
                return {}
            }
        },
        hideReportModal: {
            type: Function,
            default: () => {
            }
        },
        uploadType: {
            type: [String, Number],//1内容报告 2数据诊断报告
            default: ''
        }
    },
    data() {
        return {
            dialogVisible: false,
            dialogData: {},
            renderList: {},
            exportImageLoading:false,
            notFolder: 0,//打开文件夹 0打开 1不打开
        }
    },
    computed: {
        showHeaderInPdf() {
            return this.selectedRow.startTime && this.selectedRow.durationStr && !isEmpty(this.selectedRow.anchorInfo)
        }
    },
    watch: {},
    methods: {
        changeDialogVisible(tab, ids, status, notFolder) {
            this.renderList = {}
            this.dialogVisible = status
            this.notFolder = notFolder || 0
            this.getRenderStructureData(tab, ids)
        },
        closeDialog() {
            this.dialogVisible = false
            this.hideReportModal?.(false)
        },
        // frontUploadData(file) {
        //     let formData = new FormData();
        //     formData.append("file", file, `${this.selectedRow?.outputName || this.selectedRow.reportFileName}.pdf`);
        //     formData.append("uploadType", 1);//1诊断报告-pdf
        //     formData.append("otherObj", JSON.stringify({
        //         videoId: this.selectedRow?.videoId,
        //         notFolder: this.notFolder
        //     }));
        //     formData.append("generationType", 0);
        //
        //     this.$httpClient.uploadFile.frontUpload(formData).then(res => {
        //         if (res.code === 0 && this.notFolder === 0) {
        //             this.$message.success('pdf文件导出成功')
        //         }
        //     }).finally(() => {
        //         this.closeDialog()
        //     })
        // },
        async exportImage() {
            this.exportImageLoading = true
            const dom = await this.$refs.aiShareImgBox.getShareHtml()
            await this.createImg(dom, 'download', {fileName: this.selectedRow?.outputName || this.selectedRow.reportFileName})
            this.exportImageLoading = false
        },
        async getRenderStructureData(list, ids) {
            const renderList = {}
            for (let i = 0; i < list.length; i++) {
                const selectIdList = ids[list[i].name]
                if (selectIdList?.length > 0) {
                    renderList[list[i].name] = {
                        label: list[i].tagName,
                        ids: selectIdList,
                        list: []
                    }
                }
            }
            const cueWordsIds = Object.values(ids).flat(Infinity)
            const {data: QAList, code} = await this.$httpBack.v2500.conversationByCueWordsIds({
                videoId: this.selectedRow?.videoId,
                tradeId: this.selectedRow?.tradeId,
                cueWordsIds
            }, {load: this.notFolder !== 1})
            if (code !== 0) return
            for (const key in renderList) {
                if (renderList.hasOwnProperty(key)) {
                    const renderListIds = new Set(renderList[key].ids || [])

                    // 渲染的pdf根据tabsList===>sort排序
                    const currentSortItems = this.tabsList.find(a => a.name === key)
                    const {cueWordsList = []} = currentSortItems || {}
                    const list = QAList.filter(item => renderListIds.has(item.cueWordsId))
                    list.forEach(a => {
                        const match = cueWordsList?.find(word => word.cueWordsId === a.cueWordsId);
                        if (!isEmpty(match)) a.sort = match.sort;
                    });

                    renderList[key].list = list.sort((a, b) => a.sort - b.sort);
                }
            }
            this.renderList = renderList
            this.$nextTick(() => {
                if (this.isReport) {
                    this.$nextTick(async () => {
                        const dom = await this.$refs.aiShareImgBox.getHtml
                        await this.createImg(
                            dom,
                            'htmlPDF',
                            {
                                fileName: this.selectedRow?.outputName || this.selectedRow.reportFileName,
                                html: dom.innerHTML,
                                output: true,
                                params: {
                                    uploadType: this.uploadType,
                                    otherObj: {sourceId: this.selectedRow?.videoId, notFolder: this.notFolder}
                                }
                            }
                        )
                    })
                }
            })
        },
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
.ai-report-dialog-isReport {
    z-index: -11 !important;
    opacity: 0 !important;
    top: 9999px;
}

.footer {
    padding-top: 20px;
    text-align: center;
}
</style>
