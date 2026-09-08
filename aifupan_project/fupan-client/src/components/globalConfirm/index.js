/**
 * 全局弹窗提示配置工具
 * 基于Element UI的MessageBox组件封装，提供统一的弹窗提示样式和配置
 * @author AI Assistant
 * @version 1.0.0
 */

/*
this.$confirmWarning( {
  title: '友情提示',
  message: `是否需要保存后退出？`
}).then(() => {})
*/


/**
 * 默认弹窗配置
 * @type {Object}
 */
const DEFAULT_CONFIG = {
  title: '',
  customClass: 'custom-confirm',
  closeOnClickModal: false,
  closeOnPressEscape: false,
  dangerouslyUseHTMLString: true,
  showClose: true
};

/**
 * 默认图标配置
 * @type {Object}
 */
const DEFAULT_ICONS = {
  warning: 'icon-a-Frame9651',
  success: 'icon-a-Frame114369',
  error: 'icon-tixing',
  info: 'icon-xinxi'
};

/**
 * 创建带图标的HTML消息内容
 * @param {string} message - 消息文本
 * @param {string} iconId - 图标ID
 * @param {Object} options - 样式配置选项
 * @param {string} options.iconWidth - 图标宽度，默认'80px'
 * @param {string} options.iconHeight - 图标高度，默认'80px'
 * @param {string} options.marginTop - 图标上边距，默认'12px'
 * @param {string} options.padding - 消息文本内边距，默认'24px 0'
 * @returns {string} HTML字符串
 */
function createIconMessage(message, iconId, options = {}, title) {
  const {
    iconWidth = '48px',
    iconHeight = '48px',
    marginTop = '12px',
    padding = '24px 0'
  } = options;

  return `
    <div style="text-align:center">
      <div style="margin-top:${marginTop}">
        <svg class="icon menuImg" aria-hidden="true" style="width:${iconWidth};height:${iconHeight}">
          <use xlink:href="#${iconId}"></use>
        </svg>
      </div>
      <div class="font-s18 text-colorMain font-w500">${title}</div>
      <div style="padding:${padding}">${message}</div>
    </div>
  `;
}

/**
 * 全局弹窗提示配置类
 */
class GlobalConfirm {
  /**
   * 构造函数
   * @param {Object} Vue - Vue实例
   */
  constructor(Vue) {
    this.Vue = Vue;
  }

  /**
   * 显示确认弹窗
   * @param {string|Object} options - 消息文本或完整配置对象
   * @param {Object} config - 额外配置选项
   * @param {string} config.type - 弹窗类型：'warning'|'success'|'error'|'info'
   * @param {string} config.title - 弹窗标题
   * @param {string} config.confirmButtonText - 确认按钮文本
   * @param {string} config.cancelButtonText - 取消按钮文本
   * @param {string} config.iconId - 自定义图标ID
   * @param {Object} config.iconOptions - 图标样式配置
   * @param {boolean} config.showIcon - 是否显示图标，默认true
   * @returns {Promise} 弹窗Promise对象
   * @throws {Error} 当Vue实例不存在时抛出错误
   */
  confirm(options, config = {}) {
    if (!this.Vue) {
      throw new Error('Vue实例未初始化，请先调用install方法');
    }

    try {
      // 处理参数
      let message, finalConfig;
      
      if (typeof options === 'string') {
        message = options;
        finalConfig = { ...DEFAULT_CONFIG, ...config,title:'' };
      } else {
        message = options.message || '';
        finalConfig = { ...DEFAULT_CONFIG, ...options, ...config,title:'' };
      }

      const {
        type = 'warning',
        iconId,
        iconOptions = {},
        showIcon = true,
        confirmButtonText = '确定',
        cancelButtonText = '取消',
        ...restConfig
      } = finalConfig;

      // 构建最终配置
      const confirmConfig = {
        ...restConfig,
        confirmButtonText,
        cancelButtonText
      };

      // 如果需要显示图标，构建带图标的消息

      if (showIcon) {
        const { title } = options;
        const selectedIconId = iconId || DEFAULT_ICONS[type] || DEFAULT_ICONS.warning;
        confirmConfig.message = createIconMessage(message, selectedIconId, iconOptions, title);
      } else {
        confirmConfig.message = message;
      }

      return this.Vue.prototype.$confirm('', confirmConfig);
    } catch (error) {
      console.error('GlobalConfirm.confirm 执行出错:', error);
      throw error;
    }
  }

  /**
   * 显示警告确认弹窗
   * @param {string} message - 消息文本
   * @param {Object} config - 配置选项
   * @returns {Promise} 弹窗Promise对象
   */
  warning(message, config = {}) {
    return this.confirm(message, { ...config, type: 'warning' });
  }

  /**
   * 显示成功确认弹窗
   * @param {string} message - 消息文本
   * @param {Object} config - 配置选项
   * @returns {Promise} 弹窗Promise对象
   */
  success(message, config = {}) {
    return this.confirm(message, { ...config, type: 'success' });
  }

  /**
   * 显示错误确认弹窗
   * @param {string} message - 消息文本
   * @param {Object} config - 配置选项
   * @returns {Promise} 弹窗Promise对象
   */
  error(message, config = {}) {
    return this.confirm(message, { ...config, type: 'error' });
  }

  /**
   * 显示信息确认弹窗
   * @param {string} message - 消息文本
   * @param {Object} config - 配置选项
   * @returns {Promise} 弹窗Promise对象
   */
  info(message, config = {}) {
    return this.confirm(message, { ...config, type: 'info' });
  }

  /**
   * 显示保存退出确认弹窗
   * @param {string} message - 消息文本，默认'是否需要保存后退出？'
   * @param {Object} config - 配置选项
   * @returns {Promise} 弹窗Promise对象
   */
  saveAndExit(message = '是否需要保存后退出？', config = {}) {
    return this.confirm(message, {
      confirmButtonText: '保存并退出',
      cancelButtonText: '直接退出',
      type: 'warning',
      ...config
    });
  }
}

/**
 * Vue插件安装方法
 * @param {Object} Vue - Vue构造函数
 * @param {Object} options - 插件配置选项
 * @param {Object} options.defaultConfig - 默认配置覆盖
 * @param {Object} options.defaultIcons - 默认图标配置覆盖
 */
function install(Vue, options = {}) {
  // 合并默认配置
  if (options.defaultConfig) {
    Object.assign(DEFAULT_CONFIG, options.defaultConfig);
  }
  
  // 合并默认图标配置
  if (options.defaultIcons) {
    Object.assign(DEFAULT_ICONS, options.defaultIcons);
  }

  // 创建全局实例
  const globalConfirm = new GlobalConfirm(Vue);
  
  // 注册到Vue原型
  Vue.prototype.$globalConfirm = globalConfirm;
  
  // 注册快捷方法
  Vue.prototype.$confirmWarning = globalConfirm.warning.bind(globalConfirm);
  Vue.prototype.$confirmSuccess = globalConfirm.success.bind(globalConfirm);
  Vue.prototype.$confirmError = globalConfirm.error.bind(globalConfirm);
  Vue.prototype.$confirmInfo = globalConfirm.info.bind(globalConfirm);
  Vue.prototype.$confirmSaveExit = globalConfirm.saveAndExit.bind(globalConfirm);
}

// 导出
export default {
  install,
  GlobalConfirm
};

export { GlobalConfirm, install };