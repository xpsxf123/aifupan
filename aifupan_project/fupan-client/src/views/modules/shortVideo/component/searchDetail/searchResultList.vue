<template>
    <div class="search-list">
        <TableList :tableConfig="tableConfig" @sortChange="sortChange" @paginationChange="paginationChange">
            <template #videoTitle="{ row: item }">
                <div class="video-content-cell cursor-pointer" @click="()=>toVideoUrl(item)"
                     @mouseenter="showPopover($event, item)"
                     @mouseleave="hidePopover">
                    <div class="video-coverUrl">
                        <el-image
                            :src="item.videoCover"
                            fit="contain"
                            class="video-thumbnail">
                        </el-image>
                    </div>
                    <div class="flex justify-center flex-col video-info">
                        <div class="video-title text-clamp2 line-clamp2">{{ item.videoTitle }}</div>
                        <div class="video-date">
                            <!--                            <i class="el-icon-date"></i> -->
                            {{ item.publishTime }}
                            <i class="el-icon-time" style="margin-left: 10px;"></i> {{ minutes(item.duration) }}
                        </div>
                    </div>
                </div>
            </template>
            <template #influencerNickname="{ row: item }">
                <div class="expert-content-cell flex cursor-pointer" @click="()=>toExpertHomeForThird(item)">
                    <el-avatar class="avatar" :src="item.influencerAvatar"/>
                    <div class="text">
                        <div class="name text-left text-clamp1">{{ item.influencerNickname }}</div>
                        <div class="fans text-left text-xs">粉丝：{{ fnw(item.followersCount) }}</div>
                    </div>
                </div>
            </template>
            <template slot="emptyText">
                <slot name="emptyText">
                    <div class="empty-text">暂无数据</div>
                </slot>
            </template>
        </TableList>
        <el-popover
            v-model="popoverVisible"
            trigger="click"
            append-to-body
            popper-class="table-popover"
            :style="popoverStyle"
        >
            <div v-if="currentPopoverItem" @mouseenter="enterPopover" @mouseleave="leavePopover">
                <PopoverInfo :currentPopoverItem="currentPopoverItem"/>
            </div>
        </el-popover>
    </div>
</template>

<script>
import TableList from "../tableList.vue";
import myUtils from "@/utils/utils";
import Empty from "@/components/empty/index.vue";
import popover from "@/views/modules/shortVideo/mixins/popover";
import PopoverInfo from "@/views/modules/shortVideo/component/popoverInfo.vue";

export default {
    components: {Empty, TableList, PopoverInfo},
    mixins: [popover],
    props: {
        tableConfig: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {}
    },
    computed: {
        fnw() {
            return (num, length) => {
                return myUtils.fnw(num, length)
            }
        },
        minutes() {
            return (value) => {
                return myUtils.toformatTimeMM_ssChinse(value * 1000)
            }
        },
    },
    created() {

    },
    methods: {
        paginationChange(values) {
            this.$emit('paginationChange', values)
        },
        toVideoUrl(item) {
            if (!item.videoUrl) return this.$message.error('视频地址异常')
            window.open(item.videoUrl, '_blank')
            // this.$httpClient.system.openUrl({url: item.videoUrl});
        },
        toExpertHomeForThird(item) {
            if (!item.influencerPlatformUserId) return this.$message.error('达人主页地址异常')
            window.open(`https://www.douyin.com/user/${item.influencerPlatformUserId}`, '_blank')
            // this.$httpClient.system.openUrl({url: `https://www.douyin.com/user/${item.influencerPlatformUserId}`});
        },
        sortChange({column, prop, order}) {
            this.$emit('sortChange', {column, prop, order})
        },
    }
}
</script>

<style lang="scss" scoped>
.search-list {
    height: 100%;

    ::v-deep(.table-popover) {
        position: fixed !important;
        pointer-events: auto;
        border-radius: 10px;
        z-index: 9999 !important;
    }

    .video-coverUrl {
        flex-shrink: 0;
        width: 56px;
        text-align: center;
        height: 56px;

        .video-thumbnail {
            background: #DCDCDC;
            border-radius: 6px;
            overflow: hidden;
        }
    }

    .expert-content-cell {
        .avatar {
            flex-shrink: 0;
        }

        cursor: pointer;

        .text {
            padding-left: 6px;

            .name {
                width: 140px;
            }
        }
    }

    .empty-text {
        color: #484A4D;
    }
}
</style>