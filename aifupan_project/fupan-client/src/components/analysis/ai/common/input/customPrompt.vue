<template>
    <div>
        <el-popover
            placement="top"
            width="500"
            trigger="click"
            popper-class="custom-prompt-popover">
            <div class="flex items-center justify-between pd-r20 pd-l20 pd-t10">
                <span class="font-s14 text-color">最多可以创建{{maxLen}}条提示词</span>
                <afp-button class="mg-l16" size="small" style="background: transparent" @click="createPrompt">
                    {{ items.length ? '添加' : '创建' }}提示词
                </afp-button>
            </div>
            <div class="group-box" v-if="items.length">
                <div v-for="(item,index) in items"
                     :class="{'group-box-item':true, 'cursor-pointer':true,'select-item':item.isSelect}" :key="item.id"
                     @mouseenter="()=>hoverItem(item)" @click.stop="()=>addPromptToInput(item)">
                    <div class="flex items-center justify-between">
                        <span>{{ index + 1 }}.{{ item.promptTitle }}</span>
                        <el-dropdown @command="(command)=>handleCommand(command,item)">
                            <el-button type="text" size="mini"
                                       :class="{'px-18':true, 'pd-b4':true,'pd-t4':true,'not-select':select !== item.id}">
                                编辑
                            </el-button>
                            <el-dropdown-menu slot="dropdown">
                                <el-dropdown-item command="edit">修改</el-dropdown-item>
                                <el-dropdown-item command="del" style="color: red">删除</el-dropdown-item>
                            </el-dropdown-menu>
                        </el-dropdown>
                    </div>
                </div>
            </div>
            <el-tag slot="reference" class="cs-p" size="medium" :class="{'is-select': true,'text-colorMain':true}"
                    type="info" effect="plain">
                自定义提示词
            </el-tag>
        </el-popover>
        <CreatePrompt ref="create_prompt" @createPromptForm="createPromptForm"/>
    </div>
</template>

<script>
import popoverBt from './popoverBt.vue';
import CreatePrompt from './createPrompt.vue'
import {cloneDeep} from "lodash";

export default {
    components: {
        popoverBt,
        CreatePrompt
    },
    props: {
        textarea: {
            type: String,
            default: ''
        },
    },
    data() {
        return {
            select: '',
            // {label: '速读法似懂非懂是沸腾钢', id: 3423423}, {label: '速读法速读法身份', id: 6546456}
            items: [],
            timeOut: null,
            maxLen: 0
        };
    },
    computed: {},
    watch: {
        textarea:{
            handler(val) {
                this.ifSelectItem(val)
            },
            immediate: true,
            deep: true
        }
    },
    methods: {
        ifSelectItem(val = ''){
            if(this.timeOut){return;};
            clearTimeout(this.timeOut);
            this.timeOut = setTimeout(()=>{
                this.items.forEach(item=>{
                    this.$set(item, 'isSelect', val.indexOf(item.promptContent) >=0)
                });
                this.timeOut = null
            }, 1000);
        },
        hoverItem(item) {
            this.select = item.id
        },
        createPrompt() {
            this.$refs.create_prompt.init()
        },
        editPrompt(item) {
            this.$refs.create_prompt.init(item)
        },
        createPromptForm(form) {
            let http = null;
            if(form.id){
                http = this.$httpBack.prompt.update(form)
            }else{
                if(this.items.length >= this.maxLen){
                    this.$message({
                        message: `最多可以创建${this.maxLen}条提示词`,
                        type: 'warning'
                    })
                    return;
                }
                http = this.$httpBack.prompt.save(form)
            }
            http.then(res=>{
                this.getPromptList()
            })
        },
        // 添加提示词到内容框.
        addPromptToInput(item) {
            // 点击已选中的提示词, 取消选中.
            if(item.isSelect){
                return;
            }
            // 点击未选中的提示词, 选中并添加到内容框.
            const items = cloneDeep(this.items)
            items.forEach(d => {
                if (d.id === item.id) {
                    d.isSelect = true
                }
            })
            this.items = items
            this.$emit('addPromptToInput', item)
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
                    this.getPromptList()
                })
            }).catch(() => {

            });
        },
        handleCommand(command, item) {
            if (command === 'edit') {
                this.editPrompt(item)
            } else if (command === 'del') {
                this.delPrompt(item)
            }
        },
        getPromptList(){
            this.$httpBack.prompt.list({
                page: 1,
                limit: 30
            }).then(res=>{
                this.items = res.data?.list?.map(d=>{
                    this.$set(d, 'isSelect',  this.textarea.indexOf(d.promptContent) >=0)
                    return d;
                }) || []
            })
        },
        async getPromptMaxLength(){
            this.maxLen = await this.$httpBack.common.getByKey('user_cust_prompt_num_max').then(res=>{
                return parseInt(res?.data?.kvValue || 0)
            });
        },
    },
    created() {
        this.getPromptList();
        this.getPromptMaxLength();
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
::v-deep(.buy-in-front) {
    .el-message-box__btns {
        border: 1px red solid;
    }
}

.group-box {
    max-height: 300px;
    overflow: hidden;
    overflow-y: auto;

    .group-box-item {
        margin-top: 4px;
        padding: 4px 20px;

        &:hover {
            background: rgba(68, 77, 255, 0.15);
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