<template>
    <div class="popover-info flex justify-between">
        <div class="cursor-pointer" style="position: relative" @click="()=>toVideoUrl(currentPopoverItem)">
            <img src="@/assets/imgs/play-icon.png" class="icon" alt="">
            <el-image
                class="image brs-10"
                :src="currentPopoverItem.coverUrl || currentPopoverItem.videoCover"
                fit="contain">
            </el-image>
        </div>
        <div class="right-info flex flex-column justify-between">
            <div class="video-title text-clamp2 line-clamp2 font-bold">
                {{ currentPopoverItem.title || currentPopoverItem.videoTitle }}
            </div>
            <div class="other-info flex flex-wrap items-center justify-between">
                <span class="flex-50">点赞数：
                    <span class="font-bold">
                        {{
                            fnw(currentPopoverItem.likeCountIncrements?.[0]?.number || currentPopoverItem.likeCount, 2) || '-'
                        }}
                    </span>
                </span>
                <span class="flex-50">评论数：
                    <span class="font-bold">
                        {{
                            fnw(currentPopoverItem.commentCountIncrements?.[0]?.number || currentPopoverItem.commentCount, 2) || '-'
                        }}
                    </span>
                </span>
                <span class="flex-50">评赞比：
                    <span class="font-bold" style="color: var(--color-main)">
                        {{
                            currentPopoverItem.commentLikeRatios?.[0]?.ratio || currentPopoverItem.commentLikeRatio || '-'
                        }}
                    </span>
                </span>
                <span class="flex-50">转发数：
                    <span class="font-bold">
                        {{
                            fnw(currentPopoverItem.shareCountIncrements?.[0]?.number || currentPopoverItem.shareCount, 2) || '-'
                        }}
                    </span>
                </span>
                <span class="flex-50">收藏数：
                    <span class="font-bold">
                        {{
                            fnw(currentPopoverItem.collectCountIncrements?.[0]?.number || currentPopoverItem.collectCount, 2) || '-'
                        }}
                    </span>
                </span>
            </div>
            <div>
                <div style="font-size: 13px">发布时间:</div>
                <div class="video-text">
                    <span> {{ currentPopoverItem.publishTime }}</span>
                    <span class="mg-l12">
                          <i class="el-icon-time mg-l10"></i>
                        {{ minutes(currentPopoverItem.duration) }}
                    </span>
                </div>
            </div>
            <div class="flex items-center justify-end">
                <div class="cursor-pointer video-url"
                     @click="()=>copyUrl(currentPopoverItem)">复制链接
                </div>
                <div class="cursor-pointer video-url" style="margin-left: 12px"
                     @click="()=>toVideoUrl(currentPopoverItem)">查看视频
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import myUtils from "@/utils/utils";

export default {
    data() {
        return {};
    },
    props: {
        currentPopoverItem: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    computed: {
        minutes() {
            return (value) => {
                return myUtils.toformatTimeMM_ssChinse(value * 1000)
            }
        },
        fnw() {
            return (num, length) => {
                return myUtils.fnw(num, length, true)
            }
        },
    },
    methods: {
        async copyUrl(item) {
            await myUtils.copyToClipboard(item.videoUrl)
            this.$message.success('链接地址复制成功')
        },
        toVideoUrl(item) {
            if (!item.videoUrl) return this.$message.error('视频地址异常')
            window.open(item.videoUrl, '_blank')
        },
    },
};
</script>

<style scoped type="scss">
.popover-info {
    width: 380px;
    height: 188px;

    .icon {
        height: 36px;
        width: 36px;
        position: absolute;
        z-index: 999;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
    }

    .image {
        height: 188px;
        width: 130px;
        background: #DCDCDC;
    }

    .right-info {
        flex: 1;
        padding-left: 12px;

        .other-info {
            background: #F7F7F7;
            border-radius: 4px 4px 4px 4px;
            padding: 8px;
            margin: 6px 0;
            font-size: 13px;

            .flex-50 {
                flex: 0 0 50%
            }
        }

        .video-text {
            color: #7A7C80;
            font-size: 12px
        }

        .video-url {
            color: var(--color-main);
            margin-top: 12px;
            text-align: right;
            z-index: 9999
        }
    }
}

</style>
