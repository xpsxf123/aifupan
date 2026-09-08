import copyHandler from'@/utils/copy.js';
export default {
    components: {  },
    props: {
    },
    data() {
        return {
            contenxtData: {},
            contextMenuData: [
                {
                    label: '粘贴',
                    type: 'paste',
                },
                {
                    label: '复制（无格式）',
                    type: 'copy',
                },
                {
                    label: '复制（带格式）',
                    style: true,
                    type: 'copy',
                }
            ],
            contentConfig: null
        }
    },
    computed:{
    },
    created(){
        
    },
    watch: {
        '$route.path'(){
            if(!this.contentConfig){return}
            this.removeContentConfig();
        }
    },
    methods: {
        /**
         * 重置内容配置
         * 
         * 本函数旨在清除当前实例中的内容配置（contentConfig），将其设置为null
         * 这通常在需要重置或重新初始化内容配置时调用
         * 
         * @returns {void} 该函数不返回任何值
         */
        removeContentConfig(){
            this.contentConfig = null;
        },
        /**
         * 设置上下文菜单数据
         * 
         * 该方法用于接收并存储上下文菜单的配置数据，以便在之后的操作中使用这些配置
         * 它没有返回值，主要负责更新组件内部的状态
         * 
         * @param {Object} data - 包含上下文菜单配置的对象
         * 
         * 案例： this.setContextMenuData({
                items: [{label: "测试",type: 'test'}],
                permission: ['test','copy']
            })
         */
        setContextMenuData(data){
            this.contentConfig = data;
        },
        getRangeData(){
            const selection = window.getSelection();
            if (!selection?.toString()?.trim()) return;
            const range = selection.getRangeAt(0);
            return {range, toString: selection?.toString()};
        },
        async copyHtml(rangeData){
            if(!rangeData){return}
            const {range, toString} = rangeData;
            const div = document.createElement('div');
            div.appendChild(range.cloneContents());
            // 2. 克隆选中的内容
            const fragment = range.cloneContents();
            // 3. 创建一个容器，用于处理样式
            const container = document.createElement('div');
            container.appendChild(fragment);

            // 4. 遍历所有元素，内联计算样式
            const elements = container.querySelectorAll('*');
            elements.forEach(el => {
                const computedStyle = window.getComputedStyle(el);
                // 将计算样式转为内联样式
                el.style.cssText = computedStyle.cssText;
            });
            // 5. 写入剪贴板（富文本 + 纯文本）
            const html = container.innerHTML;
            try {
                // 检查API可用性
                if (!navigator.clipboard || !navigator.clipboard.write) {
                    throw new Error('navigator.clipboard API不可用');
                }
                // 写入富文本到剪贴板
                await navigator.clipboard.write([
                    new ClipboardItem({
                    'text/html': new Blob([html], { type: 'text/html' }),
                    'text/plain': new Blob([toString], { type: 'text/plain' })
                    })
                ]);
            } catch (err) {
                console.error('复制失败:', err);
            }
        },
        /**
         * 获取上下文菜单数据
         * 该函数根据当前配置和权限，生成并返回经过过滤和加工的上下文菜单项数组
         * @param {Object} contenxtData - 上下文数据，用于传递给菜单项的回调函数
         * @returns {Array} 经过滤和加工的上下文菜单项数组
         */
        getContextMenuData(contenxtData,range){
            // 解构赋值从contentConfig中提取菜单项、权限、显示菜单项的函数和任务回调函数
            const { items = [], permission = [], showItems, onTask } = this.contentConfig;
            // 将当前上下文菜单数据与配置中的菜单项合并后映射处理
            return [...this.contextMenuData, ...items]?.map(d => {
                // 为每个菜单项添加点击事件处理函数
                d.onClick = async (e)=>{
                    // 如果任务回调函数为有效函数，则调用它，并传递当前菜单项和上下文数据
                    if (typeof onTask === 'function') {
                        onTask(d, contenxtData);
                    }else{
                        // 默认事件
                        switch(d.type){
                            case 'copy':
                                if(d.style){
                                    this.copyHtml(range);
                                }else{
                                    copyHandler(contenxtData?.copyTxt || contenxtData);
                                }
                                break;
                        }
                    }
                };
                // 返回加工后的菜单项
                return d;
            })?.filter(d => {
                // 根据显示菜单项的函数或权限判断当前菜单项是否应该显示
                if (typeof showItems === 'function') {
                    return showItems(d.type, permission);
                } else {
                    return permission.includes(d.type);
                }
            });
        },
        /**
         * 显示自定义右键上下文菜单
         * 
         * @param {MouseEvent} event - 鼠标事件对象
         * @param {Object} data - 可选参数，传递给上下文菜单的数据
         * @returns {boolean} 总是返回false以防止默认行为
         */
        rightContextMenu(event, data) {
            // 如果contentConfig未定义，则不执行任何操作并退出方法
            if(!this.contentConfig){return;};
            if(!data?.copyTxt){return false}
            // 存储传递给上下文菜单的数据，如果没有提供则使用空对象
            this.contenxtData = data||{};

            // 调用$contextmenu插件显示自定义上下文菜单
            this.$contextmenu({
                items: this.getContextMenuData(this.contenxtData, this.getRangeData()), // 从contenxtData获取菜单项
                event, // 鼠标事件信息
                customClass: 'custom-class', // 自定义菜单 class
                zIndex: 9999, // 菜单样式 
                minWidth: 100 // 主菜单最小宽度
            });
            // this.copyHtml(event);
            // 返回false以防止默认右键菜单行为
            return false;
        }
    } 
}