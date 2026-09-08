<template>
    <el-dialog
        :title="dialogTitle"
        class="edit-expert"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :destroy-on-close="true"
        :visible.sync="visible"
        :before-close="()=>handleClose()"
        width="350px">
        <el-form :model="editForm" ref="editForm" :rules="rules" label-width="80px" size="default" v-if="visible">
            <template v-if="!autoSync">
                <el-form-item label="行业选择" prop="industryId">
                    <tradeId v-model="editForm.industryId" v-removeAriaHidden :options="tradeTreeList"
                             style="width: 100%;"></tradeId>
                </el-form-item>
                <el-form-item label="分组选择" v-if="modifyPermissions">
                    <SelectGroup v-model="editForm.groupId" :groupType="1" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="提取文案">
                    <el-switch :active-value="1" :inactive-value="0" v-model="editForm.autoSyncEnabled"></el-switch>
                </el-form-item>
            </template>

            <template v-if="editForm.autoSyncEnabled || autoSync">
                <el-form-item label="点赞大于" prop="likeCountThreshold">
                    <el-input-number placeholder="点赞数大于" :precision="0" v-model="editForm.likeCountThreshold"
                                     class="w100 input-number" controls-position="right" :min="0"/>
                </el-form-item>
                <el-form-item label="更新时间" prop="updateTimeCondition">
                    <el-select v-model="editForm.updateTimeCondition" placeholder="请选择更新时间" style="width: 100%">
                        <el-option label="近一周" :value="1"></el-option>
                        <el-option label="近半个月" :value="2"></el-option>
                        <el-option label="近一个月" :value="3"></el-option>
                        <el-option label="近三个月" :value="4"></el-option>
                        <el-option label="近半年" :value="5"></el-option>
                    </el-select>
                </el-form-item>
            </template>
            <el-form-item label-width="0" class="text-center">
                <afp-button size="default" @click="()=>handleClose()" style="margin-right: 32px">取 消</afp-button>
                <afp-button size="default" type="primary" @click="()=>handleSave()">保 存</afp-button>
            </el-form-item>
        </el-form>
    </el-dialog>
</template>

<script>
import tradeId from "@/components/tradeId/index.vue";
import {isEmpty} from "lodash";
import SelectGroup from "@/views/modules/shortVideo/component/selectGroup.vue";

export default {
    components: {SelectGroup, tradeId},
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        autoSync: {
            type: Boolean,
            default: false
        },
        editExpertDetail: {
            type: Object,
            default: () => {
                return {}
            }
        }
    },
    data() {
        return {
            tradeTreeList: [],
            rules: {
                industryId: [{required: true, message: '请选择行业', trigger: 'change'}],
                updateTimeCondition: [{required: true, message: '请选择更新时间', trigger: 'change'}],
                likeCountThreshold: [{required: true, message: '请填写点赞数',  trigger: ['change','blur']}],
            },
            editForm: {
                industryId: '',
                groupId: '',
                autoSyncEnabled: false,
                likeCountThreshold: undefined,
                updateTimeCondition: null,
            }
        };
    },
    computed: {
        dialogTitle() {
            if (!this.autoSync) return '编辑达人'
            const name = this.editExpertDetail?.nickname || ''
            return name ? `开启自动提取文案 (${name})` : '开启自动提取文案'
        },
        modifyPermissions() {
            const {id} = this.$store.getters.getUserInfo;
            return id === this.editExpertDetail?.userId
        }
    },
    watch: {
        editExpertDetail: {
            handler(val) {
                if (!isEmpty(val)) {
                    this.getTradeListTree()
                    this.editForm = {
                        industryId: this.editExpertDetail?.industryId,
                        groupId: this.editExpertDetail?.groupId,
                        autoSyncEnabled: this.editExpertDetail?.autoSyncEnabled ? 1 : 0,
                        likeCountThreshold: this.editExpertDetail?.likeCountThreshold ?? undefined,
                        updateTimeCondition: this.editExpertDetail?.updateTimeCondition || ''
                    }
                }
            },
            deep: true
        },
    },
    methods: {
        async handleClose() {
            this.$emit('update:visible', false)
            this.$emit('changeRules')
        },
        async handleSave() {
            try {
                const response = await this.$refs.editForm.validate()
                if (response) {
                    this.$emit('changeExpert', this.editForm)
                    this.$emit('update:visible', false)
                }
            } catch (e) {
            }
        },
        getTradeListTree() {
            this.$httpBack.trade.listTree({}).then((res) => {
                if (res && res.code === 0) {
                    this.tradeTreeList = res.data
                }
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
</style>