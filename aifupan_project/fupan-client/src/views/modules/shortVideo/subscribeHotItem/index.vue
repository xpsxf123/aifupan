<template>
    <div style="height: 100%">
        <el-row style="height: 100%">
            <el-col v-if="getTabsName === 'first'" :span="24" style="height: 100%">
                <SubscribeHotItemList
                    :selectGroupId="selectGroupId"
                    @addSubscribe="addSubscribe"
                    @getSubscribeData="getSubscribeData"
                    @handleSubscribe="handleSubscribe"/>
            </el-col>
            <el-col v-if="getTabsName === 'second'" :span="24" style="height: 100%">
                <SubscribeHotItemGroup
                    @addSubscribe="addSubscribe"
                    @viewList="viewList"/>
            </el-col>
        </el-row>

        <AddSubscribe
            @handleSubscribe="handleSubscribe"
            @refreshFun="refreshFun"
            :subscribeData="subscribeData"
            :visible.sync="addSubscribeVisible"/>

        <WarmHint ref="warmHint" @left-click="warmLeftClick" @right-click="warmRightClick" rightBt="立即充值"
                  leftBt="知道了">
            当前账号订阅关键词数量已满，请升级或充值
        </WarmHint>
    </div>
</template>

<script>
import tabs from "@/mixins/tabs";
import SubscribeHotItemList from './component/subscribeHotItemList.vue'
import SubscribeHotItemGroup from './component/subscribeHotItemGroup.vue'
import AddSubscribe from "./component/addSubscribe.vue";
import WarmHint from "@/components/warmHint/index.vue";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    components: {WarmHint, AddSubscribe, SubscribeHotItemList, SubscribeHotItemGroup},
    mixins: [tabs, userAssets],
    inject: ['APP'],
    data() {
        return {
            subscribeData: {},
            addSubscribeVisible: false,
            tabs: [{label: '订阅列表', name: 'first'}, {label: '管理分组', name: 'second'}],
            selectGroupId: '',
            refresh: null
        }
    },
    created() {

    },
    watch: {
        addSubscribeVisible: {
            handler(val) {
                if (!val) this.subscribeData = {};
            },
            immediate: true,
            deep: true
        },
        getTabsName: {
            handler(newVal,oldVal) {
                if(!(newVal==='first'&&this.selectGroupId)){
                    this.selectGroupId = ''
                }
            },
            deep: true,
        }
    },
    methods: {
        viewList(groupId) {
            this.selectGroupId = groupId
            this.getTabsName = 'first'
        },
        async addSubscribe(data, refresh) {
            const {userType} = this.$store.getters.getUserInfo; // 获取用户类型，0为主帐号，2为子帐号
            if (!data.subscriptionId) {
                const {useSubscribeHotVideoNum, totalSubscribeHotVideoNum} = this.userProperty
                if (Number(useSubscribeHotVideoNum) >= Number(totalSubscribeHotVideoNum)) {
                    if (userType === 0) {
                        this.$refs.warmHint.show()
                        return
                    } else if (userType === 2) {
                        return this.$confirm('订阅关键词数量已满，请删除后再添加', '友情提示', {
                            confirmButtonText: '去删除',
                            cancelButtonText: '知道了',
                            type: 'warning'
                        }).then(() => {
                        }).catch(() => {
                        })
                    }
                }
            }
            this.refresh = refresh
            this.subscribeData = data || {};
            this.addSubscribeVisible = true;
        },
        async handleSubscribe(isEdit, data, callback) {
            if (data) {
                const server = isEdit ? this.$httpBack.shortVideo.subscriptionHotEdit : this.$httpClient.shortVideo.addHotSearch
                try {
                    const result = await server(data)
                    if (result.code !== 0) return
                    this.$message.success(`${isEdit ? '编辑' : '添加'}成功`)
                    callback?.()
                } catch (e) {
                }
            }
        },
        getSubscribeData(data, refresh) {
            this.subscribeData = {...data, autoSync: true}
            this.refresh = refresh
            this.addSubscribeVisible = true;
        },
        refreshFun() {
            this.refresh?.()
        },
        warmLeftClick() {
            this.$refs.warmHint.hide()
        },
        warmRightClick() {
            this.$refs.warmHint.hide()
            this.APP?.showQrCode();
        }
    }
}
</script>

<style lang="scss" scoped>
.subscribe-expert-container {

}
</style>