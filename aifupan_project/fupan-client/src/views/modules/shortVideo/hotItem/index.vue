<template>
    <div class="hot-item">
        <Search :config="config" @getAction="getAction" :loading="loading">
            <HistoryList v-if="actionType==='history'"/>
            <SearchList
                v-if="actionType==='search'"
                @loadingEnd="loadingEnd"
                :searchValue="searchValue"/>
        </Search>
    </div>
</template>

<script>
import Search from './../component/search.vue'
import SearchList from './component/searchList.vue'
import HistoryList from './component/historyList.vue'

export default {
    components: {Search, SearchList, HistoryList},
    props: {},
    data() {
        return {
            loading: false,
            actionType: '',
            searchValue: '',
            config: {
                searchResource: '今日爆款剩余搜索量',
                searchTitle: '搜爆款',
                type: 'hotItem',
                searchSubtitle: '爆款话题搜索 积累优质素材',
                searchPlaceholder: '请输入关键字',
            },
            snapshotId: '',
            tableConfig: {}
        };
    },
    computed: {},
    watch: {},
    methods: {
        loadingEnd() {
            this.loading = false
        },
        async getAction({action, searchValue}) {
            if (action === 'search') this.loading = true
            this.actionType = action
            this.searchValue = '' // 重复搜索时 需重新触发
            this.$nextTick(()=>{
                this.searchValue = searchValue
            })
        },
    },
    created() {

    },
    mounted() {

    },
    beforeCreate() {
    }, //生命周期 - 创建之前
    beforeMount() {
    }, //生命周期 - 挂载之前
    beforeUpdate() {
    }, //生命周期 - 更新之前
    updated() {
    }, //生命周期 - 更新之后
    beforeDestroy() {
    }, //生命周期 - 销毁之前
    destroyed() {
    }, //生命周期 - 销毁完成
    activated() {
    }, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>
.hot-item {
    .video-thumbnail, .video-info {
        height: 42px;
    }
}
</style>