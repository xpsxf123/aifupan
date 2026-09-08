<template>
	<el-dialog :visible.sync="visibles" ref="dialog" class="dialog-box" :class="{ 'height-fullscreen': heightFullscreen }" v-bind="$props" v-on="$listeners">
		<div slot="title" class="el-dialog-title-box">
			<slot name="title">
				<span class="el-dialog__title">{{ title }}</span>
			</slot>
			<div class="dialog__menu"><slot name="menu"></slot></div>
		</div>
		<div class="dialog-box">
			<slot v-if="$slots.dialogContent" name="dialog-content"></slot>
			<scrollbar v-else ref="scrollbar" :height="scrollbarHeight">
				<slot></slot>
			</scrollbar>
		</div>
	</el-dialog>
</template>
 
<script>
import Scrollbar from './../scrollbar'
export default {
	name: 'DialogBox',
	components: { Scrollbar },
	props: {
		visible: {
			type: [Boolean, undefined],
			default: undefined
		},
		title: String,
		width: {
			type: String,
			default: '50%'
		},
		fullscreen: {
			type: Boolean,
			default: false
		},
		top: {
			type: String,
			default: '15vh'
		},
		modal: {
			type: Boolean,
			default: true
		},
		modalAppendToBody: {
			type: Boolean,
			default: true
		},
		appendToBody: {
			type: Boolean,
			default: false
		},
		lockScroll: {
			type: Boolean,
			default: true
		},
		customClass: String,
		closeOnClickModal: {
			type: Boolean,
			default: true
		},
		closeOnPressEscape: {
			type: Boolean,
			default: true
		},
		showClose: {
			type: Boolean,
			default: true
		},
		beforeClose: Function,
		center: {
			type: Boolean,
			default: false
		},
		destroyOnClose: {
			type: Boolean,
			default: false
		},
		height: {
			type: String,
			default: ''
		}
	},
	data() {
		return {
			dialogVisible: false,
		}
	},
	computed: {
		visibles: {
			get() {
				if (typeof this.visible !== 'undefined') {
					return this.visible
				} else {
					return this.dialogVisible
				}
			},
			set(v) {
				this.$emit('input', v)
				this.$emit('update:visible', v)
			}
		},
		heightFullscreen() {
			return this.height === '100vh'
		},
		scrollbarHeight() {
			if (!this.height) return
			return `calc(${this.height} - 119px)`
		}
	},
	watch: {},
	created() {},
	mounted() {},
	methods: {
		scrollbarHeightAuto() {
			this.$refs.scrollbar.update()
		}
	}
}
</script>

<style scoped lang="scss">
.dialog-box{
	::v-deep(.el-dialog){
		.el-dialog__header{
			position: relative;
			padding: 12px 20px;
            background: #F4F9FF;
			.el-dialog__headerbtn{
                right: 12px;
                top: 12px;
			}
		}
		.el-dialog__body{
			padding: 20px;
		}
	}
	
}
.el-dialog-title-box{

}
</style>