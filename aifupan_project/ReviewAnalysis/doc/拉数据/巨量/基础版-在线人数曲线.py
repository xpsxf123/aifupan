# -*- coding: utf-8 -*-
# @Time    : 2026/4/8 10:32
# @Author  : shark
# @File    : 直播大屏 - 基础版.py

import re
import json
import time
import random
import math
import requests
import jmespath
from utils.buyin_tool import BuyinUtils

'''
鲸启信息咨询
https://www.douyin.com/user/MS4wLjABAAAABYa2yd-9BXCD9agZ9SAjTJFfbSgLnd6_NC6SJ1adhEk
抖音号: 94339474475
uid: 710735470007475

众航信息咨询
https://www.douyin.com/user/MS4wLjABAAAAWPO-GDD0RHvsPzhJUQGQAR6MIByVMY9bUv-_W8bVQaWM58ck3bmrIk7BTM-c8Gps
抖音号: 98632953355
uid: 3160467642781443



// LUOPAN_DT过期或者LUOPAN_DT错误
{"st":100704,"msg":"服务器错误","data":null}

// 没有用LUOPAN_DT 或者 LUOPAN_DT是一个空字符串
{"st":10012,"msg":"当前视角与页面不匹配","data":null}

// room_id 不对，要么写错了，要么就是别人的直播间id
{"data":{},"msg":"达人没有直播间权限","st":625}

// room_id没传，获取传个空字符串
{"data":{},"msg":"参数校验失败","st":621000601}

// 请求参数需要 verifyFp, fp, msToken, a_bogus 而且还要正确
{"code":"10001010A","data":null,"msg":"当前环境存在风险，请稍后重试"}
{"code":11001,"data":null,"msg":"当前网络不稳定，请稍后再试","st":11001}

// cookie失效! 
{'st': 100704, 'msg': '服务器错误', 'data': None}

// 没有使用罗盘 LUOPAN_DT
{'st': 10012, 'msg': '当前视角与页面不匹配', 'data': None}

// 写错了room_id
{'data': {}, 'msg': '调用下游失败，请稍后重试', 'st': 621000704}

// 用了其他主播的 room_id
{'data': {}, 'msg': '达人没有直播间权限', 'st': 625}


'''


class Spider(object):
    def __init__(self):
        self.room_id = "7658483066440026926"

        self.product_id = "3767121942352626067"

        self.cookies = {
            'LUOPAN_DT': 'session_7659358143452889396',

        }
        self.headers = {
            "accept": "application/json, text/plain, */*",
            "accept-language": "zh-CN,zh;q=0.9",
            "cache-control": "no-cache",
            "pragma": "no-cache",
            "priority": "u=1, i",
            "referer": f"https://compass.jinritemai.com/screen/talent/main?live_room_id={self.room_id}&live_app_id=1128&source=baiying_home",
            "sec-ch-ua": "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"",
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": "\"Windows\"",
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "same-origin",
            "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"
        }


    # 直播大屏 - 基础版 - 人气趋势&互动趋势
    def basic_live_screen_data_trend(self, room_id):
        index_item = {
            "trend_popularity": "人气趋势",
            "trend_interaction": "互动趋势",
            "leave_ucnt": "累计离开人数",
            "online_user_cnt": "最高在线人数",
            "watch_ucnt": "累计进入人数",
            "comment_cnt": "累计评论数",
            "incr_fans_cnt": "累计粉丝数",
        }

        url = "https://compass.jinritemai.com/compass_api/author/live/basic_live_screen/data_trend"
        params = {
            "room_id": room_id,
            "index_selected": "trend_popularity",  # trend_popularity 人气趋势 / trend_interaction 互动趋势

        }
        response = BuyinUtils.request_handler(method="GET", url=url, headers=self.headers, cookies=self.cookies,
                                              params=params)
        print(response.text)
        print(response)


    def main(self):
        self.basic_live_screen_data_trend(self.room_id)


if __name__ == '__main__':
    s = Spider()
    s.main()
