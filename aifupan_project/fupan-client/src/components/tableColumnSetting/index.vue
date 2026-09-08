<template>
    <el-dropdown class="tableColumnSetting" trigger="click" :hide-on-click="false" placement="bottom-end">
        <afp-button class="tableColumnSetting-btn" :disabled="disabled">
            {{ buttonText }}
            <i class="el-icon-arrow-down el-icon--right"></i>
        </afp-button>
        <el-dropdown-menu slot="dropdown" class="tableColumnSetting-menu">
            <div class="tableColumnSetting-menu__body">
                <el-checkbox
                    v-for="item in normalizedOptions"
                    :key="item.prop"
                    v-model="localVisibleMap[item.prop]"
                    :disabled="disabled || item.disabled"
                    @change="emitChange"
                >
                    {{ item.label }}
                </el-checkbox>
            </div>
        </el-dropdown-menu>
    </el-dropdown>
</template>

<script>
export default {
    name: 'TableColumnSetting',
    props: {
        value: {
            type: Object,
            default: () => ({})
        },
        options: {
            type: Array,
            default: () => []
        },
        buttonText: {
            type: String,
            default: '列表配置'
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            localVisibleMap: {}
        }
    },
    computed: {
        normalizedOptions() {
            return (Array.isArray(this.options) ? this.options : []).map((item) => ({
                prop: item?.prop,
                label: item?.label || item?.prop,
                disabled: Boolean(item?.disabled)
            })).filter((item) => item.prop)
        }
    },
    watch: {
        value: {
            immediate: true,
            deep: true,
            handler(val) {
                this.localVisibleMap = { ...(val || {}) }
                this.normalizedOptions.forEach((item) => {
                    if (this.localVisibleMap[item.prop] === undefined) this.$set(this.localVisibleMap, item.prop, true)
                })
            }
        },
        options: {
            immediate: true,
            deep: true,
            handler() {
                this.normalizedOptions.forEach((item) => {
                    if (this.localVisibleMap[item.prop] === undefined) this.$set(this.localVisibleMap, item.prop, true)
                })
                this.emitChange()
            }
        }
    },
    methods: {
        emitChange() {
            this.$emit('input', { ...this.localVisibleMap })
            this.$emit('change', { ...this.localVisibleMap })
        }
    }
}
</script>

<style lang="scss" scoped>
.tableColumnSetting-btn {
    height: 34px;
    border-radius: 17px;
    padding: 0 14px;
    font-size: 12px;
}

.tableColumnSetting-menu {
    padding: 8px 0;
}

.tableColumnSetting-menu__body {
    padding: 0 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}
</style>

