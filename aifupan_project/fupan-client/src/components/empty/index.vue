<template>
	<div >
		<p >{{ desc }}</p>
		<slot></slot>
	</div>
</template>

<script>
export default {
	name: 'empty',
	props: {
		/**
		 * 图表svgName
		 */
		icon: {
			type: String,
			default: 'null'
		},
		/**
		 * 图片距离文字高度的距离
		 */
		size: {
			type: String,
			default: '80'
		},
		/**
		 * 中文描述
		 */
		desc: {
			type: String,
			default: ''
		}
	},
	methods: {}
}
</script>