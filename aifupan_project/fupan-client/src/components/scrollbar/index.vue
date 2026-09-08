<template>
	<div :style="styles" :class="{ 'overflow_hidden': isAuto }">
		<el-scrollbar ref="scrollbar" :style="scrollbarStyle">
			<div style="padding-bottom: 6px">
				<slot></slot>
			</div>
		</el-scrollbar>
	</div>
</template>

<script>
export default {
	name: 'scrollbar',
	components: {},
	props: {
		height: {
			type: String,
			validator(value) {
				if (value === '') return true
				return /^\d+(%|px|vh|rem|rm)$/.test(value)
			},
			default: ''
		}
	},
	data() {
		return {
			scrollbarHeight: '100%',
			temout: null
		}
	},
	computed: {
		isAuto() {
			return !!this.height || !!this.maxHeight
		},
		styles() {
			let o = {}
			if (this.height) {
				o.height = this.height
			}
			return o
		},
		scrollbarStyle() {
			return {
				height: '100%',
				overflow: 'hidden'
			}
		}
	},
	watch: {},
	created() {},
	mounted() {},
	methods: {
		init({ top = '', left = '' }) {
			this.$nextTick(() => {
				this.scrollTo(top, left)
				this.update()
			})
		},
		update() {
			this.$refs.scrollbar.update()
		},
		scrollTo(top, left) {
			if (top !== '' && typeof top === 'number') {
				this.$refs.scrollbar.wrap.scrollTop = top
			}
			if (left !== '' && typeof left === 'number') {
				this.$refs.scrollbar.wrap.scrollLeft = left
			}
		}
	}
}
</script>
<style lang="scss" scoped>

</style>