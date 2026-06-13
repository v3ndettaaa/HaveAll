import logging
import os
import sys
import random
import uuid
import json
import asyncio
from datetime import datetime

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
)
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

# Commands
async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Glassmorphic Home Menu."""
    # Capture group chats if bot is deployed in a group of any level
    if update.effective_chat.type in ["group", "supergroup"]:
        save_group_chat(update.effective_chat.id)

    keyboard = [
        [
            InlineKeyboardButton("🌐 Get MTProto Proxies", callback_data="get_proxies"),
            InlineKeyboardButton("🧊 Get 150 VPN Configs (.txt)", callback_data="get_configs"),
        ],
        [
            InlineKeyboardButton("🧪 View Project Status", callback_data="status"),
            InlineKeyboardButton("💎 Force Scrape / Refresh", callback_data="force_scrape"),
        ],
        [
            InlineKeyboardButton("⚙️ Admin Dashboard Panel", callback_data="admin_panel"),
        ]
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)

    await update.message.reply_text(
        text=(
            "🌌 **【 H A V E A L L 】** 🌌\n"
            "💬 *(همه برای تو)*\n"
            "━━━━━━━━━━━━━━━━━━━━━\n"
            "🫧 *Welcome to the Glass-morphic Connectivity Portal!*\n"
            "Real-time ultra-speed proxy node crawler and subscription injector.\n\n"
            "✨ *All tunnels are automatically vetted and updated every 30 minutes.*\n"
            "━━━━━━━━━━━━━━━━━━━━━\n"
            "💎 Select your glass crystal element:"
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

    if data == "get_proxies":
        try:
            res = supabase.table("proxies").select("*").limit(200).execute()
            all_proxies = res.data
            if not all_proxies:
                await query.edit_message_text(
                    text="🧊 *No active glass nodes discovered in Supabase.*\nRun /scrape or trigger via dashboard.",
                    reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Mirror Back", callback_data="back_main")]]),
                    parse_mode="Markdown"
    )
                return

            sample_size = min(20, len(all_proxies))
            selected = random.sample(all_proxies, sample_size)

            # Elegant simplified layout: Just top message "enjoy", with a grid of beautiful "have?" link buttons
            # and zero proxy info block to make it fully visual-glass.
            keyboard = []
            for idx, p in enumerate(selected, 1):
                # Put in rows of 2 for grid symmetry
                if len(keyboard) == 0 or len(keyboard[-1]) >= 2:
                    keyboard.append([])
                keyboard[-1].append(InlineKeyboardButton(f"have? 💎", url=p['tg_link']))

            keyboard.append([InlineKeyboardButton("🔙 Main Mirror", callback_data="back_main")])
            
            await query.edit_message_text(
                text="🌌 **enjoy** 🫧",
                reply_markup=InlineKeyboardMarkup(keyboard),
                parse_mode="Markdown"
            )
        except Exception as e:
            logger.error(f"Error fetching proxies: {e}")
            await query.edit_message_text("❌ Portal error. Ensure Supabase DB variables are configured successfully.")

    elif data == "get_configs":
        try:
            res = supabase.table("configs").select("*").limit(1000).execute()
            all_configs = res.data
            if not all_configs:
                await query.edit_message_text(
                    text="🧊 *No premium VPN subscription links have been scraped yet.*",
                    reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Main Mirror", callback_data="back_main")]]),
                    parse_mode="Markdown"
                )
                return

            # Mix of exactly 150 configs as requested!
            sample_size = min(150, len(all_configs))
            sampled_items = random.sample(all_configs, sample_size)
            
            configs_raw = [entry["raw_content"] for entry in sampled_items]
            file_content = "\n\n".join(configs_raw)
            file_path = "HaveAll_Mixed_150_Configs.txt"

            with open(file_path, "w", encoding="utf-8") as f:
                f.write(file_content)

            # Reply to the user
            await query.message.reply_document(
                document=open(file_path, "rb"),
                filename=file_path,
                caption=(
                    "🧪 **【 MIXED GLASS TUNNEL BUNDLE 】**\n"
                    "━━━━━━━━━━━━━━━━━━━━━\n"
                    f"💎 Extracted exactly **{sample_size}** fresh high-encryption nodes "
                    "sampled randomly from official subscription pools.\n\n"
                    "👉 Import as subscription list directly into **Hiddify**, **v2rayNG**, "
                    "or **Streisand** and enjoy the zero-latency bypass!"
                ),
                parse_mode="Markdown"
            )
            
            if os.path.exists(file_path):
                os.remove(file_path)

        except Exception as e:
            logger.error(f"Error sending configs file: {e}")
            await query.edit_message_text(f"❌ Portal failed to generate config bundle: {e}")

    elif data == "status":
        try:
            # Query stats
            ch_res = supabase.table("monitored_channels").select("*").execute()
            ch_count = len(ch_res.data) if ch_res.data else 0
            
            px_res = supabase.table("proxies").select("id").execute()
            px_count = len(px_res.data) if px_res.data else 0
            
            cf_res = supabase.table("configs").select("id").execute()
            cf_count = len(cf_res.data) if cf_res.data else 0

            # Beautiful glassy progress/stat text
            await query.edit_message_text(
                text=(
                    "🧪 **【 PORTAL STATUS INTERFACE 】** 🧪\n"
                    "━━━━━━━━━━━━━━━━━━━━━\n"
                    f"📡 Monitored Subscriptions: **{len(SUBSCRIPTION_LINKS)} channels**\n"
                    f"🔌 Registered Custom Channels: **{ch_count}**\n"
                    f"🧊 High-Performance Proxies: **{px_count} units**\n"
                    f"🔋 Premium Shadowsocks/VLess: **{cf_count} links**\n"
                    "━━━━━━━━━━━━━━━━━━━━━\n"
                    "🫧 Sync Status: *Online*\n"
                    "⏰ Automated Scraper Interval: **Every 30 Minutes**\n\n"
                    "🌈 *Powered by Glassmorphism design and supersonic speeds.*"
                ),
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Main Mirror", callback_data="back_main")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"❌ Portal failed to read stats: {e}")

    elif data == "force_scrape":
        # Check if user is admin
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("🛑 Access restricted to Glass Administrators!", show_alert=True)
            return

        await query.edit_message_text("🫧 **[SCRAPER OPERATION ACTIVE]**\nRefreshing internal matrices, purging stale caches...")
        try:
            await monitor_and_sync()
            await query.edit_message_text(
                text=(
                    "✅ **【 SYNCHRONIZATION SUCCESSFUL 】**\n"
                    "━━━━━━━━━━━━━━━━━━━━━\n"
                    "All proxy nodes and config files have been completely refreshed "
                    "from official secure sources and updated across the cluster."
                ),
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Main Mirror", callback_data="back_main")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"❌ portal compilation failed: {e}")

    elif data == "admin_panel":
        if user_id not in ADMIN_TELEGRAM_IDS:
            await query.answer("🛑 Access restricted to Administrators!", show_alert=True)
            return

        keyboard = [
            [
                InlineKeyboardButton("📋 List Custom Proxy Channels", callback_data="adm_list"),
            ],
            [
                InlineKeyboardButton("➕ Add Proxy Channel", callback_data="adm_add_prompt"),
                InlineKeyboardButton("➖ Delete Proxy Channel", callback_data="adm_del_prompt"),
            ],
            [
                InlineKeyboardButton("🔙 Main Mirror", callback_data="back_main"),
            ]
        ]
        await query.edit_message_text(
            text=(
                "🛠️ **【 GLASS ADMIN PANEL 】** 🛠️\n"
                "━━━━━━━━━━━━━━━━━━━━━\n"
                "Control system parameters, monitored channel pools, and sync triggers.\n"
                "Please choose an administrative matrix below:"
            ),
            reply_markup=InlineKeyboardMarkup(keyboard),
            parse_mode="Markdown"
        )

    elif data == "adm_list":
        try:
            res = supabase.table("monitored_channels").select("*").execute()
            channels = [row["username"] for row in res.data]
            text = "📋 **【 MONITORED CHANNELS MATRIX 】**\n━━━━━━━━━━━━━━━━━━━━━\n"
            if not channels:
                text += "_No custom channels registered. Defaults are currently active._"
            else:
                for idx, ch in enumerate(channels, 1):
                    text += f"{idx}. `@{ch}`\n"
            
            await query.edit_message_text(
                text=text,
                reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Admin Mirror", callback_data="admin_panel")]]),
                parse_mode="Markdown"
            )
        except Exception as e:
            await query.edit_message_text(f"❌ DB fetch failed: {e}")

    elif data == "adm_add_prompt":
        await query.edit_message_text(
            text="✍️ To register a custom proxy channel username, please type:\n\n`/addchannel [username_without_@]` (e.g. `/addchannel ProxyFree_Ru`)\n\nThis will write immediately into Supabase database.",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Admin Mirror", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "adm_del_prompt":
        await query.edit_message_text(
            text="✍️ To remove a custom proxy channel from database monitor, please type:\n\n`/removechannel [username]`\n\nThe configuration will update in the next 30 minutes cycle.",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("🔙 Admin Mirror", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "back_main":
        keyboard = [
            [
                InlineKeyboardButton("🌐 Get MTProto Proxies", callback_data="get_proxies"),
                InlineKeyboardButton("🧊 Get 150 VPN Configs (.txt)", callback_data="get_configs"),
            ],
            [
                InlineKeyboardButton("🧪 View Project Status", callback_data="status"),
                InlineKeyboardButton("💎 Force Scrape / Refresh", callback_data="force_scrape"),
            ],
            [
                InlineKeyboardButton("⚙️ Admin Dashboard Panel", callback_data="admin_panel"),
            ]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await query.edit_message_text(
            text=(
                "👋 **【 GLASS CONNECTIVITY MAIN 】**\n"
                "━━━━━━━━━━━━━━━━━━━━━\n"
                "Select what you would like to retrieve from the glass portal:"
            ),
            reply_markup=reply_markup,
            parse_mode="Markdown"
        )

# ADMIN commands
async def add_channel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("❌ Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("✍️ Format: `/addchannel [username_without_@]` (e.g. /addchannel ProxyFree_Ru)")
        await delete_user_message(update, context)
        return

    ch_name = context.args[0].replace("@", "").strip()
    try:
        supabase.table("monitored_channels").insert({"username": ch_name}).execute()
        await update.message.reply_text(f"✅ Registered channel `@{ch_name}` into Supabase database successfully.")
    except Exception as e:
        await update.message.reply_text(f"❌ Portal failed to register channel. (Error: {e})")
    await delete_user_message(update, context)

async def remove_channel(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("❌ Portal Admin access required!")
        await delete_user_message(update, context)
        return

    if not context.args:
        await update.message.reply_text("✍️ Format: `/removechannel [username]`")
        await delete_user_message(update, context)
        return

    ch_name = context.args[0].replace("@", "").strip()
    try:
        supabase.table("monitored_channels").delete().eq("username", ch_name).execute()
        await update.message.reply_text(f"✅ Removed channel `@{ch_name}` from system cache.")
    except Exception as e:
        await update.message.reply_text(f"❌ Deletion error: {e}")
    await delete_user_message(update, context)

async def scrape_command(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Manual glass structured /scrape command as requested: everything glassmorphic."""
    user_id = update.effective_user.id
    if user_id not in ADMIN_TELEGRAM_IDS:
        await update.message.reply_text("🛑 Access restricted to Glass Administrators!")
        await delete_user_message(update, context)
        return

    msg = await update.message.reply_text("🫧 **[Manual Sync Initiated]**\nPurging state matrices, analyzing GitHub subscriptions...", parse_mode="Markdown")
    try:
        await monitor_and_sync()
        await msg.edit_text(
            text=(
                "🧊 **【 MANUAL SCRAPE COMPLETED 】**\n"
                "━━━━━━━━━━━━━━━━━━━━━\n"
                "🌐 Sync Operation: *Successful*\n"
                "🌌 Enjoy fresh glass connections now!"
            ),
            parse_mode="Markdown"
        )
    except Exception as e:
        await msg.edit_text(f"❌ Sync failed: {e}")
    await delete_user_message(update, context)

