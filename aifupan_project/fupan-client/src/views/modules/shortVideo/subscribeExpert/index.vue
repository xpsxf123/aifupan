<template>
    <div class="subscribe-expert-container">
        <el-row style="height: 100%">
            <el-col v-if="getTabsName === 'first'" :span="24" style="height: 100%">
                <SubscribeList ref="subscribeList" @addExpert="addExpert" :selectGroupId="selectGroupId" :pendingAutoSyncExpert="pendingAutoSyncExpert"/>
            </el-col>
            <el-col v-if="getTabsName === 'second'" :span="24" style="height: 100%">
                <ManageGroups @addExpert="addExpert" @viewList="viewList"/>
            </el-col>
        </el-row>
        <AddSubscribeExpert :visible.sync="addExpertVisible" @handleExpert="handleExpert"/>
    </div>
</template>

<script>
import tabs from "@/mixins/tabs";
import SubscribeList from './component/subscribeList.vue';
import ManageGroups from './component/manageGroups.vue'
import AddSubscribeExpert from "./component/addSubscribeExpert.vue";

export default {
    components: {
        AddSubscribeExpert,
        SubscribeList,
        ManageGroups
    },
    mixins: [tabs],
    data() {
        return {
            addExpertVisible: false,
            tabs: [{label: '订阅列表', name: 'first'}, {label: '管理分组', name: 'second'}],
            selectGroupId: '',
            refresh: null,
            pendingAutoSyncExpert: null
        }
    },
    watch: {
        getTabsName: {
            handler(newVal,oldVal) {
                if(!(newVal==='first'&&this.selectGroupId)){
                    this.selectGroupId = ''
                }
            },
            deep: true,
        }
    },
    created() {

    },
    methods: {
        viewList(groupId) {
            this.selectGroupId = groupId
            this.getTabsName = 'first'
        },
        addExpert(status, refresh) {
            this.addExpertVisible = status
            this.refresh = refresh
        },
        async handleExpert(expertData) {
            this.addExpertVisible = false
            if (this.refresh) {
                await this.refresh()
            }
            this.pendingAutoSyncExpert = expertData ? { ...expertData } : null
        }
    }
}
</script>

<style lang="scss" scoped>
.subscribe-expert-container {
    //padding-block: 10px;
    height: 100%;
}
</style>