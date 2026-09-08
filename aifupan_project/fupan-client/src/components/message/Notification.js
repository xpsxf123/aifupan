import { Notification } from 'element-ui';

/**
 * 增强型通知提示组件
 * 基于Element UI的Notification组件二次封装
 */
const NotificationService = {
  // closeTagMap: {},
  // addCloseTag(tag,obj) { 
  //   if(!Array.isArray(this.closeTagMap[tag])){
  //     this.closeTagMap[tag] = [];
  //   }
  //   this.closeTagMap[tag].push(obj);
  // },
  // closeTag(tag){
  //   if(!this.closeTagMap[tag]){
  //     return;
  //   }
  //   this.closeTagMap[tag].forEach((item)=>{
  //     item.close();
  //   })
  // },
  onlyNotification: {},
  addOnlyNotification(tag,obj){
    if(this.onlyNotification[tag]){
      this.onlyNotification[tag].close();
    }
    this.onlyNotification[tag] = obj;
  },
  /**
   * 创建通知
   * @param {String|Object} options - 消息内容或配置对象
   * @param {String} type - 通知类型
   * @returns {Object} notification实例
   */
  _create(options, type = 'info') {
    if (typeof options === 'string') {
      return Notification({
        message: options,
        type
      });
    }
    let n = Notification({
      type,
      ...options
    })
    if (options.onlyTag) {
      this.addOnlyNotification(options.onlyTag,n);
    }
    return n;
  },

  /**
   * 基本通知
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Object} notification实例
   */
  notify(options) {
    if (typeof options === 'string') {
      return Notification({
        message: options
      });
    }
    let n = Notification(options)
    if (options.onlyTag) {
      this.addOnlyNotification(options.onlyTag,n);
    }
    return n;
  },

  /**
   * 成功通知
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Object} notification实例
   */
  success(options) {
    return this._create(options, 'success');
  },

  /**
   * 警告通知
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Object} notification实例
   */
  warning(options) {
    return this._create(options, 'warning');
  },

  /**
   * 错误通知
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Object} notification实例
   */
  error(options) {
    return this._create(options, 'error');
  },

  /**
   * 信息通知
   * @param {String|Object} options - 消息内容或配置对象
   * @returns {Object} notification实例
   */
  info(options) {
    return this._create(options, 'info');
  },

  /**
   * 关闭所有通知
   */
  closeAll() {
    Notification.closeAll();
  }
};

export default NotificationService; 