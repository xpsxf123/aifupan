<template>
    <div>
        <h3 class="font-s20 mg-0" style="line-height: 34px;">
            <span style="font-weight: normal;" class="font-s18"><span class="text-colorTheme">本功能需要消耗AI算力</span>，请按需使用！</span>
            <!-- 你好，{{ getNickName }} -->
            <div class="flex-ai-c">
<!--                <div>我是你的AI{{getAiTypeTitle}}，{{ getContent }}：</div>-->
                <div>以下是智能体预设问题，个性化分析请按需提问：</div>
<!--                v-if="!isCompare && isParagraph"-->
                <div>
<!--                    <afp-button type="primary" :plain="scene !== 'all'"  @click="addProblem('all')">全文问题</afp-button>-->
<!--                    <afp-button type="primary" :plain="scene === 'all'"  @click="addProblem('paragraph')">段落问题</afp-button>-->
<!--                    <afp-button :plain="false" @click="customAgent">定制智能体<i class="el-icon-arrow-down"></i></afp-button>-->

                    <el-dropdown trigger="click" @command="handleCommand">
                        <afp-button :plain="false" type="primary">
                            {{ agentType === 'all' ? '标准智能体' : '定制智能体' }}<i class="el-icon-arrow-down"></i>
                        </afp-button>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item command="all">标准智能体</el-dropdown-item>
                            <el-dropdown-item command="custom">定制智能体</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </div>
            </div>
        </h3>

        <CustomAgent ref="custom_agent"/>
    </div>
</template>

<script>
import CustomAgent from './customAgent.vue'
import aiTypeMixin from '@/mixins/aiTypeMixin'
export default {
    components: {CustomAgent},
    props:{
        isCompare: {
            type: Boolean,
            default: false
        },
        scene: {
            type: String,
            default: ''
        }
    },
    inject: ['AiVnode'],
    mixins: [aiTypeMixin],
    data() {
        return {
        };
    },
    computed: {
        isParagraph(){
            return this.type === 'assistant' || this.type === 'violation'
        },
        getNickName(){
            return this.$store.getters?.getNickName
        },
        getTitle(){
            if(this.type === 'assistant'){
                return '运营'
            }else{
                return '违规'
            }
        },
        getContent(){
            if(this.type === 'textAssistant'){
                return '抓话术更智能'
            }else{
                return "我能帮你解决"
            }
        },
        customList(){
            return this.AiVnode.problem?.custom || []
        },
        agentType(){
            return this.AiVnode.scene || 'all'
        }
    },
    watch: {},
    methods: {
        addProblem(type){
            this.AiVnode?.addProblem({},type);
        },
        customAgent(){
            this.$refs.custom_agent?.showCustomAgent?.();
        },
        handleCommand(command) {
            if(command==='custom'){
                if (!this.customList?.length){
                    this.customAgent()
                    return
                }
            }
            this.AiVnode?.addProblem({},command);
        }
    },
    created() {
        
    },
    mounted() {
        
    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style lang='scss' scoped>

</style>