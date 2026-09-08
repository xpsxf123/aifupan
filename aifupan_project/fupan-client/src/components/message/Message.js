import { MessageBox } from 'element-ui';

/**
 * 增强型消息提示组件
 * 基于Element UI的MessageBox组件二次封装
 */
const MessageService = {
  /**
   * 创建消息弹窗
   * @param {String|Object} options - 消息内容或配置对象
   * @param {String} type - 消息类型
   * @returns {Promise} alert实例Promise
   */
  _create(options, type = 'info') {
    if (typeof options === 'string') {
      return MessageBox.alert(options, '提示', {
        type,
        confirmButtonText: '确定'
      });
    }
    const { message, title = '提示', ...rest } = options;
    return MessageBox.alert(message, title, {
      type,
      confirmButtonText: '确定',
      ...rest
    });
  },

  /**
   * 成功消息
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Promise} alert实例Promise
   */
  success(options) {
    return this._create(options, 'success');
  },

  /**
   * 警告消息
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Promise} alert实例Promise
   */
  warning(options) {
    return this._create(options, 'warning');
  },

  /**
   * 错误消息
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Promise} alert实例Promise
   */
  error(options) {
    return this._create(options, 'error');
  },

  /**
   * 信息消息
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Promise} alert实例Promise
   */
  info(options) {
    return this._create(options, 'info');
  },

  /**
   * 自定义消息
   * @param {Object} options - 完整的配置对象
   * @returns {Promise} alert实例Promise
   */
  custom(options) {
    const { message, title = '提示', ...rest } = options;
    return MessageBox.alert(message, title, rest);
  },
  /**
   * 自定义确认消息
   * @param {Object} options - 完整的配置对象
   * @returns {Promise} confirm实例Promise
   */
  customConfirm(options) {
    const { message, title, ...rest } = options;
    return MessageBox.confirm(message, {
      title: title || '',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      dangerouslyUseHTMLString: true, // 允许使用 HTML
      message: `
        <div style="text-align:center">
            <div style="margin-top:12px">
                <svg v-else class="icon menuImg" aria-hidden="true" style='width:80px;height:80px'>
                    <use xlink:href="#icon-tixing"></use>
                </svg>
            </div>
            <div style="padding:24px 0"> ${message}</div>
        </div>
      `,
      ...rest,
      customClass: `custom-confirm ${rest.customClass || ''}`,
    });
  }
};

export default MessageService; 