# Inline Query Handler for Inline Chatting!
async def inline_query_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Allows users to type `@your_bot_name` in any chat to share proxies, configs, guides or status logs instantly."""
    query = update.inline_query.query.strip().lower()
    results = []

    # Detect search category
    is_log = any(kw in query for kw in ["log", "status", "stats", "stat", "about", "info", "آمار", "وضعیت"])
    is_config = any(kw in query for kw in ["config", "vless", "vmess", "ss", "shadowsocks", "trojan", "hysteria"])
    is_proxy = any(kw in query for kw in ["proxy", "mtproto", "telegram", "tg", "پروکسی"])
    is_guide = any(kw in query for kw in ["guide", "help", "tutorial", "آموزش", "راهنما"])

    # If query is non-empty and doesn't explicitly match a category, let's match both configs and proxies as falls-through
    if query and not (is_log or is_config or is_proxy or is_guide):
        is_config = True
        is_proxy = True

    # ---- CATEGORY: STATUS & LOGS ----
    if is_log or not query:
        try:
            ch_res = supabase.table("monitored_channels").select("*").execute()
            ch_count = len(ch_res.data) if ch_res.data else 0
            px_res = supabase.table("proxies").select("id").execute()
            px_count = len(px_res.data) if px_res.data else 0
            cf_res = supabase.table("configs").select("id").execute()
            cf_count = len(cf_res.data) if cf_res.data else 0

            log_text = (
                "HAVEALL STATUS LOGS\n"
                "--------------------\n"
                f"Database: Connected\n"
                f"Monitored Channels: {ch_count}\n"
                f"MTProto Proxies: {px_count}\n"
                f"V2Ray Tunnels: {cf_count}\n"
                "Sync: Every 30 minutes\n"
                "--------------------\n"
                f"UTC: {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S')}"
            )

            results.append(
                InlineQueryResultArticle(
                    id="stats_log_portal",
                    title="Status & System Logs",
                    description="Real-time counts & sync metrics",
                    input_message_content=InputTextMessageContent(
                        message_text=log_text,
                        parse_mode="Markdown"
                    )
                )
            )
        except Exception as e:
            logger.error(f"Failed to generate inline query stats: {e}")

    # ---- CATEGORY: CONFIGS ----
    if is_config or not query:
        try:
            res = supabase.table("configs").select("*").limit(20).execute()
            configs = res.data or []
            if query and not is_log:
                configs = [c for c in configs if query in c["type"].lower() or query in (c.get("remarks") or "").lower() or query in c["raw_content"].lower()]
            
            for idx, c in enumerate(configs[:5]):
                raw = c["raw_content"]
                results.append(
                    InlineQueryResultArticle(
                        id=f"inline_config_{c['id']}_{idx}",
                        title=f"Config: {c['type'].upper()} ({c['remarks'] or 'High Speed'})",
                        description="Share tunnel configuration profile",
                        input_message_content=InputTextMessageContent(
                            message_text=(
                                f"HAVEALL CONFIG | {c['type'].upper()}\n"
                                f"Remarks: {c['remarks'] or 'High Speed'}\n"
                                "--------------------\n"
                                f"```\n{raw}\n```"
                            ),
                            parse_mode="Markdown"
                        )
                    )
                )
        except Exception as e:
            logger.error(f"Inline configs search error: {e}")

    # ---- CATEGORY: PROXIES ----
    if is_proxy or not query:
        try:
            res = supabase.table("proxies").select("*").limit(20).execute()
            proxies = res.data or []
            if query and not is_log:
                proxies = [p for p in proxies if query in p["server"].lower() or query in p["tg_link"].lower()]
            
            for idx, p in enumerate(proxies[:5]):
                results.append(
                    InlineQueryResultArticle(
                        id=f"inline_proxy_{p['id']}_{idx}",
                        title=f"MTProto Proxy #{p['id']}",
                        description=f"Server: {p['server']}",
                        input_message_content=InputTextMessageContent(
                            message_text=(
                                "HAVEALL MTPROTO PROXY\n"
                                f"Server: {p['server']}\n"
                                f"Port: {p['port']}"
                            ),
                            parse_mode="Markdown"
                        ),
                        reply_markup=InlineKeyboardMarkup([
                            [InlineKeyboardButton("Connect Proxy", url=p['tg_link'])]
                        ])
                    )
                )
        except Exception as e:
            logger.error(f"Inline proxies search error: {e}")

    # ---- CATEGORY: GUIDES ----
    if is_guide or not query:
        # Farsi Guide
        results.append(
            InlineQueryResultArticle(
                id="guide_farsi",
                title="راهنمای اتصال سریع (Farsi)",
                description="آموزش نحوه استفاده از کانفیگ‌ها",
                input_message_content=InputTextMessageContent(
                    message_text=(
                        "راهنمای اتصال HAVEALL:\n"
                        "۱. برنامه Hiddify یا v2rayNG را نصب کنید.\n"
                        "۲. کانفیگ را از چت کپی کنید.\n"
                        "۳. در برنامه دکمه (+) یا Import from Clipboard را بزنید.\n"
                        "۴. دکمه دایره اتصال را بزنید."
                    ),
                    parse_mode="Markdown"
                )
            )
        )

        # English Guide
        results.append(
            InlineQueryResultArticle(
                id="guide_english",
                title="Quick Connection Guide (English)",
                description="How to import connection profiles",
                input_message_content=InputTextMessageContent(
                    message_text=(
                        "HAVEALL Connection Guide:\n"
                        "1. Install Hiddify or v2rayNG.\n"
                        "2. Copy the config block from chat.\n"
                        "3. Click (+) or Import from Clipboard in client app.\n"
                        "4. Press connect button."
                    ),
                    parse_mode="Markdown"
                )
            )
        )

    await update.inline_query.answer(results, cache_time=5)

async def private_chat_message_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Handles general text messages sent directly to the bot in private chats and deletes user trigger message."""
    if update.effective_chat and update.effective_chat.type == "private":
        await update.message.reply_text(
            text=(
                "🌌 **【 H A V E A L L 】** 🌌\n"
                "💬 *(همه برای تو)*\n"
                "━━━━━━━━━━━━━━━━━━━━━\n"
                "Please click one of the interactive dashboard buttons, use the commands, or share configs in inline mode!\n\n"
                "To access my main menu, type `/start`"
            ),
            parse_mode="Markdown"
        )
        await delete_user_message(update, context)

# Periodic Scraper trigger + Group broadcast
async def run_scraper_task(context: ContextTypes.DEFAULT_TYPE) -> None:
    """Scrapes data and posts structured glass announcements to all registered group chats."""
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
                            "🌌 **【 H A V E A L L 】** 🌌\n"
                            "💬 *(همه برای تو)*\n"
                            "━━━━━━━━━━━━━━━━━━━━━\n"
                            "🫧 **GLOBAL NETWORK REFRESH** 🫧\n"
                            "The synchronization engine has updated all internal directories.\n\n"
                            "💎 **Updates:**\n"
                            "🎯 *150 Fresh premium configs merged.*\n"
                            "🔌 *Full-throttle custom MTProto nodes synced.*\n"
                            "━━━━━━━━━━━━━━━━━━━━━\n"
                            "🌐 Status: *Operational* | Acc: *Superior*\n"
                            "✨ enjoy!"
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
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("scrape", scrape_command))
    app.add_handler(CommandHandler("addchannel", add_channel))
    app.add_handler(CommandHandler("removechannel", remove_channel))
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
