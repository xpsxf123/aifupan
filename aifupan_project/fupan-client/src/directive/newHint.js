

/**
    <template>
    <div>
        <button v-new-hint="{ key: 'button1', version: '1.0.0' }">点击我</button>
    </div>
    </template>

    参数说明:
    v-new-hint 指令接受一个对象作为参数，包含以下属性：

    key (String): 唯一标识符，用于记录元素是否已被点击。
    version (String): 版本号，用于控制提示的显示条件。
    className (String, 可选): 自定义提示类名，默认为 directive-new-hint。
    type (String, 可选): 显示类型，可选值为 'click'（点击显示）或 'always'（始终显示），默认为 'click'。

    type = 'always' 时，无论是否点击，都会显示提示。
    type = 'click' 时，只有点击元素时才会显示提示。

    如果 type = 'always' 时，key 和 version 参数是必填的。
    如果 type = 'click' 时，key 参数是必填的，version 参数是可选的。
*/

const KEY_NAME = 'new_hint_array';
const DEFAULT_CLASS_NAME = 'directive-new-hint';
// 判断版本号
function isVersion(version){
    let Ver = localStorage.getItem('aifupan_version') || '';
    // 跳过测试版本
    if(Ver?.indexOf('.test') >= 0 ){
        Ver = Ver.split('.test')[0];
    }
    return version === Ver;
}
// 移除数据
function removeNewHintData(el, removeClass){
    el.removeAttribute('data-new-hint');
    el.removeAttribute('data-new-hint-version');
    el.removeAttribute('data-new-hint-class');
    if(removeClass){
        el.removeAttribute('class', removeClass);
    }
}
// 添加数据
function addNewHintData(el, key, version, className){
    el.setAttribute('data-new-hint', key);
    el.setAttribute('data-new-hint-version', version);
    el.setAttribute('data-new-hint-class', className || DEFAULT_CLASS_NAME);
}
// 获取数据
function getNewHintData(el){
    return {
        newKey: el.getAttribute('data-new-hint'),
        newVersion: el.getAttribute('data-new-hint-version'),
        newClassName: el.getAttribute('data-new-hint-class')
    }
}

export default {
    bind(el, binding) {
        try {
            if(!binding.value){
                return;
            }
            const {key, version, className,type ='click'} = binding.value;
            if((!key || !version) && type !== 'always'){
                return;
            }
            // 判断版本
            if(!isVersion(version)){return;}
            // 始终显示
            if(type === 'always'){
                el.classList.add(className || DEFAULT_CLASS_NAME);
                return;
            }
            // 点击显示
            let newHintArray = (localStorage.getItem(KEY_NAME))?.split(',') || [];
            const isHas = newHintArray.includes(key);
            // 判断是否被点击过
            if(!isHas){
                // 添加类名
                el.classList.add(className || DEFAULT_CLASS_NAME);
                // 添加数据
                addNewHintData(el, key, version, className);
                // 添加事件
                el.addEventListener('click', (event) => {
                    const {newKey, newVersion, newClassName} = getNewHintData(el);
                    if(!isVersion(newVersion)){
                        // 移除类名
                        removeNewHintData(el, newClassName);
                        return;
                    }
                    // 添加数据
                    let nhAry = (localStorage.getItem(KEY_NAME))?.split(',') || [];
                    if(newKey){
                        nhAry?.push(newKey);
                        localStorage.setItem(KEY_NAME, nhAry.join(','));
                    }
                    // 移除数据
                    removeNewHintData(el, newClassName);
                });
            } 
        } catch (error) {
            console.error(error);
        }
    }
};