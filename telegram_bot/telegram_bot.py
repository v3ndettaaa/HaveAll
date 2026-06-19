import logging
import os
import sys
import random
import uuid
import json
import asyncio
from datetime import datetime

# test2
# Guarantee the local parent directory is inside sys.path
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from telegram import (
    Update,
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    InlineQueryResultArticle,
    InputTextMessageContent,
)
from telegram.ext import (
    ApplicationBuilder,
    CommandHandler,
    ContextTypes,
    MessageHandler,
    filters,
    CallbackQueryHandler,
    InlineQueryHandler,
    ConversationHandler,
)

# New Conversation States
REPLACE_IP_IPLIST, REPLACE_IP_CONFIGS = range(2)
from supabase import create_client, Client

def load_env():
    # Load .env manually if available
    for path in ["telegram_bot/.env", ".env", "../telegram_bot/.env"]:
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, val = line.split("=", 1)
                        key = key.strip()
                        val = val.strip().strip("'\"")
                        os.environ[key] = val
            break

# Load environment keys before imports causing global loading
load_env()

from scrapper import monitor_and_sync, SUBSCRIPTION_LINKS

# Logs
logging.basicConfig(
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", level=logging.INFO
)
logger = logging.getLogger(__name__)

# Credentials
SUPABASE_URL = os.getenv("SUPABASE_URL", "https://your-project.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_KEY", "your-supabase-service-role-or-anon-key")
TELEGRAM_BOT_TOKEN = os.getenv("BOT_TOKEN", "YOUR_TELEGRAM_BOT_TOKEN")
ADMIN_TELEGRAM_IDS = [int(id_str.strip()) for id_str in os.getenv("ADMIN_IDS", "0").split(",") if id_str.strip().isdigit()]

# Group Chat Tracking location
GROUP_CHATS_FILE = "telegram_bot/group_chats.json"

# Supabase init safely
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Persistence helper for group chats
def load_group_chats() -> list:
    if os.path.exists(GROUP_CHATS_FILE):
        try:
            with open(GROUP_CHATS_FILE, "r") as f:
                return json.load(f)
        except Exception:
            return []
    return []

def save_group_chat(chat_id: int):
    chats = load_group_chats()
    if chat_id not in chats:
        chats.append(chat_id)
        try:
            with open(GROUP_CHATS_FILE, "w") as f:
                json.dump(chats, f)
        except Exception as e:
            logger.error(f"Error saving group chat: {e}")

