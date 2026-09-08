export const myMixin = {
    methods: {
        // 备注删除
        remarksDelete() {
            return new Promise((resolve, reject) => {
                this.$prompt("请输入备注", "删除", {
                    confirmButtonText: "确定",
                    cancelButtonText: "取消",
                    inputPattern: /\S/,
                    inputErrorMessage: "内容不能为空",
                })
                    .then(({ value }) => {
                        resolve(value)
                    })
                    .catch(() => {
                        this.$message({
                            type: "info",
                            message: "取消输入",
                        });
                        reject('取消输入')
                    });
            })
        },
    }
}

export const priMixin = {
    data() {
        return {
            // 知识类型选项
            knowledgeTypeOpt: [
                { name: "安全培训", val: '0' },
                { name: "岗前培训", val: '1' },
                { name: "违章培训", val: '2' },
                { name: "法律法规", val: '3' },
                { name: "岗位职责", val: '4' },
                { name: "其它", val: '5' },
            ],
        }
    },
}