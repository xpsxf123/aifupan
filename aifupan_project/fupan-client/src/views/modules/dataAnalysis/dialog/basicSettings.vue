<template>
    <el-drawer
        :visible.sync="visible"
        :withHeader="false"
        class="custom-drawer"
        :close-on-press-escape="false"
        :wrapperClosable="false"
        :destroy-on-close="true"
        @close="setDiagnosisParams"
        size="65%">
        <div class="header">
            <i class="icon el-icon-close" @click="closeDialog"></i>
            <div class="title">基础设置</div>
        </div>
        <div style="overflow: auto;height: calc(100vh - 54px);">
            <CompereForm @addCompere="addCompere" ref="compere_form" @closeDialog="closeDialog" :tradeTreeList="tradeTreeList"
                         :isSettingChange="true" :resultData="resultData"/>
        </div>
    </el-drawer>
</template>

<script>
import CompereForm from '@/views/modules/addCompere/common/compereForm.vue'

export default {
    components: { CompereForm },
    props: {
        tradeTreeList: {
            type: Array,
            default: () => []
        }
    },
    data () {
        return {
            visible: false,
            resultData: {}
        }
    },
    computed: {},
    watch: {},
    created () {},
    mounted () {},
    methods: {
        showDialog (formData) {
            this.resultData = formData || {}
            this.visible = true
        },
        closeDialog() {
            this.resultData = {}
            this.visible = false
        },
        setDiagnosisParams(){
            this.$emit('initDiagnosisParams',{})
        },
        setFormData (data) {
          this.$refs.compere_form.setFormData(data)
        },
        async addCompere (data) {
            this.$emit('tradeConfirm', data)
            this.closeDialog()
        },
    }
}
</script>

<style scoped lang="scss">
.custom-drawer {
    .header {
        padding: 20px 0 12px 20px;
        display: flex;
        align-items: center;

        .icon {
            font-size: 18px;
            cursor: pointer;
        }

        .title {
            padding: 0 12px;
        }
    }
}
</style>
