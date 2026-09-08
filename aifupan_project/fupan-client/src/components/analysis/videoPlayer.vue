<template>
    <div class="videoContainer brs-8" ref="videoBox"  style="width: 100%;">
    <video-player 
        v-if="playUrl" 
        ref="videoPlayer" 
        class="video" 
        :options="getOption" 
        :playsinline="true"
        @loadeddata="onPlayerLoadeddata"
        @fullscreenchange="handlefullscreenchange"
        @timeupdate="timeupdate"
        @canplay="canplay"
        @ready="onPlayerReady"
        @play="onPlay"
        @pause="onPause"
        @click="onClick"
        @error="onError"
        v-on="$listeners"
    >
    </video-player>
    <slot></slot>
    </div>
</template>

<script>
export default {
    components: {
    },
    events: ['fullscreenchange'],
    props: {
        playUrl:{
            type: String,
            default: ''
        },
        startTime: {
            type: [Number,null],
            default: null
        },
        endTime: {
            type: [Number,null],
            default: null
        },
        cycle:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            playerOptions: {
                playbackRates: [ 1.0, 1.5, 2.0],
                // playbackRates: [0.7, 1.0, 1.5, 2.0], //播放速度
                autoplay: false, // 如果true,浏览器准备好时开始播放。
                muted: false, // 默认情况下将会消除任何音频。
                loop: true, // 循环播放
                preload: 'metadata', // 建议浏览器在<video>加载元素后是否应该开始下载视频数据。auto浏览器选择最佳行为,立即开始加载视频（如果浏览器支持）
                language: 'zh-CN',
                // aspectRatio: '16:9', // 将播放器置于流畅模式，并在计算播放器的动态大小时使用该值。值应该代表一个比例 - 用冒号分隔的两个数字（例如"16:9"或"4:3"）
                fluid: true, // 当true时，Video.js player将拥有流体大小。换句话说，它将按比例缩放以适应其容器。
                sources: [
                    {
                        type: 'video/mp4', // 这里的种类支持很多种：基本视频格式、直播、流媒体等，具体可以参看git网址项目
                        src: '' // url地址
                    }
                ],
                hls: true,
                notSupportedMessage: '此视频暂无法播放，请稍后再试', // 允许覆盖Video.js无法播放媒体源时显示的默认信息。
                controlBar: {
                    timeDivider: false, // 当前时间和持续时间的分隔符
                    durationDisplay: false,  // 显示持续时间
                    remainingTimeDisplay: true, // // 是否显示剩余时间功能
                    fullscreenToggle: true, // 全屏按钮
                    
                },
                disablePictureInPicture: true, // 小窗口
            },
            start: null,
            end: null,
            // player: null
            fullscreenLook: false
        };
    },
    computed: {
        getOption(){
            // 设置视频配置
            this.playerOptions.sources[0].src = this.playUrl;
            return this.playerOptions;
        },
        player(){
            return this.$refs?.videoPlayer?.player
        }
    },
    watch: {
        startTime(val,old) {
            if(val === old){return}
            this.setStartEnd('startTime');
        },
        endTime(val,old) {
            if(val === old){return}
            this.setStartEnd('endTime');
        },
    },
    methods: {
        handlefullscreenchange(){
            console.log('handlefullscreenchange')
        },
        clickFullscreenLook(){
            this.fullscreenLook = true
        },
        keydownChange(e){
            if (e.keyCode === 27 && this.fullscreenLook) {
                this.fullscreenLook = false;
                this.exitFullscreen();
            }
        },
        removeEvent(){
            // document.getElementsByClassName('vjs-fullscreen-control')[0].removeEventListener('click');
            // document.removeEventListener('keydown', (e)=>{
            //     this.keydownChange(e)
            // })
        },
        addEscExitFullscreen(){
            this.$nextTick(()=>{
                let dom = document?.getElementsByClassName('vjs-fullscreen-control')?.[0];
                if(!dom){return};
                dom?.addEventListener('click',()=>{
                    this.clickFullscreenLook()
                })
                document.addEventListener('keydown', (e) => {
                    this.keydownChange(e)
                });

            })
        },
        exitFullscreen(){
            this.player?.exitFullscreen();
        },
        vodeoPlayTime(player){
            if(!this.cycle){ return }
            if(player?.currentTime() < this.start && this.start !==null){
                player?.currentTime(this.start);
                // player?.play();
            }else if (player?.currentTime() > this.end && this.end !== null) {
                player?.currentTime(this.start);
                // player?.play();
            }
        },
        timeupdate(player){
            this.vodeoPlayTime(player,'timeupdate');
            this.$emit('timeupdate',player);
        },
        onPlay(player) {
            this.vodeoPlayTime(player,'onPlay');
            this.$emit('play');
        },
        onPause(){
            this.$emit('pause');
        },
        // 当用户点击播放器时触发
        onClick() {
            this.vodeoPlayTime(this.player,'onClick');
        },
        canplay(){
            this.$emit('canplay')
        },
        onPlayerLoadeddata(player) {
            this.$emit('playerLoadeddata', player)
        },
        onPlayerReady(player) {
            // 当播放器准备好时，可以在这里进行一些操作
        },
        onError(player){
            this.$emit('error',player);
        },
        setStartEnd(type){
            this.$nextTick(()=>{
                this.addEscExitFullscreen()
                // const { clientHeight, clientWidth} = this.$refs.videoBox;
                // this.player = videojs(this.$refs.videoRef, {
                //     autoplay: false,
                //     controls: true,
                //     preload: 'metadata',
                //     width: clientWidth,
                //     height: 360
                // });
                // this.player.src({
                //     type: 'video/mp4',
                //     src: this.playUrl
                // });
                // console.log(clientWidth, clientHeight, this.player);
                

                if(!this.cycle){return}
                if(!this.player){return}
                if(this.startTime !== null){
                    this.start =this.startTime/1000 || 0;
                    if(type === 'init'){
                        this.$nextTick(()=>{
                            this.player?.currentTime(this.start);
                        })
                    }
                }
                if(this.endTime !== null){
                    this.end = this.endTime/1000;
                }

                this.vodeoPlayTime(this.player,type);
            })
        }
    },
    created() {
        this.setStartEnd('init');
    },
    mounted() {

    },
    beforeCreate() { }, //生命周期 - 创建之前
    beforeMount() { }, //生命周期 - 挂载之前
    beforeUpdate() { }, //生命周期 - 更新之前
    updated() { }, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() { }, //生命周期 - 销毁完成
    activated() { }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.videoContainer {
    background-color: #fff;
    display: flex;
    flex-direction: column;
    align-items: end;
    box-sizing: border-box;
    overflow: hidden;
}
.video {
    width: calc(100% - 0.5px);
    position: relative;
    ::v-deep(.vjs-control-bar){
        justify-content: space-between;
        align-items: center;
        opacity: 1 !important;
        .vjs-control{
            flex: 1;
        }
        .vjs-mute-control, .vjs-volume-panel{
            width: inherit !important;
        }
        .vjs-menu .vjs-menu-item-text, .vjs-playback-rate-value{
            font-size: 12px !important;
            line-height:2.3;
        }
        .vjs-fullscreen-control{
            .vjs-icon-placeholder:after{
                line-height: 1.6;
            }
        }
        .vjs-picture-in-picture-control{
            display: none;
        }
        .vjs-volume-bar.vjs-slider-horizontal{
            width: 100%;
        }
    }
}
</style>