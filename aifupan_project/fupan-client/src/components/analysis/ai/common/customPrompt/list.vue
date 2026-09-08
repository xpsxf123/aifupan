<template>
    <div>
        <drawer
            class="prompt-drawer-list"
            width="60%"
            title="自定义提示词"
            :visible.sync="drawerVisible"
            :wrapperClosable="false"
            :modal="false"
            :append-to-body="true"
        >
            <div class="content">
                <div class="flex-jc-sb">
                    <span class="font-s14 text-color"><b>最多可以创建{{maxLen}}条提示词</b></span>
                    <afp-button class="mg-l16" size="small" style="background: transparent" @click="createPrompt">
                        {{ items.length ? '添加' : '创建' }}提示词
                    </afp-button>
                </div>
                <div class="group-box mg-t12 pd-14">
                    <div class="flex-jc-sb font-s14 text-color3 pd-b12 pd-l10 pd-r10">
                        <span>内容</span>
                        <span>操作</span>
                    </div>
                    <div v-for="(item,index) in items"
                        class="group-box-item cursor-pointer brs-8" :key="item.id"
                        @mouseenter="()=>hoverItem(item)" @click.stop="()=>addPromptToInput(item)">
                    <div class="flex items-center justify-between">
                        <span class="flex-ai-c"><img src="~@/assets/imgs/new/sz.png" style="max-width: 30px;" class="mg-r8 propmt-pointer" />{{ index + 1 }}.{{ item.promptTitle }}</span>
                        <span>
                            <el-button type="text" size="mini" @click.stop="onEdit(item)">编辑</el-button>
                            <el-button type="text" size="mini" style="color: red;" @click.stop="delPrompt(item)">删除</el-button>
                        </span>
                    </div>
                </div>
            </div>
            </div>
        </drawer>

        <CreatePrompt ref="create_prompt" :moreConfigProps="moreConfigProps" @createPromptForm="createPromptForm" />
    </div>
</template>

<script>
/**
 * @file 自建问题列表抽屉。
 * @description 负责展示、维护自建问题列表，并在本地为每条提示词挂载独立的更多配置预设。
 */
import drawer from '@/components/drawer/index.vue'
import CreatePrompt from './create.vue'
import drawerMixin from '@/mixins/drawer'
import {
    mergePromptMoreConfigList,
    removePromptMoreConfig,
    resolvePromptId,
    savePromptMoreConfig
} from './promptMoreConfigStorage'
export default {
    components: {
        CreatePrompt,
        drawer
    },
    props: {
        textarea: {
            type: String,
            default: ''
        },
        aiCueType:{
            type: [String,Number],
            default: ''
        },
        moreConfigProps: {
            type: Object,
            default: () => ({})
        }
    },
    mixins: [drawerMixin],
    data() {
        return {
            select: '',
            items: [],
            timeOut: null,
            maxLen: 0
        };
    },
    computed: {},
    watch: {
    },
    methods: {
        onEdit(item){
            this.editPrompt(item)
        },
        delPrompt() {
            this.createPrompt();
        },
        hoverItem(item) {
            this.select = item.id;
        },
        createPrompt() {
            console.log('createPrompt');
            this.$refs.create_prompt.show();
        },
        editPrompt(item) {
            console.log(item);
            this.$refs.create_prompt.show({data:item});
        },
        createPromptForm({form,next}) {
            const promptMoreConfig = form?.promptMoreConfig || {}
            const requestForm = {
                ...form
            }
            delete requestForm.promptMoreConfig
            let http = null;
            if(this.aiCueType !== ''){
                requestForm.aiCueType = this.aiCueType;
            }
            if(requestForm.id){
                http = this.$httpBack.prompt.update(requestForm);
            }else{
                if(this.items.length >= this.maxLen){
                    this.$message({
                        message: `最多可以创建${this.maxLen}条提示词`,
                        type: 'warning'
                    });
                    return;
                }
                http = this.$httpBack.prompt.save(requestForm);
            }
            http.then(async (res)=>{
                const latestList = await this.getPromptList();
                const promptId = resolvePromptId({
                    response: res,
                    form: requestForm,
                    list: latestList
                })
                savePromptMoreConfig(promptId, promptMoreConfig, this.moreConfigProps)
                this.items = mergePromptMoreConfigList(this.items, this.moreConfigProps)
                this.$message({
                    message: `${requestForm.id ? '编辑' : '新增'}提示词成功`,
                    type: 'success'
                });
                next();
            });
        },
        // 添加提示词到内容框.
        addPromptToInput(item) {
            // 点击未选中的提示词, 选中并添加到内容框.
            this.$emit('select', item);
        },
        delPrompt(item) {
            this.$confirm(`
            <div class="buyInInfo">您确定要删除该提示词吗？此操作不可撤销，数据将被永久删除。</div>
            `, '友情提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
                dangerouslyUseHTMLString: true,
                customClass: 'buy-in-front prompt-confirm',
                distinguishCancelAndClose: true,
                closeOnClickModal: false,
                closeOnPressEscape: false,
                showClose: false
            }).then(async () => {
                this.$httpBack.prompt.delete({
                    id: item.id
                }).then(res=>{
                    removePromptMoreConfig(item.id)
                    this.getPromptList();
                });
            }).catch(() => {

            });
        },
        handleCommand(command, item) {
            if (command === 'edit') {
                this.editPrompt(item);
            } else if (command === 'del') {
                this.delPrompt(item);
            }
        },
        getPromptList(){
            return this.$httpBack.prompt.list({
                aiCueType: this.aiCueType,
                page: 1,
                limit: 30
            }).then(res=>{
                this.items = mergePromptMoreConfigList(res.data?.list || [], this.moreConfigProps);
                return this.items
            });
        },
        async getPromptMaxLength(){
            this.maxLen = await this.$httpBack.common.getByKey('user_cust_prompt_num_max').then(res=>{
                return parseInt(res?.data?.kvValue || 0);
            });
        },
        initList(){
            this.getPromptList();
            this.getPromptMaxLength();
        },
        async showCallback() {
            this.initList();
        },
        hideCallback() {
            this.select = '';
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

<style lang="scss">
.prompt-drawer-list{
    .el-drawer__header{
        margin-bottom: 0;
        padding: 12px 12px 0 12px;
        background: #F4F9FF;
    }
    .el-drawer__body{
        padding: 10px;
    }
}
</style>

<style lang='scss' scoped>

// .propmt-pointer{
//     animation: pointerMove 1s ease-in-out infinite;
// }
// @keyframes pointerMove {
//     0%, 100% { transform: translateX(0); }
//     50% { transform: translateX(6px); }
// }

::v-deep(.buy-in-front) {
    .el-message-box__btns {
        border: 1px red solid;
    }
}

.group-box {
    height: 370px;
    overflow: hidden;
    overflow-y: auto;
    border-radius: 10px;
    border: 1px solid #DFEAF6;
    .group-box-item {
        padding: 10px;
        &:hover {
           background: #F7F7F7;
        }
    }

    .select-item {
        background: rgba(0, 0, 0, 0.1);
        cursor: not-allowed;
    }

    .not-select {
        color: #798AAD;;
    }
}

.is-select {
    border-color: var(--color-main) !important;
    color: var(--color-main) !important;
}
</style>
