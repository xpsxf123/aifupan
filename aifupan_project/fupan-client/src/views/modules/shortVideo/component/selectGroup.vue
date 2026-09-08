<template>
    <div class="selectGroup">
        <el-select v-model="$attrs.value" :placeholder="placeholder" :clearable="clearable" style="width: 100%"
                   popper-class="popper-class"
                   @change="changeHandle">
            <el-option v-for="item in groupList" :label="item.groupName" :value="item.groupId"
                       :key="item.groupId"/>
            <template slot="empty">
                <div style="padding: 6px">
                    <el-empty description="暂无数据" :image-size="42"></el-empty>
                    <div class="add-select-option flex items-center justify-between">
                        <el-input v-model="selectOption" placeholder="请输入内容"></el-input>
                        <el-button type="text" style="padding-inline: 6px" @click="addOptions">添加分组</el-button>
                    </div>
                </div>
            </template>
            <div class="add-select-option flex items-center justify-between">
                <el-input v-model="selectOption" placeholder="请输入内容"></el-input>
                <el-button type="text" style="padding-inline: 6px" @click="addOptions">添加分组</el-button>
            </div>
        </el-select>
    </div>
</template>

<script>

export default {
    components: {},
    props: {
        placeholder: {
            type: String,
            default: '请选择分组'
        },
        disabled: {
            type: Boolean,
            default: false
        },
        groupType: {
            type: [String, Number],
            default: 1
        },
        clearable: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            innerValue: '',
            selectOption: '',
            groupList: []
        };
    },
    computed: {},
    watch: {
    },
    methods: {
        async initData() {
            const serverResult = await this.$httpBack.shortVideo.options({groupType: this.groupType})
            if (serverResult.code !== 0) return
            this.groupList = serverResult.data || []
        },
        changeHandle(val) {
            this.$emit('input',val);
            this.$emit('change',val);
        },
        async addGroup(groupName) {
            const result = await this.$httpBack.shortVideo.addGroup({
                groupName,
                groupType: this.groupType,
                groupDescription: ''
            })
            if (result.code !== 0) return this.$message.error(result.msg)
            await this.initData()
            this.$message.success('添加分组成功')
        },
        addOptions() {
            if (!this.selectOption) return;
            if (this.groupList.filter(a => a.groupName === this.selectOption).length > 0) {
                this.$message.error('该行业已存在，请重新输入');
                return;
            }
            this.addGroup(this.selectOption)
            this.selectOption = ''
        },
    },
    created() {

    },
    mounted() {
        this.initData();
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
.selectGroup {
    display: inline-block;

}

.add-select-option {
    background: #fff;
    height: 42px;
    padding-inline: 6px;
    position: sticky;
    bottom: 0;
    width: 100%;
}

.popper-class {
    border: 1px red solid;
}
</style>