<template>
    <div class="title-box">
        <el-row>
            <el-col v-if="isTabs || isReturn" :span="12">
                <div class="title-left flex-ai-c cursor-pointer">
                    <div class="flex items-center" v-if="isReturn" @click="toPathUrl">
                        <el-button class="backspace" type="text" icon="el-icon-arrow-left"></el-button>
                        <span style="color: #484A4D" v-if="getReturnTitle">{{ getReturnTitle }}</span>
                    </div>
                    <div v-else-if="isTabs" class="title-left-tabs-container" :style="{padding:getTabsList[0].showTextKey?'0 20px 0 50px':'0 50px'}">
                        <Tabs class="title-left-tabs" v-model="getTabsName" :tabs="getTabsList" @click="(val)=>{setTabsName(val,'click')}"></Tabs>
                    </div>
                    <img v-if="getReturnLogo" src="@/assets/imgs/aiTitle.png" alt="" style="max-height: 28px;margin-left: 18px" />
                </div>
            </el-col>
            <el-col :span="isSpan">
                <div class="title-right">
                    <div class="user-info-content">
                        <template>
                            <MembershipExpires v-if="isShowMenberExpires" ></MembershipExpires>
                            <NetworkStatusBadge ref="network_status_badge" />
                            <ThirdPartyAuthStatusBadge v-if="false" ref="third_party_auth_badge" :platform="0" />
                            <Render v-if="$store.state.titleRightRender" :renderHandle="$store.state.titleRightRender"></Render>
                            <user-info v-else></user-info>
                            <slot name="title-right-after"></slot>
                        </template>
                    </div>
                </div>
            </el-col>
        </el-row>
        <NetworkFatigueDialog ref="network_fatigue_dialog" :platform="0" />
    </div>
</template>
<script>
/**
 * @description 顶部标题栏组件。
 * 负责组合返回区、标签页、会员提示、网络状态、第三方授权状态和用户信息展示。
 */
import Tabs from '/src/components/Tabs/index.vue'
import UserInfo from './userInfo.vue';
import tabs from './../../../../mixins/tabs.js';
import Render from './render.js';
import MembershipExpires from './membershipExpires.vue';
import NetworkStatusBadge from './networkStatusBadge.vue';
import ThirdPartyAuthStatusBadge from './thirdPartyAuthStatusBadge.vue';
import NetworkFatigueDialog from './networkFatigueDialog.vue';

export default {
    components: {
        UserInfo,
        Render,
        Tabs,
        MembershipExpires,
        NetworkStatusBadge,
        ThirdPartyAuthStatusBadge,
        NetworkFatigueDialog
    },
    mixins: [tabs],
    props:{
    },
    data() {
        return {
            // 用于阻止tabsMixin中的初始化
            notInitTabs: true
        };
    },
    computed: {
        getRouteMeta(){
            return this.$route.meta || {};
        },
        isTabs(){
            return (this.getTabsList.length> 0 && !(this.getRouteMeta?.notTabs))
        },
        isSpan(){
            return (!this.isTabs && !this.isReturn ) ? 24 : 12
        },
        isReturn(){
            return this.getRouteMeta.returnPath !== 'undefined' ? this.getRouteMeta.returnPath : false;
        },
        getReturnTitle(){
            return this.getRouteMeta.title || '';
        },
        getReturnLogo(){
            return this.getRouteMeta.logo || false;
        },
        isFree(){
            return this.$store.getters.isFree
        },
        getUserInfo() {
            return this.$store.state.userInfo;
        },
        isDiffSevenDays() {
            const now = new Date()
            const target = new Date(this.getUserInfo?.expirationDate)

            if (target instanceof Date && !isNaN(target.getTime())) {
                now.setHours(0, 0, 0, 0)
                target.setHours(0, 0, 0, 0)
                const diffTime = target - now // 毫秒差
                const diffDays = diffTime / (1000 * 60 * 60 * 24)

                return Math.abs(diffDays) <= 7
            } else {
                return false
            }
        },
        isShowMenberExpires(){
            return !this.isFree && this.isDiffSevenDays
        },
    },
    watch: {
    },
    methods: {
        toPathUrl(){
            if(this.isReturn){
                if(typeof this.$store.state.routerPathBack === 'function'){
                    this.$store.state.routerPathBack();
                }else if(typeof this.isReturn === 'boolean'){
                    this.$router.back();
                }else{
                    this.$router.go(this.isReturn)
                }
            }
        }
    },
    created() {

    },
    mounted() {},
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
.backspace{
    font-size: 24px!important;
    border: none;
    color: #484A4D;
    background: transparent!important;
    padding: 12px 15px!important;
    &:hover{
        color: #484A4D;
    }
}
.title-box{
    padding: 0 10px;
    //border-bottom: 1px solid #E4E7ED;
}
.title-left-tabs-container {
    display: inline-block;
    border-radius: 12px;
    background: #fff;
    margin-top: 8px;
    width: auto;

    .title-left-tabs {
        ::v-deep(.el-tabs__header) {
            margin: 0;
        }

        ::v-deep(.el-tabs__item) {
            height: 38px;
            line-height: 38px;
            position: relative;
            padding: 0 50px;

            &::after {
                content: ' ';
                position: absolute;
                left: 0;
                top: 12px;
                width: 1px;
                height: 14px;
                background: #cccccc;
            }
        }

        ::v-deep(.el-tabs__nav-wrap::after) {
            height: 0;
        }

        ::v-deep(.el-tabs__active-bar ) {
            & + .el-tabs__item {
                &::after {
                    width: 0;
                }
            }
        }
    }
}
.title-right{
    display: flex;
    flex-direction: row-reverse;
}
.user-info-content{
    height: 46px;
    display: flex;
    align-items: center;
}
</style>
