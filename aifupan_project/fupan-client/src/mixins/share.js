import myUtils from "@/utils/utils";

export default {
    data() {
        return {}
    },
    methods: {
        async copyToClipboard() {
            const {data: result, code} = await this.$httpBack.v2500.getUserInviteUrl()
            if (code !== 0) return this.$message.error(result.msg)
            // const input = document.createElement('input')
            // input.setAttribute('value', `给你推荐一款最近很火的直播复盘工具，可以录同行，抓话术，拆竞品、还可以查违规，我用了非常棒，他们今天在搞免费试用的活动，你赶紧点这个链接去注册一下：${result}`)
            // document.body.appendChild(input)
            // input.select()
            // document.execCommand('copy')
            // document.body.removeChild(input)

            await myUtils.copyToClipboard(`这个直播AI智能体最近很火，比claude和codex还懂直播，我用他最近业绩增长了，我给你申请了一个会员。点这个链接去注册一下：${result}`)
            this.$message({
                message: '邀请链接已复制，快去微信上分享给好友吧!',
                type: 'success'
            })
        }
    },
}


