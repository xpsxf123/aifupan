<template>
    <div>
        <div  v-if="isLayoutModel" class="layout-box layout-box-h" @contextmenu="preventRightClick" >
            <el-container>
                <el-aside width="150px" class="left-box">
                    <el-row style="display: flex;" class="flex-column">
                        <el-col :class="{'logo-box':versionType!==VERSION_TYPE.PURE}" :span="24">
                            <!-- logo -->
<!--                            <img v-if="clickCount<3" src="@/assets/imgs/1_9_30/logo.png" @click="clickHide" class="logoImg">-->
                            <div v-if="clickCount<3" class="logoImg" @click="clickHide" style="position: relative">
                                <img src="@/assets/imgs/2_6_3/lu-logo.png" alt="" class="lu-logo"
                                     v-if="versionType === VERSION_TYPE.PURE && isPackagePure">
                            </div>
                            <!-- 显示用户当前版本 -->
                            <div class="logoImgAddress" :class="{'pd-t30': clickCount>=3}">
                                <img v-if="getUserInfo.logoImgAddress" :src="getUserInfo.logoImgAddress"
                                    style="width: 90px">
                            </div>
                            <div class="logoImgAddress" style="margin-bottom: 0"
                                 v-if="isPackageFree&&versionType === VERSION_TYPE.PURE">
                                <div class="version cursor-pointer" @click="versionChange">
                                    <i class="icon text-xs el-icon-sort"></i>
                                    <span class="text-xs" style="color: #515C73;">
                                        切换成AI全能版
                                    </span>
                                </div>
                            </div>
                        </el-col>
                        <el-col class="nav-box" :span="24">
                            <!-- 导航栏 -->
                            <Nav ref="nav" :style="{zoom:getZoom>1?1:getZoom}"></Nav>
                        </el-col>
                        <el-col class="desk-box" :span="24">
                            <!-- 磁盘空间 -->
                            <Disk ref="disk"></Disk>
                        </el-col>
                        <el-col class="version-box" :span="24">
                            <!-- 版本号 -->
                            <Version></Version>
                        </el-col>
                    </el-row>
                </el-aside>
                <el-container direction="vertical">
                    <el-header class="el-header-dom" height="48px" v-if="isShowTabs">
                        <el-row>
                            <el-col :span="24">
                                <div class="title-box">
                                    <Title>
                                        <template #title-right-after>
                                            <!-- 临时关闭方案 -->
