<template>
    <el-dialog
        :visible.sync="dialogVisible"
        width="900px"
        top="40px"
        :close-on-click-modal="false"
        :append-to-body="true"
        custom-class="scriptRestorationExampleDialog"
    >
        <div class="exampleContainer">
            <div class="exampleTitle">白酒直播话术脚本</div>
            <div class="exampleTableWrap">
                <table class="exampleTable">
                    <thead>
                        <tr>
                            <th class="colTime">时间段</th>
                            <th class="colFrame">框架标题</th>
                            <th class="colContent">直播话术</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="(row, index) in rows" :key="`example-row-${index}`">
                            <td class="cellTime">{{ row.timeRange }}</td>
                            <td class="cellFrame">{{ row.frameTitle }}</td>
                            <td class="cellContent">
                                <div class="contentText">{{ row.script }}</div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </el-dialog>
</template>

<script>
export default {
    name: 'ScriptRestorationExampleDialog',
    props: {
        visible: {
            type: Boolean,
            default: false
        }
    },
    computed: {
        dialogVisible: {
            get() {
                return this.visible
            },
            set(val) {
                this.$emit('update:visible', val)
            }
        }
    },
    data() {
        return {
            rows: [
                {
                    timeRange: '00:00-01:00',
                    frameTitle: '开场留人 + 福利预告',
                    script:
                        '来！所有刚刷到我直播间的酒友，先别急着走！今天不搞虚的，不玩套路，直接给大家上酒厂直供的硬福利！\n'
                        + '新来的哥哥姐姐，把"酒"字打在公屏上，让我看看有多少爱喝纯粮酒的家人！今天只要扣了"酒"的，全部给你们安排专属福利价！\n'
                        + '左上角没点关注的，先点个免费的关注，点亮个粉丝灯牌，今天的福利只给自家粉丝！\n'
                        + '平时超市卖128一瓶的52度纯粮大曲，今天在我这，不要99，不要89，前50单下单的，我再白送你一瓶同款品鉴酒！\n'
                        + '就这一波，库存就200箱，卖完直接下链接，想要的家人把"想要"打在公屏，我统计完人数，马上给大家上车！'
                },
                {
                    timeRange: '01:00-02:30',
                    frameTitle: '产品介绍 + 品质背书',
                    script:
                        '很多家人问，为什么今天能卖这么便宜？因为我们是酒厂直接发货，没有经销商、没有代理商、没有超市进场费，层层加价全部砍掉，直接把利润让给大家！\n'
                        + '听好了，咱们这一款纯粮酒，执行标准GB/T10781.1，配料表干干净净，只有水、高粱、小麦，一滴酒精、一滴香精、一滴甜蜜素都没有，假一赔十，支持任何机构检测！\n'
                        + '采用传统老五甑工艺，陶坛窖藏整整5年，不是那种三两个月的新酒！大家看，倒出来酒花绵密饱满，挂杯持久，闻着是浓郁的粮香和窖香，没有一点刺鼻的酒精味！\n'
                        + '所有资质全部齐全，生产许可证、质检报告都在，大家可以放心拍，放心喝！'
                },
                {
                    timeRange: '02:30-04:00',
                    frameTitle: '场景讲解 + 饮用体验',
                    script:
                        '我知道很多家人都踩过酒精酒的坑，喝着辣嗓子、烧胃，喝完第二天头疼欲裂，一整天都不舒服，对不对？\n'
                        + '咱们这款纯粮酒，入口绵甜柔和，中段醇厚饱满，尾净余长，一点都不辣喉，不烧胃，喝多了也不上头，第二天该干嘛干嘛！\n'
                        + '平时下班回家，炒两个小菜，自己小酌两杯，解乏又舒服；周末朋友聚会，拿出来喝，大家都说好喝；家里来客人，摆上桌也有面子；逢年过节走亲访友，送这个纯粮酒，比送什么都实在！'
                },
                {
                    timeRange: '04:00-06:00',
                    frameTitle: '价格公布 + 福利政策',
                    script:
                        '好了，话不多说，今天的福利价格，我只说一遍，大家听清楚了！ 平时线下超市，一瓶就要128块钱，今天在我直播间，单瓶到手只要59块9！ 更划算的是整箱装，6瓶日常价768，今天直播间直接砍到299块钱！而且前50单下单整箱的家人，我再额外送你一瓶同款品鉴酒，相当于299块钱，到手整整7瓶！ 这个价格，只有今天直播间才有，下播立刻恢复原价！运营准备好，3、2、1，1号链接，给我上车！'
                },
                {
                    timeRange: '06:00-07:30',
                    frameTitle: '逼单促单 + 打消顾虑',
                    script:
                        '1号链接已经上车了！家人们抓紧时间去拍！现在已经拍了120多单了，库存只剩最后80箱了！\n'
                        + '我再跟大家说一遍，今天所有下单的家人，全部支持开箱品鉴！收到货你先开一瓶喝，喝着不好喝、不满意、不是纯粮酒，剩下的你给我退回来，来回运费我们出，你没有任何损失！\n'
                        + '而且我们全程京东/顺丰包邮，破损包赔，路上有任何漏酒、碎酒的情况，拍个照片发给客服，立刻给你补发或者退款！\n'
                        + '还有最后50单了！手慢无！想要的家人抓紧时间，错过今天，再想这个价格，就没有了！\n'
                        + '最后30秒！最后20单库存了！拍完这20单，我们直接下链接，下播就恢复原价！已经下单的家人，在公屏扣个"已拍"，我给这20单家人们优先发货！'
                }
            ]
        }
    }
}
</script>

<style lang="scss">
.scriptRestorationExampleDialog .el-dialog__body {
    padding: 0;
}

.scriptRestorationExampleDialog .el-dialog__header {
    padding: 0;
}

.scriptRestorationExampleDialog .el-dialog__headerbtn {
    top: 14px;
    right: 14px;
}

.scriptRestorationExampleDialog .el-dialog__close {
    font-size: 18px;
}
</style>

<style scoped lang="scss">
.exampleContainer {
    padding: 18px 22px 22px;
    background: #fff;
}

.exampleTitle {
    text-align: center;
    font-size: 18px;
    font-weight: 600;
    color: #222;
    margin: 6px 0 16px;
}

.exampleTableWrap {
    border: 1px solid #e9e9e9;
    border-radius: 2px;
    overflow: hidden;
}

.exampleTable {
    width: 100%;
    border-collapse: collapse;
    table-layout: fixed;
}

.exampleTable thead th {
    background: #fff;
    font-size: 12px;
    font-weight: 600;
    color: #333;
    border-bottom: 1px solid #e9e9e9;
    border-right: 1px solid #e9e9e9;
    padding: 10px 12px;
    text-align: left;
}

.exampleTable thead th:last-child {
    border-right: none;
}

.exampleTable tbody td {
    border-bottom: 1px solid #e9e9e9;
    border-right: 1px solid #e9e9e9;
    padding: 12px;
    vertical-align: top;
    font-size: 12px;
    color: #333;
}

.exampleTable tbody tr:last-child td {
    border-bottom: none;
}

.exampleTable tbody td:last-child {
    border-right: none;
}

.colTime {
    width: 120px;
}

.colFrame {
    width: 180px;
}

.cellTime {
    color: #555;
    white-space: nowrap;
}

.cellFrame {
    color: #333;
    font-weight: 500;
}

.contentText {
    white-space: pre-wrap;
    line-height: 18px;
}
</style>
