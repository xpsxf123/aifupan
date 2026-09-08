<template>
    <div class="common-bg">
        <!-- 面板页 -->
        <div v-if="currentPage == 'tabs'">
            <Tabs class="main-bg" style="padding: 20px 20px 0 20px;margin-bottom: 20px;" v-model="currentMenuValue"  :tabs="menuList"></Tabs>
            <!-- <el-tabs v-model="currentMenuValue">
                <el-tab-pane v-for="item in menuList" :key="item.value" :label="item.label"
                    :name="item.name"></el-tab-pane>
            </el-tabs> -->
            <div class="replay-content">
                <!-- 全部 -->
                <record-list v-if="currentMenuValue == 'all'" @toPage="toPage"></record-list>
                <!-- 文件上传 -->
                <file-upload v-if="currentMenuValue == 'file'" @toPage="toPage" @toMenu="toMenu"></file-upload>
                <!-- 已完成分析 -->
                <analysis-finish-list v-if="currentMenuValue == 'finish'" @toMenu="toMenu"
                    @toPage="toPage"></analysis-finish-list>
                <!-- 对比分析列表 -->
                <analysis-contrast-list v-if="currentMenuValue == 'compare'"></analysis-contrast-list>
                <!-- 分享复盘 -->
                <anchor-contrast v-if="currentMenuValue == 'online'"></anchor-contrast>
            </div>
        </div>
        <!-- xz -->
        <analysis v-if="currentPage == 'analysis'" @toPage="toPage" @showUploadPop="showUploadPop"></analysis>
        <!-- 上传文件分析页 -->
        <!-- <upload-file-analysis v-if="currentPage == 'fileUploadAnalysis'" @toPage="toPage"></upload-file-analysis> -->

        <!-- 在线复盘弹窗 -->
        <online-analysis-dialog v-if="onlineAnalysisDialogVisible" ref="onlineAnalysisDialog"></online-analysis-dialog>
    </div>
</template>

<script>
import recordList from "./record-list.vue";

import analysisFinishList from "./analysis/analysis-finish-list.vue";
import analysis from "./analysis/index.vue";
import analysisContrastList from "./contrast/analysis-contrast-list.vue";
import fileUpload from "./fileUpload/index.vue";
import uploadFileAnalysis from "./fileUpload/file-upload-analysis.vue";
import anchorContrast from "./online/index.vue"
import Tabs from '/src/components/Tabs/index.vue';
import OnlineAnalysisDialog from '@/views/commonComponent/onlineAnalysisDialog.vue';
export default {
    components: {
        recordList,
        analysisFinishList,
        analysis,
        analysisContrastList,
        fileUpload,
        uploadFileAnalysis,
        anchorContrast,
        Tabs,
        OnlineAnalysisDialog
    },
    data() {
        return {
            currentPage: "tabs",
            currentMenuValue: 'all',
            menuList: [
                { label: "全部", name: 'all' },
                // { label: "播前分析", name: 'file' },
                { label: "已完成分析", name: 'finish' },
                { label: "智能对比分析", name: 'compare' },
                // { label: "分享复盘", name: 'online' },
            ],
            onlineAnalysisDialogVisible: false
        };
    },

    mounted() {
        let replayPageMenu = this.$store.state.replayPageMenu;
        if (replayPageMenu) {
            this.currentMenuValue = replayPageMenu.menu;
            this.currentPage = replayPageMenu.page;
            this.$store.commit("saveReplayPageMenu", null);
        }
    },

    methods: {
        // showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {
        //     this.$emit("showUploadPop", userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo)
        // },
        showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {
            this.onlineAnalysisDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.onlineAnalysisDialog.init(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo);
            })
        },
        toMenu(menu) {
            this.currentMenuValue = menu;
        },
        toPage(url) {
            this.currentPage = url;
        }
    },
};
</script>

<style scoped lang="less">
/deep/ .el-tabs__header {
    margin: 0px;
}
.replay-content{
    height: calc(100vh - 110px);
    padding: 20px;
    padding-bottom: 0;
    padding-top: 0px;
    box-sizing: border-box;
    overflow: hidden;
    overflow-y: auto;
}
</style>