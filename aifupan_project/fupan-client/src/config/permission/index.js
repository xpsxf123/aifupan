function permission(Vue, option) {
    // 定义一个存储权限码的列表
    let authCodeList = [];

    /**
     * 向权限码列表中添加新的权限码
     * @param {Array} list - 新的权限码列表
     */
    const addAuthCode = (list) => {
        authCodeList.push(...list);
    }

    /**
     * 检查给定的权限码或权限码列表是否存在于权限码列表中
     * @param {string | Array} codeOrCodes - 单个权限码或权限码列表
     * @returns {boolean} - 如果存在则返回true，否则返回false
     */
    const isAuth = (codeOrCodes, type) => {
        if (Array.isArray(codeOrCodes)) {
            if (codeOrCodes?.length === 0) { return false }
            if(type === 'every'){
                return codeOrCodes.every(code => {
                    return authCodeList.includes(code);
                })
            }else{
                return codeOrCodes.some(code => {
                    return authCodeList.includes(code);
                })
            }
        } else {
            return authCodeList.includes(codeOrCodes)
        }
    }

    // 在Vue原型上添加添加权限码的方法
    Vue.prototype.$addAuthCode = function (list) {
        addAuthCode(list);
    };

    // 在Vue原型上添加检查权限的方法
    Vue.prototype.$auth = function (codeOrCodes, type) {
        return isAuth(codeOrCodes, type);
    }

    Vue.prototype.$removeAuthIds=function (){
        authCodeList = []
    }

    // 创建一个Vue指令用于在DOM中根据权限控制元素的显示
    Vue.directive('auth', {
        inserted: function (el, binding, vnode) {
            try {
                const { value, type } = binding;
                // 确保权限数据格式正确 - value不能是Object和不能是数组
                if (!Array.isArray(value) && (value instanceof Object || typeof value === 'undefined' || isNaN(value) || value === null || typeof value === 'function')) {
                    throw new Error(`权限数据格式错误，请输入字符串，布尔，数字格式数据！`)
                }
                let hasPermission = isAuth(value, type);
                // 如果没有相应的权限，则移除该元素
                if (!hasPermission) {
                    el.parentNode && el.parentNode.removeChild(el);
                }
            } catch (e) {
                console.error(e.message);
            }
        }
    })
}

permission.install = permission;

export default permission