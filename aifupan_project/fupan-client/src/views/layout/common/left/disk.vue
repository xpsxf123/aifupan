<template>
    <div class="leftBottomContainer">
        <div class="diskContainer">
          <div class="flex-ji-c">本地磁盘空间<el-button type="text" :icon="getIcon" @click="getDisk"></el-button></div>
          <div>
            <span>{{ diskInfo.driveUsedSpace + "G / " }}</span>
            <span>{{ diskInfo.driveTotalSize + "G" }}</span>
          </div>
          <div style="width: 80%;padding-top: 9px;">
            <el-progress 
            :percentage="percentage" 
            :text-inside="true"
            :show-text="false"
            :stroke-width="6"
            :color="customColors"
            ></el-progress>
          </div>
          <!-- <el-button type="text" @click="rechargeTime">申请扩容</el-button> -->
        </div>
        <!-- <customer-service-qr-code  ref="customerServiceQrCode"></customer-service-qr-code> -->
    </div>
</template>

<script>
// src\views\commonComponent\customerServiceQrCode.vue
// import CustomerServiceQrCode from './../../../commonComponent/customerServiceQrCode.vue'
export default {
    components: {
        // CustomerServiceQrCode,
    },
    props:{
        
    },
    data() {
        return {
            diskInfo: {},
            iconRef: 'el-icon-refresh',
            iconLoad:'el-icon-loading',
            loadDisk: false,
            customColors: [
                {color: '#1989fa', percentage: 90},
                {color: '#f56c6c', percentage: 100},
            ]
        };
    },
    computed: {
        percentage(){
            return (parseInt(parseFloat(this.diskInfo.driveUsedSpace/this.diskInfo.driveTotalSize) * 10000) / 100) || 0; 
        },
        configInfo(){
            return this.$store.getters?.getBiscInfo || {};
        },
        getIcon(){
            return this.loadDisk? this.iconLoad : this.iconRef
        }
    },
    watch: {},
    methods: {
        // 在线复盘时长充值
        // rechargeTime() {
        //     // this.kefuDialogVisible = true;
        //     this.$nextTick(() => {
        //         this.$refs.customerServiceQrCode.init()
        //     })
        // },
        // 读盘空间
        getDisk() {
            if(this.loadDisk)return;
            this.loadDisk = true
            this.$httpClient.setup.getdisksize({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.diskInfo = res.data;
                    if (this.diskInfo.driveAvailablepace < 10) {
                        // 磁盘空间不足10G
                        if (this.configInfo && this.configInfo.IsRocord == 1) {
                            this.$notify({
                                title: '磁盘空间不足',
                                message: '磁盘可用空间已不足10G，为保证系统正常运行，将停止录制',
                                duration: 0,
                                type: 'error'
                            });
                            // 当前正在录制
                            // this.$message({
                            //     showClose: true,
                            //     message: '磁盘可用空间已不足10G，为保证系统正常运行，将停止录制',
                            //     type: 'error',
                            //     duration: 0
                            // });
                            // 去列表页停止检测
                            this.$store.commit("saveStopRecord", true);
                        }
                    }
                }else{
                    this.$notify({
                        title: '磁盘无法访问',
                        message: '请检查磁盘是否正常',
                        duration: 5000,
                        type: 'error'
                    });
                }
            }).catch(err => {
                this.$notify({
                    title: '磁盘无法访问',
                    message: '请检查磁盘是否正常',
                    duration: 5000,
                    type: 'error'
                });
            }).finally(()=>{
                setTimeout(()=>{
                    this.loadDisk = false
                },2000)
            })
        },
    },
    created() {
        this.getDisk();
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
.diskContainer {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 12px;
  color: #2E3742;
  width: 100%;
  padding: 0 10px;
}
.leftBottomContainer {
    width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}
::v-deep(.el-progress-bar__outer){
    background: #D9D9D9 !important;
}
</style>