<template>
    <div class="hot-item-list">
        <el-card class="header" ref="header">
            <div class="header-title">
                <div class="flex items-center">
                    <span class="title">关键字：</span>
                    <el-tag type="success" style="max-width: 385px;font-size: 15px;">
                        <div class="text-clamp1">{{ keyWords }}</div>
                    </el-tag>
                </div>
                <div style="margin-left: 20px">
                    <span>符合条件的爆款数：</span>
                    {{ tableConfig?.pagination?.total || 0 }}
                </div>
            </div>
            <el-form :inline="true" :model="searchForm" class="form-inline" size="default" label-width="73px">
                <el-form-item label="点赞大于:">
                    <el-input-number style="width: 15vw" placeholder="点赞数大于" :precision="0"
                                     v-model="searchForm.likeCount"
                                     class="input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="转发大于:">
                    <el-input-number style="width: 15vw" placeholder="转发数大于" :precision="0"
                                     v-model="searchForm.shareCount"
                                     class="input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="收藏大于:">
                    <el-input-number style="width: 15vw" placeholder="收藏数大于" :precision="0"
                                     v-model="searchForm.collectCount"
                                     class="input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="视频时长:">
                    <el-select v-model="searchForm.videoDuration" style="width: 15vw" placeholder="请选择视频信息"
                               clearable>
                        <el-option label="一分钟以内" :value="[0,60]"></el-option>
                        <el-option label="1-3分钟" :value="[60,180]"></el-option>
                        <el-option label="3-5分钟" :value="[180,300]"></el-option>
                        <el-option label="5分钟以上" :value="[300,'']"></el-option>
                    </el-select>
                </el-form-item>
                <br>
                <el-form-item label="发布时间:" style="margin-bottom: 0">
                    <el-select v-model="searchForm.publishTimeValue" style="width: 15vw" placeholder="请选择发布时间"
                               clearable>
                        <el-option label="只看今日" :value="-1"></el-option>
                        <el-option label="近三天" :value="0"></el-option>
                        <el-option label="近一周" :value="1"></el-option>
                        <el-option label="近半个月" :value="2"></el-option>
                        <el-option label="近一个月" :value="3"></el-option>
                        <el-option label="近三个月" :value="4"></el-option>
                        <el-option label="近半年" :value="5"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="粉丝量:" style="margin-bottom: 0">
                    <el-select v-model="searchForm.followersCountFilter" style="width: 15vw" placeholder="请选择达人粉丝量"
                               clearable>
                        <el-option label="5000以内" :value="1"></el-option>
                        <el-option label="1万以内" :value="2"></el-option>
                        <el-option label="2万以内" :value="3"></el-option>
                        <el-option label="5万以内" :value="4"></el-option>
                        <el-option label="10万以内" :value="5"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="评赞比:" style="margin-bottom: 0">
                    <el-select v-model="searchForm.commentLikeRatioFilter" style="width: 15vw" placeholder="请选择评赞比值"
                               clearable>
                        <el-option label="大于0.05" :value="1"></el-option>
                        <el-option label="大于0.1" :value="2"></el-option>
                        <el-option label="大于0.15" :value="3"></el-option>
                        <el-option label="大于0.2" :value="4"></el-option>
                        <el-option label="大于0.25" :value="5"></el-option>
                    </el-select>
                </el-form-item>

                <!--                <el-form-item label="发布时间">-->
                <!--                    <el-date-picker-->
                <!--                        clearable-->
                <!--                        value-format="yyyy-MM-dd"-->
                <!--                        v-model="searchForm.releaseTime"-->
                <!--                        type="daterange"-->
                <!--                        range-separator="至"-->
                <!--                        start-placeholder="开始时间"-->
                <!--                        end-placeholder="结束时间">-->
                <!--                    </el-date-picker>-->
                <!--                </el-form-item>-->
                <el-form-item style="margin-bottom: 0">
                    <afp-button type="primary" size="default" @click="onSubmit">查询</afp-button>
                    <afp-button type="primary" size="default"  @click="$emit('updateData')" v-if="isIgnore">手动更新</afp-button>
                </el-form-item>
            </el-form>
        </el-card>
        <el-card class="content" :body-style="{ height: '100%' }">
            <SearchList :tableConfig="tableConfig" @sortChange="sortChange" @paginationChange="paginationChange">
                <template #emptyText>
                    <div class="empty-text">暂无数据</div>
                </template>
            </SearchList>
        </el-card>
    </div>
</template>

<script>
import TableList from "../tableList.vue";
import SearchList from './searchResultList.vue'

export default {
    components: {TableList, SearchList},
    props: {
        tableConfig: {
            type: Object,
            default: () => {
                return {}
            }
        },
    },
    data() {
        return {
            keyWords: '',
            searchForm: {
                likeCount: undefined,
                shareCount: undefined,
                collectCount: undefined,
                // releaseTime: null,
                publishTimeValue: '',
                followersCountFilter:'',
                commentLikeRatioFilter:'',
                videoDuration: ''
            }
        }
    },
    computed: {
        isIgnore() {
            const {query: param, path} = this.$route
            return path === '/subscribeHotItem/hotItemList' && param.isIgnore == 1
        }
    },
    created() {

    },
    mounted() {
        const {query} = this.$route
        this.searchForm = {
            ...this.searchForm,
            likeCount: query?.subscriptionLikeCountThreshold || undefined
        }
        this.keyWords = decodeURIComponent(query?.keyWords || '')
    },
    methods: {
        onSubmit() {
            this.$emit('onSubmitSearch', this.searchForm)
        },
        paginationChange(values) {
            this.$emit('paginationChange', values)
        },
        sortChange({column, prop, order}) {
            this.$emit('sortChange', {column, prop, order})
        },
    }
}

</script>

<style lang="scss" scoped>
.hot-item-list {
    height: 100%;
    display: flex;
    flex-direction: column;

    ::v-deep(.el-card__body) {
        padding: 0;
    }

    .header {
        padding: 16px;

        .header-title {
            display: flex;
            align-items: center;

            .title {
                color: var(--color-main);
            }
        }

        .form-inline {
            margin-top: 12px;

            ::v-deep(.el-form-item__label) {
                text-align: left;
            }
        }
    }

    .content {
        flex: 1;
        margin-top: 12px;

        .video-thumbnail, .video-info {
            height: 42px;
            justify-content: center;
        }
    }

    .empty-text {
        color: #484A4D;
    }
}
</style>