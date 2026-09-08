
from copy import deepcopy

import hashlib
import random
import uuid
import time
import json
import jmespath
import requests
from datetime import datetime, timedelta


class XGorgon:
    """
    XGorgon算法实现类
    用于生成特定平台请求所需的X-Gorgon和X-Khronos签名头信息
    """

    def __init__(self):
        """初始化算法参数"""
        self.length = 20  # 固定长度参数
        # 算法核心参数数组
        self.hex_str = [30, 64, 224, 217, 147, 69, 0, 180]

    def __encryption(self):
        """
        加密算法核心步骤
        生成一个经过特定规则处理的256位数组
        """
        hex_zu = list(range(256))  # 初始化0-255的数组
        tmp = ''  # 临时变量

        for i in range(256):
            # 确定A的值
            if i == 0:
                A = 0
            elif tmp:
                A = tmp
            else:
                A = hex_zu[i - 1]

            # 计算B值，从hex_str中循环取值
            B = self.hex_str[i % 8]

            # 特殊处理A=85的情况
            if A == 85 and i != 1 and tmp != 85:
                A = 0

            # 计算C值，确保在0-255范围内
            C = A + i + B
            while C >= 256:
                C -= 256

            # 更新临时变量
            if C < i:
                tmp = C
            else:
                tmp = ''

            # 交换值
            hex_zu[i] = hex_zu[C]

        return hex_zu

    def __initialize(self, input_data, hex_zu):
        """
        初始化处理方法
        对输入数据进行初始化转换

        参数:
            input_data: 输入的字节数组
            hex_zu: __encryption生成的数组

        返回:
            处理后的字节数组
        """
        tmp_add = []
        tmp_hex = deepcopy(hex_zu)  # 深拷贝避免修改原数组

        for i in range(self.length):
            A = input_data[i]

            # 确定B值
            B = tmp_add[-1] if tmp_add else 0

            # 计算C值，确保在0-255范围内
            C = hex_zu[i + 1] + B
            while C >= 256:
                C -= 256

            tmp_add.append(C)
            D = tmp_hex[C]
            tmp_hex[i + 1] = D

            # 计算E值，确保在0-255范围内
            E = D + D
            while E >= 256:
                E -= 256

            F = tmp_hex[E]
            G = A ^ F  # 异或操作
            input_data[i] = G

        return input_data

    def __reverse(self, num):
        """
        反转十六进制数字的字节顺序

        参数:
            num: 待处理的数字

        返回:
            反转后的数字
        """
        # 转换为两位十六进制字符串
        tmp_string = hex(num)[2:]
        if len(tmp_string) < 2:
            tmp_string = '0' + tmp_string

        # 反转并转换回整数
        return int(tmp_string[1:] + tmp_string[:1], 16)

    def __RBIT(self, num):
        """
        对8位二进制数进行位反转

        参数:
            num: 待处理的数字(0-255)

        返回:
            位反转后的数字
        """
        # 转换为8位二进制字符串
        tmp_string = bin(num)[2:]
        while len(tmp_string) < 8:
            tmp_string = '0' + tmp_string

        # 反转二进制位并转换回整数
        reversed_bits = ''.join([tmp_string[7 - i] for i in range(8)])
        return int(reversed_bits, 2)

    def __handle(self, input_data):
        """
        处理输入数据的主方法
        应用一系列转换操作

        参数:
            input_data: 输入的字节数组

        返回:
            处理后的字节数组
        """
        for i in range(self.length):
            A = input_data[i]
            B = self.__reverse(A)  # 反转字节
            C = input_data[(i + 1) % self.length]  # 下一个元素(循环)
            D = B ^ C  # 异或操作
            E = self.__RBIT(D)  # 位反转
            F = E ^ self.length  # 与长度异或
            G = ~F  # 取反

            # 处理负数，转为无符号整数
            while G < 0:
                G += 4294967296  # 2^32

            # 取低8位
            H = int(hex(G)[-2:], 16)
            input_data[i] = H

        return input_data

    def __hex2string(self, num):
        """
        将数字转换为两位十六进制字符串

        参数:
            num: 待转换的数字

        返回:
            两位十六进制字符串
        """
        tmp_string = hex(num)[2:]
        if len(tmp_string) < 2:
            tmp_string = '0' + tmp_string
        return tmp_string

    def __main(self, gorgon):
        """
        主处理流程

        参数:
            gorgon: 待处理的字节数组

        返回:
            生成的X-Gorgon字符串
        """
        # 执行完整处理流程
        processed = self.__handle(self.__initialize(gorgon, self.__encryption()))

        # 转换为十六进制字符串
        result = ''.join([self.__hex2string(item) for item in processed])

        # 按照特定格式组合最终结果
        return '0401{hash1}{hash2}{hash3}{hash4}{hash5}'.format(
            hash1=self.__hex2string(self.hex_str[7]),
            hash2=self.__hex2string(self.hex_str[3]),
            hash3=self.__hex2string(self.hex_str[1]),
            hash4=self.__hex2string(self.hex_str[6]),
            hash5=result)

    def calculate(self, params: str, headers: dict = None):
        """
        计算X-Gorgon和X-Khronos值的入口方法

        参数:
            params: 请求参数字符串
            headers: 请求头字典

        返回:
            包含X-Gorgon和X-Khronos的字典
        """
        if headers is None:
            headers = {}

        gorgon = []
        headers_lower = {}

        # 生成Khronos时间戳(当前时间戳的十六进制表示)
        khronos_hex = hex(int(time.time()))[2:]

        # 处理URL参数的MD5值
        url_md5 = hashlib.md5(params.encode("UTF-8")).hexdigest()
        for i in range(4):
            gorgon.append(int(url_md5[2 * i: 2 * i + 2], 16))

        # 转换headers为小写键，方便查找
        for k, v in headers.items():
            headers_lower[k.lower()] = v

        # 处理x-ss-stub头信息
        if "x-ss-stub" in headers_lower:
            data_md5 = headers_lower['x-ss-stub']
            for i in range(4):
                gorgon.append(int(data_md5[2 * i: 2 * i + 2], 16))
        else:
            gorgon.extend([0] * 4)  # 补4个0

        # 处理cookie头信息
        if "cookie" in headers_lower:
            cookie_md5 = hashlib.md5(headers_lower['cookie'].encode("UTF-8")).hexdigest()
            for i in range(4):
                gorgon.append(int(cookie_md5[2 * i: 2 * i + 2], 16))
        else:
            gorgon.extend([0] * 4)  # 补4个0

        # 补充4个0
        gorgon.extend([0] * 4)

        # 处理Khronos时间戳
        for i in range(4):
            gorgon.append(int(khronos_hex[2 * i: 2 * i + 2], 16) if 2 * i + 2 <= len(khronos_hex) else 0)

        # 生成并返回结果
        return {
            'X-Gorgon': self.__main(gorgon),
            'X-Khronos': str(int(khronos_hex, 16))  # 转换为十进制时间戳
        }


