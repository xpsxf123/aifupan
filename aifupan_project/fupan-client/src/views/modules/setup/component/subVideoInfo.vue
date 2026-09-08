<template>
    <div class="sub-video-info">
        <el-drawer
            title="我是标题"
            :visible.sync="drawer"
            :wrapperClosable="false"
            :destroy-on-close="true"
            :close-on-press-escape="false"
            size="72vw"
            :with-header="false">
            <div class="header flex items-center">
                <el-button
                    type="text"
                    class="close-btn"
                    icon="el-icon-close"
                    @click="()=>changeStatus(false)">
                </el-button>
                <span class="title">子账号录制信息</span>
            </div>
            <el-tabs v-model="activeName" @tab-click="handleClick">
                <el-tab-pane label="录制主播数" name="first"/>
                <el-tab-pane label="昨日录制场次" name="second"/>
                <el-tab-pane label="昨日小结" name="third"/>
            </el-tabs>
            <div>
                <el-radio-group v-model="accountType">
                    <el-radio :label="''" class="radio-as-checkbox">全部账号</el-radio>
                    <el-radio :label="0" class="radio-as-checkbox">自有账号</el-radio>
                    <el-radio :label="1" class="radio-as-checkbox">同行业账号</el-radio>
                </el-radio-group>
            </div>
            <Anchor v-if="activeName==='first'" :currentItem="currentItem" :accountType="accountType"/>
            <Session v-if="activeName==='second'" :currentItem="currentItem" :accountType="accountType"/>
            <Summary v-if="activeName==='third'" :currentItem="currentItem" :accountType="accountType"/>
        </el-drawer>
    </div>
</template>

<script>
import Anchor from './anchor.vue'
import Session from './session.vue'
import Summary from './summary.vue'

export default {
    components: {Anchor, Session, Summary},
    data() {
        return {
            drawer: false,
            activeName: 'first',
            currentItem: {},
            accountType: ''
        };
    },
    watch: {
        activeName: {
            handler(val) {
                if (val) this.accountType = ''
            },
            deep: true
        }
    },
    mounted() {

    },
    methods: {
        changeStatus(status, item, currentTab) {
            this.currentItem = item
            this.activeName = currentTab || this.activeName
            this.drawer = status
        },
        handleClick() {

        }
    },
};
</script>

<style lang='scss' scoped>
.sub-video-info {
    .header {

        .close-btn {
            padding-right: 12px;
            font-size: 20px;
            color: #DCDCDC;
        }
    }

    ::v-deep(.el-drawer__body) {
        padding-inline: 12px;
    }

    ::v-deep(.el-tabs__active-bar) {
        height: 2px;
        background: var(--color-main);
        border-radius: 29px;
    }

    ::v-deep(.el-tabs__nav-wrap) {
        &::after {
            display: none;
        }
    }
}
</style>