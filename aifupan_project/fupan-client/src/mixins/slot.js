export default {
    computed: {
      slotList() {
        return {
          ...this.$slots,
          ...this.$scopedSlots
        };
      }
    },
    methods: {
      getSlotName(item = {}, type = '', slot) {
        let result = {
          L: '-label',
          LT: '-label-tooltip',
          D: '-detail',
          I: '-item',
          A: '-after',
          B: '-before',
          ST: '-suffix-tooltip',
          BI: '-item-before',
          AI: '-item-after',
          CA: '-col-after',
          CB: '-col-before',
          RB: '-row-before',
          RA: '-row-after'
        };
        const suffix = result[type] ? result[type] : type;
        let prop = '';
        if (item instanceof Object) {
          prop = item.slot || item.prop || '';
        } else if (typeof item === 'string') {
          prop = item;
        }
        let name = prop + suffix;
        if (slot) return slot[name];
        return name;
      },
      /**
      * @param {Object|String|undefined} itemOrPrefix 插槽项数据，或者是直接的插槽名称前缀
      * @param {String} suffix 插槽后缀
      * @description 例如itemOrPrefix = { slot: 'aaa' }
      * 则表示检查插槽名称为aaa的函数或对象（后面都称为插槽对象）
      * itemOrPrefix = 'aaa' 检查插槽名称包含aaa的插槽对象
      * suffix = 'bb' 则是在插槽名称后面拼接一个用以区分特殊插槽
      * itemOrPrefix = undefined 则表示获取所有
      */
      getSlotList(itemOrPrefix, suffix = '') {
        const slotFilter = !itemOrPrefix;
  
        let prop = '';
        if (itemOrPrefix instanceof Object) {
          itemOrPrefix = itemOrPrefix || {};
          prop = itemOrPrefix.slot || itemOrPrefix.prop;
        } else if (typeof itemOrPrefix === 'string') {
          prop = itemOrPrefix;
        }
        const slot = {
          ...this.$slots,
          ...this.$scopedSlots
        };
        const list = Object.keys(slot).filter(ele => {
          if (slotFilter && !prop) return true;
          return ele.indexOf(prop + suffix) >= 0;
        });
        return list;
      },
      // 如果等于自身则输出默认插槽
      ifSlotSelf(item, slotName) {
        return item.slot || item.prop === slotName ? 'default' : slotName;
      },
      delSlotName(slotName, item = {}, type = '') {
        let prop = '';
        if (item instanceof Object) {
          prop = (item.slot || item.prop || '') + type;
        } else if (typeof item === 'string') {
          prop = (item || '') + type;
        }
        let slotTarget = slotName.split(prop);
        if (slotTarget.length > 1) {
          slotTarget = slotTarget[1];
        } else {
          slotTarget = slotTarget[0];
        }
        if (slotTarget.indexOf('-') === 0) {
          return slotTarget.slice(1);
        } else {
          return slotTarget;
        }
      }
    }
  };