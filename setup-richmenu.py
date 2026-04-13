#!/usr/bin/env python3
"""
一次性建立 LINE Rich Menu 腳本
執行：python3 setup-richmenu.py
"""

import json
import urllib.request
import urllib.error
import os
from PIL import Image, ImageDraw, ImageFont

CHANNEL_TOKEN = os.environ.get("LINE_CHANNEL_TOKEN", "")  # 設定環境變數或直接填入
LIFF_ID = os.environ.get("LIFF_ID", "")  # 設定環境變數或直接填入

BASE_URL = "https://api.line.me/v2/bot"
HEADERS = {
    "Authorization": f"Bearer {CHANNEL_TOKEN}",
    "Content-Type": "application/json",
}

W, H = 2500, 843  # Rich Menu 標準尺寸


def call_api(method, path, body=None, binary=None, content_type=None):
    url = f"{BASE_URL}{path}"
    data = binary if binary else (json.dumps(body).encode() if body else None)
    headers = {"Authorization": f"Bearer {CHANNEL_TOKEN}"}
    if content_type:
        headers["Content-Type"] = content_type
    elif body is not None:
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        print(f"  Error {e.code}: {e.read().decode()}")
        raise


def generate_image(path="richmenu.png"):
    img = Image.new("RGB", (W, H), "#1DB954")  # 綠色底
    draw = ImageDraw.Draw(img)

    sections = [
        {"x": 0,    "w": 833,  "color": "#1aad54", "emoji": "📅", "label": "預約場次"},
        {"x": 833,  "w": 834,  "color": "#17963f", "emoji": "📋", "label": "我的預約"},
        {"x": 1667, "w": 833,  "color": "#1aad54", "emoji": "🕐", "label": "查看時段"},
    ]

    # 嘗試載入系統字體，找不到就用預設
    font_large = None
    font_small = None
    for font_path in [
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
        "/System/Library/Fonts/PingFang.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]:
        if os.path.exists(font_path):
            try:
                font_large = ImageFont.truetype(font_path, 120)
                font_small = ImageFont.truetype(font_path, 90)
                break
            except Exception:
                continue

    for s in sections:
        x, w, color = s["x"], s["w"], s["color"]
        # 背景色塊
        draw.rectangle([x, 0, x + w, H], fill=color)
        # 分隔線
        if x > 0:
            draw.line([(x, 20), (x, H - 20)], fill="white", width=3)

        cx = x + w // 2

        # emoji
        if font_large:
            draw.text((cx, H // 2 - 120), s["emoji"], font=font_large, anchor="mm", fill="white")
        else:
            draw.text((cx, H // 2 - 100), s["emoji"], anchor="mm", fill="white")

        # 文字
        if font_small:
            draw.text((cx, H // 2 + 60), s["label"], font=font_small, anchor="mm", fill="white")
        else:
            draw.text((cx, H // 2 + 80), s["label"], anchor="mm", fill="white")

    img.save(path, "PNG")
    print(f"  圖片已生成：{path}")
    return path


def main():
    print("=== 建立 LINE Rich Menu ===\n")

    # 1. 建立 Rich Menu 結構
    print("1. 建立 Rich Menu 結構...")
    menu_body = {
        "size": {"width": W, "height": H},
        "selected": True,
        "name": "麻將預約選單",
        "chatBarText": "功能選單",
        "areas": [
            {
                "bounds": {"x": 0, "y": 0, "width": 833, "height": H},
                "action": {
                    "type": "uri",
                    "label": "預約場次",
                    "uri": f"https://liff.line.me/{LIFF_ID}/calendar",
                },
            },
            {
                "bounds": {"x": 833, "y": 0, "width": 834, "height": H},
                "action": {
                    "type": "uri",
                    "label": "我的預約",
                    "uri": f"https://liff.line.me/{LIFF_ID}/my",
                },
            },
            {
                "bounds": {"x": 1667, "y": 0, "width": 833, "height": H},
                "action": {
                    "type": "message",
                    "label": "查看時段",
                    "text": "查看時段",
                },
            },
        ],
    }
    result = call_api("POST", "/richmenu", body=menu_body)
    rich_menu_id = result["richMenuId"]
    print(f"  Rich Menu ID: {rich_menu_id}")

    # 2. 生成圖片
    print("\n2. 生成 Rich Menu 圖片...")
    img_path = generate_image()

    # 3. 上傳圖片
    print("\n3. 上傳圖片...")
    with open(img_path, "rb") as f:
        img_data = f.read()

    url = f"https://api-data.line.me/v2/bot/richmenu/{rich_menu_id}/content"
    req = urllib.request.Request(
        url,
        data=img_data,
        headers={
            "Authorization": f"Bearer {CHANNEL_TOKEN}",
            "Content-Type": "image/png",
        },
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        print(f"  上傳結果: {resp.status}")

    # 4. 設為預設
    print("\n4. 設為所有用戶預設選單...")
    call_api("POST", f"/richmenu/{rich_menu_id}/users/all")
    print("  設定完成！")

    print(f"\n✅ Rich Menu 建立成功！")
    print(f"   Rich Menu ID: {rich_menu_id}")
    print(f"   重新開啟 LINE 對話即可看到底部選單")


if __name__ == "__main__":
    main()