<!--                                            <div class="windowCtrlContainer">-->
<!--                                                <div class="windowCtrlContainer-content">-->
<!--                                                    <img class="windowCtrlImg" src="@/assets/imgs/min.png"-->
<!--                                                        @click="minsize" />-->
<!--                                                    <img class="windowCtrlImg" v-if="togglemaxsizeFlag == 'normal'"-->
<!--                                                        src="@/assets/imgs/max.png" @click="togglemaxsize" />-->
<!--                                                    <img class="windowCtrlImg" v-else src="@/assets/imgs/normal.png"-->
<!--                                                        @click="togglemaxsize" />-->
<!--                                                    <img class="windowCtrlImg" src="@/assets/imgs/close.png"-->
<!--                                                        @click="close" />-->
<!--                                                </div>-->
<!--                                            </div>-->
                                        </template>
                                    </Title>
                                </div>
                            </el-col>
                        </el-row>
                    </el-header>
                    <el-main class="el-main-dom">
                        <el-row>
                            <el-col :span="24">
                                <div :style="{position: 'relative',height:!isShowTabs?'100vh':''}" class="app-view-windon app-view-windon-h common-bg">
                                    <!-- 刷新加载 -->
                                    <div v-if="APP.isRefresh || analyserShowLoading" class="app-mask-loading">
                                        <div class="el-loading-spinner"></div>
                                    </div>
                                    <!-- 正文 -->
                                    <div style="height: 100%;" class="app-view-content">
                                        <transition name="slide">
                                            <keep-alive v-if="!$route.meta.notKeepAlive">
                                                <router-view @showUploadPop="showUploadPop"/>
                                            </keep-alive>
                                            <router-view v-else @showUploadPop="showUploadPop"/>
                                        </transition>
                                    </div>
                                </div>
                            </el-col>
                        </el-row>
                    </el-main>
                </el-container>
            </el-container>
        </div>
        <div v-else>
            <div style="min-height: 100%;">
                <transition name="slide">
                    <keep-alive>
                        <router-view />
                    </keep-alive>
                </transition>
            </div>
        </div>

        <!-- 其他附加功能，弹窗 -->
        <!-- 任务小图标 -->
        <TaskQueue v-if="reduce" :uploading="uploading" @click="taskQueueClick"></TaskQueue>
        <!-- 分享复盘弹窗 -->
        <online-analysis-dialog v-if="onlineAnalysisDialogVisible" @reducePop="reducePop"
            @updateLoading="updateLoading" @close="closePop" ref="onlineAnalysisDialog">
        </online-analysis-dialog>
        <!-- 对比分析弹窗 -->
        <online-contrast-analysis-dialog v-if="onlineContrastAnalysisDialogVisible" @reducePop="reducePop"
            @updateLoading="updateLoading" ref="onlineContrastAnalysisDialog" :shareUrl="shareUrl" @close="closePop"
            @uploadVodSuccess="confirmShareAnalysis">
        </online-contrast-analysis-dialog>

        <!-- 校验时间弹窗 -->
        <timeDialog v-if="timeDialogVisible" ref="timeD" @closeClient="close"></timeDialog>
        <!-- 版本二维码 -->
        <versionQrCode ref="versionQrCode"></versionQrCode>
        <!-- 新手引导 -->
        <tour :userInfo="getUserInfo" ref="tour"></tour>

        <!-- 每天第一次打开提醒弹窗 -->
        <dayFirstDialog v-if="dayFirstVisible" ref="dayFirst"></dayFirstDialog>
    </div>
</template>

