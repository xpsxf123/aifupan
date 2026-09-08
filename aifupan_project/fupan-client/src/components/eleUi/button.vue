<template>
    <el-button
        class="afp-button"
        :type="buttonType"
        :size="buttonSize"
        :plain="buttonPlain"
        :round="buttonRound"
        :circle="buttonCircle"
        :loading="buttonLoading"
        :disabled="buttonDisabled"
        :icon="icon"
        :style="computedStyle"
        :autofocus="autofocus"
        :native-type="nativeType"
        :class="customClass"
        @click="handleClick"
        @focus="handleFocus"
        @blur="handleBlur"
    >
        <slot></slot>
    </el-button>
</template>

<script>
/**
 * 全局按钮组件 - 基于Element UI Button的封装
 * 用于统一管理按钮样式和默认配置
 * @author AI Assistant
 * @version 1.0.0
 */
export default {
    name: 'afp-button',
    props: {
        /**
         * 按钮类型
         * @type {String}
         * @default 'default'
         */
        type: {
            type: String,
            default: '',
        },
        /**
         * 按钮尺寸
         * @type {String}
         * @default 'medium'
         */
        size: {
            type: String,
            default: ''
        },
        /**
         * 是否朴素按钮
         * @type {Boolean}
         * @default false
         */
        plain: {
            type: [Boolean, undefined],
            default: undefined
        },
        /**
         * 是否圆角按钮
         * @type {Boolean}
         * @default false
         */
        round: {
            type: [Boolean, undefined],
            default: undefined
        },
        /**
         * 是否圆形按钮
         * @type {Boolean}
         * @default false
         */
        circle: {
            type: Boolean,
            default: false
        },
        /**
         * 是否加载中状态
         * @type {Boolean}
         * @default false
         */
        loading: {
            type: Boolean,
            default: false
        },
        /**
         * 是否禁用状态
         * @type {Boolean}
         * @default false
         */
        disabled: {
            type: Boolean,
            default: false
        },
        /**
         * 图标类名
         * @type {String}
         * @default ''
         */
        icon: {
            type: String,
            default: ''
        },
        /**
         * 是否默认聚焦
         * @type {Boolean}
         * @default false
         */
        autofocus: {
            type: Boolean,
            default: false
        },
        /**
         * 原生type属性
         * @type {String}
         * @default 'button'
         */
        nativeType: {
            type: String,
            default: 'button',
            validator: (value) => {
                return ['button', 'submit', 'reset'].includes(value);
            }
        },
        /**
         * 自定义CSS类名
         * @type {String}
         * @default ''
         */
        customClass: {
            type: String,
            default: ''
        },
    },
    computed: {
        options() {
            return this.$CONFIG?.button || {}
        },
        /**
         * 计算按钮类型
         * @returns {String} 按钮类型
         */
        buttonType() {
            return this.type || this.options?.type || 'primary';
        },
        /**
         * 计算按钮尺寸
         * @returns {String} 按钮尺寸
         */
        buttonSize() {
            const size = this.size || this.options.size;
            if (this.options?.sizes?.includes(size)) {
                return size
            } else {
                if (this.options?.sizes?.length) {
                    return this.options?.sizes?.[this.options.sizes.length - 1] || 'default';
                } else {
                    return size || 'default';
                }
            }
        },
        computedStyle() {
            if(this.buttonCircle){
                return {}
            }
            const text = this.$slots.default?.[0]?.text?.replace(/\s+/g, '') || "";
            const length = text.length || 1;
            if (length > 2 || text.length === 0) {
                return {
                    paddingLeft: this.size !== 'default' ? `10px` : `18px`,
                    paddingRight: this.size !== 'default' ? `10px` : `18px`,
                };
            } else {
                return {
                    paddingLeft: this.type === 'danger' ? '10px' : `24px`,
                    paddingRight: this.type === 'danger' ? '10px' : `24px`
                };
            }
        },
        /**
         * 计算朴素按钮状态
         * @returns {Boolean} 朴素按钮状态
         */
        buttonPlain() {
            // 如果传入值为false，检查全局配置
            if (this.plain === undefined && this.options.plain !== undefined) {
                return this.options.plain;
            }
            return this.plain || false;
        },
        /**
         * 计算圆角按钮状态
         * @returns {Boolean} 圆角按钮状态
         */
        buttonRound() {
            // 如果传入值为false，检查全局配置
            if (this.round === undefined && this.options.round !== undefined) {
                return this.options.round;
            }
            return this.round || false;
        },
        /**
         * 计算圆形按钮状态
         * @returns {Boolean} 圆形按钮状态
         */
        buttonCircle() {
            // 如果传入值为false，检查全局配置
            if (this.circle === false && this.options.circle !== undefined) {
                return this.options.circle;
            }
            return this.circle;
        },
        /**
         * 计算加载状态
         * @returns {Boolean} 加载状态
         */
        buttonLoading() {
            // 如果传入值为false，检查全局配置
            if (this.loading === false && this.options.loading !== undefined) {
                return this.options.loading;
            }
            return this.loading;
        },
        /**
         * 计算禁用状态
         * @returns {Boolean} 禁用状态
         */
        buttonDisabled() {
            // 如果传入值为false，检查全局配置
            if (this.disabled === false && this.options.disabled !== undefined) {
                return this.options.disabled;
            }
            return this.disabled;
        }
    },
    methods: {
        /**
         * 处理按钮点击事件
         * @param {Event} event 点击事件对象
         * @emits click 点击事件
         */
        handleClick(event) {
            if (this.disabled || this.loading) {
                return;
            }
            this.$emit('click', event);
        },
        /**
         * 处理按钮聚焦事件
         * @param {Event} event 聚焦事件对象
         * @emits focus 聚焦事件
         */
        handleFocus(event) {
            this.$emit('focus', event);
        },
        /**
         * 处理按钮失焦事件
         * @param {Event} event 失焦事件对象
         * @emits blur 失焦事件
         */
        handleBlur(event) {
            this.$emit('blur', event);
        }
    }
};
</script>

