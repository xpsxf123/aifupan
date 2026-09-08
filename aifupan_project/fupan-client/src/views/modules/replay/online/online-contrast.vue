<template>
    <div>
        <CoreTable :table-height="tableH || `calc(100vh - 208px)`"
        :notSearch="iframeNotLogin"
        :notPage="iframeNotLogin"
        :buffer="isIframe?'online-offcial-contrast-list':'online-contrast-list-'+ getTabsName" 
        :searchConfig="formConfig" :menuConfig="menuConfig"
            :getDataApi="getContrastList" ref="table" :column="column" :table-config="{
                'show-overflow-tooltip': false
            }">
            <template #btns>
                <afp-button type="primary" plain @click="contrastDialog"  size="default">新增对比分析</afp-button>
            </template>
            <template #searchRight>
                <div v-if="space">容量：{{ space }}
                </div>
            </template>
            <template #table="param">
                <slot name="table" v-bind="param"></slot>
            </template>
            <template #anchorVideoOne="{ row: item }">
                <AnchorName
                    :anchorVideo="{...item.videoOneInfo,optimize:true,syncScene:item.syncScene}"
                    :anchorInfo="item.anchorOneInfo"
                    :fileInfo="item.fileOneInfo">
                </AnchorName>
            </template>
            <template #anchorVideoTwo="{ row: item }">
                <AnchorName
                    :anchorVideo="{...item.videoTwoInfo,optimize:false,syncScene:item.syncScene}"
                    :anchorInfo="item.anchorTwoInfo"
                    :fileInfo="item.fileTwoInfo">
                </AnchorName>
            </template>
            <template #updateDate="{ row: item }">
                <div class="analysisDateColContainer">
                    <div>{{ item.updateDate.substring(0, 10) }}</div>
                    <div>{{ item.updateDate.substring(10) }}</div>
                </div>
            </template>
            <template #menu="{ row,menu }">
                <template v-for="option in menu.options">
                    <Operation :options="option" :data="row"></Operation>
                </template>
            </template>
            <template #empty>
                <slot name="empty"></slot>
            </template>
            <template #page-before>
                <div v-if="$isAifupan" class="font-s12 flex-ji-c text-colorTheme cs-p" @click="toOfficialWebsite">
                    <img src="@/assets/imgs/rightgif.gif" style="max-width: 24px;" alt="" srcset="">
                    <span style="width: 200px;">前往官网，可进行网页查看云空间</span>
                </div>
            </template>
            <template #page-after>
                <div style="width: 230px;"></div>
            </template>
        </CoreTable>
        <FileRemark ref="fileRemark"/>
        <ContrastDialog ref="ContrastDialog" :replayType="getTabsName" :type="getType" :compereList="compereList" @contrastSubmit="getList"></ContrastDialog>
    </div>
</template>

