# 🌌 HaveAll (همه برای تو): Multi-Source VPN Config Scraper & Client Engine 🫧

HaveAll (همه برای تو) is a fully-automated, offline-first connectivity toolkit spanning an ultra-high speed scraping Telegram bot and a modern responsive Android application. Powered by Supabase, the system curates zero-latency network tunnel configurations and MTProto proxy servers to bypass censorship with single-tap fluidity.

---

## 🤖 Telegram Scraper Bot (@HaveAllBot)
The Telegram bot acts as the automated scraping and administrative brain of the system:
- **Automatic 30-Minute Sync**: Every 30 minutes, the bot runs a synchronized crawler over official, high-quality GitHub subscription sources and specified crawlable Telegram proxy channels.
- **Protocol Scraper & Decoder**: Handles complex formats including manual YAML parser arrays and base64-encoded subscription strings for standard protocols (`vless`, `vmess`, `shadowsocks`, `hysteria`, `trojan`).
- **Dynamic Group Broadcast Alert**: Automatically saves every telegram group ID it joins and broadcasts beautiful, glassmorphic network update announcements on every scraper completion cycle.
- **All User Commands**:
  - `/start` - Access the responsive main keyboard dashboard.
  - `/replaceip` - Replace server IPs in VPN configs (supports vmess, vless, trojan, ss, hysteria, tuic).
  - `/cancel` - Cancel the current operation.
- **Administrative Commands**:
  - `/scrape` - Triggers an immediate manual network sync bypassing the timer.
  - `/addchannel [username]` - Registers custom Telegram proxy channels directly into the centralized Supabase database.
  - `/removechannel [username]` - Revokes monitored custom channel access.
  - `/addsub [url] [remarks]` - Adds a new subscription URL to scrape from.
  - `/removesub [id]` - Removes a subscription URL by ID.
- **Inline Mode**: Type `@YourBotName` in any chat to share proxies or configs instantly.
- **Glassmorphic Interactive Messages**:
  - **MTProto Proxies Selector**: Outputs a clean "enjoy" greeting containing zero raw IP metadata with a layout grid of sleek "have?" buttons pointing to connecting proxy schemes.
  - **150 Configs Bundle**: Randomly mixes and exports exactly 150 elite VPN tunnel subscription nodes inside a cleanly generated `.txt` file for direct import.

---

## 📱 Jetpack Compose Android Client App
A stunning, space-themed Material 3 client application built natively to retrieve and import nodes:
1. **Adaptive Glassmorphic Layout**: Implements translucent glass panels, neon blue/cyan gradients, and smooth spring-based transitions for high surface contrast.
2. **Language Toggle Switch (English & Farsi)**: Includes a global title banner, tabs, labels, and helper descriptions instantly localized to and from native Farsi.
3. **Advanced Farsi Fonts pairing**: Includes native Vazirmatn downloadable typography for Persian layouts, beautifully paired with Outfit headings for standard English indicators.
4. **Interactive Channel Pool Admin Console**:
  - Administrators can directly manage monitored channel usernames with simple edit text fields and custom insertions updating the PostgREST Supabase pool on form submit.
  - Dynamic removal triggers delete queries smoothly, keeping caches aligned.
5. **Low-Latency Connectors**:
  - **One-Tap Proxy Joiner**: Launches system intents directly into active Telegram client instances to register proxy connections.
  - **One-Tap Hiddify Config Import**: Encodes and injects raw tunnel nodes into Hiddify or alternative client apps via direct URI application schema triggers.
  - **Compact 20-Node Paginess**: Displays exactly 20 items per paginated layout to optimize cellular byte usage.

---

## ⚙️ Configuration Setup
To initialize the cluster locally or in cloud container hosting, save the credentials below inside your `.env` workspace:
`BOT_TOKEN`=Your_Telegram_Token_From_BotFather  
`ADMIN_IDS`=Comma_Separated_Admin_Chat_IDs_Example_5196798256  
`SUPABASE_URL`=https://your-project.supabase.co  
`SUPABASE_KEY`=your-secret-anon-or-service-role-key  

---

## 📊 Server Terminal Logging
The bot features clean, beautifully organized console logs right in your server terminal:
- **Instant Activity Feed**: Key events like `/start`, interactive clicks, database scraper updates, and configurations requested are printed to the console standard output.
- **Detailed Formats**: Includes user names, active Telegram IDs, actions taken, and precise UTC action times.
- **Easy Inspection**: Readily readable with a simple docker command: `docker logs -f haveall_telegram_bot`

---

## 🐳 Running on a VPS using Docker
You can run the bot on any Linux VPS with seamless, offline-first reliability using Docker and Docker Compose.

### Step 1: Create your environment configuration
Create a `.env` file inside the `./telegram_bot` directory:
```env
BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN
ADMIN_IDS=YOUR_TELEGRAM_CHAT_ID
SUPABASE_URL=YOUR_SUPABASE_PROJECT_URL
SUPABASE_KEY=YOUR_SUPABASE_SERVICE_ROLE_KEY
```

### Step 2: Spin up the container stack
Simply execute the following command in either the **root directory** or inside **`./telegram_bot`**:
```bash
docker-compose up -d --build
```
This will build the slim Python container, mount a permanent Docker volume to persist registered group chats across system restarts (`group_data`), and daemonize the bot with automated standard crash recovery policies.

### Administrative Management commands:
- **View Live Activity Logs**: `docker logs -f haveall_telegram_bot`
- **Stop current stack**: `docker-compose down`
- **Restart Stack**: `docker-compose restart`

---

## 🛠 BotFather Setup Guide

After deploying, configure your bot in **@BotFather** for the best user experience:

### 1. Register Menu Commands
Send `/setcommands` to BotFather, select your bot, then paste:
```
start - Open main menu
replaceip - Replace IPs in VPN configs
cancel - Cancel current operation
scrape - Manual scraper sync (admin)
addchannel - Add channel to scrape (admin)
removechannel - Remove scraped channel (admin)
addsub - Add subscription URL (admin)
removesub - Remove subscription URL (admin)
```

### 2. Set Menu Button
Send `/setmenubutton` to BotFather, select your bot, then set:
- **Button text**: `Open Portal`
- **URL**: `https://t.me/YourBotName?start=menu`

### 3. Enable Inline Mode
Send `/setinline` to BotFather, select your bot, then set:
- **Placeholder text**: `Search proxies or configs...`

### 4. Set Bot Description
Send `/setdescription` and set a clear description users see before starting:
```
HaveAll Portal — Free VPN proxies & configs. Scraped every 30 min from verified sources.
```

### 5. Set About Section
Send `/setabouttext`:
```
Zero-cost, high-speed tunnel scraper. Proxies + configs for v2ray, Hiddify, v2rayNG.
```

### 6. Set Profile Photo
Send `/setuserpic` and upload a logo/icon for your bot.

### 7. Group Privacy (Optional)
Send `/setprivacy` → set to **Disabled** if you want the bot to see all group messages (required for auto-detecting group chat IDs for broadcast).

***Enjoy pristine, zero-throttle networking with style!*** 🧊💎