<style lang="scss" scoped>
.global-button {
    // 全局按钮基础样式
    &.el-button {
        font-weight: 400;
        border-radius: 4px;
        transition: all 0.3s ease;

        // 主要按钮样式增强
        &.el-button--primary {
            background: linear-gradient(135deg, #409eff 0%, #66b3ff 100%);
            border-color: #409eff;

            &:hover {
                background: linear-gradient(135deg, #66b3ff 0%, #409eff 100%);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
            }

            &:active {
                transform: translateY(0);
            }
        }

        // 成功按钮样式增强
        &.el-button--success {
            background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
            border-color: #67c23a;

            &:hover {
                background: linear-gradient(135deg, #85ce61 0%, #67c23a 100%);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
            }
        }

        // 警告按钮样式增强
        &.el-button--warning {
            background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
            border-color: #e6a23c;

            &:hover {
                background: linear-gradient(135deg, #ebb563 0%, #e6a23c 100%);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(230, 162, 60, 0.3);
            }
        }

        // 危险按钮样式增强
        &.el-button--danger {
            background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
            border-color: #f56c6c;

            &:hover {
                background: linear-gradient(135deg, #f78989 0%, #f56c6c 100%);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
            }
        }

        // 信息按钮样式增强
        &.el-button--info {
            background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
            border-color: #909399;

            &:hover {
                background: linear-gradient(135deg, #a6a9ad 0%, #909399 100%);
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(144, 147, 153, 0.3);
            }
        }

        // 禁用状态
        &.is-disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none !important;
            box-shadow: none !important;
        }

        // 加载状态
        &.is-loading {
            pointer-events: none;
        }

        // 圆角按钮
        &.is-round {
            border-radius: 20px;
        }

        // 圆形按钮
        &.is-circle {
            border-radius: 50%;
        }
    }
}

// 全局按钮尺寸定制
.global-button {
    &.el-button--medium {
        padding: 12px 20px;
        font-size: 14px;
    }

    &.el-button--small {
        padding: 9px 15px;
        font-size: 12px;
    }

    &.el-button--mini {
        padding: 7px 12px;
        font-size: 12px;
    }
}
</style>