# Deletion helper
async def delete_user_message(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Safely deletes the triggering user message to keep chat history clean."""
    if update.message:
        try:
            await context.bot.delete_message(
                chat_id=update.effective_chat.id,
                message_id=update.message.message_id
            )
        except Exception as e:
            logger.info(f"Failed to delete message: {e}")

async def send_log_to_admins(context: ContextTypes.DEFAULT_TYPE, action: str, user) -> None:
    """Logs the user interaction beautifully in the server console."""
    username_str = f"@{user.username}" if user.username else "No Username"
    full_name_str = f"{user.first_name} {user.last_name or ''}".strip()
    
    # Beautiful Console Log format
    logger.info(
        "\n⚡ [HAVEALL ACTIVITY] ⚡\n"
        "👤 User: %s (%s, ID: %d)\n"
        "⚙️ Action: %s\n"
        "📅 Time: %s UTC\n"
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        username_str,
        full_name_str,
        user.id,
        action,
        datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')
    )

# Actions
# --- New Conversation Handlers for IP Replacement ---
def replace_host_in_uri(conf: str, new_ip: str) -> str:
    """Replace the host in any protocol:// URI with new_ip, preserving port and params."""
    try:
        proto, rest = conf.split("://", 1)
        proto = proto.lower()
        anchor = ""
        if "#" in rest:
            rest, anchor = rest.rsplit("#", 1)
            anchor = "#" + anchor
        query = ""
        if "?" in rest:
            rest, query = rest.split("?", 1)
            query = "?" + query

        if proto == "vmess":
            try:
                data = json.loads(base64.b64decode(rest).decode("utf-8"))
                data["add"] = new_ip
                if "port" not in data or not data["port"]:
                    data["port"] = "443"
                return "vmess://" + base64.b64encode(json.dumps(data).encode("utf-8")).decode("utf-8")
            except Exception:
                return conf

        # vless://uuid@host:port  or  trojan://password@host:port  or  ss://base64@host:port
        if "@" in rest:
            auth, host_port = rest.rsplit("@", 1)
        else:
            auth = ""
            host_port = rest

        # Split host:port — handle [ipv6]:port too
        if host_port.startswith("["):
            bracket_end = host_port.index("]")
            port_part = host_port[bracket_end + 1:]
            host_port = new_ip + port_part
        elif ":" in host_port:
            _, port_part = host_port.rsplit(":", 1)
            host_port = new_ip + ":" + port_part
        else:
            host_port = new_ip

        rebuilt = proto + "://" + (auth + "@" if auth else "") + host_port
        if query:
            rebuilt += query
        if anchor:
            rebuilt += anchor
        return rebuilt
    except Exception:
        return conf


async def start_replace_ip(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    chat_id = update.effective_chat.id
    if update.callback_query:
        await update.callback_query.answer()
        await context.bot.send_message(chat_id=chat_id, text="Send the list of new **clean IPs** (one per line, as text or a .txt file).\n\nUse /cancel to abort.", parse_mode="Markdown")
    else:
        await update.message.reply_text("Send the list of new **clean IPs** (one per line, as text or a .txt file).\n\nUse /cancel to abort.", parse_mode="Markdown")
    return REPLACE_IP_IPLIST

async def receive_ips(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    raw_lines = []
    if update.message.text:
        raw_lines = update.message.text.splitlines()
    elif update.message.document:
        doc = await context.bot.get_file(update.message.document.file_id)
        content = await doc.download_as_bytearray()
        raw_lines = content.decode("utf-8", errors="ignore").splitlines()

    ips = [line.strip() for line in raw_lines if line.strip() and not line.strip().startswith("#")]
    context.user_data["new_ips"] = ips

    if not ips:
        await update.message.reply_text("No valid IPs found. Please send IPs again (one per line), or /cancel.")
        await delete_user_message(update, context)
        return REPLACE_IP_IPLIST

    await update.message.reply_text(
        f"Received **{len(ips)}** IP(s).\nNow send the **configs** (text or .txt/.yaml file).\n\nUse /cancel to abort.",
        parse_mode="Markdown",
    )
    await delete_user_message(update, context)
    return REPLACE_IP_CONFIGS

async def receive_configs(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    new_ips = context.user_data.get("new_ips", [])
    if not new_ips:
        await update.message.reply_text("No IPs stored. Restart with /start.")
        context.user_data.clear()
        return ConversationHandler.END

    config_text = ""
    if update.message.text:
        config_text = update.message.text
    elif update.message.document:
        doc = await context.bot.get_file(update.message.document.file_id)
        content = await doc.download_as_bytearray()
        config_text = content.decode("utf-8", errors="ignore")

    raw_lines = [line.strip() for line in config_text.splitlines() if line.strip()]
    processed = []
    skipped = 0
    for i, line in enumerate(raw_lines):
        if "://" in line or line.startswith("vmess://") or line.startswith("ss://"):
            ip = new_ips[i % len(new_ips)]
            result = replace_host_in_uri(line, ip)
            if result == line:
                skipped += 1
            processed.append(result)
        else:
            processed.append(line)

    result_text = "\n".join(processed)
    file_path = "Modified_Configs.txt"
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(result_text)

    caption = f"Done — {len(processed)} configs processed, {len(new_ips)} IPs cycled."
    if skipped:
        caption += f" ({skipped} unmodified)"

    with open(file_path, "rb") as doc_file:
        await context.bot.send_document(
            chat_id=update.effective_chat.id,
            document=doc_file,
            filename=file_path,
            caption=caption,
        )

    os.remove(file_path)
    await delete_user_message(update, context)
    context.user_data.clear()
    return ConversationHandler.END

async def cancel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> int:
    if update.callback_query:
        await update.callback_query.answer()
        await context.bot.send_message(chat_id=update.effective_chat.id, text="Replacement cancelled.")
    else:
        await update.message.reply_text("Replacement cancelled.")
        await delete_user_message(update, context)
    context.user_data.clear()
    return ConversationHandler.END

# Commands
async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Glassmorphic Home Menu."""
    # Capture group chats if bot is deployed in a group of any level
    if update.effective_chat.type in ["group", "supergroup"]:
        save_group_chat(update.effective_chat.id)

    # Track caller details in bot_users table safely
    try:
        user = update.effective_user
        supabase.table("bot_users").upsert({
            "id": user.id,
            "username": user.username,
            "first_name": user.first_name,
            "last_name": user.last_name
        }).execute()
    except Exception as e:
        logger.error(f"Error registering user in bot_users: {e}")

    await send_log_to_admins(context, "Opened Main Menu (/start)", update.effective_user)

    is_admin = update.effective_user.id in ADMIN_TELEGRAM_IDS
    keyboard = [
        [
            InlineKeyboardButton("⚡ Proxies", callback_data="get_proxies"),
            InlineKeyboardButton("🔌 Configs File", callback_data="get_configs"),
            InlineKeyboardButton("🔁 Replace IP", callback_data="start_replace_ip"),
        ],
        [
            InlineKeyboardButton("📊 Status", callback_data="status"),
        ]
    ]
    if is_admin:
        keyboard[1].append(InlineKeyboardButton("🔄 Scrape", callback_data="force_scrape"))
        keyboard.append([InlineKeyboardButton("🛠 Admin Panel", callback_data="admin_panel")])

    reply_markup = InlineKeyboardMarkup(keyboard)

    await update.message.reply_text(
        text=(
            "💎 **HAVEALL PORTAL** 💎\n"
            "همه برای تو\n"
            "--------------------\n"
            "⚡ Speed proxies and tunnel scraper.\n\n"
            "Verified every 30 minutes.\n"
            "--------------------\n"
            "Select action:"
        ),
        reply_markup=reply_markup,
        parse_mode="Markdown"
    )
    await delete_user_message(update, context)

async def monitor_groups_message_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Listens to all messages to dynamically detect when added to groups."""
    if update.effective_chat and update.effective_chat.type in ["group", "supergroup"]:
        save_group_chat(update.effective_chat.id)

async def button_click_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Handles all callback queries with beautiful glass emojis."""
    query = update.callback_query
    await query.answer()

    user_id = query.from_user.id
    data = query.data

    # Match tapped button to clean label
    button_map = {
        "get_proxies": "Tapped '⚡ Proxies'",
        "get_configs": "Requested '🔌 Configs File' (150 VPN links)",
        "status": "Viewed '📊 Status' dashboard",
        "force_scrape": "Forced manual '🔄 Scrape' sync",
        "admin_panel": "Opened '🛠 Admin Panel'",
        "adm_list": "Listed target database scraper channels",
        "adm_add_prompt": "Admin: Requested add channel guidelines",
        "adm_del_prompt": "Admin: Requested delete channel guidelines",
        "back_main": "Returned to Main Menu"
    }
    action_label = button_map.get(data, f"Tapped button: {data}")
    await send_log_to_admins(context, action_label, query.from_user)

    if data == "get_proxies":
        try:
            # Always query newest first (descending) so we slice freshest elements
            res = supabase.table("proxies").select("*").order("created_at", desc=True).limit(200).execute()
            all_proxies = res.data
            if not all_proxies:
                text = "No active nodes found in Supabase.\nRun /scrape or build in app."
                if query.message:
                    await query.edit_message_text(
                        text=text,
                        reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="back_main")]]),
                        parse_mode="Markdown"
                    )
                else:
                    await query.answer(text, show_alert=True)
                return

            sample_size = min(20, len(all_proxies))
            selected = random.sample(all_proxies, sample_size)

            keyboard = []
            for idx, p in enumerate(selected, 1):
                if len(keyboard) == 0 or len(keyboard[-1]) >= 4:
                    keyboard.append([])
                # Simple neat proxy buttons (P1, P2, P3... no emojis inside)
                keyboard[-1].append(InlineKeyboardButton(f"P{idx}", url=p['tg_link']))

            if query.message:
                keyboard.append([InlineKeyboardButton("Back", callback_data="back_main")])

            await query.edit_message_text(
                text="⚡ **HAVEALL PROXIES** ⚡\nSelect a proxy to connect instantly:",
                reply_markup=InlineKeyboardMarkup(keyboard),
                parse_mode="Markdown"
            )
        except Exception as e:
            logger.error(f"Error fetching proxies: {e}")
            if query.message:
                await query.edit_message_text("Portal error. Verify database configuration.")
            else:
                await query.answer("Portal error.", show_alert=True)

    elif data == "get_configs":
        try:
            # Always order by created_at descending so we extract fresh elements first
            res = supabase.table("configs").select("*").order("created_at", desc=True).limit(1000).execute()
            all_configs = res.data
            if not all_configs:
                text = "No configs found in Supabase."
                if query.message:
                    await query.edit_message_text(
                        text=text,
                        reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="back_main")]]),
                        parse_mode="Markdown"
                    )
                else:
                    await query.answer(text, show_alert=True)
                return

            # Fair stratified sampling: distribute 150 across sources proportionally
            TARGET = 150
            by_source = {}
            for c in all_configs:
                src = c.get("remarks", "Unknown")
                by_source.setdefault(src, []).append(c)

            total = len(all_configs)
            sampled_items = []
            for src, items in by_source.items():
                share = max(1, round(len(items) / total * TARGET))
                sampled_items.extend(random.sample(items, min(share, len(items))))

            # Trim or pad to exactly TARGET
            if len(sampled_items) > TARGET:
                sampled_items = random.sample(sampled_items, TARGET)
            elif len(sampled_items) < TARGET and len(all_configs) >= TARGET:
                remaining = [c for c in all_configs if c not in sampled_items]
                sampled_items.extend(random.sample(remaining, TARGET - len(sampled_items)))

            random.shuffle(sampled_items)
            
            configs_raw = [entry["raw_content"] for entry in sampled_items]
            file_content = "\n\n".join(configs_raw)
            file_path = "HaveAll_Mixed_150_Configs.txt"

            with open(file_path, "w", encoding="utf-8") as f:
                f.write(file_content)

            dest_chat_id = query.message.chat_id if query.message else query.from_user.id

            with open(file_path, "rb") as doc_file:
                await context.bot.send_document(
                    chat_id=dest_chat_id,
                    document=doc_file,
                    filename=file_path,
                    caption=(
                        "📂 **HAVEALL CONFIGS** 📂\n"
                        f"Extracted {len(sampled_items)} configs from {len(by_source)} sources. Import into Hiddify or v2rayNG."
                    ),
                    parse_mode="Markdown"
                )
            
            if os.path.exists(file_path):
                os.remove(file_path)

            if not query.message:
                await query.answer("📂 Configs sent to your private chat!", show_alert=True)

        except Exception as e:
            logger.error(f"Error sending configs file: {e}")
            err_msg = f"Failed to generate configs: {e}"
            if query.message:
                await query.edit_message_text(err_msg)
            else:
                await query.answer(err_msg, show_alert=True)

    elif data == "status":
        try:
            ch_res = supabase.table("monitored_channels").select("*").execute()
            ch_count = len(ch_res.data) if ch_res.data else 0
            
            sub_res = supabase.table("subscriptions").select("*").execute()
            sub_count = len(sub_res.data) if sub_res.data else 0

            px_res = supabase.table("proxies").select("id").execute()
            px_count = len(px_res.data) if px_res.data else 0
            
            cf_res = supabase.table("configs").select("id").execute()
            cf_count = len(cf_res.data) if cf_res.data else 0

            await query.edit_message_text(
                text=(
                    "📊 **HAVEALL STATUS** 📊\n"
                    "--------------------\n"
                    f"Subscriptions: {sub_count}\n"
                    f"Custom Channels: {ch_count}\n"
                    f"Proxies total: {px_count}\n"
                    f"Configs total: {cf_count}\n"
                    "--------------------\n"
                    "Status: Online\n"
                    "Interval: 30 Min"
                ),
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="back_main")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"Failed to read stats: {e}")

    elif data == "force_scrape":
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("Access restricted to Administrators!", show_alert=True)
            return

        await query.edit_message_text("🔄 Scraper operation active. Synchronizing...")
        try:
            await monitor_and_sync()
            await query.edit_message_text(
                text=(
                    "🔄 **SYNCHRONIZATION SUCCESSFUL** 🔄\n"
                    "--------------------\n"
                    "All proxy nodes and config files refreshed."
                ),
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="back_main")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"Sync failed: {e}")

    elif data == "admin_panel":
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("Access restricted to Administrators!", show_alert=True)
            return

        # Fetch total started registered users
        try:
            usr_res = supabase.table("bot_users").select("id", count="exact").execute()
            usr_count = usr_res.count if hasattr(usr_res, 'count') and usr_res.count is not None else len(usr_res.data or [])
        except Exception:
            usr_count = 0

        # Fetch current message search limit setting value
        try:
            val_res = supabase.table("settings").select("value").eq("key", "messages_count").execute()
            msg_limit = val_res.data[0]["value"] if val_res.data else "10"
        except Exception:
            msg_limit = "10"

        keyboard = [
            [
                InlineKeyboardButton("📋 Channels Admin", callback_data="adm_list"),
                InlineKeyboardButton("🔗 Sub Links", callback_data="adm_subs"),
            ],
            [
                InlineKeyboardButton("➕ Add Channel", callback_data="adm_add_prompt"),
                InlineKeyboardButton("❌ Del Channel", callback_data="adm_del_prompt"),
            ],
            [
                InlineKeyboardButton(f"⚙️ Mess. Limit (Curr: {msg_limit})", callback_data="adm_toggle_limit"),
            ],
            [
                InlineKeyboardButton("🔙 Back to Menu", callback_data="back_main"),
            ]
        ]
        await query.edit_message_text(
            text=(
                "🛠 **HAVEALL ADMINISTRATIVE PORTAL** 🛠\n"
                "--------------------\n"
                f"👤 **Total Started Users:** `{usr_count}`\n"
                f"⚙️ **Checking Scan Limit:** `{msg_limit} posts`\n\n"
                "Manage dynamic scraper pools and global parameters:"
            ),
            reply_markup=InlineKeyboardMarkup(keyboard),
            parse_mode="Markdown"
        )

    elif data == "adm_list":
        try:
            res = supabase.table("monitored_channels").select("*").execute()
            channels = [row["username"] for row in res.data]
            text = "📋 **MONITORED CHANNELS** 📋\n--------------------\n"
            if not channels:
                text += "No custom channels registered."
            else:
                for idx, ch in enumerate(channels, 1):
                    text += f"{idx}. `@{ch}`\n"
            
            await query.edit_message_text(
                text=text,
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="admin_panel")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"DB fetch failed: {e}")

    elif data == "adm_add_prompt":
        await query.edit_message_text(
            text="Type: `/addchannel [username]` (without @)\n\nExample:\n`/addchannel bypassfilter`",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "adm_del_prompt":
        await query.edit_message_text(
            text="Type: `/removechannel [username]` (without @)\n\nExample:\n`/removechannel bypassfilter`",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "adm_subs":
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("Access restricted!", show_alert=True)
            return
        try:
            res = supabase.table("subscriptions").select("*").execute()
            subs = res.data or []
            
            text = "🔗 **DYNAMIC SUBSCRIPTION POOLS** 🔗\n--------------------\n"
            if not subs:
                text += "No subscription links registered."
            else:
                for idx, sb in enumerate(subs, 1):
                    short_url = sb["url"][:35] + "..." if len(sb["url"]) > 38 else sb["url"]
                    text += f"{idx}. `Ref: {sb['remarks']}` (ID: `{sb['id']}`)\n📍 `{short_url}`\n\n"
            
            keyboard = [
                [
                    InlineKeyboardButton("➕ Add Sub Link", callback_data="add_sub_prompt"),
                    InlineKeyboardButton("❌ Remove Sub Link", callback_data="del_sub_prompt"),
                ],
                [InlineKeyboardButton("🔙 Back to Admin", callback_data="admin_panel")]
            ]
            await query.edit_message_text(
                text=text,
                reply_markup=InlineKeyboardMarkup(keyboard),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"DB Fetch Error: {e}")

    elif data == "add_sub_prompt":
        await query.edit_message_text(
            text="Type: `/addsub [URL] [REMARKS]` to register a new subscription pool.\n\nExample:\n`/addsub https://raw...txt RussiaPool`",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Back to Subs", callback_data="adm_subs")]]),
            parse_mode="Markdown"
        )

    elif data == "del_sub_prompt":
        await query.edit_message_text(
            text="Type: `/removesub [ID_Number]` to delete that subscription by index identifier.\nFirst check dynamic subscription list to view ID indices.",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Back to Subs", callback_data="adm_subs")]]),
            parse_mode="Markdown"
        )

    elif data == "adm_toggle_limit":
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("Access restricted!", show_alert=True)
            return
        keyboard = [
            [
                InlineKeyboardButton("5 Messages", callback_data="set_limit_5"),
                InlineKeyboardButton("10 Messages", callback_data="set_limit_10"),
            ],
            [
                InlineKeyboardButton("15 Messages", callback_data="set_limit_15"),
                InlineKeyboardButton("20 Messages", callback_data="set_limit_20"),
            ],
            [InlineKeyboardButton("🔙 Back to Admin", callback_data="admin_panel")]
        ]
        await query.edit_message_text(
            text="⚙️ **SET SCAN DEPTH LIMIT** ⚙️\nChoose the number of last messages to scan per Telegram channel when scraping proxies/configs:",
            reply_markup=InlineKeyboardMarkup(keyboard),
            parse_mode="Markdown"
        )

    elif data.startswith("set_limit_"):
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("Access restricted!", show_alert=True)
            return
        new_lim = data.replace("set_limit_", "")
        try:
            supabase.table("settings").upsert({"key": "messages_count", "value": new_lim}).execute()
            await query.answer(f"Success! Adjusted scan depth to last {new_lim} messages per channel.", show_alert=True)
        except Exception as e:
            await query.answer(f"Failed to update limit: {e}", show_alert=True)
        
        # Redisplay admin panel refreshed
        # Fetch total started registered users
        try:
            usr_res = supabase.table("bot_users").select("id", count="exact").execute()
            usr_count = usr_res.count if hasattr(usr_res, 'count') and usr_res.count is not None else len(usr_res.data or [])
        except Exception:
            usr_count = 0

        keyboard = [
            [
                InlineKeyboardButton("📋 Channels Admin", callback_data="adm_list"),
                InlineKeyboardButton("🔗 Sub Links", callback_data="adm_subs"),
            ],
            [
                InlineKeyboardButton("➕ Add Channel", callback_data="adm_add_prompt"),
                InlineKeyboardButton("❌ Del Channel", callback_data="adm_del_prompt"),
            ],
            [
                InlineKeyboardButton(f"⚙️ Mess. Limit (Curr: {new_lim})", callback_data="adm_toggle_limit"),
            ],
            [
                InlineKeyboardButton("🔙 Back to Menu", callback_data="back_main"),
            ]
        ]
        await query.edit_message_text(
            text=(
                "🛠 **HAVEALL ADMINISTRATIVE PORTAL** 🛠\n"
                "--------------------\n"
                f"👤 **Total Started Users:** `{usr_count}`\n"
                f"⚙️ **Checking Scan Limit:** `{new_lim} posts`\n\n"
                "Manage dynamic scraper pools and global parameters:"
            ),
            reply_markup=InlineKeyboardMarkup(keyboard),
            parse_mode="Markdown"
        )

    elif data == "back_main":
        is_admin = user_id in ADMIN_TELEGRAM_IDS
        keyboard = [
            [
                InlineKeyboardButton("⚡ Proxies", callback_data="get_proxies"),
                InlineKeyboardButton("🔌 Configs File", callback_data="get_configs"),
            ],
            [
                InlineKeyboardButton("📊 Status", callback_data="status"),
            ]
        ]
        if is_admin:
            keyboard[1].append(InlineKeyboardButton("🔄 Scrape", callback_data="force_scrape"))
            keyboard.append([InlineKeyboardButton("🛠 Admin Panel", callback_data="admin_panel")])

        reply_markup = InlineKeyboardMarkup(keyboard)
        await query.edit_message_text(
            text=(
                "💎 **HAVEALL PORTAL** 💎\n"
                "Select action:"
            ),
            reply_markup=reply_markup,
            parse_mode="Markdown"
        )

# ADMIN commands
async def add_channel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("Format: /addchannel [username] (without @)")
        await delete_user_message(update, context)
        return

    ch_name = context.args[0].replace("@", "").strip()
    try:
        supabase.table("monitored_channels").insert({"username": ch_name}).execute()
        await update.message.reply_text(f"Registered channel @{ch_name} successfully.")
        await send_log_to_admins(context, f"Admin added channel: @{ch_name}", update.effective_user)
    except Exception as e:
        await update.message.reply_text(f"Failed to register channel: {e}")
    await delete_user_message(update, context)

async def remove_channel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("Format: /removechannel [username]")
        await delete_user_message(update, context)
        return

    ch_name = context.args[0].replace("@", "").strip()
    try:
        supabase.table("monitored_channels").delete().eq("username", ch_name).execute()
        await update.message.reply_text(f"Removed channel @{ch_name}.")
        await send_log_to_admins(context, f"Admin removed channel: @{ch_name}", update.effective_user)
    except Exception as e:
        await update.message.reply_text(f"Deletion error: {e}")
    await delete_user_message(update, context)

async def add_subscription_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("Format: `/addsub [URL] [Optional_Remarks]`")
        await delete_user_message(update, context)
        return

    url = context.args[0].strip()
    remarks = " ".join(context.args[1:]).strip() if len(context.args) > 1 else "Custom Added"
    try:
        supabase.table("subscriptions").insert({"url": url, "remarks": remarks}).execute()
        await update.message.reply_text(f"Registered subscription successfully:\n`Ref: {remarks}`")
        await send_log_to_admins(context, f"Admin added subscription list: {remarks}", update.effective_user)
    except Exception as e:
        await update.message.reply_text(f"Failed to register subscription: {e}")
    await delete_user_message(update, context)

async def remove_subscription_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("Format: `/removesub [ID]`")
        await delete_user_message(update, context)
        return

    req_id = context.args[0].strip()
    try:
        if req_id.isdigit():
            supabase.table("subscriptions").delete().eq("id", int(req_id)).execute()
            await update.message.reply_text(f"Successfully deleted subscription ID: {req_id}")
        else:
            supabase.table("subscriptions").delete().eq("url", req_id).execute()
            await update.message.reply_text(f"Successfully deleted subscription URL: {req_id}")
        await send_log_to_admins(context, f"Admin removed subscription ID/URL: {req_id}", update.effective_user)
    except Exception as e:
        await update.message.reply_text(f"Subscription deletion error: {e}")
    await delete_user_message(update, context)

async def scrape_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("Access restricted to Administrators!")
        await delete_user_message(update, context)
        return

    await send_log_to_admins(context, "Initiated manual /scrape command", update.effective_user)
    msg = await update.message.reply_text("Manual sync initiated. Synchronizing...", parse_mode="Markdown")
    try:
        await monitor_and_sync()
        await msg.edit_text(
            text=(
                "MANUAL SCRAPE COMPLETED\n"
                "--------------------\n"
                "Sync Operation: Successful\n"
                "Fresh connections synced."
            ),
            parse_mode="Markdown"
        )
    except Exception as e:
        await msg.edit_text(f"Sync failed: {e}")
    await delete_user_message(update, context)

# Inline Query Handler for Inline Chatting!
async def inline_query_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Allows users to type `@your_bot_name` in any chat to share proxies, configs, guides or status logs instantly."""
    results = []

    # Get proxies from Supabase to generate dynamic grid
    try:
        px_res = supabase.table("proxies").select("*").limit(200).execute()
        all_proxies = px_res.data or []
        sample_size = min(20, len(all_proxies))
        selected_proxies = random.sample(all_proxies, sample_size) if all_proxies else []
    except Exception as e:
        logger.error(f"Error fetching proxies for inline: {e}")
        selected_proxies = []

    # 1. Configs Option
    results.append(
        InlineQueryResultArticle(
            id="inline_get_configs_main",
            title="🔌 Get 150 VPN Configs",
            description="Receive 150 premium configs file in private chat",
            input_message_content=InputTextMessageContent(
                message_text="💎 **HAVEALL CONFIGS** 💎\n\nClick the button below to retrieve 150 premium configs sent to your private chat:",
                parse_mode="Markdown"
            ),
            reply_markup=InlineKeyboardMarkup([
                [InlineKeyboardButton("🔌 Get Configs File", callback_data="get_configs")]
            ])
        )
    )

    # 2. Proxies Option
    if selected_proxies:
        keyboard = []
        for idx, p in enumerate(selected_proxies, 1):
            if len(keyboard) == 0 or len(keyboard[-1]) >= 4:
                keyboard.append([])
            keyboard[-1].append(InlineKeyboardButton(f"P{idx}", url=p['tg_link']))
        
        results.append(
            InlineQueryResultArticle(
                id="inline_get_proxies_main",
                title="⚡ Get 20 MTProto Proxies",
                description="Instant grid of 20 secure MTProto connections",
                input_message_content=InputTextMessageContent(
                    message_text="⚡ **HAVEALL PROXIES** ⚡\n\nSelect a proxy below to connect instantly:",
                    parse_mode="Markdown"
                ),
                reply_markup=InlineKeyboardMarkup(keyboard)
            )
        )

    await update.inline_query.answer(results, cache_time=5)

async def private_chat_message_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Handles general text messages sent directly to the bot in private chats and deletes user trigger message."""
    if update.effective_chat and update.effective_chat.type == "private":
        raw_text = update.message.text if update.message else ""
        await send_log_to_admins(context, f"Sent direct message: '{raw_text}'", update.effective_user)
        
        await update.message.reply_text(
            text=(
                "HAVEALL PORTAL\n"
                "همه برای تو\n"
                "--------------------\n"
                "Please click one of the interactive dashboard buttons, use the commands, or share configs in inline mode!\n\n"
                "To access main menu, type /start"
            ),
            parse_mode="Markdown"
        )
        await delete_user_message(update, context)

# Periodic Scraper trigger + Group broadcast
async def run_scraper_task(context: ContextTypes.DEFAULT_TYPE) -> None:
    """Scrapes data and posts structured announcements to all registered group chats."""
    logger.info("Executing periodic scrape and broad group notifications...")
    try:
        await monitor_and_sync()
        
        # Broadcast to all saved group chats
        chats = load_group_chats()
        if chats:
            logger.info(f"Broadcasting updates to {len(chats)} groups...")
            for chat_id in chats:
                try:
                    await context.bot.send_message(
                        chat_id=chat_id,
                        text=(
                            "HAVEALL PORTAL SYNCED\n"
                            "همه برای تو\n"
                            "--------------------\n"
                            "The synchronization engine has updated all internal directories.\n\n"
                            "Updates:\n"
                            "150 Fresh configs merged.\n"
                            "MTProto nodes synced."
                        ),
                        parse_mode="Markdown"
                    )
                except Exception as e:
                    logger.error(f"Failed to post to group {chat_id}: {e}")
        logger.info("Periodic scrape & broadcast task completely finished.")
    except Exception as e:
        logger.error(f"Error in automatic recurring job: {e}")

def main():
    if not TELEGRAM_BOT_TOKEN:
        print("CRITICAL ERROR: BOT_TOKEN is missing!")
        return

    app = ApplicationBuilder().token(TELEGRAM_BOT_TOKEN).build()

    # Handlers
    replace_ip_handler = ConversationHandler(
        entry_points=[
            CallbackQueryHandler(start_replace_ip, pattern="^start_replace_ip$"),
            CommandHandler("replaceip", start_replace_ip),
        ],
        states={
            REPLACE_IP_IPLIST: [MessageHandler((filters.TEXT | filters.Document.ALL) & ~filters.COMMAND, receive_ips)],
            REPLACE_IP_CONFIGS: [MessageHandler((filters.TEXT | filters.Document.ALL) & ~filters.COMMAND, receive_configs)],
        },
        fallbacks=[
            CommandHandler("cancel", cancel),
            CommandHandler("start", start),
            CommandHandler("scrape", scrape_command),
            CommandHandler("addchannel", add_channel),
            CommandHandler("removechannel", remove_channel),
            CommandHandler("addsub", add_subscription_command),
            CommandHandler("removesub", remove_subscription_command),
        ],
    )
    app.add_handler(replace_ip_handler)
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("scrape", scrape_command))
    app.add_handler(CommandHandler("addchannel", add_channel))
    app.add_handler(CommandHandler("removechannel", remove_channel))
    app.add_handler(CommandHandler("addsub", add_subscription_command))
    app.add_handler(CommandHandler("removesub", remove_subscription_command))
    app.add_handler(CallbackQueryHandler(button_click_handler))
    app.add_handler(InlineQueryHandler(inline_query_handler))
    
    # Generic Message Handler to auto capture when bot is in groups/supergroups!
    app.add_handler(MessageHandler(filters.ChatType.GROUPS, monitor_groups_message_handler))
    
    # General private chat messages (and auto delete trigger)
    app.add_handler(MessageHandler(filters.ChatType.PRIVATE & (~filters.COMMAND), private_chat_message_handler))

    # Add 30 minutes interval job (1800 seconds)
    job_queue = app.job_queue
    job_queue.run_repeating(run_scraper_task, interval=1800, first=20)

    print("🤖 HaveAll Telegram Bot running successfully with 30-min scraper and group announcer.")
    app.run_polling()

if __name__ == "__main__":
    main()