<script>
import CoreTable from '@/components/coreTable/index.vue'
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue'
import AnchorName from './anchorName.vue';
import commonHttp from '@/mixins/commonHttp';
import commonUtils from '@/utils/common.js';
import ContrastDialog from './contrastDialog.vue';
import {toOfficialWebsite } from '@/utils/common';
import tabsMixin from '@/mixins/tabs';
import myUtils from "@/utils/utils";
import Operation from "@/components/Table/operation.vue";
import fileRemarkMixin from "@/components/fileRemark/mixin";
export default {
    name: 'ReplayClientAnalysisFinishList',
    components: {
        Operation,
        CoreTable,
        Anchor,
        AnchorName,
        ContrastDialog
    },
    inject: ['appVnode'],
    mixins: [commonHttp,tabsMixin,fileRemarkMixin],
    props: {
        tableH: {
            type:String,
            default: ''
        },
        isIframe: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            formConfig: {
                items: [
                    {
                        label: '上传账号', prop: 'userKeyword',
                        placeholder: "输入上传账号",
                        config: {
                            style: {
                                width: '150px'
                            }
                        },
                    },

                ],
            },
            // 
            column: [
                {
                    label: '对比1',
                    prop: 'anchorVideoOne',
                    align: 'left'
                },
                {
                    label: '对比2',
                    prop: 'anchorVideoTwo',
                    align: 'left'
                },
                {
                    label: '对比时间',
                    prop: 'updateDate'
                },
                {
                    label: '文件备注',
                    prop: 'fileRemark',
                    formatter: (row) => {
                        return '查看';
                    },
                    option: {
                        classNameFn: (row) => {
                            return row.cloudRemarks ? 'color-main cursor-pointer' : 'text-color3';
                        },
                        on: {
                            click: (e, row) => {
                                if (row.cloudRemarks) {
                                    this.onClickFileRemark(row);
                                }
                            }
                        }
                    }
                },
                {
                    label: '上传账号',
                    prop: 'userNickName'
                }
            ],
            // 
            menuConfig: {
                width: '310px',
                options: [
                    [{
                        label: "查看对比分析",
                        click: (item) => {
                            if (this.isIframe) {
                                window.open(commonUtils.copyShareUrl(item.shareUrl), "_blank");
                                return
                            }
                            this.toContrast(item.contrastId)
                        }
                    }, {
                        label: "复制分享链接",
                        hidden: (item) => {
                            return item.isShard !== 1
                        },
                        click: (item) => {
                            if (item.isShard === 1) {
                                let shareUrl = item.shareUrl
                                if (!item.shareUrl) {
                                    shareUrl = commonUtils.assemblyShareUrl(`contrastOnlineAnalysis/${item.contrastId}`)
                                }
                                this.copyShareAnalysisLink(shareUrl)
                            }
                        }
                    }],
                    [{
                        label: "变换定位",
                        hidden: (item) => {
                            if (this.isIframe) {
                                return true
                            }
                            return item.userId !== this.$store?.state?.userInfo?.id;
                        },
                        click: (item) => {
                            this.toSwitchContrast(item.contrastId)
                        }
                    },{
                        label: (item) => {
                            if (item?.hasStar === 0) {
                                const unStartImg = require('@/assets/imgs/unStart.png')
                                return `<img src="${unStartImg}" style="width: 25px;height: 25px" alt="">`
                            } else {
                                const startImg = require('@/assets/imgs/start.png')
                                return `<img src="${startImg}" style="width: 25px;height: 25px" alt="">`
                            }
                        },
                        dangerouslyUseHTMLString:true,
                        classNameFn: (item) => {
                            if (item?.hasStar !== 0) {
                                return 'text-y'
                            }
                            return ''
                        },
                        type: 'text',
                        click: (item) => {
                            this.changeStart(item)
                        },
                        hidden: () => {
                            return this.isIframe;
                        },
                        disabled: (item) => {
                            if (this.isIframe) return true
                            if (item.hasStar) {
                                const {userType, id: currentUserId} = this.$store?.state?.userInfo;
                                if (userType === 0) {
                                    return false
                                } else {
                                    return item?.sourceStarInfo?.userId !== currentUserId
                                }
                            }else {
                                return false
                            }
                        },
                    }, {
                        label: "删除",
                        type: 'danger',
                        hidden: (item) => {
                            if (this.isIframe) {
                                return true
                            }
                            return item.userId !== this.$store?.state?.userInfo?.id;
                        },
                        disabled: (item) => {
                            return item.hasStar === 1
                        },
                        click: (item) => {
                            this.deleteShare(item)
                        }
                    }]
                ]
            },
            compereList: [],
            tabs: [
                {
                    label: '整场复盘',
                    name: 'replayAll'
                },
                // {
                //     label: '文件复盘',
                //     name: 'b'
                // },
                {
                    label: '切片复盘',
                    name: 'replaySection'
                }
            ]
        };
    },
    computed:{
        iframeNotLogin(){
            return this.isIframe && !this.$store?.state?.userInfo?.id;
        },
        userProperty(){
            return this.$store.getters.getUserproperty
        },
        space(){
            if(this.userProperty?.totalStorageNum === undefined || this.userProperty?.storageNum === undefined){
                return '0/0 G';
            }
            let a = (this.userProperty?.totalStorageNum - this.userProperty?.storageNum) / 1024 / 1024;
            let b = this.userProperty?.totalStorageNum / 1024 / 1024;
            return `${this.retainDecimals(a)} / ${this.retainDecimals(b)} G`
        },
        getParams(){
            // 云空间
            if(this.isIframe){
                return {
                    contrastType: 0,
                    sliceContrastType: 0
                }
            }
            if(this.getTabsName === 'replaySection'){
                return {
                    sliceContrastType: 1,
                    contrastType: 0
                }
            }else{
                return {
                    sliceContrastType: 0,
                    contrastType: this.getTabsName === 'replayAll' ? 0 : 1
                }
            }
        },
        getType(){
            return this.getTabsName === 'b'?'file': ''
        },
        getDialogParams(){
            let param = {
                videoSliceType: 0,
            };
            switch(this.getTabsName){
                case 'replayAll':
                    param.videoSliceType = 0;
                    break;
                case 'b':
                    break;
                case 'replaySection':
                    param.videoSliceType = 1;
                    break;
            }
            return param;
        }
    },
    mounted() {
        this.initVideoList();
        // this.getCList();
        this.initTabs();
    },
    
    methods: {
        initTabs(){
            this.setTabs(this.tabs);
            this.tabsClickCallbackHandler()
            // const {query} = this.$route
            // if (query.type) {
            //     this.setTabsName(query.type)
            // }
        },
        getUserProperty(){
            this.appVnode?.getUserproperty?.();
        },
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
        async changeStart(item) {
            try {
                const httpServer = item?.hasStar === 1 ? this.$httpBack.v2500.cancelStar : this.$httpBack.v2500.addStar
                const result = await httpServer({
                    sourceId: item.contrastId,
                    sourceType: 2
                })
                if (result.code === 0) {
                    this.$refs.table.getList();
                    return this.$message.success(`${item.hasStar === 1 ? '取消' : '添加'}星标成功`)
                }
            } catch (e) {
            }
        },
        // async getCList () {
        //     let cList = await this.getCompereBackList();
        //     this.setFormConfigDic({0:cList.map(d=>{
        //         return d?.anchorInfo;
        //     })},this.formConfig,{
        //         secUid:(opts,form)=>{
        //             let a = [].concat(form.config?.options?.shift(),opts);
        //             this.compereList = a;
        //             return a;
        //         }
        //     })
        // },
        contrastDialog(){
            this.$refs.ContrastDialog.show({data:this.getDialogParams});
        },
        toOfficialWebsite(){
            toOfficialWebsite(this.$httpClient);
        },
        async initVideoList(){
            let cList = await this.clientTenantAnchorList();
            let anchor = await this.listByTenantAnchor();
            this.setFormConfigDic({0:anchor,1:cList?.map(d=>d.anchorInfo)},this.formConfig,{
                tradeId:(opts,form)=>{
                    return [].concat(form.config?.options?.shift(),opts);
                }
            })
        },
        preview(videoId) {
            this.$httpClient.video.preview({ videoId }).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, "_blank");
                }
            });
        },
        toSwitchContrast(contrastId) {
            this.$httpBack.contrast.switchContrastPosition({contrastId}).then((res) => {
                if (res.code == 0) this.getList()
            })
        },
        // 删除分析
        deleteShare(item) {
            let size1 = parseInt(item.videoOneInfo.vedioSizie);
            let size2 = parseInt(item.videoTwoInfo.vedioSizie);
            let size = Math.floor((size1 + size2) / 1024 / 1024);
            this.$confirm('将删除在线对比复盘，并将视频从云空间清除，预计腾出' + size + 'M空间，是否继续?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.$httpBack.contrast.clientDeleteCloudContrast({ contrastId: item.contrastId }).then(res => {
                    if (res.code == 0) {
                        this.$message.success("删除成功");
                        this.$refs.table.getList();
                        // this.$emit("updateProperty");
                        this.getUserProperty()
                    }
                })
            });
        },
        // 跳转到对比页
        toContrast(contrastId) {
            this.$router.push({ path: this.$route.path + '/contrast', query: { "contrastId": contrastId } });
        },
        // 复制分享链接
        copyShareAnalysisLink(shareUrl) {
            commonUtils.copyShareUrl(shareUrl, this.$message.success)
        },
        // 切换tab跟新数据
        tabsClickCallbackHandler(){
            this.setTabsClickCallback((tabName)=>{
                this.$nextTick(()=>{
                    this.initGetList();
                })
            })
        },
        // 获取对比页列表数据
        getContrastList(param) {
            let requestData = {
                page:param.pageIndex,
                limit: param.pageSize,
                contrastStartDate: param.searchDate?.[0] || '',
                contrastEndDate: param.searchDate?.[1] || '',
                ...param,
                ...this.getParams
                // secUid: '',
            }
            delete requestData.searchDate;
            return this.$httpBack.contrast.clientListCloudContrast(requestData, {load: false}).then(res => {
                if (res.code === 0) {
                    this.totalCount = res.data.totalPage;
                    let contrastList = res.data.list;
                    contrastList?.forEach(item => {
                        if (item.videoOneId && item.videoTwoId) {
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.replaceAll("AF_", "");
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.replaceAll(".ts", "");
                            item.videoOneInfo.videoName = item.videoOneInfo.videoName.substring(item.videoOneInfo.videoName.indexOf("_") + 1);
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.replaceAll("AF_", "");
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.replaceAll(".ts", "");
                            item.videoTwoInfo.videoName = item.videoTwoInfo.videoName.substring(item.videoTwoInfo.videoName.indexOf("_") + 1);
                        }
                    });
                    // this.contrastList = contrastList;
                }
                return res;
            })
        },
        initGetList(){
            this.$refs?.table?.search?.({});
        },
        getList(type){
            this.$refs.table?.getList?.()
        }
    },
};
</script>

<style scoped lang="less">
.analysisDateColContainer {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 2;
    font-weight: 400;
    font-size: 13px;
    color: #2E3742;
}
</style>