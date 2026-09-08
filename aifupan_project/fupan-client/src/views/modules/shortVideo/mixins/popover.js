export default {
    data(){
      return {
          popoverVisible: false,
          insidePopover: false,
          currentPopoverItem: null,
          popoverX: 0,
          popoverY: 0,
          popoverWidth: 410,
          popoverHeight: 240,
          hideTimer: null,
      }
    },
    computed: {
        popoverStyle() {
            return {
                position: "fixed",
                left: this.popoverX + "px",
                top: this.popoverY + "px",
                pointerEvents: "none",
                zIndex: 9999,
                maxWidth: this.popoverWidth + "px",
            };
        },
    },
    methods: {
        // Popover相关
        showPopover(event, item) {
            clearTimeout(this.hideTimer)
            this.insidePopover = true
            this.currentPopoverItem = item;
            this.popoverVisible = true;

            const offsetX = 10; // 鼠标右侧偏移
            const offsetY = 0;  // 垂直微调

            let x = event.clientX + offsetX;
            let y = event.clientY - this.popoverHeight / 2 + offsetY;

            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;

            // 右边界检测
            if (x + this.popoverWidth > viewportWidth) {
                x = event.clientX - this.popoverWidth - offsetX;
            }

            // 上下边界检测
            if (y < 0) y = 0;
            if (y + this.popoverHeight > viewportHeight) y = viewportHeight - this.popoverHeight;
            this.popoverX = x;
            this.popoverY = y;
        },
        hidePopover() {
            this.insidePopover = false
            clearTimeout(this.hideTimer)
            this.hideTimer = setTimeout(() => {
                if (!this.insidePopover) {
                    this.popoverVisible = false;
                    this.currentPopoverItem = null;
                }
            }, 300)
        },
        enterPopover() {
            this.insidePopover = true
            clearTimeout(this.hideTimer)
        },
        leavePopover() {
            this.insidePopover = false
            this.hideTimer = setTimeout(() => {
                if (!this.insidePopover) {
                    this.popoverVisible = false;
                    this.currentPopoverItem = null;
                }
            }, 300)
        },
    },
}


