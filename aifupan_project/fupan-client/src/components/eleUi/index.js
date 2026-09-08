import button from './button.vue';

const uiComponents = [button];
const install = function(Vue, options = {}) {
  uiComponents.forEach(component => {
    Vue.component(component.name, component);
  })
};


export default {
  install
}