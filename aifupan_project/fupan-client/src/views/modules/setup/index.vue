<template>
    <div style="padding: 20px;">
        <el-tabs v-model="currentMenuValue">
            <el-tab-pane v-for="item in menuList" :key="item.value" :label="item.label" :name="item.name"
                v-if="item.leavelVisible"></el-tab-pane>
        </el-tabs>
        <!-- 基本设置 -->
        <basic-setup v-if="currentMenuValue == 'basicSetup'"></basic-setup>
        <!-- 消息推送 -->
        <msg-push-setup v-if="currentMenuValue == 'pushMsg'"></msg-push-setup>
        <!-- 账号设置 -->
        <account-setup v-if="currentMenuValue == 'accountSetup'" @updateUserInfo="updateUserInfo"
            @updateMenuIndex="updateMenuIndex"></account-setup>
        <!-- 本地词库 -->
        <lexicon v-if="currentMenuValue == 'lexicon'"></lexicon>
        <!-- 联系客服 -->
        <customer-service v-if="currentMenuValue == 'customerService'"></customer-service>
        <!-- 子账号 -->
        <subAccount v-if="currentMenuValue == 'subAccount'"></subAccount>

    </div>
</template>

<script>
import basicSetup from './basicSetup.vue';
import msgPushSetup from "./msgPushSetup.vue";
import accountSetup from "./accountSetup.vue";
import customerService from "./customerService.vue";
import subAccount from "./subAccount.vue";
import lexicon from './lexicon.vue';
export default {
    components: {
        basicSetup,
        msgPushSetup,
        accountSetup,
        customerService,
        subAccount,
        lexicon
    },
    data() {
        return {

            // 当前用户信息
            user: {
                name: 'test',
                leavel: 'SVIP'
            },

            currentMenuValue: 'basicSetup',
            menuList: [
                { label: "基本设置", name: 'basicSetup', leavelVisible: true },
                // { label: "推送消息", name: 'pushMsg', leavelVisible: true },
                { label: "账号设置", name: 'accountSetup', leavelVisible: true },
                // { label: "子账号", name: 'subAccount', leavelVisible: true },
                { label: '本地词库', name: 'lexicon', leavelVisible: true },
                { label: "联系客服", name: 'customerService', leavelVisible: true },
            ],
        };
    },

    mounted() {
        this.getUserInfo();

        let setupMenu = this.$store.state.setupMenu;
        
        if (setupMenu) {
            this.currentMenuValue = setupMenu;
            this.$store.commit("saveSetupMenu", null);
        }
        // 只有当前用户是专业用户或旗舰用户才显示子账号入口
        // if (this.user.leavel == 'VIP' || this.user.leavel == 'SVIP') {
        //     this.menuList[4].leavelVisible = true
        // }
    },

    methods: {
        // 获取用户信息
        getUserInfo() {
            this.$httpBack.user.infoByClient({}).then(res => {
                if (res.code == 0 && res.data) {
                    if (!res.data.parentId || res.data.parentId == "0") {
                        this.menuList = [];
                        this.menuList.push({ label: "基本设置", name: 'basicSetup', leavelVisible: true });
                        this.menuList.push({ label: "账号设置", name: 'accountSetup', leavelVisible: true });
                        this.menuList.push({ label: '本地词库',name: 'lexicon',leavelVisible: true });
                        this.menuList.push({ label: "子账号", name: 'subAccount', leavelVisible: true });
                        this.menuList.push({ label: "联系客服", name: 'customerService', leavelVisible: true });
                    }
                }
            });
        },
        updateUserInfo() {
            this.$emit("updateUserInfo", "");
        },
        updateMenuIndex(menuIndex) {
            this.$emit("updateMenuIndex", menuIndex);
        }
    },
};
</script>

<style scoped>
.bodyContainer {
    padding: 40px 32px;
    display: flex;
}

.menuSelect {
    font-weight: 500;
    font-size: 14px;
    color: var(--color-main);
    cursor: pointer;
    margin-right: 52px;
    border-bottom: 2px var(--color-main) solid;
    height: 36px;
    line-height: 36px;
}

.menu {
    font-weight: 400;
    font-size: 14px;
    color: #2E3742;
    cursor: pointer;
    margin-right: 52px;
    height: 36px;
    line-height: 36px;
}

.menuContainer {
    display: flex;

    align-items: center;
    border-bottom: 1px solid #DCE0E7;
}
</style>