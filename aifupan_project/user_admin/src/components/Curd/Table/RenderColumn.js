import { h } from 'vue'

export default {
  props: {
    row: Object,
    index: Number,
    render: Function
  },
  setup(props) {
    return () => {
      return props.render ? props.render(props.row, props.index) : null
    }
  }
}
