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


    def main(self):

        self.live_screen_blend_trend_v2(self.room_id)

        # self.live_screen_product_overall_trend(self.room_id, self.product_id)


if __name__ == '__main__':
    s = Spider()
    s.main()
