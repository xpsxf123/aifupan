<template>
    <el-dialog
        v-dialogDrag
        title="查看文案"
        class="view-text"
        :visible.sync="visible"
        :before-close="handleClose"
        @close="handleClose"
        append-to-body
        :close-on-click-modal="false"
        :destroy-on-close="true"
        width="600px">
        <template #title>
            <div class="flex items-center">
                <span class="text-lg">查看文案</span>
                <span class="video-content-tips text-xs">
                    经过上万轮调试，爱复盘短视频提取文案准确率高达99.9%， 请放心使用！
                </span>
            </div>
        </template>
        <!--        <slot name="header"></slot>-->
        <template v-if="!!currentVideoItem.extractId">
            <div class="video-content-cell" v-if="!isHasLike">
                <div class="video-coverUrl">
                    <el-image
                        fit="contain"
                        :src="currentVideoItem.coverUrl||currentVideoItem.videoCover"
                        class="video-thumbnail">
                    </el-image>
                </div>
                <div class="video-info flex flex-col justify-between">
                    <div class="video-title text-clamp2 line-clamp2">
                        {{ currentVideoItem.videoTitle }}
                    </div>
                    <div class="video-date">
                        <!--                        <i class="el-icon-date"></i>-->
                        {{ currentVideoItem.extractTime }}
                        <i class="el-icon-time" style="margin-left: 10px;"></i>
                        {{ minutes(currentVideoItem.duration) }}
                    </div>
                </div>
            </div>
            <div class="flex items-start justify-between" v-if="isHasLike">
                <div class="video-content-cell" style="flex:1;">
                    <div class="video-coverUrl">
                        <el-image
                            fit="contain"
                            :src="currentVideoItem?.coverUrl||currentVideoItem?.videoCover"
                            class="video-thumbnail">
                        </el-image>
                    </div>
                    <div class="video-info">
                        <div class="video-title text-clamp2 line-clamp2">
                            {{ currentVideoItem.title || currentVideoItem.videoTitle }}
                        </div>
                        <div class="video-date">
                            <!--                            <i class="el-icon-date"></i> -->
                            {{ currentVideoItem.publishTime }}
                            <i class="el-icon-time" style="margin-left: 10px;"></i>
                            {{ minutes(currentVideoItem?.duration) }}
                        </div>
                    </div>
                </div>
                <div class="user-stats flex flex-wrap justify-center" style="width: auto">
                    <div class="stat-item">
                        <p class="stat-title">获赞数</p>
                        <p class="stat-value">{{ fnw(likeCount, 2) }}</p>
                    </div>
                    <div class="stat-item">
                        <p class="stat-title">评论数</p>
                        <p class="stat-value">{{ fnw(commentCount, 2) }}</p>
                    </div>
                    <div class="stat-item">
                        <p class="stat-title">转发数</p>
                        <p class="stat-value">{{ fnw(shareCount, 2) }}</p>
                    </div>
                </div>
            </div>
        </template>

        <div class="text-content" v-loading="loading">
            <span v-if="text" style="line-height: 1.6;white-space: pre-line" v-html="text"></span>
            <el-empty description="暂无文案" v-if="!text" :image-size="60"></el-empty>
        </div>

        <span slot="footer">
            <afp-button type="primary" class="px-18" @click="copy" :plain="false">复  制</afp-button>
        </span>
    </el-dialog>
</template>

<script>
import myUtils from "@/utils/utils";

export default {
    components: {},
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        currentVideoItem: {
            type: Object,
            default: () => {
                return {}
            }
        },
        isHasLike: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            text: '',
            loading: false,
        };
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
        likeCount() {
            return this.currentVideoItem.likeCount || this.currentVideoItem?.likeCountIncrements?.[0]?.number
        },
        commentCount() {
            return this.currentVideoItem.commentCount || this.currentVideoItem?.commentCountIncrements?.[0]?.number
        },
        shareCount() {
            return this.currentVideoItem.shareCount || this.currentVideoItem?.shareCountIncrements?.[0]?.number
        }
    },
    watch: {
        currentVideoItem(val) {
            if (val?.extractId) this.getContent(val?.extractId)
        },
    },
    methods: {
        handleClose(done) {
            this.$emit('update:visible', false)
            this.text = ''
        },
        async copy() {
            await myUtils.copyToClipboard(this.text?.replace(/<[^>]+>/g, ''))
            this.$message.success('文案复制成功')
        },
        async getContent(id) {
            try {
                this.loading = true
                const result = await this.$httpBack.shortVideo.getContent(id)
                this.loading = false
                if (result.code === 0 && result.data) {
                    this.text = result.data?.audioContent?.replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
                }
            } catch (e) {
                this.loading = false
            }
        }
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
.view-text {
    z-index: 9999;

    ::v-deep(.el-dialog) {
        left: 25%;
    }

    .text-content {
        background: rgba(0, 119, 255, 0.05);
        border-radius: 8px;
        padding: 12px;
        margin-top: 12px;
        max-height: 250px;
        overflow: auto;
        min-height: 60px;
    }

    .video-content-tips {
        color: #FF0000;
        padding-left: 6px;
    }

    ::v-deep(.el-dialog__header) {
        padding: 12px;
    }

    ::v-deep(.el-dialog__body) {
        padding: 12px;
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

    .user-stats {
        width: 550px;

        .stat-item {
            padding: 0px 20px;
            text-align: center;

            .stat-title {
                font-size: 14px;
                color: #909399;
                margin: 0 0 5px;
            }

            .stat-value {
                font-size: 16px;
                font-weight: bold;
                color: #303133;
                margin: 0;
            }
        }
    }
}
</style>