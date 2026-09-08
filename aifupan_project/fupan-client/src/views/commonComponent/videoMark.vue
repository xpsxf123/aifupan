<template>
    <div>
        <div style="display: flex; justify-content: space-between;">
            <div style="margin: 10px 0px 0px 29px; color: #2E3742 ; font-size: 14px;">视频直达</div>
            <!-- <div class="video-mark-add" @click="videoMarkAddClick">视频标记</div> -->
            <afp-button  type="primary" style="margin-right: 10px;"
                @click="videoMarkAddClick">视频标记</afp-button>
        </div>
        <!-- 点击时长跳转对应视频 -->
        <div class="video-jump" v-for="item in videoMarkList" :key="item.id">
            <div :style="'display: flex; color:' + item.color" @click="videoJumpInfoClick(item.startDate)">
                <div style="margin-right: 20px; width: 180px;">{{ item.startDate + " - " + item.endDate }}</div>
                <div>{{ item.typeStr }}</div>
                <div v-if="item.remarks">{{ "(" + item.remarks + ")" }}</div>
            </div>
        </div>

        <!-- 添加视频标记 -->
        <VideoMarkAdd v-if="videoMarkAddVisible" ref="videoMarkAdd" :fileUUID="fileUUID" :videoLength="videoLength"
            @refreshDataList="getDataList">
        </VideoMarkAdd>
    </div>
</template>

<script>
import VideoMarkAdd from './videoMarkAdd';
import myUtils from '../../utils/utils';

export default {
    components: {
        VideoMarkAdd
    },
    props: {
        // 视频唯一标识
        fileUUID: {
            type: String,
            required: true
        },
        // 视频时长，单位：秒
        videoLength: {
            type: Number,
            required: true
        }
    },
    data() {
        return {
            videoMarkList: [], // 视频标记列表
            // 添加视频标记弹窗
            videoMarkAddVisible: false,
            sliderValue: 0,
        }
    },
    created() {
        this.getDataList();
    },
    methods: {
        // 获取视频标记列表
        getDataList() {
            this.$httpBack.videomark.listByUUID({ uuid: this.fileUUID }).then((res) => {
                if (res && res.code === 0) {
                    this.videoMarkList = res.data;
                    if (this.videoMarkList != null && this.videoMarkList.length > 0) {
                        this.videoMarkList.sort((a, b) => {
                            const startDateA = myUtils.toSecond(a.startDate);
                            const startDateB = myUtils.toSecond(b.startDate);
                            return startDateA - startDateB;
                        });
                    }

                }
            });
        },
        // 视频直达(将数据传给父组件)
        videoJumpInfoClick(startDate) {
            this.$emit('videoProgress', myUtils.toSecond(startDate))
        },

        // 视频标记弹窗
        videoMarkAddClick() {
            this.videoMarkAddVisible = true
            this.$nextTick(() => {
                this.$refs.videoMarkAdd.init();
            });
        },
    }
}
</script>

<style>
/* 视频标记添加文本 */
.video-mark-add {
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 13px;
    color: #FFFFFF;
    width: 90px;
    height: 28px;
    background-color: var(--color-main);
    border: 1px solid var(--color-main);
    border-radius: 3px;
    margin-right: 25px;
}

.video-mark-add:hover {
    cursor: pointer;
    color: red;
}

.video-jump {
    margin: 16px 0px 0px 29px;
}

.video-jump:hover {
    cursor: pointer;
    color: var(--color-main);
}
</style>