<template>
    <el-drawer
        class="add-expert"
        :size="800"
        :destroy-on-close="true"
        :close-on-press-escape="false"
        :wrapperClosable="false"
        :visible.sync="visible"
        :with-header="false">
        <div class="add-expert-container">
            <div class="add-expert-header flex items-center justify-start">
                <i class="el-icon-close" @click="handleClose"></i>
                <span class="title">添加订阅</span>
            </div>
            <div class="add-expert-describe">
                在下方粘贴要订阅的达人账号，支持以下格式：<br/>
                <component :is="domName[currentName]" class="tipContainer" @showDyHelp="showDyHelp"
                           :isRecord="false"></component>
                小Tips：添加达人后，如达人更新，将会自动转译并分析<br/>
                您的会员套餐版本是：<span class="red">{{$store.getters.getPackageLevelName}}</span>，剩余添加主播数量：
                <span class="red">{{ userProperty?.userSubscribeInfluencerNum || 0 }}</span>
                <span>/{{ userProperty?.totalUserSubscribeInfluencerNum || 0 }}</span>
                <span style="color: var(--color-main);padding-left: 20px;cursor: pointer"
                      @click="showQr">立即升级</span>
            </div>
            <el-form ref="form" :model="formData" size="default" :rules="rules" label-width="80px" class="form-container">
                <el-form-item label="抖音号" prop="searchKeyword">
                    <el-input v-model="formData.searchKeyword" placeholder="抖音号"></el-input>
                </el-form-item>
                <el-form-item label="行业选择" prop="tradeId">
                    <tradeId v-model="formData.tradeId" v-removeAriaHidden :options="tradeTreeList"
                             style="width: 100%;"></tradeId>
                </el-form-item>
                <el-form-item label="分组选择">
                    <SelectGroup v-model="formData.groupId" :groupType="1" style="width: 100%"/>
                </el-form-item>
                <el-form-item label-width="0" style="margin-top: 50px">
                    <afp-button type="primary" size="default" @click="onSubmit">添加达人</afp-button>
                </el-form-item>
            </el-form>
        </div>
        <el-dialog title="帮助" top="10px" :visible.sync="dialogVisible" width="40%" :close-on-click-modal="true"
                   :modal="false">
            <div style="max-height: 600px;overflow: hidden;overflow-y: auto;">
                <div v-html="helpHtml"></div>
            </div>
        </el-dialog>
        <customer-service-qr-code v-if="kefuDialogVisible" ref="customerServiceQrCode"></customer-service-qr-code>
    </el-drawer>
</template>

<script>
import tradeId from "@/components/tradeId/index.vue";
import Douyin from '@/views/modules/addCompere/common/douyin.vue'
import Kuaishou from '@/views/modules/addCompere/common/kuaishou.vue'
import customerServiceQrCode from "@/views/commonComponent/customerServiceQrCode.vue";
import SelectGroup from "@/views/modules/shortVideo/component/selectGroup.vue";
import userAssets from "@/views/modules/shortVideo/mixins/userAssets";

export default {
    components: {SelectGroup, customerServiceQrCode, tradeId, Douyin},
    mixins: [userAssets],
    props: {
        visible: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            selectOption: '',
            tradeTreeList: [],
            kefuDialogVisible: false,
            dialogVisible: false,
            helpHtml: '',
            domName: {
                douyin: 'Douyin',
                kuaishou: 'Kuaishou'
            },
            currentName: 'douyin',
            formData: {
                searchKeyword: '',
                tradeId: '',
                groupId: '',
                // douyinAuth: ''
            },
            rules: {
                searchKeyword: [
                    {
                        required: true,
                        message: '请填写抖音号',
                        trigger: ['change', 'blur']
                    },
                    {
                        pattern: /^[a-zA-Z0-9_.]{3,23}$/,
                        message: '抖音号格式不正确',
                        trigger: ['change', 'blur']
                    }],
                tradeId: [{required: true, message: '请选择行业', trigger: 'change'}]
            }
        };
    },
    computed: {},
    watch: {
        visible: {
            handler(val) {
                if (!!val) {
                    this.getTradeListTree()
                }
                if (!val) {
                    this.resetForm()
                }
            },
            immediate: true,
            deep: true
        }
    },
    methods: {
        handleClose() {
            this.$emit('update:visible', false);
        },
        async addExpert() {
            const result = await this.$httpClient.shortVideo.addInfluencerInfo(this.formData)
            if (result.code !== 0) {
                this.$message.error(result.msg)
                return null
            }
            this.$message.success('添加达人成功')
            return result
        },
        getTradeListTree() {
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        async onSubmit() {
            try {
                const response = await this.$refs.form.validate()
                if (!response) return
                const result = await this.addExpert()
                if (!result) return
                this.$emit('handleExpert', {
                    resultData: result?.data || {},
                    groupId: this.formData.groupId,
                    industryId: this.formData.tradeId,
                    searchKeyword: this.formData.searchKeyword,
                })
            } catch (e) {
            }
        },
        showQr() {
            this.kefuDialogVisible = true
            this.$nextTick(() => {
                this.$refs.customerServiceQrCode.init()
            })
        },
        showDyHelp() {
            this.$httpBack.article.list({limit: -1, type: 0}).then((res) => {
                if (res.code === 0 && res.data) {
                    this.helpHtml = res.data.list[0].content;
                    this.helpHtml = this.helpHtml.replaceAll("<img", "<img style='width: 100%'");
                    this.dialogVisible = true;
                } else {
                    this.$message.error("暂未支持");
                }
            })
        },
        resetForm() {
            this.formData = {
                searchKeyword: '',
                tradeId: '',
                groupId: '',
                // douyinAuth: ''
            }
            this.$refs?.form?.resetFields();
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
<style lang='scss'>
.add-expert {
    .add-expert-container {
        padding: 20px;

        .add-expert-header {
            font-size: 20px;

            .el-icon-close {
                cursor: pointer;
            }

            .title {
                font-weight: bold;
                padding-left: 12px;
            }
        }

        .add-expert-describe {
            line-height: 26px;
            color: #484A4D;
            font-size: 14px;
            margin-top: 12px;
            padding-bottom: 12px;
            border-bottom: 1px solid #E4E7ED;

            .tipContainer {
                font-size: 14px;
                color: #2E3742;
            }
        }

        .form-container {
            margin-top: 20px;
            width: 65%;
        }
    }

    .red {
        color: #FC4F52;
    }

    .tips {
        color: red;
        font-size: 12px;
        margin-left: 8px;
        margin-bottom: 18px;
    }

    .buy_in {
        ::v-deep(.el-radio-button__inner) {
            color: var(--color-main);
            border-color: var(--color-main) !important;
            background-color: rgba(0, 119, 255, 0.05);
            border-radius: 5px;
        }
    }

    .buy_in_selected {
        ::v-deep(.el-radio-button__inner) {
            color: #28BD6C;
            border-color: #28BD6C !important;
            background-color: rgba(40, 189, 108, 0.05);
            border-radius: 5px;
        }
    }
}
</style>