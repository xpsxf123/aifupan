<template>
    <div class="contrast-item-box analysis-flex-column">
        <analysisTitle ref="title" readonly :notWords="notWords" :sentenceMarkData="sentenceMarkData" :wordsInfo="wordsInfo" >
            <template #anchor-left>
                <slot name="anchor-left"></slot>
            </template>
            <template #anchor-right>
                <slot name="anchor-right"></slot>
            </template>
            <template #time-top>
                <slot name="time-top"></slot>
            </template>
            <template #word-control="{ videoInfo, fileInfo }">
                <slot name="word-control" v-bind="{ videoInfo, fileInfo }"></slot>
            </template>
            <template #title-center>
                <slot name="title-center"></slot>
            </template>
        </analysisTitle>
        <!-- 视频/音频分析 -->
        <div class="videoAnalysisContainer pd-t6">
            <div class="videoAndTradeContainer mg-r12" :style="'width: ' + videoWidth">
                <!-- 视频/音频 -->
                <contrastVideo ref="videoPlayer"  readonly :sentenceMarkData="sentenceMarkData"
                @setDuration="setDuration" @playerTimeupdate="onPlayerTimeupdate" :targetType="targetType">
                    <template #video-bottom>
                        <slot name="video-bottom"></slot>
                    </template>
                </contrastVideo>
            </div>
            <wordDiscern ref="textDom" class="h100" readonly :hideContent="hideContent" :sentenceMarkData="sentenceMarkData" @playerReadied="onPlayerReadied">
                <template #tabs="item">
                    <slot name="tabs" v-bind="item"></slot>
                </template>
                <template #content>
                    <slot name="content"></slot>
                </template>
                <template #content-after>
                    <slot name="content-after"></slot>
                </template>
                <template #content-before>
                    <slot name="content-before"></slot>
                </template>
            </wordDiscern>
            
        </div>
    </div>
</template>

<script>
import analysisTitle from './component/analysisTitle.vue';
import wordDiscern from './component/wordDiscern.vue';
import Sensitive from '/src/components/analysis/sensitive.vue'
import Keyword from '/src/components/analysis/keyword.vue'
import Pace from '/src/components/analysis/pace.vue'
import DiscernSearchContainer from '/src/components/DiscernSearchContainer/index.vue'
import videoPlayer from '/src/components/analysis/videoPlayer.vue';
import contrastVideo from './contrast-video.vue';
import LocatingBar from '/src/components/analysis/locatingBar.vue';
import wordsMixin from './mixin/wordsMixin';
import textMixin from './mixin/textMixin';
import commonMixin from './mixin/commonMixin'
import publicMixin from './mixin/publicMixin';
import Title from '/src/components/title/index.vue';
import {VERSION_TYPE} from "@/enum";

export default {
    components: {
        analysisTitle,
        wordDiscern,
        videoPlayer,
        contrastVideo,
        LocatingBar,
        DiscernSearchContainer,
        Sensitive,
        Keyword,
        Pace,
        Title
    },
    mixins: [commonMixin, publicMixin, wordsMixin, textMixin],
    props: {
        // 暂时不用
        type: {
            type: String,
            default: ''
        },
        deafultWordsInfo: {
            type: Object,
            default: null
        },
        // 视频播放器宽度，如：28%
        videoWidth: {
            type: String,
            default: '23%'
        },
        targetType: {
            type:String,
            default: ''
        },
        isCompare: {
            type:Boolean,
            default: false
        },
        notWords: {
            type: Boolean,
            default: false
        },
        hideContent:{
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            VERSION_TYPE,
            wordsInfo: {}
        };
    },
    watch: {
    },
    computed: {
        getVideoVnode() {
            return this.$refs?.videoPlayer
        },
        // 重写节点
        getTextVnode() {
            return this.$refs?.textDom
        },
        versionType(){
            return this.$store.getters.getVersionType
        }
    },
    methods: {
    /**
     * 修复视频并通知父组件
     * @param {boolean} val - 修复视频的状态
     */
    repairVideo(val) {
        this.$emit('repairVideo', val);
    },
    onPlayerReadied(second) {
        this.getVideoVnode.ponlayerReadied(second);
    },
    /**
     * 播放器进度回调
     * @param {number} time - 当前播放时间（秒）
     */
     onPlayerTimeupdate(time) {
        this.getTextVnode?.setVideoCurrentTime(time);
    },
    /**
     * 设置视频时长和字符数量
     * @param {Object} param - 包含视频时长和数学时长的对象
     * @param {number} param.videoDuration - 视频时长（秒）
     * @param {number} param.MathDuration - 数学时长（秒）
     */
    setDuration({ videoDuration, MathDuration }) {
        this.$nextTick(() => {
            this.getTextVnode?.marksTitle(MathDuration);
            this.$refs.title.setCharAndTime(videoDuration);
        });
    },
    /**
     * 添加视频修复成功的事件监听
     */
    addReEncodeSuccess() {
        this.$CSharpNotify.addTask('reEncodeSuccess', (res, resolve) => {
            this.getVideoVnode?.reEncodeSuccess();
            this.$notify({
                title: '修复成功',
                message: '视频修复成功',
                duration: 3000,
                type: 'success'
            });
        });
    },

    /**
     * 添加视频修复失败的事件监听
     */
    addReEncodeError() {
        this.$CSharpNotify.addTask('reEncodeFail', (res, resolve) => {
            this.getVideoVnode?.reEncodeError();
            this.$notify({
                title: '修复失败',
                message: '视频修复失败',
                duration: 3000,
                type: 'error'
            });
        });
    },

    /**
     * 检查视频修复是否正在进行，如果正在进行则提示用户是否中断修复
     * @param {Function} callback - 回调函数，用于处理修复状态
     */
    isRepairStop(callback) {
        if (this.getVideoVnode) {
            this.getVideoVnode?.isRepairStop(callback);
        } else {
            callback(true);
        }
    }
},
    created() {
        // 添加频错误修复事件监听
        this.addReEncodeSuccess();
        // 添加视频错误修复事件监听
        this.addReEncodeError()
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
};
</script>
<style scoped>
.discernContainer {
    /* margin-left: 12px; */
    box-sizing: border-box;
    flex-grow: 1;
    width: 0;
    position: relative;
    display: flex;
    flex-direction: column;
}

.videoAnalysisContainer {
    display: flex;
    margin-top: 10px;
    flex-grow: 1;
    height: calc(100% - 138px);
    /* height: calc(100% - 116px); */
}

.contrast-item-box {
    display: flex;
    flex-direction: column;
    height: 100%;
    /* min-height: 400px; */
    /* >div{
        flex: 1;
    } */
}
</style>
