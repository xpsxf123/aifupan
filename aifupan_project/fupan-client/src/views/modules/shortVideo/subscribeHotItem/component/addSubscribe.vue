<template>
    <el-dialog
        :title="isEdit?'编辑订阅':'添加订阅'"
        class="edit-subscribe"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :destroy-on-close="true"
        :visible.sync="visible"
        :before-close="close"
        width="420px">
        <el-form :model="subscribeForm" ref="subscribeForm" :rules="rules" label-width="100px"
                 size="default" v-if="visible">
            <template v-if="!subscribeData.autoSync">
                <el-form-item label="订阅关键字" prop="keyword">
                    <el-input
                        :disabled="isEdit"
                        type="textarea"
                        maxlength="10"
                        show-word-limit
                        placeholder="请输入10个字内的用户搜索关键词，不要带空格和标点符号"
                        v-model.trim="subscribeForm.keyword"/>
                </el-form-item>
                <el-form-item label="行业选择" prop="industryId">
                    <tradeId v-model="subscribeForm.industryId" v-removeAriaHidden :options="tradeTreeList"
                             style="width: 100%;"></tradeId>
                </el-form-item>
                <el-form-item label="分组名称" v-if="modifyPermissions">
                    <SelectGroup v-model="subscribeForm.groupId" :groupType="2" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="点赞数大于" prop="likeCountMin">
                    <el-input-number placeholder="点赞数大于" :precision="0" v-model="subscribeForm.likeCountMin"
                                     class="w100 input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <div class="flex text-xs">
                    <el-form-item label="自动提取文案">
                        <el-switch :active-value="1" :inactive-value="0"
                                   v-model="subscribeForm.autoSyncEnabled"></el-switch>
                    </el-form-item>
                    <div class="tips">打开并选择后将自动提取文案</div>
                </div>
            </template>
            <div v-if="subscribeForm.autoSyncEnabled||subscribeData.autoSync">
                <el-form-item label="点赞数大于" prop="likeCountThreshold">
                    <el-input-number placeholder="点赞数大于" :precision="0" v-model="subscribeForm.likeCountThreshold"
                                     class="w100 input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="更新时间" prop="updateTimeCondition">
                    <el-select v-model="subscribeForm.updateTimeCondition" placeholder="请选择更新时间"
                               style="width: 100%">
                        <el-option label="近一周" :value="1"></el-option>
                        <el-option label="近半个月" :value="2"></el-option>
                        <el-option label="近一个月" :value="3"></el-option>
                        <el-option label="近三个月" :value="4"></el-option>
                        <el-option label="近半年" :value="5"></el-option>
                    </el-select>
                </el-form-item>
            </div>
            <el-form-item label-width="0" style="text-align: center;margin-top: 12px">
                <afp-button type="primary" size="default" @click="()=>addSubscribe()">{{ isEdit ? '编辑' : '添加' }}订阅
                </afp-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>

<script>
import {isEmpty, pick} from "lodash";
import tradeId from "@/components/tradeId/index.vue";
import SelectGroup from './../../component/selectGroup.vue'

export default {
    components: {tradeId, SelectGroup},
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        subscribeData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            tradeTreeList: [],
            rules: {
                keyword: [
                    {required: true, message: '请输入订阅关键字', trigger: ['change', 'blur']},
                    {
                        pattern: /^[\u4e00-\u9fa5a-zA-Z0-9]+$/,
                        message: '关键字格式错误',
                        trigger: ['change', 'blur']
                    }
                ],
                industryId: [{required: true, message: '请选择行业', trigger: 'change'}],
                likeCountMin: [{required: true, message: '请填写点赞数', trigger: ['change', 'blur']}],
                updateTimeCondition: [{required: true, message: '请选择更新时间', trigger: ['change', 'blur']}],
                likeCountThreshold: [{required: true, message: '请填写点赞数', trigger: ['change', 'blur']}],
            },
            subscribeForm: {
                platformType: 1,//平台类型: 1-抖音, 2-快手, 3-视频号,
                keyword: '',
                industryId: '',
                groupId: '',
                likeCountMin: 0,
                autoSyncEnabled: 0,
                likeCountThreshold: undefined,
                updateTimeCondition: ''
            }
        };
    },
    computed: {
        isEdit() {
            return !isEmpty(this.subscribeData)
        },
        modifyPermissions() {
            const {id} = this.$store.getters.getUserInfo;
            return id === this.subscribeData?.userId || !this.isEdit
        }
    },
    watch: {
        visible: {
            handler(val) {
                if (val) this.getTradeListTree()
                if (!val) this.resetFields()
            },
            deep: true
        },
        subscribeData: {
            handler(val) {
                if (!isEmpty(val)) {
                    this.subscribeForm = {
                        ...pick(val, ['keyword', 'industryId', 'groupId', 'subscriptionId', 'autoSyncEnabled']),
                        likeCountMin: val.subscriptionLikeCountThreshold ?? 0,
                        likeCountThreshold: val.likeCountThreshold ?? undefined,
                        updateTimeCondition: val.updateTimeCondition || '',
                    }
                }
            },
            deep: true
        },
    },
    methods: {
        close() {
            this.$emit('refreshFun')
            this.resetFields()
            this.$emit('update:visible', false)
        },
        getTradeListTree() {
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
            })
        },
        async addSubscribe() {
            try {
                const response = await this.$refs.subscribeForm.validate()
                if (response) {
                    this.$emit('handleSubscribe', this.isEdit, this.subscribeForm, this.close)
                }
            } catch (e) {

            }
        },
        resetFields() {
            this.subscribeForm = {
                platformType: 1,//平台类型: 1-抖音, 2-快手, 3-视频号,
                keyword: '',
                industryId: '',
                groupId: '',
                likeCountMin: 0,
                autoSyncEnabled: 0,
                likeCountThreshold: undefined,
                updateTimeCondition: ''
            }
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
.edit-subscribe {
    ::v-deep(.el-form-item) {
        margin-bottom: 18px;
    }

    ::v-deep(.el-textarea .el-input__count){
        background: transparent;
    }

    ::v-deep(.el-dialog__body) {
        padding: 10px 32px
    }

    ::v-deep(.el-dialog__header) {
        padding: 12px
    }

    .tips {
        margin-top: 12px;
        margin-left: 20px;
        color: var(--color-main);
    }
}
</style>