# 时间戳转日期
def convert_timestamp(timestamp):
    if timestamp:
        dt_object = datetime.fromtimestamp(timestamp)
        formatted_time = dt_object.strftime('%Y-%m-%d %H:%M:%S')
        return formatted_time
    else:
        return None


def generate_id():
    return str(random.randint(10 ** 15, 10 ** 16 - 1))


def generate_cdid():
    return str(uuid.uuid4())


def generate_openudid():
    return "".join([hex(i)[2:] for i in [random.randint(1, 255) for i in range(8)]])


def get_aweme_list(sec_uid):
    current_page = 1
    max_cursor = 0
    items = []
    max_count = 100  # 爬取的最大作品数
    should_exit = False  # 用于控制是否退出整个爬取循环

    while True:
        print(f"正在爬取第{current_page}页。。。。。")
        # time.sleep(1)

        cdid = generate_cdid()
        openudid = generate_openudid()  # 手机 设备id

        _rticket = int(time.time() * 1000)
        ts = int(time.time())

        device_id = generate_id()
        iid = generate_id()

        headers = {
            "User-Agent": "ttnet okhttp/3.10.0.2",
            "Connection": "Keep-Alive",
            "Accept-Encoding": "gzip",
            "X-SS-REQ-TICKET": str(_rticket),
            "sdk-version": "1",
            "X-Khronos": "1753704354",
            "X-Gorgon": "0404b4d94000fdbb9182cf0918c5fbeac413362c8d0acb0011ba"
        }
        base_url = "https://aweme.snssdk.com/aweme/v1/aweme/post/"
        params = {
            "source": "0",
            "max_cursor": str(max_cursor),
            "sec_user_id": sec_uid,
            "count": "25",  # 第一页是20，后面都是10
            "manifest_version_code": "100001",
            "_rticket": str(_rticket),
            "ac": "wifi",
            "app_type": "normal",
            "device_id": device_id,
            "iid": iid,
            "os_version": "12",
            "channel": "huawei",
            "version_code": "100000",
            "device_type": "SM-G9910",
            "language": "zh",
            "resolution": "900*1600",
            "openudid": openudid,
            "update_version_code": "10009900",
            "app_name": "douyin_lite",
            "cdid": cdid,
            "version_name": "10.0.0",
            "os_api": "32",
            "device_brand": "samsung",
            "ssmix": "a",
            "device_platform": "android",
            "dpi": "240",
            "aid": "2329",
            "ts": str(ts)
        }

        params_str = '&'.join([f'{k}={v}' for k, v in params.items()])
        url = f"{base_url}?{params_str}"

        # 计算签名
        xgorgon = XGorgon()
        result = xgorgon.calculate(params_str)
        print("生成的签名信息:")
        print(f"X-Gorgon: {result['X-Gorgon']}")
        print(f"X-Khronos: {result['X-Khronos']}")

        headers['X-Khronos'] = result['X-Khronos']
        headers['X-Gorgon'] = result['X-Gorgon']

        response = requests.get(url, headers=headers)
        json_data = response.json()

        if json_data.get('status_code') == 0 and json_data.get('uid') != None:

            # 更新max_cursor为当前页面返回的值
            max_cursor = json_data.get('max_cursor')
            print(f'翻页游标: {max_cursor}')
            has_more = json_data.get('has_more')
            if has_more:
                print('还有下一页')
            else:
                print('没有下一页')
                print(f'全部作品也没有{max_count}条')
                break

            aweme_list = json_data.get('aweme_list')

            if aweme_list:
                for i in aweme_list:
                    is_top = i.get("is_top")  # 1 就是置顶
                    create_time = i.get("create_time")
                    aweme_id = i.get("aweme_id")
                    ip_attribution = i.get("ip_attribution")
                    statistics = i.get("statistics")
                    item = {
                        "is_top": is_top,
                        "aweme_id": aweme_id,
                        'aweme_type': None,
                        "aweme_url": None,
                        "desc": i.get("desc"),
                        "ip_attribution": ip_attribution,
                        "images": None,
                        "create_time": convert_timestamp(create_time),
                        'music_play_url': jmespath.search('music.play_url.uri', i),
                        "video_play_addr": None,
                        'duration': int(i.get('duration') / 1000) if i.get('duration') else None,  # 秒
                        'cover_url': jmespath.search('video.cover.url_list[-1]', i)  # 封面

                    }
                    item.update(statistics)

                    # 判断是不是图文
                    images = i.get('images')
                    if images:
                        item['images'] = [img['url_list'][0] for img in images]
                        item['aweme_type'] = 'note'
                        item['aweme_url'] = f'https://www.douyin.com/note/{aweme_id}'
                    else:
                        item['aweme_type'] = 'video'
                        item['aweme_url'] = f'https://www.douyin.com/video/{aweme_id}'

                    # 获取视频下载地址
                    url_list = jmespath.search('video.play_addr.url_list', i)
                    if url_list:
                        for addr in url_list:
                            if 'api-play' in addr:
                                item['video_play_addr'] = addr

                    print(item)
                    items.append(item)

                    if len(items) >= max_count:
                        print(f'满足{max_count}条')
                        should_exit = True
                        break

        # 检查是否需要退出整个爬取循环
        if should_exit:
            break
        current_page += 1

    print('======== 爬取完成 ==========')
    print(f"共{current_page - 1}页，一共{len(items)}个作品")


if __name__ == "__main__":
    # sec_uid = "MS4wLjABAAAAm_ltVqZsWCgxkJ5OIvv6E4qaSgqBOE9TLUnlNPbzvzYlNzayX7VmJOI3GOUiKaPl"
    sec_uid = "MS4wLjABAAAAyiUBvOEjnzVYHl3xyIopxDpk1e7ECR-TW10I14EdS80"
    # sec_uid = "MS4wLjABAAAAp_yEgcroC6JKvr6y4d3-2UhfqBcKdjkUUIHPTFsA--wIP55pRGswugH3uqaM9zel"

    get_aweme_list(sec_uid)


