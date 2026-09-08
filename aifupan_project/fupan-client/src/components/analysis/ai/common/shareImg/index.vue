<template>
    <dialog-box :visible.sync="dialogVisible" title="分享图片预览" class="share-window-dialog-box"
        :close-on-click-modal="false" width="900px">
        <div>
            <shareImgModel ref="imgModel" :sponsorship="getSponsorship">
                <template #title-after>
                    <div class="font-s12 flex-ji-c pd-t10">
                        <span class="pd-6 text-color3">直播间名称: <b class="text-colorc3">{{ dialogData.anchorName }}</b></span>
                        <span class="pd-6 text-color3">录制开始时间:{{ dialogData.startTime }}</span>
                        <!-- <span class="pd-6 text-color3">录制结束时间:{{ dialogData.endTime }}</span> -->
                        <span class="pd-6 text-color3">视频时长:{{ dialogData.duration }}</span>
                    </div>
                </template>
                <aiContent ref="htmlBox" class="main-bg" :els="dialogData.list" readonly></aiContent>
            </shareImgModel>
            <div class="flex-jc-sb pd-t10">
                <div style="width: 50%;">
                    <el-input v-model="dialogData.imgName" style="width: 100%;" size="mini" placeholder="请输入导出文件名称" class="pd-l10" />
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
                    <!-- <afp-button @click="downloadWord">导出Word</afp-button> -->
                    <afp-button v-if="isDev" @click="downloadHtml">下载html文件</afp-button>
                </div>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import aiContent from './../aiContent.vue';
import aiCanvasImage from '@/mixins/aiCanvasImage';
import shareImgModel from '/src/components/shareImgModel/index.vue';
import env from '/src/env';
import downloadHtml from '@/utils/downloadHtml';
import { exportHtmlToWord } from '@/utils/downFile';
export default {
    components: {
        DialogBox,
        aiContent,
        shareImgModel
    },
    mixins: [dialogMixin, aiCanvasImage],
    props: {
    },
    data() {
        return {

        };
    },
    computed: {
        isDev(){
            return env.dev;
        },
        getSponsorship(){
            return this.dialogData?.modelName;
        }
    },
    watch: {},
    methods: {
        handlePdfExportCommand(command){
            if (command === 'vector') {
                this.downloadPDF();
                return;
            }
            if (command === 'image') {
                this.downloadPDFByImg();
            }
        },
        handleImgExportCommand(command){
            if (command === 'download') {
                this.downloadImg();
                return;
            }
            if (command === 'copy') {
                this.copyImg();
            }
        },
        downloadWord(){
            this.$nextTick(()=>{
                this.$nextTick(()=>{
                    let html = this.$refs.htmlBox?.$el?.innerHTML;
                    if(this.$isAifupan){
                        exportHtmlToWord(html, this.getFileName, '.docx', {output: true})
                            .then(blob => {
                                this.frontUpload(blob, this.dialogData?.imgName, {notFolder: 0, uploadType: 0, generationType:1, extension:'.docx'});
                            })
                    }else{
                        exportHtmlToWord(html, this.dialogData?.imgName, '.docx')
                    }
                })
            })
        },
        copyImg() {
            this.createImg(this.$refs.imgModel.getHtml,'copy')
        },
        downloadImg() {
            this.createImg(this.$refs.imgModel.getHtml,'download',{fileName: this.dialogData?.imgName})
        },
        downloadPDF(){
            this.createImg(this.$refs.imgModel.getHtml,'htmlPDF',{fileName: this.dialogData?.imgName})
        },
        downloadPDFByImg(){
            this.createImg(this.$refs.imgModel.getHtml,'pdf',{fileName: this.dialogData?.imgName})
        },

        onCancel() {
            this.hide();
        },
        async showCallback() {
            
        },
        downloadHtml(){
            const html = downloadHtml(this.$refs.imgModel?.$el?.innerHTML);
            return
            const url = window.URL.createObjectURL(new Blob([html]));
            
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', this.dialogData?.imgName+'.html'); // 设置下载文件名
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
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
<style lang='scss'>
.share-window-dialog-box {
    .share-window-action-group{
        display: inline-flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
        justify-content: flex-end;
    }
    .img-window-box {
        height: 70vh;
    }
    .ai-box {
        height: auto;
        .deepThinking {
            display: none;
        }
        .deepThinkingTitle {
            display: none;
            padding: 0;
            .font_family {
                // display: none;
            }
        }
    }

    .ai-content-box-left {
        background: #fff;
    }
}
</style>
