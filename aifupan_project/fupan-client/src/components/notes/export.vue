<template>
    <dialog-box :visible.sync="dialogVisible" title="导出笔记" class="share-window-dialog-box"
        :close-on-click-modal="false" custom-class="notes-export-dialog" @closed="hide" width="900px">
        <div style="margin: 0 auto;">
            <shareImgModel sponsorship="" ref="imgModel" width="794px">
                <template #title-after>
                    <div class="font-s12 flex-ji-c pd-t10 share-title-info">
                        <span class="pd-6 text-color3">直播间名称: <b class="text-colorc3">{{ dialogData.anchorName }}</b></span>
                        <span class="pd-6 text-color3">录制开始时间:{{ dialogData.startTime }}</span>
                        <span class="pd-6 text-color3">视频时长:{{ dialogData.duration }}</span>
                    </div>
                </template>
                <div class="pd-l10 pd-r10 notes-export-html-box">
                    <div style="width: 100%;" ref="htmlBox" v-html="getHtml"></div>
                </div>
            </shareImgModel>
            <div class="flex-jc-sb pd-t10">
                <div>
                    <span class="text-color3">文件名称：</span>
                    <el-input v-model="fileName" style="width: 450px;" placeholder="请输入文件名称"></el-input>
                </div>
                <div class="text-center">
                    <afp-button @click="downloadTxt">TXT</afp-button>
                    <!-- <afp-button @click="downloadWord">Word</afp-button> -->
                    <afp-button @click="downloadPDF">PDF</afp-button>
                    <afp-button type="primary" @click="downloadImg">图片</afp-button>
                </div>
            </div>
        </div>
    </dialog-box>
</template>

<script>
import DialogBox from '/src/components/dialog/index'
import dialogMixin from '/src/mixins/dialog';
import aiCanvasImage from '@/mixins/aiCanvasImage';
import shareImgModel from '/src/components/shareImgModel/index.vue';
import { downloadBlobTxt } from '@/utils/common';
import { exportHtmlToWord } from '@/utils/downFile';
export default {
    components: {
        DialogBox,
        shareImgModel
    },
    mixins: [dialogMixin, aiCanvasImage],
    props: {
    },
    data() {
        return {
            fileName: ''
        };
    },
    computed: {
        getFileName(){
            return this.fileName || this.dialogData?.name;
        },
        getHtml(){
            return this.dialogData.html;
            // return this.dialogData.html?.replace(/<span/g,'<div class="inlineDiv"').replace(/<\/span>/g,'</div>');
        }
    },
    watch: {},
    methods: {
        downloadTxt(){
            this.$nextTick(()=>{
                let txt = this.$refs.htmlBox?.innerText;
                if(this.$isAifupan){
                    this.frontUpload(new Blob([txt]),  this.getFileName, {notFolder: 0, uploadType: 0, generationType:1, extension:'.txt'});
                }else{
                    downloadBlobTxt(txt,{
                        fileName: this.getFileName
                    })
                }
            })
        },
        downloadWord(){
            this.$nextTick(()=>{
                this.$nextTick(()=>{
                    let html = this.$refs.htmlBox?.innerHTML;
                    if(this.$isAifupan){
                        exportHtmlToWord(html, this.getFileName, '.docx', {output: true})
                            .then(blob => {
                                this.frontUpload(blob, this.getFileName, {notFolder: 0, uploadType: 0, generationType:1, extension:'.docx'});
                            })
                    }else{
                        exportHtmlToWord(html, this.getFileName, '.docx')
                    }
                })
            })
        },
        downloadImg() {
            this.createImg(this.$refs.imgModel.getHtml,'download',{fileName: this.getFileName, typeName: ''})
        },
        downloadPDF(){
            this.createImg(this.$refs.imgModel.getHtml,'htmlPDF',{fileName: this.getFileName, typeName: ''})
        },
        onCancel() {
            this.hide();
        },
        hideCallback() { 
            document.body.classList.remove('notes-export-body');
        },
        async showCallback() {
            this.$nextTick(()=>{
                this.$refs.imgModel?.addWatermark();
            });
            this.fileName = this.dialogData?.name + this.dialogData?.typeName;
            document.body.classList.add('notes-export-body');
        },
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
<style>
.inlineDiv{
    display: inline !important;
}
</style>

<style lang='scss'>
.notes-export-body{
    .share-window-dialog-box {
        z-index: 2001 !important;
    }
    .notes-export-dialog{
        z-index: 2000 !important;
    }
    .v-modal {
        z-index: 1999 !important;
    }
}
.notes-export-html-box{
    // word-break: break-all;
    background: #fff;
    padding: 10px 0;
    min-height: 415px;
    span{
        // overflow: hidden;
        // text-overflow: ellipsis;
        // display: -webkit-box;
        // -webkit-line-clamp: 1;
        // -webkit-box-orient: vertical;
        // display: inline !important;
        // box-decoration-break: clone;
        // -webkit-box-decoration-break: clone;
        // background-clip: padding-box;
        // position: relative;
        // z-index: 4;
    }
    *{
        // word-break: break-all;
    }
}

</style>