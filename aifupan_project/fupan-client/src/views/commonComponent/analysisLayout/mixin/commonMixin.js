import {PLATFORM_TYPE_ENUM} from '@/enum';
export default {
    props:{
        // 用于区分对比分析
        name: {
            type: String,
            default: ''
        },
        isCompare: {
            type: Boolean,
            default: false
        },
        // 数据
        sentenceMarkData: {
            type: Object,
            required: ()=>{return {}},
            require: true
        },
        readonly: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            sentenceMarkList: this.sentenceMarkData?.sentenceMarkList,
            videoInfo: this.sentenceMarkData?.videoInfo,
            anchorInfo: this.sentenceMarkData?.anchorInfo,
            fileInfo: this.sentenceMarkData?.fileInfo,
            isAiElfStatus:true
        };
    },
    computed: {
        getPlayUrl() {
            return this.sentenceMarkData?.playUrl
        },
        
        isVideoId() {
            return !!(this.videoInfo && this.videoInfo?.VideoId)
        },
        isFileId(){
            return !!(this.fileInfo && this.fileInfo?.fileId) && this.fileInfo?.fileType !== 2;
        },
        isVideo(){
            return this.isVideoId || this.isFileId;
        },
        isFileText(){
            // 云空间视频详情偶尔会同时携带 uploadFile/fileInfo，不能仅凭 fileType=2 就把视频误判成纯文本。
            return !this.isVideoId && this.fileInfo?.fileType === 2;
        },
        isPlayUrl() {
            return !this.isFileText;
            // return !!this.getPlayUrl 
        },
        isWebOnline() {
            return this.targetType === 'webOnline'
        },
        isOnline(){
            return this.targetType === 'online'
        },
        getAnalysisId(){
            return this.videoInfo?.VideoId || this.fileInfo?.fileId
        },
        isAiElf(){
            // if(this.fileInfo?.fileType == 2){return false}
            // 'online'
            // return (['webOnline'].every(d=>d !== this.targetType));
            return this.isAiElfStatus
            // return (['online'].every(d=>d !== this.targetType));
        },
        getId() {
            if(this.isCompare){
                // console.log(this.sentenceMarkData?.info?.ContrastId,'--------sentenceMarkData?.info?.ContrastId')
                return this.sentenceMarkData?.info?.ContrastId || this.sentenceMarkData?.fileInfo?.fileId;
            }
            return this.getAnalysisId
        },
        getSourceType() {
            return this.isCompare ? 2 : this.isVideoId ? 0 : 1; // 源类型
        },
        getPlatform(){
            return this.videoInfo?.PlatformType || this.fileInfo?.platformType
        },
        isKuaishou() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.kuaishou
        },
        isShipinhao() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.shipinhao
        },
        isDouyin() {
            return this.getPlatform == PLATFORM_TYPE_ENUM.douyin
        },
        /**
         * @description 是否为自有账号（AccountType === 0 表示自有账号）
         * 兼容云空间场景：云空间API返回的AccountType在basicSettingsVo中，而非anchorInfo中
         */
        isSelfAccount() {
            const accountType = Number(
                this.sentenceMarkData?.basicSettingsVo?.accountType ??
                this.sentenceMarkData?.videoInfo?.basicSettingsVo?.accountType ??
                this.sentenceMarkData?.videoInfo?.accountType ??
                this.sentenceMarkData?.anchorInfo?.accountType ??
                this.sentenceMarkData?.anchorInfo?.AccountType ??
                0
            )
            return accountType === 0
        },
    },
    methods: {
        // 初始化数据
        initSentenceMarkData(){
            this.setSentenceMarkData(this.sentenceMarkData)
        },
        // 设置数据
        setSentenceMarkData(data) {
            this.$set(this, 'sentenceMarkList', data.sentenceMarkList)
            this.$set(this, 'videoInfo', data.videoInfo)
            this.$set(this, 'anchorInfo', data.anchorInfo)
            this.$set(this, 'fileInfo', data.fileInfo)
        },
        // 获取平均语速
        // getCharCountNum(){
        //     let charCountNum = 0;
        //     // 计算语速
        //     if (this.sentenceMarkList && this.sentenceMarkList.length > 0) {
        //         this.sentenceMarkList.forEach(item => {
        //             let tempContent = item.content.replaceAll("，", "").replaceAll("。", "").replaceAll("？", "").replaceAll("、", "");
        //             charCountNum += tempContent.length;
        //             this.$set(item, 'charNumSecond', tempContent.length / ((item.endTime - item.startTime) / 1000) * 60);
        //         })
        //     }
        //     return charCountNum;
        // }
    },
    mounted() {
        this.$nextTick(()=>{
            this.initSentenceMarkData()
        })
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {
        this.$nextTick(()=>{
            this.initSentenceMarkData()
        })
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
