<template>
    <el-row :gutter="10">
        <el-col :xs="12" :sm="8" :md="6" :lg="6" :xl="4" v-for="(item, index) in list" :key="index" class="mg-t10">
            <el-card :body-style="{ padding: '0px' }">
                <div style="padding: 14px;">
                    <div>
                        <Anchor :item="item.anchorInfo" :notLiveStatus="true"></Anchor>
                    </div>
                    <el-tooltip class="item" effect="dark" placement="top">
                        <div slot="content">
                            <span>视频名称：{{ item.videoName }}</span><br>
                            <span>直播标题：{{ item.liveTitle }}</span>
                        </div>
                        <div class="flex-jc-s flex-column pd-t10 font-s14">
                            <div class="videoNameText slh">
                                <span>视频名称：{{ item.videoName }}</span>
                            </div>
                            <div class="compereNameText slh">
                                <span>直播标题：{{ item.liveTitle }}</span>
                            </div>
                        </div>
                    </el-tooltip>
                    <div class="flex-jc-s flex-column pd-t10 font-s14">
                        <div>录制时间：{{ item.startTime.substring(0, 16) }}</div>
                        <div>录制时长：{{ item.durationStr }}</div>
                    </div>
                    <div class="flex-jc-s flex-column pd-t10 font-s14">
                       <span>文件大小: {{ item.vedioSizie }}M</span>
                    </div>
                    <div class="flex-jc-s flex-column pd-t10 font-s14">
                        <span>上传账号: {{ item.userNickName }}</span>
                    </div>
                    <div class="flex-jc-s flex-column pd-t10 font-s14">
                        <span>AI诊断报告: 
                            <span style="color: #15ACFE;cursor: pointer" @click="$emit('reportEd',item)" v-if="item.hasDiagnosisReport===1">
                                查看诊断分析
                            </span>
                            <span style="color: #ABAEB3;cursor:not-allowed" v-else>
                                查看诊断分析
                            </span>
                        </span>
                    </div>
                    <div class="pd-t20">
                        <template v-for="(btn,index) in menuConfig?.options">
                            <el-button type="text" @click="btn.click(item)" v-if="!(btn.hidden&& btn.hidden(item))">
                                <span :class="btn?.type === 'danger' ? 'text-colorErr' : ''">{{ btn.label }}</span>
                            </el-button>
                        </template>
                    </div>
                </div>
            </el-card>
        </el-col>
    </el-row>
</template>

<script>
import Anchor from '@/views/modules/dataAnalysis/component/common/anchor.vue';
export default {
    components: {
        Anchor
    },
    props:{
        list: {
            type: Array,
            default: ()=>{
                return []
            }
        },
        menuConfig: {
            type:Object,
            default: ()=>{
                return {}
            }
        }
    },
    data() {
        return {
        
        };
    },
    computed: {},
    watch: {},
    methods: {
        
    },
    created() {
        
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