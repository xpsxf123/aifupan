<template>
    <div>
        <div style="margin-top: 8px; display: flex;flex-direction: column; align-items: center; cursor: pointer;"
            @click="showUpdateDialog" >
            <div style="color: red;font-size: 12px;margin-top: 5px;" v-if="versionInfo && versionInfo.VersionNum">(可更新)</div>
            <div style="font-size: 12px; color: #aaa;">版本号：{{ configInfo.SerialNumber }}</div>
        </div>
        <!-- <update-version ref="updateVersion"></update-version> -->
    </div>
</template>

<script>
// import UpdateVersion from './../../../commonComponent/updateVersion.vue';
export default {
    components: {
        // UpdateVersion
    },
    props:{
        
    },
    data() {
        return {
            versionInfo: {
                VersionNum: "",
            },
            updateVersionTimeout: null
        };
    },
    computed: {
        configInfo(){
            return this.$store.getters?.getBiscInfo || {};
        }
    },
    watch: {
        "$route.path"(){
            this.getVersionUpdate('watchPath');
        }
    },
    methods: {
        // 新代弹窗(录制中弹窗)
        showUpdateDialog() {
            if (this.versionInfo && this.versionInfo.VersionNum) {
                this.$nextTick(() => {
                    this.$httpClient.setup.handUpdateVersion();
                })
            }
        },
        // 更新版本(录制中不弹窗)
        updateVersion() {
            if (this.versionInfo && this.versionInfo.VersionNum) {
                this.$nextTick(() => {
                    this.$httpClient.setup.updateVersion();
                })
            }
        },
        // 获取版本更新信息
        getVersionUpdate(type) {
            this.$httpClient.setup.getVersionUpdate({}).then(res => {
                this.versionInfo = res.data;
                if (this.versionInfo && this.versionInfo.VersionNum) {
                    if (!localStorage.getItem(this.versionInfo.VersionNum)) {
                        if(type == 'watchPath'){
                            this.updateVersion();
                        }else{
                            // this.showUpdateDialog();
                        }
                    }
                }
            });
        },
    },
    created() {
        this.getVersionUpdate();
        // 清空定时器
        if (this.updateVersionTimeout) {
            clearInterval(this.updateVersionTimeout);
        }
        this.updateVersionTimeout = setInterval(() => {
            this.getVersionUpdate();
        }, 300000);
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