<template>
    <el-table-column
		:prop="prop"
		:label="getLabel"
		:align="columnOption.align||align"
		v-bind="columnOption"
		:key="prop"
	>
		<template v-if="$scopedSlots[getSlotName(prop, '-header')]" v-slot:header="scope">
			<slot :name="getSlotName(prop, '-header')" v-bind="scope"></slot>
		</template>
        <template slot-scope="{row, column, $index}">
            <slot v-if="isSlotDefault" :name="getSlotName(prop)" v-bind="{row, column, $index}"></slot>
			<template v-else>
				<div v-if="isRender" v-html="handlerRender(row, columnOption, column, $index)" v-on="getColumnOn(row)"></div>
				<component  v-else :is="getColumnType(column)" :class="getClass(option, row)" v-bind="getColumnBind(row, column)" v-on="getColumnOn(row)">
					{{ onFormatter(row, column, row[prop], $index) }}
				</component>
			</template>
        </template>
	</el-table-column>
</template>

<script>
import slot from './../../mixins/slot';
export default {
    components: {},
    mixins: [slot],
    props:{
		type: {
			type: String,
			default: ''
		},
		align: {
			type: String,
			default: 'center'
		},	
		index: {
			type: [Number, Function],
			default: null
		},
		columnKey: {
			type: String,
			default: ''
		},
		label: {
            type: [Function, String],
			default: ''
		},
        formatter: {
			type: [Function, Object, String],
			default: null
		},
		prop: {
			type: String,
			default: ''
		},
		option: {
			type: Object,
			default: ()=>{
				return {}
			}
		},
		dicData: {
			type:[Array,Boolean],
			default: false
		}
    },
    data() {
        return {
			
        };
    },
    computed: {
        getLabel(){
            return typeof this.label === 'function' ? this.label?.() : this.label
        },
        columnOption() {
			let o = {
				...this.$props,
				...this.option
			}
			o.showOverflowTooltip = o.overHidden || o.showOverflowTooltip || true
			delete o.overHidden
			delete o.dicData
			delete o.props
			return o
		},
		allAttrsOption() {
			return {
				...this.$attrs,
				...this.columnOption
			}
		},
		isRender() {
			return typeof this.columnOption.render === 'function'
		},
		isSlotDefault(){
			if(!!this.$scopedSlots[this.prop] || !!this.$scopedSlots.default){
				return true
			}else{
				return false
			}
		},
    },
    watch: {},
    methods: {
		onFormatter(row, column, cellValue, index) {
			let text = row[this.prop];
			if (typeof this.formatter === 'function') {
				text = this.formatter(row, column, cellValue, index)
			}else if (typeof this.formatter === 'string') {
				let str = `row.${this.formatter}`;
				try {
					text = eval(str)
				} catch (err) {
					text = str
				}
			}else if(this.dicData){
				// 查询数组数据
				text = this.dicData?.find(d=>d.value === text)?.label;
			}else if(this.option?.suffix){
				text = text + this.option?.suffix;
			}
			// 如果返回数据为对象类型则格式化失败
			if (text !== null && typeof text === 'object') {
				text = cellValue
				console.error('表格column格式化失败！返回参数有问题，使用默认参数值！并给出警告～～～～～～', text, this.prop)
			} else if (text === null) {
				return this.defaultText
			}
			return typeof text !== 'undefined' ? text : this.defaultText
		},
        handlerRender(row, option, column, index) {
			if(option.renderFormatter){
				this.$set(option,'formatterText',this.onFormatter(row,column, row[this.prop],index))
				// option.formatterText = this.onFormatter(row,column, row[this.prop],index)
			}
			return this.columnOption.render(row, option, column)
		},
        getColumnBind(row) {
			let o = {}
			if (this.allAttrsOption.url || this.allAttrsOption.link) {
				o.href = 'javascript:;'
			}
			if (this.allAttrsOption.class) {
				o.class = this.allAttrsOption.class
			}
			return o
		},
        getColumnOn(row) {
			let o = {};
			Object.keys(this.columnOption.on ||{}).forEach(key => {
				o[key] = (e) => {
					this.columnOption.on[key](e, row);
				}
			})
			return o
		},
        getColumnType() {
			let type = 'span'
			if (this.allAttrsOption.url) {
				return 'a'
			}
			return type
			},
		getClass(option, row){
			if(option.classNameFn){
				return option.classNameFn(row)
			}else if(option.className){
				return option.className
			}else if(option.numberText){
				return 'text-b-number'
			}else{
				return ''
			}
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
.text-b-number{
	// font-family: 'Alibaba PuHuiTi 3.0,Alibaba PuHuiTi 30';
	// font-family: Alibaba PuHuiTi 3.0, Alibaba PuHuiTi 30;
	font-size: 14px;
	color: #4D4D4D;
	line-height: 20px;
	text-align: left;
	font-style: normal;
	text-transform: none;
}
.export_script, .record-summary {
    cursor: pointer;
    color: #636CBD;
}
</style>