<template>
    <div class="main-bg pd-16 brs-10" style="height: 100%">
        <!-- {{ getTabsName }} -->
        <!-- 基本设置 -->
        <basic-setup v-if="ifTabsIndex(0)"></basic-setup>
        <!-- 消息推送 -->
        <!-- <msg-push-setup v-if="ifTabsIndex(1)"></msg-push-setup> -->
        <!-- 账号设置 -->
        <account-setup  v-if="ifTabsIndex(1)"></account-setup>
        <!-- 套餐权益 -->
        <package v-if="ifTabsIndex(2)"></package>
        <!-- 子账号 -->
        <subAccount v-if="ifTabsIndex(3)"></subAccount>
        <!-- 本地词库 -->
        <lexicon v-if="ifTabsIndex(4)"></lexicon>
        <!-- 联系客服 -->
        <customer-service  v-if="ifTabsIndex(5)"></customer-service>
        
        <!-- <hr> -->
        <!-- <setup></setup>  -->
    </div>
</template>

<script>
import tabs from '@/mixins/tabs.js';
import basicSetup from './../setup/basicSetup.vue';
import msgPushSetup from "./../setup/msgPushSetup.vue";
import accountSetup from "./../setup/accountSetup.vue";
import customerService from "./../setup/customerService.vue";
import subAccount from "./../setup/subAccount.vue";
import setup from './../setup/index.vue'
import lexicon from './../setup/lexicon.vue';
import Package from './../setup/package.vue';
export default {
    components: {
        basicSetup,
        msgPushSetup,
        accountSetup,
        customerService,
        subAccount,
        setup,
        lexicon,
        Package
    },
    mixins: [tabs],
    props:{
        
    },
    data() {
        return {
            tabs: [
                { label: '基本设置', name: 'basicSetup'}, 
                { label: '账号设置', name: 'accountSetup'},
                { label: '套餐权益', name: 'package'},
                { label: '子账号', name: 'subAccount', hidde:(item)=>{
                    return this.userInfo.parentId || res.data.parentId == "0";
                }},
                { label: '本地词库', name: 'lexicon' },
                { label: '联系客服', name: 'customerService'},
            ],
            // menuList: [],
            userInfo: {}
        };
    },
    computed: {},
    watch: {},
    methods: {
        // 获取用户信息
        getUserInfo() {
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    this.userInfo = res.data;
                    // if (!res.data.parentId || res.data.parentId == "0") {
                    //     this.menuList = [];
                    //     this.menuList.push({ label: "基本设置", name: 'basicSetup', leavelVisible: true });
                    //     this.menuList.push({ label: "账号设置", name: 'accountSetup', leavelVisible: true });
                    //     this.menuList.push({ label: "子账号", name: 'subAccount', leavelVisible: true });
                    //     this.menuList.push({ label: "联系客服", name: 'customerService', leavelVisible: true });
                    // }
                }
            });
        },
    },
    created() {
        // 获取用户信息。
        this.getUserInfo();
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