<script>
import Title from './common/title/title.vue';
import Nav from './common/left/nav.vue';
import Disk from './common/left/disk.vue';
import Version from './common/left/version.vue';
import TaskQueue from '@/components/TaskQueue/index.vue';
import OnlineAnalysisDialog from '@/views/commonComponent/onlineAnalysisDialog.vue';
import timeDialog from '@/views/modules/compere/timeDialog.vue';
// 临时方案加入弹窗
import onlineContrastAnalysisDialog from '@/views/commonComponent/onlineContrastAnalysisDialog.vue';
// import onlineContrastAnalysisDialogMixin from '@/mixins/onlineContrastAnalysisDialog.js'
import versionQrCode from '../commonComponent/versionQrCode.vue';
import Frame from '@/assets/imgs/Frame.png';
import tour from './common/tour.vue';
import myUtils from '../../utils/utils';
import dayFirstDialog from '@/views/commonComponent/dayFirstDialog.vue';
import autoUploadcloud from '@/utils/autoUploadCloud.js';
import banError from '@/utils/banError.js'
import diskError from '@/utils/diskError.js';
import contextmenu from '@/mixins/contextmenu';
import {clearLoginInfo} from '@/utils/index';
import hintImage from '@/assets/imgs/hint.png';
import {VERSION_TYPE} from "@/enum";
export default {
    provide() {
        return {
            appVnode: this
        }
    },
    inject: ['APP'],
    components: {
        versionQrCode,
        Title,
        Nav,
        Disk,
        Version,
        TaskQueue,
        timeDialog,
        OnlineAnalysisDialog,
        onlineContrastAnalysisDialog,
        tour,
        dayFirstDialog
    },
    data() {
        return {
            VERSION_TYPE,
            type: {},
            // uploadType: '',
            timeDialogVisible: false,
            // 是否显示图标
            reduce: false,
            // 图标状态
            uploading: false,
            onlineAnalysisDialogVisible: false,
            onlineContrastAnalysisDialogVisible: true,
            uploadVodSuccess: null,
            shareUrl: '',
            togglemaxsizeFlag: 'normal',
            getUserpropertyFns:[],
            getUserpropertyDebounce: myUtils.debounce(100,true),
            dayFirstVisible: false,
            clickCount: 0,
            analyserShowLoading: false
        };
    },
    computed: {
        isLayoutModel(){
            return this.$route.meta.layoutModel !== false
        },
        getUserInfo() {
            return this.$store.state.userInfo;
        },
        
        isOnlineType() {
            return this.type['online']
        },
        isContrastType() {
            return this.type['contrast']
        },
        isUploading() {
            return this.uploading
        },
        getZoom () {
           return this.$store.getters.getDomZoom||1
        },
        getRouteMeta(){
            return this.$route.meta || {};
        },
        isShowTabs(){
            return !this.getRouteMeta?.hideTabs
        },
        versionType(){
           return this.$store.getters.getVersionType
        },
        isPackageFree() {
            return this.$store.getters.isFree || this.$store.getters.isActivated|| this.$store.getters.isPure;
        },
        isPackagePure(){
            return  this.$store.getters.isPure
        }
    },
    watch: {
        '$store.getters.getTimeAccurate': {
            handler(val) {
                this.calibrationTime();
            }
        },
        '$route.path'(){
            if(this.$store.state?.userInfo?.packageLevel === -1){
                this.versionQrCodeShow()
            }
            this.ifRemainingHint();
        }
    },
    mixins: [contextmenu],
    methods: {
        setAnalyserShowLoading(val){
            this.analyserShowLoading = val;
        },
        versionQrCodeShow(data){
            this.$refs.versionQrCode?.show(data);
        },
        clickHide(){
            this.clickCount++;
            localStorage.setItem("clickCount", this.clickCount);
        },
        async versionChange() {
            if (this.$store.state.detectionStatus) return this.$message.info('录制中暂时无法切换版本');
            const target = this.versionType === VERSION_TYPE.PURE ? VERSION_TYPE.AGENT : VERSION_TYPE.PURE
            // const fullPath = this.$route.fullPath
            //
            // this.$store.commit("setVersionType", target);
            // if (target === VERSION_TYPE.PURE) {
            //     const list = commonUtils.pureRouterList
            //     if (fullPath !== '/' && !(list.some(item => fullPath.split('?')[0]===item))) {
            //         this.$router.replace({path: '/dataAnalysis'})
            //     }
            // }
            try {
                const result = await this.$httpClient.setup.setClientVersionConfig({
                    clientVersion: target === VERSION_TYPE.AGENT ? 'replay' : 'record',
                    pageType: 0
                })
                if (result.code === 0) {
                    this.$store.commit("setVersionType", target);
                    await this.$router.replace({
                        path: '/dataAnalysis', query: {
                            ...this.$route.query, // 保留原有的其他参数
                            _t: new Date().getTime()
                        }
                    })
                    this.$router.go(0)
                }
            }catch (e) {

            }
        },
        getDomZoom () {
            const zoom =(document.querySelector('.nav-box')?.offsetHeight / this.$refs.nav?.getHeight()+0.05) || 1
            this.$store.commit('setDomZoom',zoom)
        },
        toIfupanWebsite () {
            this.APP.toIfupanWebsite()
        },
        getDisk(){
            this.$refs.disk?.getDisk();
        },
        // 加载新手引导
        onTour(len){
            this.$refs.tour?.startTour(0).finally(()=>{
            // this.$refs.tour?.startTour(len).finally(()=>{
                // 如果url携带login参数，则表示登录跳转，直接弹出二维码
                if(this.$route.query.login){
                    // 从login页面进入时判段用户版本
                    this.isVersionQrCode('init');
                }else{
                    // 没有的情况进行24小时检查弹窗
                    this.isVersionQrCode();
                }
            });
        },
        refresh() {
            location.reload();
        },
        showQrCode(){
            this.APP.showQrCode();
        },
        // 禁止鼠标右键
        preventRightClick(event) {
            event.preventDefault();
        },
        // 获取基本设置信息
        getBasinSetupInfo() {
            this.$httpClient.setup.getmodel({}).then((res) => {
                if (res.code == 0) {
                    this.$store.commit('setBiscInfo', res.data)
                    if (res?.data?.IsRocord == 1) {
                        this.$store.commit('saveDetectionStatus', true)
                        if (!this.$store.state.startDetectionTime) {
                            this.$store.commit('saveDetectionTime', Date.now())
                        }
                    } else {
                        this.$store.commit('saveDetectionStatus', false)
                        this.$store.commit('saveDetectionTime', null)
                    }
                }
            });
        },// 最大化/恢复正常
        togglemaxsize() {
            this.$httpClient.form.togglemaxsize().then((res) => {
                if (res.code == 0) {
                    if (this.togglemaxsizeFlag == 'normal') {
                        this.togglemaxsizeFlag = 'max';
                    } else {
                        this.togglemaxsizeFlag = 'normal';
                    }
                }
            })
        },
        // 最小化
        minsize() {
            this.$httpClient.form.minsize().then((res) => {})
        },
        setShareUrl(url) {
            this.shareUrl = url
        },
        reducePop(bl) {
            this.reduce = bl;
        },
        closePop(type) {
            this.type[type] = false;
        },
        updateLoading(bl) {
            this.uploading = bl
        },
        openDayFirst(callback){
            this.dayFirstVisible = true;
            this.$nextTick(() => {
                this.$refs.dayFirst.checkAndShowPopup(callback)
            })
        },
        taskQueueClick() {
            this.reduce = false;
            if (this.isOnlineType) {
                this.onlineAnalysisDialogVisible = true
                this.$refs.onlineAnalysisDialog.visible = true
            }
            if (this.isContrastType) {
                this.onlineContrastAnalysisDialogVisible = true;
                this.$refs.onlineContrastAnalysisDialog.visible = true
            }
        },
        // 单个分析页面弹窗
        showUploadPop(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo) {
            this.onlineAnalysisDialogVisible = true;
            this.type['online'] = true;
            this.$nextTick(() => {
                this.$refs.onlineAnalysisDialog.init(userProperty, onlineFileInfo, shareUrl, videoInfo, anchorInfo);
            })
        },
        // 对比分析页面弹窗
        showContrastAnalysis(userProperty, onlineFileInfo) {
            // 只渲染弹窗
            if (!userProperty) {
                this.onlineContrastAnalysisDialogVisible = true;
                return
            }
            // 渲染弹窗并且唤醒弹窗
            this.type['contrast'] = true;
            this.onlineContrastAnalysisDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.onlineContrastAnalysisDialog.init(userProperty, onlineFileInfo);
            })
        },
        // 储存对比分析成功逻辑
        addUploadVodSuccess(fn) {
            this.uploadVodSuccess = fn
        },
        // 上传完成执行储存的成功逻辑
        confirmShareAnalysis(list, cloudRemarks, viewCrowdType) {
            if (typeof this.uploadVodSuccess === 'function') {
                this.uploadVodSuccess(list, cloudRemarks, viewCrowdType);
            }
        },
        // 获取当前系统时间是否准确
        getTimeAccurate() {
            // 软件首次进入检查时间
            if (sessionStorage.getItem('TimeAccurate') || !this.$store.state.userInfo) { return };
            sessionStorage.setItem('TimeAccurate', '1')
            this.$httpClient.setup.getTimeAccurate().then(res => {
                if (!res.data) {
                    this.calibrationTime();
                }
            })
        },
        // 校验时间弹窗
        calibrationTime() {
            this.timeDialogVisible = true;
            this.$nextTick(() => {
                this.$refs.timeD.init();
            })
        },
        // 关闭上传
        closeUpload() {
            if (this.uploading) {
                // 关闭弹窗中的上传。
                if (this.isOnlineType) {
                    this.$refs.onlineAnalysisDialog.cancelUpload();
                }
                if (this.isContrastType) {
                    this.$refs.onlineContrastAnalysisDialog.cancelUpload();
                }
            }
        },
        // 自动判定关闭弹窗
        autoCloseDialog() {
            let titles = [];
            if (this.uploading) {
                titles.push('上传云空间')
            }
            this.$httpClient.setup.getmodel({}).then((res) => {
                if (res.data.IsRocord == 1) {
                    titles.push('录制');
                    this.closeDialog(titles);
                } else {
                    if (titles.length) {
                        this.closeDialog(titles);
                    } else {
                        this.logoutCloseExe();
                    }
                }
            });
        },
        // 关闭弹窗
        closeDialog(titles) {
            // 录制和上传云空间，关闭后停止录制和上传，是否关闭？
            this.$confirm(`<div><img style="margin-bottom:10px;" src="${Frame}"></div>目前正在 <span style="color: red;">${titles.join('和')}</span> 。<br>关闭后停止<span style="color:red;"> ${titles.join('和')} </span>，是否确认关闭?`, '', {
                distinguishCancelAndClose: true,
                dangerouslyUseHTMLString: true,
                confirmButtonText: '继续' + titles.join('和'),
                cancelButtonText: '确认关闭',
                showClose: false,
                customClass: 'confirm-common-box close-exe-dialog confirm-text-center',
            }).then(() => {

            }).catch((action) => {
                if (action == "cancel") {
                    this.logoutCloseExe()
                }
            });
        },
        // 关闭退出软件
        async logoutCloseExe() {
            this.closeUpload()
            await this.$httpClient.video.stopAutoAnalysis();
            await this.$httpBack.user.logout();
            await this.$httpClient.form.close();
        },
        // 关闭
        close() {
            this.autoCloseDialog();
        },
        isVersionQrCode(type){
            return this.$refs.versionQrCode.autoShow(type)
        },
        getUserproperty(callback,name) {
            if(typeof callback === 'function'){
                if(name){
                    this.getUserpropertyFns[name]=callback
                }else {
                    if(!this.getUserpropertyFns['other']){
                        this.getUserpropertyFns['other'] = [];
                    }
                    this.getUserpropertyFns['other'].push(callback)
                }
            }
            this.getUserpropertyDebounce(()=>{
                this.$httpBack.userProperty.info({},{load:false}).then(res => {
                    if (res.code === 0 && res.data) {
                        let others = this.getUserpropertyFns['other'] || [];
                        this.getUserpropertyFns['other'] = null;
                        const funs = [...Object.values(this.getUserpropertyFns), ...others];
                        while(funs.length){
                            const fun = funs.shift();
                            fun&&fun(res.data);
                        }
                        this.$store.commit("saveUserproperty", res.data);
                    }
                });
            })
        },
        // 复制分享链接
        copyShareUrl(shareUrl) {
            this.APP.copyShareUrl(shareUrl)
        },
        //  添加刷新云盘任务
        addRefreshDisk(){
            this.$CSharpNotify.addTask('refreshDisk',(res,resolve)=>{
                this.$nextTick(()=>{
                    this.getDisk();
                })
            });
        },
        addNotice(){
            this.$CSharpNotify.addTask('notice',(res,resolve)=>{
                /*
                    {
                        "code": "0",
                        "status": "200",
                        "action": "notice",
                        "data": {
                            "alertType": 0, (弹窗类型 0：仅通知 1：需点击确认)
                            "statusType": "success", (状态类型 success：成功 info：消息 warning：警告 error：错误)
                            "title": "弹窗标题"
                            "msg": "弹窗内容"
                            "duration": "显示时长，单位毫秒"
                        }
                    }
                */
               let opt = {
                    title: res.title,
                    type: res.statusType,
                    dangerouslyUseHTMLString: false,
                    message: res.msg,
                    duration: res.duration,
                    onlyTag: res.title === '断网提示' ? 'ntwork': ''
                }
                this.$nextTick(()=>{
                    if(res.alertType === 0){
                        this.$cMsg.custom(opt);
                    }else{
                        this.$cNotify.notify(opt);
                    }
                })
            });
        },
        // 添加用户冻结任务
        addUserFrozen(){
            this.$CSharpNotify.addTask('userFrozen',(res,resolve)=>{
                this.$nextTick(()=>{
                    // 账号已被冻结，跳回登录页
                    this.$message.error(res?.msg || "账号已被冻结，将停止录制和分析。");
                    clearLoginInfo('user');
                    this.$router.push({ path: "login" });
                })
            });
        },
        /**
         * 检查剩余资源并进行30天提示机制
         * @description 检查AI分析时长和算力包余量，根据30天提示机制决定是否显示提醒
         */
        async ifRemainingHint() {
            await this.$store.dispatch('getPurePackageLevel')
            if (this.$store.getters.isFree || this.$store.getters.isActivated || this.$store.getters.isPure) {
                return;
            }
            /*
            aiAnalysisTime ai语音分析时长(分钟)
            aiTokenNum AI分析文本数量
            提醒时机:
                智能分析时长低于10个小时(600分钟)
                AI算力包低于10万字的时候提醒

            提醒文案:
	            智能分析时长：您目前的智能分析时长不足10个小时，请按需使用；您也可以找产品顾问咨询增量包或升级会员版本
                AI算力包：您目前的AI算力包低于10万字，请按需使用；您也可以找产品顾问咨询增量包或升级会员版本！
            */
            try {
                const { aiAnalysisTime, aiTokenNum } = this.$store.getters.getUserproperty;
                if(typeof aiAnalysisTime === 'undefined' || typeof aiTokenNum === 'undefined'){
                    return;
                }
                // 修正变量命名逻辑：检查是否需要提醒
                const isAiAnalysisTimeLow = aiAnalysisTime < 600; // 智能分析时长低于10小时(600分钟)
                const isAiTokenNumLow = aiTokenNum < 10000 * 10; // AI算力包低于10万字
                // 如果任一资源不足，则检查30天提示机制
                if (isAiAnalysisTimeLow || isAiTokenNumLow) {
                    // 获取当前时间戳
                    const currentTime = Date.now();
                    // 从本地存储获取上次提示时间戳
                    const lastHintTimeStr = localStorage.getItem('ifRemainingHint');
                    const lastHintTime = lastHintTimeStr ? Number(lastHintTimeStr) : 0;
                    // 计算30天的毫秒数 (30天 * 24小时 * 60分钟 * 60秒 * 1000毫秒)
                    const thirtyDaysInMs = 30 * 24 * 60 * 60 * 1000;
                    // 计算时间差
                    const timeDifference = currentTime - lastHintTime;
                    
                    // 检查是否超过30天或没有记录（首次提示）
                    const shouldShowHint = lastHintTime === 0 || timeDifference >= thirtyDaysInMs;
                    
                    if (shouldShowHint) {
                        // 触发提示逻辑
                        this.showRemainingHint(isAiAnalysisTimeLow, isAiTokenNumLow);
                        
                        // 更新本地存储的时间戳
                        localStorage.setItem('ifRemainingHint', currentTime.toString());
                    }
                    // 如果30天内已提示过，则跳过本次提示（不执行任何操作）
                } else {
                    // 资源充足时，重置提示时间戳
                    localStorage.setItem('ifRemainingHint', '0');
                }
            } catch (error) {
                console.error('检查剩余资源提示时发生错误:', error);
                // 发生错误时不影响主流程，静默处理
            }
        },
        
        /**
         * 显示资源不足提示
         * @param {boolean} isAnalysisTimeLow 分析时长是否不足
         * @param {boolean} isTokenNumLow 算力包是否不足
         */
        showRemainingHint(isAnalysisTimeLow, isTokenNumLow) {
            let message = '';
            if (isAnalysisTimeLow) {
                // 仅分析时长不足
                message = '您目前的智能分析时长不足10个小时，请按需使用；您也可以找产品顾问咨询增量包或升级会员版本';
            } else if (isTokenNumLow) {
                // 仅算力包不足
                message = '您目前的AI算力包低于10万字，请按需使用；您也可以找产品顾问咨询增量包或升级会员版本！';
            }
            if (message) {
                this.versionQrCodeShow({
                    html: `
                    <div class="pd-l20 pd-r20">
                        <div class="font-s18 mg-b8 text-colorMain"><img style="max-width:15px" class="mg-r10" src="${hintImage}" />${isAnalysisTimeLow ? '智能分析时长' : 'AI算力包'}</div>
                        <div class="mg-b4">${message}</div>
                    </div>
                    `
                })
            }
        }
    },
    created() {
        this.getBasinSetupInfo();
        this.getUserproperty();
        this.ifRemainingHint()
    },
    mounted () {
        this.getTimeAccurate()
        this.$nextTick(() => {
            window.addEventListener("resize", this.getDomZoom);
        })
        
        // 自动上传，传入上下文环境，以及任务监听对象
        autoUploadcloud(this,this.$CSharpNotify);
        banError(this.$CSharpNotify);
        diskError(this.$CSharpNotify);
        this.addRefreshDisk();
        this.addUserFrozen();
        // 全局通用通知推送事件
        this.addNotice();

        

        if(localStorage.getItem('clickCount')){
            this.clickCount = parseInt(localStorage.getItem('clickCount')) || 0;
        }
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
        window.removeEventListener("resize", this.getDomZoom);
    }, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.layout-box {
    width: 100%;
    // overflow: hidden;
    // padding-top: 15px;
    // border-top: 15px solid #fff;
    box-sizing: border-box;
    background: url("~@/assets/imgs/back_img.png") no-repeat center center;
    background-size: cover;
}

