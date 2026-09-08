<!--
@description 对比/详情视频区组件：负责直播画面播放、播放器联动与公屏弹幕区域显示控制。
注意：浏览器云空间详情页拿不到客户端版本信息，弹幕区显隐需按来源环境分别判定。
-->
<template>
    <div :class="{'is-compare-video': isCompare || isKuaishou}" style="height: 100%;" class="contrast-video pd-8 main-bg flex-column flex-ai-s">
        <div style="width: 100%;" :style="isScrolling?{}:{height: '100%'}">
            <Title class="pd-b8 flex-jc-sb flex-ai-c">
                {{isText?'文件分析':'直播画面'}} 
                <div v-if="!$refs?.flod?.flod && isVideoId">
                    <!-- 首次刷新 -->
                    <span v-if="!isRefresh"  class="font-s12 text-color3">
                        <!-- <span>无法播放？</span> -->
                         <el-button type="text" @click="onOpenVideo" class="pd-0 font-s12">
                            打开视频
                         </el-button>
                        <span>/</span>
                        <el-button type="text" @click="onRefresh" class="pd-0 font-s12">
                            点击刷新
                        </el-button>
                    </span>
                    <!-- 二次修复 -->
                    <span v-if="isShowRepair" class="font-s12 text-color3">
                        <!-- <span>无法播放？</span> -->
                         <el-button type="text" @click="onOpenVideo" class="pd-0 font-s12">
                            打开视频
                         </el-button>
                        <span>/</span>
                        <el-button type="text" @click="onRepair" class="pd-0 font-s12">
                            点我修复
                        </el-button>
                    </span>
                </div>
            </Title>
            <div v-if="repairLoading" class="flex-ji-c" style="height: 300px;">
                <div>
                    <div><img style="max-width: 120px;" src="@/assets/imgs/loading.gif" ></div>
                    <div class="font-s12 text-color3">视频正在努力修复中......</div>
                </div>
            </div>
            <div  v-else style="height: calc(100% - 27px);">
                <video-player :style="{display: $refs?.flod?.flod?'none':'block'}" 
                    ref="videoPlayer"
                    :cycle="cycle || ai"
                    :startTime="getStartTime" 
                    :endTime="getEndTime" 
                    class="analysis-video-player"
                    :class="{'show-video-notUrl': getIsVideo && !getPlayUrl, 'show-text-bg':isText}"
                    @error="onError"
                    :playUrl="getVideoRepairPlayUrl" @timeupdate="(player) => {
                        onPlayerTimeupdate(player, name)
                    }" @canplay="onCanplay" @playerLoadeddata="onPlayerLoadeddata" @ready="playerReadied" @play="onPlay" @pause="onPause">
                    <div v-if="$isAifupan" class="show-video-notUrl-hint">在本电脑上没有找到该视频，请确认是否删除了视频或者文件有移动过。</div>
                    <div v-else class="show-video-notUrl-hint">没有找到视频文件，请确认该场直播是否上传到了云空间。</div>
                </video-player>
            </div>
        </div>
        <div v-if="canShowScrollingPanel" style="width: 100%;overflow: hidden;" class="h100 pd-t8 flex-ai-s flex-column" >
            <Title class="pd-b8 flex-jc-sb">
                <span>公屏弹幕</span><flod ref="flod" @click="flodClick"></flod>
            </Title>
            <scrolling-list ref="scrolling" :targetType="targetType" class="h100" :sentenceMarkData="sentenceMarkData" :search="$refs?.flod?.flod"></scrolling-list>
        </div>
        <slot name="video-bottom"></slot>
    </div>
</template>
<script>
/**
 * @description 对比/详情视频区组件：统一处理客户端与云空间场景下的视频播放、公屏弹幕和播放器联动逻辑。
 */
