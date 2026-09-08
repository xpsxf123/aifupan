# -*- coding: utf-8 -*-
# @Time    : 2025/12/18 10:41
# @Author  : shark
# @File    : 直播大屏.py


import re
import json
import time
import random
import math
import requests
import jmespath

from loguru import logger
from utils.buyin_tool import BuyinUtils


class Spider(object):
    def __init__(self):
        self.room_id = "7658483066440026926"

        self.product_id = "3767121942352626067"

        self.cookies = {
            'LUOPAN_DT': 'session_7659358143452889396',

        }
        self.headers = {
            'accept': 'application/json, text/plain, */*',
            'accept-language': 'zh-CN,zh;q=0.9',
            'cache-control': 'no-cache',
            'pragma': 'no-cache',
            'priority': 'u=1, i',
            'referer': f'https://compass.jinritemai.com/screen/live/talent?live_room_id={self.room_id}',
            'sec-ch-ua': '"Microsoft Edge";v="143", "Chromium";v="143", "Not A(Brand";v="24"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"',
            'sec-fetch-dest': 'empty',
            'sec-fetch-mode': 'cors',
            'sec-fetch-site': 'same-origin',
            'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0',
        }

    # 直播大屏 - 专业版 - 直播信息
    def live_screen_live_base_info(self, room_id):
        index_item = {
            "app_platform": "开播端",
            "avatar_uri": "头像",
            "live_end_time": "直播结束时间",
            "live_end_ts": "直播结束时间戳",
            "live_start_time": "直播开始时间",
            "live_start_ts": "直播开始时间戳",
            "live_status": "直播状态(2直播中/4已结束)",
            "live_duration": "直播时长",
            "nickname": "主播昵称",
            "server_cur_time": "当前时间",
            "server_cur_ts": "当前时间戳"
        }

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/live_base_info"
        params = {
            "room_id": room_id,
        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response.text)
        print(response)

        json_data = response.json()

        live_status = json_data['data']['live_status']
        live_start_ts = json_data['data']['live_start_ts']
        live_end_ts = json_data['data']['live_end_ts']
        server_cur_ts = json_data['data']['server_cur_ts']

        item = {
            "app_platform": jmespath.search("data.app_platform", json_data),
            "avatar_uri": jmespath.search("data.avatar_uri", json_data),
            "live_end_time": jmespath.search("data.live_end_time", json_data),
            "live_end_ts": jmespath.search("data.live_end_ts", json_data),
            "live_start_time": jmespath.search("data.live_start_time", json_data),
            "live_start_ts": jmespath.search("data.live_start_ts", json_data),
            "live_status": jmespath.search("data.live_status", json_data),
            "nickname": jmespath.search("data.nickname", json_data),
            "server_cur_time": jmespath.search("data.server_cur_time", json_data),
            "server_cur_ts": jmespath.search("data.server_cur_ts", json_data)
        }

        if live_end_ts == 0 and live_status == 2:
            print('直播中')
            live_duration = server_cur_ts - live_start_ts
            item['live_duration'] = self.format_seconds(live_duration)

        else:
            print('直播已结束')
            live_duration = live_end_ts - live_start_ts
            item['live_duration'] = BuyinUtils.format_seconds(live_duration)

        print(item)
        item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
        print(json.dumps(item_zh, indent=2, ensure_ascii=False))

    # 直播大屏 - 专业版 - 数据 - 核心指标
    def live_screen_core_data(self, room_id):
        index_groups = [
            {"index_name": "online_user_cnt", "index_display": "平均在线人数",
             "hover_tip": "直播间内平均每分钟在线人数", "group_display": "流量指标"},
            {"index_name": "live_show_watch_cnt_ratio", "index_display": "曝光-观看率(次数)",
             "hover_tip": "直播间进入人次/直播间曝光人次", "group_display": "流量指标"},
            {"index_name": "watch_ucnt", "index_display": "累计观看人数",
             "hover_tip": "直播间累计观看人数，数据可能存在一定的延迟", "group_display": "流量指标"},
            {"index_name": "live_show_cnt", "index_display": "曝光次数", "hover_tip": "", "group_display": "流量指标"},
            {"index_name": "avg_watch_duration", "index_display": "人均观看时长",
             "hover_tip": "直播间内平均每位用户的观看时长", "group_display": "互动指标"},
            {"index_name": "avg_min_comment_cnt", "index_display": "平均评论次数(分钟)",
             "hover_tip": "评论总次数/开播总时长(分钟)", "group_display": "互动指标"},
            {"index_name": "watch_interact_ucnt_ratio", "index_display": "观看-互动率(人数)",
             "hover_tip": "直播间互动人数/直播间观看人数(互动包括：点赞、评论、分享)", "group_display": "互动指标"},
            {"index_name": "follow_anchor_ucnt", "index_display": "新增粉丝数",
             "hover_tip": "在直播间内点击关注达人的人数，不去除后续取关用户\n注意：若一个人点击关注后取消关注，依然计算为一个关注人数",
             "group_display": "互动指标"},
            {"index_name": "watch_follow_ucnt_ratio", "index_display": "观看-关注率(人数)",
             "hover_tip": "直播间新增粉丝数/直播间观看人数", "group_display": "互动指标"},
            {"index_name": "fans_club_join_ucnt", "index_display": "新加直播团人数",
             "hover_tip": "直播间内，点击加入直播粉丝团的人数，不去除退出用户。不计算购物粉丝团。",
             "group_display": "互动指标"},
            {"index_name": "watch_fans_club_join_ucnt_ratio", "index_display": "观看-加直播团率(人数)",
             "hover_tip": "新加直播团人数/直播间观看人数", "group_display": "互动指标"},
            {"index_name": "incr_ecf_club_ucnt", "index_display": "新加购物团人数",
             "hover_tip": "直播期间，点击加入购物粉丝团的人数，不去除退出用户。不计算直播粉丝团。",
             "group_display": "互动指标"},
            {"index_name": "incr_ecf_club_ucnt_ratio", "index_display": "观看-加购物团率(人数)",
             "hover_tip": "直播期间，新加购物团人数/直播间观看人数", "group_display": "互动指标"},
            {"index_name": "pay_ucnt", "index_display": "成交人数", "hover_tip": "", "group_display": "交易指标"},
            {"index_name": "gpm", "index_display": "千次观看成交金额",
             "hover_tip": "直播间平均每千次观看次数所带来的成交金额", "group_display": "交易指标"},
            {"index_name": "watch_pay_ucnt_ratio", "index_display": "观看-成交率(人数)",
             "hover_tip": "直播间成交人数/直播间观看人数", "group_display": "交易指标"},
            {"index_name": "product_click_pay_ucnt_ratio", "index_display": "商品点击-成交率(人数)",
             "hover_tip": "直播间成交人数/直播间商品点击人数", "group_display": "交易指标"},
            {"index_name": "real_refund_amt", "index_display": "退款金额", "hover_tip": "",
             "group_display": "交易指标"},
            {"index_name": "real_refund_amt_ratio", "index_display": "退款金额占比",
             "hover_tip": "直播间退款金额/直播间成交金额", "group_display": "交易指标"},
            {"index_name": "pay_deposit_pre_order_cnt", "index_display": "预售订单数",
             "hover_tip": "支付定金的预售订单数（包含定制类预售订单）", "group_display": "交易指标"},
            {"index_name": "presale_depay_deamt", "index_display": "预售定金金额",
             "hover_tip": "统计所有的定金金额，限定已成功支付定金、不管尾款是否确定（包含定制类预售订单）。",
             "group_display": "交易指标"},
            {"index_name": "pay_deposit_pre_order_amt", "index_display": "预售全款金额",
             "hover_tip": "假设全部预售订单均支付尾款的情况下，商品于预售期间产生的总成交金额(包含定金金额和尾款金额)，不去除退款订单，仅对参加定金预售的商品有效（包含定制类预售订单）；若尾款金额未确定，不计入统计范围。",
             "group_display": "交易指标"},
            {"index_name": "pay_combo_cnt", "index_display": "成交件数",
             "hover_tip": "直播间成交订单的总商品件数(含本场直播间仅加购物车结束后成交的订单)，不去除退款的订单",
             "group_display": "交易指标"},
            {"index_name": "old_fans_pay_ucnt_ratio", "index_display": "成交老粉占比",
             "hover_tip": "在下单前一天或更早之前关注账号，且下单当天未取关的用户占比", "group_display": "交易指标"},
            {"index_name": "livetoind_pay_amt", "index_display": "指示器成交金额",
             "hover_tip": "观众点击直播间右上方卡片（指示器）进入落地页购买商品的成交金额，如果直播间未挂出指示器或挂出后无成交，该指标值为0。注：由付费流量带来的观众在指示器里成交的金额会计入到“直播间成交金额”中",
             "group_display": "交易指标"},
            {"index_name": "stat_cost", "index_display": "千川消耗",
             "hover_tip": "仅支持投放全域推广的直播间，查看千川消耗", "group_display": "交易指标"}
        ]
        key_index_item = {i["index_name"]: i["index_display"] for i in index_groups}

        index_item = key_index_item | {
            "pay_amt": "直播间成交金额",
            "pay_amt_all": "直播间成交金额（含异常交易）",
        }

        index_selected = ','.join(list(key_index_item.keys()))
        params = {
            'room_id': room_id,
            'index_selected': index_selected,

        }
        try:

            url = 'https://compass.jinritemai.com/compass_api/author/live/live_screen/core_data'

            response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)

            logger.debug(response)
            logger.debug(response.text)

            json_data = response.json()

            if json_data.get('st') == 0 and json_data.get('msg') == "" and json_data.get('data') is not None:

                pay_amt_items = [
                    {
                        "index_name": "pay_amt",
                        "index_display": '直播间成交金额',
                        "value": jmespath.search('data.pay_amt', json_data)
                    }
                ]
                pay_amt_all = jmespath.search('data.pay_amt_all', json_data)
                if pay_amt_all:
                    pay_amt_items.append({
                        "index_name": "pay_amt_all",
                        "index_display": '直播间成交金额（含异常交易）',
                        "value": jmespath.search('data.pay_amt_all', json_data)
                    })

                core_data = jmespath.search('data.core_data', json_data)

                core_data_items = pay_amt_items + core_data

                item = {}
                item_zh = {}
                for i in core_data_items:
                    item[i['index_name']] = BuyinUtils.format_unit_value(i['value'])
                    item_zh[i['index_display']] = BuyinUtils.format_unit_value(i['value'])

                print(item)
                print(item_zh)

            #  {"st":10012,"msg":"当前视角与页面不匹配","data":null}
            elif json_data.get('st') == 10012 and json_data.get('msg') == "当前视角与页面不匹配":
                logger.success(f'没有使用罗盘的cookie, LUOPAN_DT {json_data}')

            #  {"code":11001,"data":null,"msg":"当前网络不稳定，请稍后再试","st":11001}
            elif json_data.get('st') == 11001 and json_data.get('msg') == "当前网络不稳定，请稍后再试":
                logger.warning(f"重试一下 {json_data}")

            #  {"code":"10001010A","data":null,"msg":"当前环境存在风险，请稍后重试"}
            elif json_data.get('code') == "10001010A" and json_data.get('msg') == "当前环境存在风险，请稍后重试":
                logger.warning(f"重试一下 {json_data}")

            # {"st":100704,"msg":"服务器错误","data":null}
            elif json_data.get('st') == 100704 and json_data.get('msg') == "服务器错误":
                logger.critical(f"cookie失效! {json_data}")

            # {'data': {}, 'msg': '达人没有直播间权限', 'st': 625}
            elif json_data.get('st') == 625 and json_data.get('msg') == "达人没有直播间权限":
                logger.warning(f"用了其他主播的 room_id {json_data}")

            # {'data': {}, 'msg': '调用下游失败，请稍后重试', 'st': 621000704}
            elif json_data.get('st') == 621000704 and json_data.get('msg') == "调用下游失败，请稍后重试":
                logger.warning(f"写错了room_id {json_data}")

            else:
                logger.warning(f"未知错误 {json_data}")

        except Exception as e:
            logger.error(f"请求错误: {str(e)}")

    # 直播大屏 - 专业版 - 数据 - 在线曲线
    def live_screen_blend_trend_v2(self, room_id):
        index_groups = [
            {"index_name": "viewing_rate", "index_display": "曝光-观看率",
             "hover_tip": "直播间进入人次/曝光人次，是影响流量的重要指标，建议关注。", "group_display": "流量指标"},
            {"index_name": "online_user_cnt", "index_display": "在线人数", "hover_tip": "",
             "group_display": "流量指标"},
            {"index_name": "watch_ucnt", "index_display": "进入人数", "hover_tip": "", "group_display": "流量指标"},
            {"index_name": "leave_ucnt", "index_display": "离开人数", "hover_tip": "", "group_display": "流量指标"},
            {"index_name": "stat_cost", "index_display": "千川消耗", "hover_tip": "", "group_display": "流量指标"},
            {"index_name": "per_capita_viewing_time", "index_display": "人均观看时长",
             "hover_tip": "过去15分钟平均每位用户的观看时长，是影响流量的重要指标，建议关注。",
             "group_display": "互动指标"},
            {"index_name": "interaction_rate", "index_display": "互动率",
             "hover_tip": "直播间评论&分享&点赞去重人数/在线人数，是影响流量的重要指标，建议关注。",
             "group_display": "互动指标"},
            {"index_name": "attention_rate", "index_display": "关注率",
             "hover_tip": "直播间新增关注人数/在线人数，是影响流量的重要指标，建议关注。", "group_display": "互动指标"},
            {"index_name": "negative_feedback_rate", "index_display": "负反馈率",
             "hover_tip": "直播间长按不感兴趣人次/曝光人次，是影响流量的重要指标，建议关注。",
             "group_display": "互动指标"},
            {"index_name": "negative_feedback_cnt", "index_display": "负反馈次数",
             "hover_tip": "直播间长按不感兴趣的次数，是影响流量的重要指标，建议关注。", "group_display": "互动指标"},
            {"index_name": "comment_cnt", "index_display": "新增评论数", "hover_tip": "", "group_display": "互动指标"},
            {"index_name": "incr_fans_cnt", "index_display": "新增粉丝数", "hover_tip": "",
             "group_display": "互动指标"},
            {"index_name": "fans_club_ucnt", "index_display": "新加团人数", "hover_tip": "",
             "group_display": "互动指标"},
            {"index_name": "gpm", "index_display": "千次观看成交金额",
             "hover_tip": "直播间平均每千次观看次数产生的成交金额，是影响流量的重要指标，建议关注。",
             "group_display": "交易指标"},
            {"index_name": "viewing_click_rate", "index_display": "商品点击率",
             "hover_tip": "直播间商品点击人数/在线人数，是影响流量的重要指标，建议关注。", "group_display": "交易指标"},
            {"index_name": "click_transaction_rate", "index_display": "商品点击-成交率",
             "hover_tip": "直播间成交人数/商品点击人数，是影响流量的重要指标，建议关注。", "group_display": "交易指标"},
            {"index_name": "uv_value", "index_display": "UV价值", "hover_tip": "", "group_display": "交易指标"},
            {"index_name": "pay_ucnt", "index_display": "成交人数", "hover_tip": "", "group_display": "交易指标"},
            {"index_name": "pay_cnt", "index_display": "成交订单数", "hover_tip": "", "group_display": "交易指标"},
            {"index_name": "pay_amt", "index_display": "成交金额", "hover_tip": "", "group_display": "交易指标"},
        ]
        index_item = {i["index_name"]: i["index_display"] for i in index_groups}

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/blend_trend_v2"

        # 事件
        o = {
            "anchor": "主播",
            "warn": "预警",
            "record": "场记",
            "bless": "福袋",
            "product": "讲解",
            "violation": "违规",
            "ad": "广告"
        }
        params = {
            "date_type": "100", #  默认100 点击旧版本可以选  10/近1小时  12/近2小时  15/近4小时  16/近8小时
            "room_id": room_id,
            "index_selected": "pay_amt,pay_cnt,pay_ucnt", # 只能取两个指标
            "vertical_order": "warn,record,violation,bless,product",

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        print(response.text)

        json_data = response.json()

        # 事件
        key_points = json_data['data']['key_points']

        # 趋势
        item = {
            "key_points": key_points
        }
        trends = json_data['data']['trends']
        for trend in trends:
            point_name = trend['point_name']
            display_name = trend['display_name']
            if point_name not in item:
                item[point_name] = []
            item[point_name].append(trend)

        print(item)

    # 直播大屏 - 专业版 - 数据 -  讲解
    def live_screen_product(self, room_id):
        index_item = {
            "pay_gmv": "讲解成交金额",
            "product_id": "商品id",
            "product_img": "商品图片",
            "product_title": "商品标题",
            "talk_end_time": "讲解结束时间",
            "talk_start_time": "讲解开始时间"
        }
        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product"
        params = {
            "date_type": "12", #  点击旧版本可以选  10/近1小时  12/近2小时  15/近4小时  16/近8小时
            "room_id": room_id,

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)
        json_data = response.json()
        if json_data.get("st") == 0:
            for i in json_data['data']['product_list']:
                item = {
                    "pay_gmv": i['pay_gmv'],
                    "product_id": i['product_id'],
                    "product_img": i['product_img'],
                    "product_title": i['product_title'],
                    "talk_end_time": BuyinUtils.convert_timestamp(i['talk_end_time']),
                    "talk_start_time": BuyinUtils.convert_timestamp(i['talk_start_time']),
                }
                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response.text)
            print(response)




    # 直播大屏 - 专业版 - 数据 - 福袋
    def live_screen_lottery_info(self, room_id):
        index_item = {
            "avg_online_ucnt": "平均在线人数",
            "condition_scope": "参与范围",
            "condition_type": "参与方式",
            "fans_club_ucnt": "新加团人数",
            "lottery_id": "福袋ID",
            "lottery_prize_cnt": "福袋奖品数量",
            "lottery_prize_name": "福袋奖品名称(钻石=抖币)",
            "lottery_prize_type": "福袋奖品类型",
            "lottery_real_draw_time": "福袋实际开奖时间",
            "lottery_start_time": "福袋抽奖开始时间",
            "participant_cnt": "参与人数"
        }


        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/lottery_info"
        params = {
            "date_type": "12", #  点击旧版本可以选  10/近1小时  12/近2小时  15/近4小时  16/近8小时
            "room_id": room_id,
            "is_asc": "false", # 是否升序
            "sort_field": "2", # 排序字段 2 时间  1 参与人数

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)
        json_data = response.json()
        if json_data.get("st") == 0:
            lottery_list = json_data['data']['lottery_list']
            for i in lottery_list:

                item = {}
                for k, v in i.items():
                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        if k in ["lottery_real_draw_time", "lottery_start_time"]:
                            item[k] = BuyinUtils.convert_timestamp(v['value'])
                        else:
                            item[k] = v['value']
                    else:
                        item[k] = v

                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response.text)
            print(response)

    # 直播大屏 - 专业版 -  数据 - 流量分析 - 分钟级流量结构
    def live_screen_flow_trend(self, room_id):
        channel_name_list = ['全部流量', '直播推荐', '短视频引流', '关注', '搜索', '个人主页&店铺&橱窗', '抖音商城推荐', '活动页', '其他', '头条西瓜',
                             "全部付费流量", "千川PC版", "品牌投放", "小店随心推", "其他投放", "千川品牌投放"]

        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/flow_trend"
        params = {
            "date_type": "100",
            "room_id": room_id,
            "index_selected": ','.join(channel_name_list),

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        json_data = response.json()
        res = {}
        for trend in json_data['data']['trends']:
            point_name = trend['point_name']
            if point_name not in res:
                res[point_name] = []
            res[point_name].append(trend)

        print(res)


    # 直播大屏 - 专业版 -  数据 - 流量分析 - 全场流量结构
    def live_screen_flow_distribution(self, room_id):
        def parse_item(data):
            index_item = {
                "watch_ratio": "流量占比",
                "pay_amt": "成交金额",
                "pay_ratio": "成交金额占比",
                "gpm": "渠道千次观看成交",
            }

            item = {}
            for i in data:
                channel_name = i['channel_name']  # 渠道名称
                value_item = {
                    'watch_ratio': BuyinUtils.format_unit_value(i['watch_ratio']),
                    'pay_amt': BuyinUtils.format_unit_value(i['pay_amt']),
                    'pay_ratio': BuyinUtils.format_unit_value(i['pay_ratio']),
                    'gpm': BuyinUtils.format_unit_value(i['gpm']),
                }
                value_item_zh = {index_item[en_key]: value for en_key, value in value_item.items() if en_key in index_item}
                item[channel_name] = value_item_zh

                if "sub_flow" in i:
                    for s in i['sub_flow']:
                        sub_channel_name = s['channel_name']
                        sub_value_item = {
                            'watch_ratio': BuyinUtils.format_unit_value(s['watch_ratio']),
                            'pay_amt': BuyinUtils.format_unit_value(s['pay_amt']),
                            'pay_ratio': BuyinUtils.format_unit_value(s['pay_ratio']),
                            'gpm': BuyinUtils.format_unit_value(s['gpm']),
                        }
                        sub_value_item_zh = {index_item[en_key]: value for en_key, value in sub_value_item.items() if en_key in index_item}
                        item[sub_channel_name] = sub_value_item_zh

            print(item)

        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/flow_distribution"
        params = {
            "room_id": room_id,
        }

        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        json_data = response.json()

        # 自然流量
        natural_data = json_data['data']['natural_data']
        if natural_data:
            parse_item(natural_data)

        # 付费流量
        pay_data = json_data['data']['pay_data']
        if pay_data:
            parse_item(pay_data)


    # 直播大屏 - 专业版 - 数据 - 引流短视频
    def live_screen_flow_video(self, room_id):
        index_item = {
            "drainage_in_live_cnt": "引流直播间次数",
            "img": "视频封面",
            "publish_time": "发布时间",
            "show_in_live_cnt": "直播入口曝光次数",
            "show_to_drainage_rate": "直播入口点击率",
            "title": "视频标题",
            "url": "视频链接",
            "video_id": "视频ID",
            "video_status": "视频状态"
        }

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/flow_video"

        params = {
            "room_id": room_id,
            "page_no": "1",  # 页码
            "page_size": "20",  # 默认10 改为20
            "sort_field": "show_to_drainage_rate", # 排序字段： publish_time:发布时间  show_in_live_cnt:直播入口曝光次数  drainage_in_live_cnt:引流直播间次数  show_to_drainage_rate:直播入口点击率
            'is_asc': False,  # 是否升序

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)
        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}, 每页{page_size}条数据，共{total}条数据")

            for item in json_data['data']['data_result']:
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)



    # 直播大屏 - 专业版  - 商品 - 当前购物车商品(直播中)
    def live_screen_product_list(self, room_id):
        '''
        虽然是直播中接口，直播结束之后也可以调用
        :param room_id:
        :return:
        '''

        index_groups = [
            {"index_name":"product_bind_time","index_display":"直播间上架时间","hover_tip":"","group_display":"基本信息"},
            {"index_name":"market_price","index_display":"到手价","hover_tip":"","group_display":"基本信息"},
            {"index_name":"stock_cnt","index_display":"库存","hover_tip":"","group_display":"基本信息"},
            {"index_name":"campaign_stock_cnt","index_display":"活动库存","hover_tip":"","group_display":"基本信息"},
            {"index_name":"explain_cnt","index_display":"讲解次数","hover_tip":"商品点击讲解次数","group_display":"基本信息"},
            {"index_name":"product_click_cnt_5min","index_display":"近 5 分钟商品点击次数","hover_tip":"近五分钟商品点击人次求和","group_display":"近 5 分钟数据"},
            {"index_name":"pay_amt_5min","index_display":"近 5 分钟成交金额","hover_tip":"近五分钟商品成交金额求和","group_display":"近 5 分钟数据"},
            {"index_name":"product_show_ucnt","index_display":"商品曝光人数","hover_tip":"","group_display":"流量数据"},
            {"index_name":"product_click_ucnt","index_display":"商品点击人数","hover_tip":"","group_display":"流量数据"},
            {"index_name":"product_show_click_ucnt_ratio","index_display":"曝光 - 点击转化率","hover_tip":"商品点击人数 / 商品曝光人数","group_display":"流量数据"},
            {"index_name":"product_show_pay_ucnt_ratio","index_display":"曝光 - 成交转化率","hover_tip":"商品支付人数 / 商品曝光人数","group_display":"流量数据"},
            {"index_name":"product_click_pay_ucnt_ratio","index_display":"点击 - 成交转化率","hover_tip":"商品支付人数 / 商品点击人数","group_display":"流量数据"},
            {"index_name":"gpm","index_display":"商品千次曝光成交","hover_tip":"1000 * 商品直播间成成交金额 / 商品曝光人次","group_display":"流量数据"},
            {"index_name":"pay_amt","index_display":"累计成交金额","hover_tip":"该商品在主播直播期间，用户从直播间下单的全部成交订单金额，包含直播期间生成订单金额和直播期间加入购物车的订单金额（仅包含直播间下单，不包含直播期间短视频、橱窗等其他渠道下单）","group_display":"成交数据"},
            {"index_name":"avg_max_pay_amt_min","index_display":"分钟最高成交金额","hover_tip":"该商品整场单分钟成交金额最大值","group_display":"成交数据"},
            {"index_name":"pay_combo_cnt","index_display":"累计成交件数","hover_tip":"该商品在主播直播期间，用户从直播间下单的全部成交件数（仅包含直播间下单，不包含直播期间短视频、橱窗等其他渠道下单）","group_display":"成交数据"},
            {"index_name":"pay_cnt","index_display":"累计成交订单数","hover_tip":"该商品在直播期间，用户从直播间下单的全部成交订单数","group_display":"成交数据"},
            {"index_name":"create_cnt","index_display":"创建订单数","hover_tip":"该商品在直播期间，用户从直播间创建的订单数","group_display":"成交数据"},
            {"index_name":"unpay_cnt","index_display":"未支付订单数","hover_tip":"该商品在直播期间，用户从直播间创建但未支付的订单数","group_display":"成交数据"},
            {"index_name":"create_pay_ucnt_ratio","index_display":"订单支付率","hover_tip":"该商品在直播期间，支付订单数 / 创建订单数","group_display":"成交数据"},
            {"index_name":"pay_deposit_pre_order_cnt","index_display":"预售订单数","hover_tip":"","group_display":"成交数据"},
            {"index_name":"presale_depay_deamt","index_display":"预售定金金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"pay_deposit_pre_order_amt","index_display":"预售全款金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"refund_cnt","index_display":"退款订单数","hover_tip":"","group_display":"成交数据"},
            {"index_name":"real_refund_amt","index_display":"退款金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"refund_rate","index_display":"退款率","hover_tip":"退款金额 / 成交金额","group_display":"成交数据"}
        ]
        key_index_item = {i["index_name"]: i["index_display"] for i in index_groups}
        index_item = {
            "can_explain": "是否能被讲解",
            "explaining": "是否正在讲解",
            "feature": "特征",
            "image_uri": "商品图片",
            "product_id": "商品id",
            "promotion_id": "促销id",
            "shop_id": "店铺id",
            "title": "商品标题",
            "room_cart_num": "直播间小黄车商品数量"
        } | key_index_item

        index_selected = ','.join(key_index_item.keys())

        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list"
        params = {
            "category_id": "0",
            "product_filter_type": "0", # 0全部 / 2压单商品 --- 有客户拍下未付款 / 1库存告急 --- (商品库存 + 活动库存) < 10
            "explained_filter_type": "0", # 0全部讲解状态 /  1已讲解 / 2未讲解
            "index_selected": index_selected,
            "room_id": room_id,
            "page_no": "1",
            "page_size": "50",

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['data_result']:
                item = {}
                for k, v in i.items():
                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        if k == "product_bind_time":
                            item[k] = BuyinUtils.convert_timestamp(v['value'])
                        else:
                            item[k] = BuyinUtils.format_unit_value(v)
                    else:
                        item[k] = v

                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response.text)
            print(response)

    # 直播大屏  - 专业版 - 商品 - 全部商品(直播后)
    def live_screen_product_list_after_live(self, room_id):
        '''
        必须等直播结束之后才能调用，否则会报错
        相比于直播中接口，少了
        "can_explain": "是否能被讲解",
        "promotion_id": "促销id",
        "shop_id": "店铺id",
        :param room_id:
        :return:
        '''
        index_groups = [
            {"index_name":"product_bind_time","index_display":"直播间上架时间","hover_tip":"","group_display":"基本信息"},
            {"index_name":"market_price","index_display":"到手价","hover_tip":"","group_display":"基本信息"},
            {"index_name":"stock_cnt","index_display":"库存","hover_tip":"","group_display":"基本信息"},
            {"index_name":"campaign_stock_cnt","index_display":"活动库存","hover_tip":"","group_display":"基本信息"},
            {"index_name":"explain_cnt","index_display":"讲解次数","hover_tip":"商品点击讲解次数","group_display":"基本信息"},
            {"index_name":"product_click_cnt_5min","index_display":"近 5 分钟商品点击次数","hover_tip":"近五分钟商品点击人次求和","group_display":"近 5 分钟数据"},
            {"index_name":"pay_amt_5min","index_display":"近 5 分钟成交金额","hover_tip":"近五分钟商品成交金额求和","group_display":"近 5 分钟数据"},
            {"index_name":"product_show_ucnt","index_display":"商品曝光人数","hover_tip":"","group_display":"流量数据"},
            {"index_name":"product_click_ucnt","index_display":"商品点击人数","hover_tip":"","group_display":"流量数据"},
            {"index_name":"product_show_click_ucnt_ratio","index_display":"曝光 - 点击转化率","hover_tip":"商品点击人数 / 商品曝光人数","group_display":"流量数据"},
            {"index_name":"product_show_pay_ucnt_ratio","index_display":"曝光 - 成交转化率","hover_tip":"商品支付人数 / 商品曝光人数","group_display":"流量数据"},
            {"index_name":"product_click_pay_ucnt_ratio","index_display":"点击 - 成交转化率","hover_tip":"商品支付人数 / 商品点击人数","group_display":"流量数据"},
            {"index_name":"gpm","index_display":"商品千次曝光成交","hover_tip":"1000 * 商品直播间成成交金额 / 商品曝光人次","group_display":"流量数据"},
            {"index_name":"pay_amt","index_display":"累计成交金额","hover_tip":"该商品在主播直播期间，用户从直播间下单的全部成交订单金额，包含直播期间生成订单金额和直播期间加入购物车的订单金额（仅包含直播间下单，不包含直播期间短视频、橱窗等其他渠道下单）","group_display":"成交数据"},
            {"index_name":"avg_max_pay_amt_min","index_display":"分钟最高成交金额","hover_tip":"该商品整场单分钟成交金额最大值","group_display":"成交数据"},
            {"index_name":"pay_combo_cnt","index_display":"累计成交件数","hover_tip":"该商品在主播直播期间，用户从直播间下单的全部成交件数（仅包含直播间下单，不包含直播期间短视频、橱窗等其他渠道下单）","group_display":"成交数据"},
            {"index_name":"pay_cnt","index_display":"累计成交订单数","hover_tip":"该商品在直播期间，用户从直播间下单的全部成交订单数","group_display":"成交数据"},
            {"index_name":"create_cnt","index_display":"创建订单数","hover_tip":"该商品在直播期间，用户从直播间创建的订单数","group_display":"成交数据"},
            {"index_name":"unpay_cnt","index_display":"未支付订单数","hover_tip":"该商品在直播期间，用户从直播间创建但未支付的订单数","group_display":"成交数据"},
            {"index_name":"create_pay_ucnt_ratio","index_display":"订单支付率","hover_tip":"该商品在直播期间，支付订单数 / 创建订单数","group_display":"成交数据"},
            {"index_name":"pay_deposit_pre_order_cnt","index_display":"预售订单数","hover_tip":"","group_display":"成交数据"},
            {"index_name":"presale_depay_deamt","index_display":"预售定金金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"pay_deposit_pre_order_amt","index_display":"预售全款金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"refund_cnt","index_display":"退款订单数","hover_tip":"","group_display":"成交数据"},
            {"index_name":"real_refund_amt","index_display":"退款金额","hover_tip":"","group_display":"成交数据"},
            {"index_name":"refund_rate","index_display":"退款率","hover_tip":"退款金额 / 成交金额","group_display":"成交数据"}
        ]
        key_index_item = {i["index_name"]: i["index_display"] for i in index_groups}
        index_selected = ','.join(key_index_item.keys())

        index_item = {
            "explaining": "是否正在讲解",
            "feature": "特征",
            "image_uri": "商品图片",
            "product_id": "商品id",
            "title": "商品标题",
            "room_cart_num": "直播间小黄车商品数量"
        } | key_index_item

        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_list_after_live"
        params = {
            "index_selected": index_selected,
            "data_range": "0",
            # "sort_field": "product_click_ucnt", # 从 key_index_item 选择排序字段
            # "is_asc": "true", # 是否升序
            "room_id": room_id,
            "page_no": "1",
            "page_size": "50",

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)
        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['data_result']:
                item = {}
                for k, v in i.items():
                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        if k == "product_bind_time":
                            item[k] = BuyinUtils.convert_timestamp(v['value'])
                        else:
                            item[k] = BuyinUtils.format_unit_value(v)
                    else:
                        item[k] = v

                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)

    # 直播大屏 - 专业版 - 商品 - 下架商品
    def live_screen_unbind_product(self):
        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/unbind_product"
        params = {
            "room_id": "7626679143285164835",
            "_lid": "177570022",
            "msToken": "6F4vqcpU59rMk9uBH_sW2YzjYj7NRERARTKmajmGYko_JYi9wFR-vyImuxinn2mggUzSI7SBfIWyHjCRCXpXJCYqNasYotKT_dXra5TraMGz1qmYupQSjXthKEAKsYo7Uzu3FI6n2pWDwA8-Ne6MuR7ENxiWcIfiMIlgxgVJyP1oyOvl4ZLywh8=",
            "a_bogus": "QvURhFU7xoWbCplGYKkDeXalDFxANsWybqidWi1N9oOfP7FYCe3mWaeXaoo61a14xuM8N2C7ldPAOjVbuUi0ZMcpomkkus4ygzV5VU0o8qqsGMJQLH8wezYFFwMn0c4ql554iIRI0UJH6VnAhqQD/B-ytKoeQbSBF1xSkZubO9shZ0yALpnlPBbDihPPUUc6",
            "verifyFp": "verify_mniknc2a_H1GyTU4o_P2WC_49V1_8uRI_HOadY66VJa6M",
            "fp": "verify_mniknc2a_H1GyTU4o_P2WC_49V1_8uRI_HOadY66VJa6M"
        }
        response = requests.get(url, headers=self.headers, cookies=self.cookies, params=params)
        print(response.text)
        print(response)


    # 直播大屏  - 专业版 - 商品 - 直播间订单
    def live_screen_live_order(self, room_id):
        index_item = {
            "nick_name": "买家昵称（脱敏）",
            "order_amount": "订单金额",
            "order_id": "订单编号",
            "order_status": "订单状态（已支付3/待支付1/超时未支付2/退款4）",
            "order_ts": "订单时间",
            "product_id": "商品 ID",
            "product_title": "商品标题",
            "sku_product_id": "商品 SKU ID",
            "sku_product_img": "商品 SKU 图片链接",
            "sku_product_title": "商品 SKU 名称"
        }

        params = {
            "room_id": room_id,
            "order_status": "3", # 已支付3  待支付1  超时未支付2  退款4
            # "query": "6951918126885049742", # 根据订单编号查询
            "page_no": "1",
            "page_size": "20",

        }
        url = 'https://compass.jinritemai.com/compass_api/content_live/author/live_screen/live_order'
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)

        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['order_list']:
                item = {}
                for k, v in i.items():
                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        item[k] = BuyinUtils.format_unit_value(v)
                    else:
                        if k == "order_ts":
                            item[k] = BuyinUtils.convert_timestamp(v)
                        else:
                            item[k] = v

                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)


    # 直播大屏 - 专业版 - 商品 - 商品详情
    def live_screen_product_explain_detail(self, room_id, product_id=None, promotion_id=None):
        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/product_explain_detail"

        # 直播中的参数
        params = {
            'room_id': room_id,
            'without_explain_duration': 'true',
        }

        # 直播中，点击“查看商品详情”的参数
        # params = {
        #     'room_id': room_id,
        #     "product_id": product_id,
        #     "show_feature": "true",
        #     "promotion_id": "3767123054631715130",
        #     "without_explain_duration": "false",
        # }
        #
        # 直播后的参数
        # params = {
        #     'room_id': room_id,
        #     "product_id": product_id,
        #     "show_feature": "true",
        #     "without_explain_duration": "false",
        # }

        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)


        json_data = response.json()
        if json_data.get("st") == 0:
            index_item = {
                "campaign_stock_cnt": "活动库存",
                "click_data": "近5分钟商品点击数据",
                "explain_cnt": "讲解次数",
                "explain_duration": "讲解时长",
                "explaining": "是否正在讲解",
                "feature": "特征",
                "market_price": "到手价",
                "pay_amt": "累计成交金额",
                "pay_amt_data": "近5分钟成交金额数据",
                "pay_combo_cnt": "累计成交件数",
                "price_text": "价格类型文本",
                "product_id": "商品ID",
                "product_img": "商品图片链接",
                "product_name": "商品名称",
                "promotion_id": "促销id",
                "product_show_pay_ucnt_ratio": "曝光-成交转化率",
                "stock_cnt": "库存",
                "unpay_cnt": "未支付订单数量",

                # 直播中比直播后多出这两个
                "can_explain": "是否能被讲解",
                "shop_cart_num": "小黄车商品数量",
            }

            item = {}
            for k, v in json_data['data'].items():
                if isinstance(v, dict) and 'unit' in v and 'value' in v:
                    item[k] = BuyinUtils.format_unit_value(v)
                else:
                    item[k] = v

            print(item)
            item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
            print(item_zh)
        else:
            print(response)
            print(response.text)


    # 直播大屏 - 专业版 - 商品 - 在线曲线
    def live_screen_product_overall_trend(self, room_id ,product_id):

        index_item = {
            "explain_start_ts": "商品讲解开始时间",
            "explain_end_ts": "商品讲解结束时间",
            "product_id": "讲解商品id",

            "pay_order_gmv": "商品用户支付金额/商品成交金额",
            "product_click_cnt": "商品点击次数",
            "watch_cnt": "直播间进入次数（自然推荐Feed）",
            "online_user_cnt": "在线人数",

        }

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_overall_trend"
        params = {
            "room_id": room_id,
            "product_id": product_id,
        }

        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)


        json_data = response.json()
        if json_data.get("st") == 0:

            # 商品讲解时间段列表
            explain_list = json_data['data']['explain_list']
            for explain in explain_list:
                explain_end_time = explain['explain_end_time']
                explain_end_ts = BuyinUtils.convert_timestamp(explain['explain_end_ts'])
                explain_start_time = explain['explain_start_time']
                explain_start_ts = BuyinUtils.convert_timestamp(explain['explain_start_ts'])
                product_id = explain['product_id']

                item = {
                    'explain_start_ts': explain_start_ts,
                    'explain_end_ts': explain_end_ts,
                    'product_id': product_id,
                }
                print(f"讲解商品时间段：{item}")

            left_trends = json_data['data']['left_trend']['trends']
            right_trends = json_data['data']['right_trend']['trends']
            trends = left_trends + right_trends

            res = {}
            for trend in trends:
                point_name = trend['point_name']
                if point_name not in res:
                    res[point_name] = []
                res[point_name].append(trend)

            print(res)
        else:
            print(response)
            print(response.text)

    # 直播大屏 - 专业版 - 商品 - 讲解效果分析
    def live_screen_product_explain_analysis(self, room_id, product_id):
        '''
        需要加密，而且需要罗盘cookie 和 ttwid
        否则会出现
        <Response [200]>
        {"code":11001,"data":null,"msg":"当前网络不稳定，请稍后再试","st":11001}
        :param room_id:
        :param product_id:
        :return:
        '''
        index_item = {
            "explain_start_ts": "商品讲解开始时间",
            "explain_end_ts": "商品讲解结束时间",
            "explain_time": "商品讲解时间",
            "explain_duration": "讲解时长",
            "product_click_cnt": "商品点击次数",
            "product_pay_amt": "商品成交金额",
            "watch_cnt": "直播间进入次数（推荐 feed）",
            "avg_online_ucnt": "平均在线人数"
        }


        params = {
            "room_id": room_id,
            "product_id": product_id,
            "role": "talent",
            "page_no": "1",
            "page_size": "5",
        }

        cookies = {
            'LUOPAN_DT': self.cookies['LUOPAN_DT'],
            "ttwid": "1|gRqk2cPv8x6WBsUrgeSgotBqFQjZ7nuxwT6-jNjwSxM|1775552198|4c7ef808bb39a4ad2327efe6643bbec71a28b67f0dada3463ecaad7703a91a3b",
        }

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_explain_analysis"


        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=cookies, params=params, need_ab=True)


        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['data_result']:
                item = {}
                for k, v in i.items():

                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        if k == "explain_duration":
                            item[k] = BuyinUtils.format_seconds(v['value'])
                        else:
                            item[k] = BuyinUtils.format_unit_value(v)
                    else:
                        if k in ["explain_start_ts", "explain_end_ts"]:
                            item[k] = BuyinUtils.convert_timestamp(v)
                        else:
                            item[k] = v
                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)

    # 直播大屏 - 专业版 - 商品 - SKU数据
    def live_screen_product_sku_detail(self, room_id, product_id):
        index_item = {
            "num": "序号",
            "sku_id": "商品 SKU ID",
            "sku_img": "商品 SKU 图片链接",
            "sku_name": "商品 SKU 名称",
            "sku_price": "商品到手价",
            "pay_amt": "成交金额",
            "pay_cnt": "订单数量",
            "unpay_cnt": "未支付订单数量",
            "pay_deposit_pre_order_cnt": "预售订单数",
            "campaign_stock_cnt": "活动库存",
            "stock_cnt": "库存"
        }

        url = "https://compass.jinritemai.com/compass_api/author/live/live_screen/product_sku_detail"

        params = {
            "room_id": room_id,
            "product_id": product_id,
            "role": "talent",
            "page_no": "1",
            "page_size": "5",
        }

        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params, need_ab=True)

        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            # 计算需要的页数
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            for i in json_data['data']['data_result']:
                item = {}
                for k, v in i.items():
                    if isinstance(v, dict) and 'unit' in v and 'value' in v:
                        item[k] = BuyinUtils.format_unit_value(v)
                    else:
                        item[k] = v

                print(item)
                item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
                print(json.dumps(item_zh, indent=2, ensure_ascii=False))

        else:
            print(response)
            print(response.text)

    # 直播大屏 - 专业版 - 人群 - 用户画像
    def live_screen_portrait_info(self, room_id):
        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/portrait_info"
        params = {
            "room_id": room_id,
            "time_type": "1",
        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        index_item = {
            "watch_user": "全场看播用户",
            "pay_user": "全场购买用户",
        }

        json_data = response.json()
        if json_data.get("st") == 0:
            # 画像总结
            portrait_summary = json_data['data']['portrait_summary']
            print(portrait_summary)
            print(json_data)
        else:
            print(response)
            print(response.text)

    # 直播中 - 直播流地址
    def live_screen_live_stream(self, room_id):
        index_item = {
            "flv_url": "flv直播流地址",
            "hls_url": "m3u8直播流地址",
            "room_cover": "直播间封面",
            "vcodec": "视频编码",
        }

        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/live_stream"
        params = {
            "room_id": room_id,
            "live_app_id": "1128",
            # "verifyFp": "verify_mnresrcc_qRGSvntJ_Ndxv_49IO_9pm4_EJM9Qnyi7n4q",
            # "fp": "verify_mnresrcc_qRGSvntJ_Ndxv_49IO_9pm4_EJM9Qnyi7n4q",
            # "msToken": "bnoCawV18G597uflEYtWqbKsJs6K6Szekpay3BU6-EZ1eoZeemKDEpKj8uMhR8mPOGLCENhIQMIHlXE13mfiE_pxBIsjpJUgma6rJ3xs6yjrzMEkJ5jgdNqM-qXwhKIBV3EWfWDJMUlNzlTHxHtmM4CfNqDpm11KclOLL1-eqAWJCg==",
            # "a_bogus": "m6Ufk7XiYZR5K3AGYCGmHHllrIglrBWybBT/bycNeoKzaHzTjP-mbNCQJxoLs/E6-uBmZqI7ZxMlYxdc80ikZ29kFmkfuhtS4T559UvoMqNsT0iQDHjmCzsFFw0NUb4qaQVXilRIZUrHgjxAiqdY/QA9CKoeQ5SBB3xbkMYbx9sXZ06AgZnaPpbkO7Jqxj=="
        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        print(response.json())

        json_data = response.json()
        if json_data.get("st") == 0:
            item = json_data.get('data')
            item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
            print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)

    # 直播中 - 直播评论
    def author_live_bigscreen_comment(self, room_id):
        url = "https://compass.jinritemai.com/business_api/author/live_bigscreen/comment"

        index_item = {
            "nick_name": "用户昵称",
            "content": "评论内容",
            "comment_tag": "评论标签",
            "msg_id": "评论消息id",
        }

        device_id = str(random.randint(10 ** 15, 10 ** 16 - 1))
        cursor = "0"
        internal_ext = ""

        for _ in range(15):
            params = {
                "live_room_id": room_id,
                "cursor": cursor,
                "version_code": "100",
                "device_id": device_id,
                "internal_ext": internal_ext,
            }

            response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
            json_data = response.json()
            if json_data.get("st") == 0:
                cursor = jmespath.search('data.cursor', json_data)
                fetch_interval = jmespath.search('data.fetch_interval', json_data)
                internal_ext = jmespath.search('data.internal_ext', json_data)

                comments = jmespath.search('data.comments', json_data)
                if comments:
                    for i in comments:
                        comment_tag = i.get("comment_tag")
                        item = {
                            "nick_name": i.get("nick_name"),
                            "content": i.get("content"),
                            "comment_tag": "老客" if comment_tag else "",
                            "msg_id": i.get("msg_id"),
                        }
                        print(item)
                else:
                    print('-' * 100)

                if fetch_interval:
                    sleep_time = fetch_interval / 1000
                    time.sleep(sleep_time)
                else:
                    time.sleep(1)
            else:
                print(response)
                print(response.text)

    # 直播后 - 直播流地址
    def live_screen_review_video_link(self, room_id):
        index_item = {
            "end_ts": "直播结束时间",
            "expire_ts": "直播流过期时间（相对于请求时间）",
            "start_ts": "直播开始时间",
            "url": "m3u8直播流地址",
        }

        params = {
            'room_id': room_id,
        }

        url = 'https://compass.jinritemai.com/compass_api/content_live/author/live_screen/review_video_link'
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)
        print(response.json())

        json_data = response.json()
        if json_data.get("st") == 0:
            item = {
                'end_ts': BuyinUtils.convert_timestamp(jmespath.search('data.end_ts', json_data)),
                'expire_ts': BuyinUtils.convert_timestamp(jmespath.search('data.expire_ts', json_data)),
                'start_ts': BuyinUtils.convert_timestamp(jmespath.search('data.start_ts', json_data)),
                'url': jmespath.search('data.url', json_data)
            }
            item_zh = {index_item[en_key]: value for en_key, value in item.items() if en_key in index_item}
            print(json.dumps(item_zh, indent=2, ensure_ascii=False))
        else:
            print(response)
            print(response.text)


    # 直播后 - 直播评论
    def live_screen_review_comment(self, room_id):
        url = "https://compass.jinritemai.com/compass_api/content_live/author/live_screen/review_comment"
        params = {
            "page_no": "1",
            "page_size": "500",
            "room_id": room_id,
            "comment_start_ts": "1775905080",
            "comment_end_ts": "1775905680",
            "is_important": "false",
        }

        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies, params=params)
        print(response)

        json_data = response.json()
        if json_data.get("st") == 0:
            page_no = jmespath.search('data.page_result.page_no', json_data)
            page_size = jmespath.search('data.page_result.page_size', json_data)
            total = jmespath.search('data.page_result.total', json_data)
            total_pages = math.ceil(int(total) / int(page_size))
            print(f"当前页：{page_no}/{total_pages}，每页{page_size}条， 共{total}条数据")

            comments = jmespath.search('data.comments', json_data)
            if comments:
                for i in comments:
                    item = {
                        "nick_name": "用户",
                        "content": i.get('content'),
                        "event_ts": BuyinUtils.convert_timestamp(i.get('event_ts')),
                        "is_important": i.get('is_important'),
                    }
                    print(item)
        else:
            print(response)
            print(response.text)

    def main(self):

        self.live_screen_review_video_link(self.room_id)

        # self.live_screen_product_overall_trend(self.room_id, self.product_id)


if __name__ == '__main__':
    s = Spider()
    s.main()
