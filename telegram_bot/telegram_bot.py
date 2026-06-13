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

# Commands
async def start(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """Glassmorphic Home Menu."""
    # Capture group chats if bot is deployed in a group of any level
    if update.effective_chat.type in ["group", "supergroup"]:
        save_group_chat(update.effective_chat.id)

    await send_log_to_admins(context, "Opened Main Menu (/start)", update.effective_user)

    keyboard = [
        [
            InlineKeyboardButton("⚡ Proxies", callback_data="get_proxies"),
            InlineKeyboardButton("🔌 Configs File", callback_data="get_configs"),
        ],
        [
            InlineKeyboardButton("📊 Status", callback_data="status"),
            InlineKeyboardButton("🔄 Scrape", callback_data="force_scrape"),
        ],
        [
            InlineKeyboardButton("🛠 Admin Panel", callback_data="admin_panel"),
        ]
    ]
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
            res = supabase.table("proxies").select("*").limit(200).execute()
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
            res = supabase.table("configs").select("*").limit(1000).execute()
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

            sample_size = min(150, len(all_configs))
            sampled_items = random.sample(all_configs, sample_size)
            
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
                        f"Extracted {sample_size} configs. Import into Hiddify or v2rayNG client app."
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
            
            px_res = supabase.table("proxies").select("id").execute()
            px_count = len(px_res.data) if px_res.data else 0
            
            cf_res = supabase.table("configs").select("id").execute()
            cf_count = len(cf_res.data) if cf_res.data else 0

            await query.edit_message_text(
                text=(
                    "📊 **HAVEALL STATUS** 📊\n"
                    "--------------------\n"
                    f"channels: {len(SUBSCRIPTION_LINKS)}\n"
                    f"Custom: {ch_count}\n"
                    f"Proxies: {px_count}\n"
                    f"Configs: {cf_count}\n"
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

        keyboard = [
            [
                InlineKeyboardButton("List Proxy Channels", callback_data="adm_list"),
            ],
            [
                InlineKeyboardButton("Add Proxy Channel", callback_data="adm_add_prompt"),
                InlineKeyboardButton("Delete Proxy Channel", callback_data="adm_del_prompt"),
            ],
            [
                InlineKeyboardButton("Back", callback_data="back_main"),
            ]
        ]
        await query.edit_message_text(
            text=(
                "🛠 **HAVEALL ADMIN** 🛠\n"
                "--------------------\n"
                "Manage database channels and sync."
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
            text="Type: /addchannel [username] (without @)",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "adm_del_prompt":
        await query.edit_message_text(
            text="Type: /removechannel [username] (without @)",
            reply_markup=InlineKeyboardMarkup([[InlineKeyboardButton("Back", callback_data="admin_panel")]]),
            parse_mode="Markdown"
        )

    elif data == "back_main":
        keyboard = [
            [
                InlineKeyboardButton("⚡ Proxies", callback_data="get_proxies"),
                InlineKeyboardButton("🔌 Configs File", callback_data="get_configs"),
            ],
            [
                InlineKeyboardButton("📊 Status", callback_data="status"),
                InlineKeyboardButton("🔄 Scrape", callback_data="force_scrape"),
            ],
            [
                InlineKeyboardButton("🛠 Admin Panel", callback_data="admin_panel"),
            ]
        ]
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
