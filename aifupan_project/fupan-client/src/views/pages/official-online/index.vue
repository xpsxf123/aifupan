<template>
    <div style="height: calc(100vh);width: 100%;" class="official-box pd-t10">
        <el-tabs v-model="activeName" class="main-bg">
            <el-tab-pane label="单场复盘" name="only">
                <onlineVideo v-if="activeName === 'only'" type="video" ref="onlineVideo" isIframe   tableH="calc(100vh  - 255px)">
                    <template #empty>
                        <div style="line-height: 26px;" class="text-center">
                            <img style="max-width: 270px;" src="@/assets/imgs/onlin-empty.png" alt="" srcset="">
                            <div class="text-center text-colorMain">云空间，可将优秀的直播场次和复盘分析结果存储<br>到云端，并分享给工作伙伴进行协作复盘</div>
                            <div class="text-center font-s12 text-color3">目前仅支持由“爱复盘软件”自动录制分析的<br>素材上传到云空间</div>
                        </div>
                    </template>
                </onlineVideo>
            </el-tab-pane>
            <el-tab-pane label="对比分析" name="contrast">
                <onlineContrast  v-if="activeName === 'contrast'" ref="onlineContrast" isIframe   tableH="calc(100vh - 255px)">
                    <template #empty>
                        <div style="line-height: 26px;">
                            <img style="max-width: 270px;" src="@/assets/imgs/onlin-empty.png" alt="" srcset="">
                            <div class="text-center text-colorMain">云空间，可将优秀的直播场次和复盘分析结果存储<br>到云端，并分享给工作伙伴进行协作复盘</div>
                            <div class="text-center font-s12 text-color3">目前仅支持由“爱复盘软件”自动录制分析的<br>素材上传到云空间</div>
                        </div>
                    </template>
                </onlineContrast>
            </el-tab-pane>
            <el-tab-pane label="切片分析" name="sliceVideo">
                <onlineVideo v-if="activeName === 'sliceVideo'" type="slice" ref="sliceVideo" isIframe  tableH="calc(100vh  - 255px)">
                    <template #empty>
                        <div style="line-height: 26px;" class="text-center">
                            <img style="max-width: 270px;" src="@/assets/imgs/onlin-empty.png" alt="" srcset="">
                            <div class="text-center text-colorMain">云空间，可将优秀的直播场次和复盘分析结果存储<br>到云端，并分享给工作伙伴进行协作复盘</div>
                            <div class="text-center font-s12 text-color3">目前仅支持由“爱复盘软件”自动录制分析的<br>素材上传到云空间</div>
                        </div>
                    </template>
                </onlineVideo>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import myUtils from '@/utils/utils';
import onlineVideo from '../../modules/replay/online/online-video.vue';
import onlineContrast from '../../modules/replay/online/online-contrast.vue';
export default {
    components: {
        onlineVideo,
        onlineContrast
    },
    props:{
        
    },
    data() {
        return {
            activeName: 'only'
        };
    },
    computed: {
        userProperty(){
            return this.$store.getters.getUserproperty
        },
        getSpace(){
            if(this.userProperty?.totalStorageNum === undefined || this.userProperty?.storageNum === undefined){
                return '0/0 G';
            }
            let a = (this.userProperty?.totalStorageNum - this.userProperty?.storageNum) / 1024 / 1024;
            let b = this.userProperty?.totalStorageNum / 1024 / 1024;
            return `${this.retainDecimals(a)} / ${this.retainDecimals(b)} G`
        },
        getOnlineDom(){
            if (this.activeName === 'only') {
                return this?.$refs?.onlineVideo
            } else if (this.activeName === 'contrast') {
                return this.$refs?.onlineContrast
            } else {
                return this?.$refs?.sliceVideo
            }
        }
    },
    
    watch: {},
    methods: {
        retainDecimals(val) {
            return myUtils.retainDecimals(val);
        },
        getUserProperty(){
            this.$httpBack.userProperty.getUserProperty({},{load:false}).then(res => {
                if (res.code === 0 && res.data) {
                    this.$store.commit("saveUserproperty", res.data);
                }
            });
        },
        getList(){
            this.getOnlineDom?.getList()
        },
        initPostMessage(){
            window.addEventListener('message', (event) => {
                if (event.data.type === 'get-online') {
                    // 响应父窗口
                    this.getList();
                }
            })
        }
    },
    created() {
        this.initPostMessage()
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>