import videoPlayer from '/src/components/analysis/videoPlayer.vue'
import commonMixin from './mixin/commonMixin';
import Title from '/src/components/title/index.vue';
import flod from '/src/components/fold/index.vue';
import scrollingList from '/src/components/analysis/scrolling/scrollingList.vue'
import {VERSION_TYPE} from "@/enum";
export default {
    mixins: [commonMixin],
    components: {
        videoPlayer,
        Title,
        flod,
        scrollingList
    },
    props: {
        // 视频播放器宽度，如：28%
        videoWidth: {
            type: String,
            default: '18%'
        },
        targetType: {
            type:String,
            default: ''
        },
        notScrolling: {
            type: Boolean,
            default: false
        },
        sentenceMarkData: {
            type: Object,
            default: ()=>{return {}}
        },
        ai: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            VERSION_TYPE,
            videoDuration: "", // 视频时长
            charCountNum: "", // 文字总数量
            currentParagraphIndex: -1, // 当前段落
            repairLoading: false,
            repairUrl: '',
            cycle: false,
            playType: '',
            cycleOpt:{},
            currentTime: 0,
        };
    },
    computed: {
        // 重写
        getVideoVnode() {
            if(this.repairLoading){return null};
            return this.$refs.videoPlayer
        },
        player() {
            return this.getVideoVnode.player;
        },
        isScrolling(){
            return this.isVideoId && !this.isCompare && !this.notScrolling && !this.readonly && this.isDouyin
        },
        /**
         * @description 公屏弹幕面板显示开关。
         * 云空间（`online/webOnline`）依赖页面来源即可放行；客户端环境仍需判断是否为 AI 全能版。
         * @returns {boolean}
         */
        canShowScrollingPanel() {
            if (!this.isScrolling) {
                return false;
            }
            if (this.isUseBack) {
                return true;
            }
            return this.versionType === VERSION_TYPE.AGENT;
        },
        getStartTime(){
            if(this.cycle){
                return this.cycleOpt?.startTime;
            }
            return this.sentenceMarkData?.sentenceMarkList?.[0]?.startTime
        },
        getEndTime(){
            if(this.cycle){
                return this.cycleOpt?.endTime;
            }
            return this.sentenceMarkData?.sentenceMarkList?.[this.sentenceMarkData?.sentenceMarkList.length - 1]?.endTime
        },
        getIsVideo(){
            return this.isVideoId || this.isFileId
        },
        isText(){
            return this.sentenceMarkData?.fileInfo?.fileType === 2
        },
        isUseBack(){
            return this.targetType === 'online' || this.targetType === 'webOnline'
        },
        isRefresh(){
            return sessionStorage.getItem('isRefreshId') === this.getAnalysisId
        },
        isShowRepair(){
            return this.getPlayUrl && !this.ai && !this.isCompare && !this.repairLoading && !this.isUseBack && this.isRefresh
        },
        getVideoRepairPlayUrl(){
            return this.repairUrl || this.getPlayUrl
        },
        versionType() {
            return this.$store.getters.getVersionType
        }
    },
    watch: {},
    methods: {
        onError(player){
            if(this.videoDuration - this.currentTime <= 2){
                player.error(null);
                player.src(player.currentSrc());
                this.ponlayerReadied(0, 'play');
            }
        },
        onOpenVideo(){

            this.$httpClient.video.preview({ videoId: this.getAnalysisId }).then((res) => {
                if (res.code == 0) {
                    window.open(res.data, '_blank')
                }
            })
        },
        playTask(taskData){
            const {type,data} = taskData || {};
            
            if(type === 'stop'){
                this.playerPause();
            }else if(type === 'init'){
                this.playType = 'video';
                this.palyOrStop('stop');
            }else if(type === 'play'){
                let time = data.endTime <= this.player.cache_.currentTime * 1000 ? data.startTime : data?.stopTime ? 'notSecond' : data.startTime;
                
                this.ponlayerReadied(time, 'play');
            }else if(type === 'cycle'){
                this.$set(this,'cycle', true);
                this.$set(this,'cycleOpt', data);
                this.ponlayerReadied(data.stopTime?'notSecond':data.startTime,'cycle');
            }
        },
        onRefresh(){
            sessionStorage.setItem('isRefreshId', this.getAnalysisId);
            window.location.reload();
        },
        /**
         * 检查视频修复是否正在进行，如果正在进行则提示用户是否中断修复
         * @param {Function} callback - 回调函数，用于处理修复状态
         */
        isRepairStop(callback){
            if(!this.repairLoading){
                callback(true);
                return
            }
            this.$confirm('视频修复中，您确定要中断修复吗？', '中断修复', {
                confirmButtonText: '确认中断',
                cancelButtonText: '继续修复',
                type: 'warning'
            }).then(() => {
                this.$httpClient.anchorvideo.cancelRepairVideo().then(()=>{
                    callback(true);
                    this.$notify({
                        title: '修复失败',
                        message: '视频修复失败',
                        duration: 3000,
                        type: 'error',
                        customClass: 'cancelRepairVideo-notify',
                        appendTo: document.body
                    });
                });
            }).catch(()=> {
                if(typeof callback === 'function'){
                    callback(false);
                }
            })
        },

        /**
         * 提示用户修复视频，并开始修复过程
         */
        onRepair(){
            this.$confirm('视频修复需要几分钟的时间，修复过程中请勿退出当前页面，否则修复可能中断', '视频修复', {
                confirmButtonText: '马上修复',
                cancelButtonText: '暂不修复',
                type: 'warning'
            }).then(() => {
                this.repairLoading = true
                this.$httpClient.anchorvideo.repairVideo({
                    type: this.isVideoId ? 0 : 1,
                    uuid: this.getAnalysisId,
                }).catch((err) => {
                    this.repairLoading = false
                })
                this.$emit('repairVideo', true)
            })
        },

        /**
         * 修复失败后的处理逻辑
         */
        reEncodeError(){
            this.repairLoading = false;
            setTimeout(()=>{
                window.location.reload();
            },500)
        },

        /**
         * 修复成功后的处理逻辑
         */
        reEncodeSuccess(){
            this.repairLoading = false;
            this.$emit('repairVideo', false);
            this.repairUrl = this.getPlayUrl;
            setTimeout(()=>{
                window.location.reload();
            },500)
        },
        palyOrStop(type){
            if(type === 'play'){
                this.$emit('playStaus',{
                    type: 'play',
                    playType: this.playType
                })
            }else{
                this.$emit('playStaus',{
                    type: 'stop',
                    playType: this.playType
                })
            }
        },
        onPlay(){
            this.palyOrStop('play');
        },
        onPause(){
            this.palyOrStop('stop');
        },
        /**
         * 折叠或展开弹幕列表时的操作
         * @param {boolean} flod - 是否折叠
         */
        flodClick(flod){
            if(flod){
                this.playerPause()
            }else{
                this.getVideoVnode?.player?.play();
            }
            this.$emit('flodClick', flod)
        },

        /**
         * 设置播放器进度
         * @param {number} second - 播放器进度（秒）
         */
        ponlayerReadied(second,type) {
            if (this.getVideoVnode?.player) {
                this.$set(this,'playType',type)
                if(type !== 'cycle'){
                    this.cycle = false;
                }
                if(second !== 'notSecond'){
                    this.getVideoVnode?.player?.currentTime(second / 1000);
                }
                this.getVideoVnode?.player?.play();
                if(type ==='click'){
                    this.palyOrStop('play')
                }
            }
        },

        /**
         * 视频停止播放
         */
        playerPause() {
            if (this.getVideoVnode) {
                this.getVideoVnode?.player?.pause();
            }
        },

        /**
         * 设置视频时间段落
         * @param {number} index - 段落索引
         */
        setVideoParagraphIndex(index) {
            this.currentParagraphIndex = index
        },

        /**
         * 视频加载完成后的回调事件
         */
        onCanplay() {
            if (this.getVideoVnode) {
                this.$nextTick(() => {
                    this.$emit('canplay')
                })
            }
        },

        /**
         * 视频加载数据完成后的回调事件
         * @param {Object} player - 播放器实例
         */
        onPlayerLoadeddata(player){
            this.$nextTick(() => {
                this.setCharCountAndDuration(player)
            })
        },

        /**
         * 播放器准备就绪后的回调事件
         * @param {Object} player - 播放器实例
         */
        playerReadied(player){
            this.$nextTick(() => {
                this.setCharCountAndDuration(player)
            })
        },

        /**
         * 设置视频时长和字符数量
         * @param {Object} player - 播放器实例
         * @returns {Object} - 包含视频时长和数学时长的对象
         */
        setCharCountAndDuration(player){
            let videoDuration = 0;
            if(player?.duration && !(isNaN(player?.duration()))){
                videoDuration = player?.duration()
            }else{
                const { videoInfo, uploadFile } = this.sentenceMarkData;
                videoDuration =videoInfo?.durationTime || uploadFile?.durationTime;
            }
            let o = {
                videoDuration: videoDuration,
                MathDuration: Math.round(videoDuration)
            }
            this.videoDuration = videoDuration;
            this.$emit('setDuration',o);
            // 初始默认从0开始加载弹幕
            this.playerOperationSScrolling(0);
            return o;
        },

        /**
         * 播放运行弹幕信息
         * @param {number} time - 当前播放时间（秒）
         */
        playerOperationSScrolling(time){
            this.$nextTick(()=>{
                this.$refs?.scrolling?.operation(time)
            })
        },

        /**
         * 播放器进度回调
         * @param {Object} player - 播放器实例
         * @param {string} name - 播放器名称
         */
        onPlayerTimeupdate(player, name) {
            this.currentTime = player.cache_.currentTime
            this.$emit('playerTimeupdate', player.cache_.currentTime)
            // 当前时间错传入弹幕组件
            this.playerOperationSScrolling(player.cache_.currentTime)
            // 通过每一帧的段落索引获取当前帧数在段落中第几段，不用循环直接通过建立好的索引查寻
            let paragraphIndex = window.payerAllIndexMap?.[name || 'default']?.[Math.ceil(player.cache_.currentTime)];
            
            // 判断当前段和运行段是否一样不一样直接跳转下一段
            if (this.currentParagraphIndex !== paragraphIndex) {
                // 重置视频段落
                this.setVideoParagraphIndex(paragraphIndex);
                // 监听改变
                this.$emit('changeParagraphIndex', paragraphIndex)
            }
        },
    },
    created() {

    },
    mounted() {
        this.$nextTick(()=>{
            this.setCharCountAndDuration();
        })
    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() { 
        sessionStorage.removeItem('isRefreshId');
     }, //生命周期 - 销毁完成
    activated() {
        this.$nextTick(()=>{
            this.setCharCountAndDuration()
        })
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
        
<style lang='scss' scoped>
 .contrast-video{
     border-radius: 10px;
     //.videoContainer {
     //    height: calc(100% - 27px);
     //}
 }
.is-compare-video{
    height: 100%;
    max-height: 500px;
    .videoContainer {
        height: 100%;
    }

    ::v-deep(.video-player) {
        // max-width: 100%;
        // max-height: 100%;
        // width: 100%;
        height: 100%;

        .video-js {
            padding: 0;
            height: 100%;
        }

        .vjs-tech {
            max-width: 100%;
            max-height: 100%;
            margin: auto;
            display: block;
            // position: relative;
            position: relative;
        }
    }
}
.analysis-video-player{
    background-image: url('~@/assets/imgs/bqfxText.png');
    background-size: cover;
    background-position: center;
    position: relative;
}
.show-video-notUrl-hint{
    display: none;
    opacity: 0;
    visibility: hidden;
}
.show-video-notUrl{
    background-color: #DEE1E9;
    background-image: url('~@/assets/imgs/subtract.png');
    background-repeat: no-repeat;
    background-size: 80px 56px;
    height: 300px;
    .show-video-notUrl-hint{
        display: block;
        opacity: 1;
        visibility: inherit;
        padding-inline: 12px;
        font-size: 12px;
        color: #A0A8B8;
        margin: 0 auto;
        position: absolute;
        top: 63%;
    }
}
.show-text-bg{
    height: 100%;
}

</style>