.title-box {
    height: 48px;
}

.left-box {
    //background-image: linear-gradient(180deg, #F4F9FF 60%, #E2EAFD 100%);
    //box-shadow: 1px 0px 2px 0px #E3E3E5;
    height: calc(100vh);
    overflow: hidden;
}

.logo-box {
    height: 100px;
}
.lu-logo{
    position: absolute;
    right: 0;
    height: 15px;
    width: 15px;
    top: 2px;
}

.nav-box {
    height: calc(100vh - 270px);
    flex: auto;
    overflow-y: auto;
    overflow-x: hidden;
    /* 滚动条轨道 */
    &::-webkit-scrollbar-track {
        background: #f1f1f1;  /* 轨道背景 */
        border-radius: 4px;
    }

    /* 滚动条滑块 */
    &::-webkit-scrollbar-thumb {
        background: linear-gradient( 180deg, #F4F9FF 61%, #E2EAFD 100%);
        border-radius: 4px;
    }

    /* 鼠标悬浮在滑块上 */
    &::-webkit-scrollbar-thumb:hover {
        background: linear-gradient( 180deg, #F4F9FF 61%, #E2EAFD 100%);
    }
}

.desk-box {
    // height: 90px;
    padding-top: 10px;
}

.version-box {
    // height: 80px;
    box-sizing: border-box;
}

.el-header-dom {
    padding: 0;
}

.el-main-dom {
    padding: 0;
}

.app-view-windon {
    width: 100%;
    padding: 10px;
    overflow: hidden;
    box-sizing: border-box;
    overflow-y: auto;
    // >* {
    //     overflow: initial;
    // }
}

.logoImg {
    width: 95%;
    height: 56px;
    margin-top: 5px;
    display: inline-block;
    background-image: url("~@/assets/imgs/theme/icon.png");
    background-repeat: no-repeat;
    background-position: 14px -154px;
}

.logoImgAddress {
    text-align: center;
    margin-bottom: 6px;

    .version {
        position: relative;
        display: inline-flex;
        align-items: center;
        padding: 3px 16px;
        border-radius: 50px;
        background: linear-gradient(42deg, rgba(9, 207, 255, 1), rgba(148, 45, 254, 1));
        cursor: pointer;

        &::before {
            content: '';
            position: absolute;
            top: 1px;
            left: 1px;
            right: 1px;
            bottom: 1px;
            background: #fff;
            border-radius: 50px;
            z-index: 0;
        }

        .icon {
            transform: rotate(90deg);
        }
    }

    .version i,
    .version span {
        position: relative;
        z-index: 1;
    }
}

// 必填标识
.requireSyb {
    color: red;
    margin-right: 2px;
    font-size: 18px;
}

.windowCtrlImg {
    width: 16px;
    height: 16px;
    margin-left: 21px;
    cursor: pointer;
}

.windowCtrlContainer {
    height: 100%;
    text-align: right;
    display: flex;
    align-items: center;
    border-left: 1px solid #E6E6E6;
    margin-left: 20px;
    padding-right: 50px;

    .windowCtrlContainer-content {
        display: inline-block;
    }

    // display: flex;
    // align-items: center;
    // justify-content: flex-end;
}
</style>
