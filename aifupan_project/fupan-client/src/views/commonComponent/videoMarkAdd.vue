<template>
    <div>
        <el-dialog :visible.sync="visible" width="546px" :close-on-click-modal="false">
            <div class="video-mark-add-close"><img src="../../assets/imgs/close.png" style="width: 16px;height: 16px;"
                    @click="visible = false">
            </div>
            <div class="video-mark-add-title">视频标记</div>
            <el-form :model="videoMarkForm" :rules="dataRule" ref="videoMarkForm" label-width="100px">

                <!-- <el-form-item label="视频时间" prop="startDate">
                    <el-time-picker is-range v-model="timeArr" range-separator="-" start-placeholder="开始时间"
                        end-placeholder="结束时间" placeholder="选择时间范围" value-format="HH:mm:ss" :picker-options="{
                            selectableRange: '18:30:00 - 20:30:00'
                        }" @change="selectTime">
                    </el-time-picker>
                </el-form-item> -->

                <el-form-item label="开始时间" prop="startDate">
                    <el-time-picker v-model="videoMarkForm.startDate" value-format="HH:mm:ss" :picker-options="{
                        selectableRange: startSelectableRange
                    }">
                    </el-time-picker>
                </el-form-item>
                <el-form-item label="结束时间" prop="endDate">
                    <el-time-picker v-model="videoMarkForm.endDate" value-format="HH:mm:ss" :picker-options="{
                        selectableRange: startSelectableRange
                    }">
                    </el-time-picker>
                </el-form-item>

                <el-form-item label="标记类型" prop="type">
                    <div>
                        <el-radio-group v-model="videoMarkForm.type">
                            <el-radio v-for="item in typeList" :key="item.value" :label="item.value"
                                style="margin-top: 12px;">{{ item.label
                                }}</el-radio>
                        </el-radio-group>
                    </div>
                </el-form-item>

                <el-form-item label="标记颜色" prop="color">
                    <el-color-picker v-model="videoMarkForm.color"></el-color-picker>
                </el-form-item>

                <el-form-item label="备注" prop="remarks" style="padding-bottom: 31px;margin: 0;">
                    <el-input type="textarea" rows="5" style="width: 350px;" v-model="videoMarkForm.remarks">
                    </el-input>
                </el-form-item>

            </el-form>
            <div style="display: flex;justify-content: center; padding-bottom: 31px;">
                <afp-button class="dialog-button" @click="visible = false">取消</afp-button>
                <afp-button class="dialog-button add-yes" @click="submitAdd()">确定</afp-button>
            </div>
        </el-dialog>
    </div>
</template>

<script>
import myUtils from '../../utils/utils';


export default {
    props: {
        // 视频id
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
            startSelectableRange: "00:00:00 - " + myUtils.toformatTime(this.videoLength * 1000),
            visible: false,
            videoMarkForm: {
                id: 0,
                fileUuid: "",
                startDate: "00:00:00",
                endDate: "00:00:00",
                color: '#409EFF',
                remarks: '',
                type: '',
            },
            typeList: [],
            dataRule: {
                startDate: [
                    { required: true, message: '标记开始时间不能为空', trigger: 'blur' }
                ],
                endDate: [
                    { required: true, message: '标记结束时间不能为空', trigger: 'blur' }
                ],
                type: [
                    { required: true, message: '类型不能为空', trigger: 'blur' }
                ],
                color: [
                    { required: true, message: '颜色不能为空', trigger: 'blur' }
                ]
            }
        };
    },
    created() {
        this.getVideoMarkType();
    },
    methods: {
        // 获取视频标记类型
        getVideoMarkType() {
            this.typeList = [];
            this.$httpBack.dictdata.list({ limit: -1, typeLogo: "video_mark_type" }).then((res) => {
                if (res.code == 0) {
                    this.typeList = res.data.list;
                    this.typeList.forEach(item => {
                        item.value = parseInt(item.value)
                    });
                }
            });
        },
        init(fileUuid) {
            this.videoMarkForm.fileUuid = fileUuid;
            this.visible = true;
        },
        submitAdd() {
            this.$refs["videoMarkForm"].validate((valid) => {
                if (valid) {
                    let requestData = JSON.parse(JSON.stringify(this.videoMarkForm));
                    requestData.fileUuid = this.fileUUID;
                    requestData.typeStr = this.typeList.filter(item => item.value == requestData.type)[0].label;

                    if (myUtils.toSecond(requestData.startDate) > myUtils.toSecond(requestData.endDate)) {
                        this.$message.error("开始时间不能大于结束时间")
                        return;
                    }

                    this.$httpBack.videomark.save(requestData).then((res) => {
                        if (res && res.code === 0) {
                            this.$message({
                                message: res.msg,
                                type: 'success',
                                duration: 1500,
                                onClose: () => {
                                    this.visible = false
                                    this.$emit("refreshDataList");
                                },
                            });
                        } else {
                            this.$message.error(res.msg);

                        }
                    });
                }
            })
        }
    }
}
</script>

<style>
.video-mark-add-close {
    display: flex;
    justify-content: end;
    margin-right: 13px;
    padding-top: 14px;
    cursor: pointer;
}

.video-mark-add-title {
    display: flex;
    justify-content: center;
    font-size: 16px;
    color: #2E3742;
    margin-bottom: 19px;
}

.form-time {
    display: flex;
    align-items: center;
    margin: 19px 0px 0px 39px
}

.form-tiem-item {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 88px;
    height: 32px;
    border: 1px solid #CFD4DB;
    border-radius: 4px;
}

.select-bacc-color {
    display: flex;
    justify-content: center;
    position: absolute;
    color: #2E3742;
    z-index: 2;
    width: 66px;
    border-radius: 4px;
}

.select-color-red {
    background-color: red;
}

.select-color-yellow {
    background-color: yellow;
}

.select-color-blue {
    background-color: var(--color-main);
}

.dialog-button {
    width: 110px;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #B4BCCA;
    color: 14px;
}

.add-yes {
    background-color: var(--color-main);
    color: #FFFFFF;
}
</style>