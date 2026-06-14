import re
import os
import base64
import random
import asyncio
from datetime import datetime
import html
from urllib.parse import unquote
import httpx
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

# Load env values before client instantiation
load_env()

# Supabase Credentials
SUPABASE_URL = os.getenv("SUPABASE_URL", "https://your-project.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_KEY", "your-supabase-service-role-or-anon-key")

# Set up Supabase Client safely
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Regex Patterns for Proxies (MTProto)
MTPROTO_RE = re.compile(
    r"(?:tg|https?://t\.me)/proxy\?server=([a-zA-Z0-9\.\-_]+)&(?:amp;)?port=(\d+)&(?:amp;)?secret=([a-zA-Z0-9\-_]+)"
)

# Regex Patterns for V2Ray / Shadowsocks / Hysteria / Vless / VMess / Trojan
CONFIG_RE = re.compile(
    r"\b((?:vmess|vless|ss|ssr|trojan|hysteria|hysteria2|tuic|hy2)://[^\s\"'<>]+)"
)

# Static Subscription Txt Links (used as defaults if database is empty)
SUBSCRIPTION_LINKS = [
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt",
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt",
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-checked.txt",
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt",
    "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS+All_RUS.txt",
    "https://raw.githubusercontent.com/Mosifree/-FREE2CONFIG/refs/heads/main/FRAGMENT",
    "https://raw.githubusercontent.com/ThomasJasperthecat/sub/main/sublist1.txt",
    "https://raw.githubusercontent.com/masir-sefid/Sub/main/@Masir_Sefid.txt",
    "https://sub.iampedi5.live/sub/base64.txt",
    "https://sub.whitedns.one/sub/mihomo.yaml"
]

# Static Proxy MTProto telegram channels
PROXY_CHANNELS = ["ProxyFree_Ru", "TProxyRU", "iRoProxy", "proxyy"]

async def fetch_monitored_proxy_channels():
    """Fetches custom channels from database and merges with static defaults."""
    try:
        response = supabase.table("monitored_channels").select("username").execute()
        db_items = [row["username"] for row in response.data]
        merged = list(set(PROXY_CHANNELS + db_items))
        return merged
    except Exception as e:
        print(f"Error accessing Supabase monitored_channels: {e}")
        return PROXY_CHANNELS

async def fetch_messages_count_setting() -> int:
    """Fetches the number of last messages to scan from Supabase settings."""
    try:
        res = supabase.table("settings").select("value").eq("key", "messages_count").execute()
        if res.data:
            return int(res.data[0]["value"])
    except Exception as e:
        print(f"Error fetching messages_count setting: {e}")
    return 10  # Fallback to last 10 messages

async def fetch_db_subscriptions() -> list:
    """Fetches subscription links dynamically from Supabase."""
    try:
        res = supabase.table("subscriptions").select("url", "remarks").execute()
        if res.data:
            return [{"url": row["url"], "remarks": row.get("remarks") or "Sub Link"} for row in res.data]
    except Exception as e:
        print(f"Error fetching subscriptions from Supabase: {e}")
    return [{"url": u, "remarks": "Static Sub"} for u in SUBSCRIPTION_LINKS]

async def wipe_prev_data():
    """Wipes existing proxy and config tables to ensure fresh non-expired options."""
    try:
        supabase.table("proxies").delete().neq("id", 0).execute()
        supabase.table("configs").delete().neq("id", 0).execute()
        print("Successfully cleared database to receive fresh elements.")
    except Exception as e:
        print(f"Wiping existing Supabase records failed: {e}")

def try_decode_base64(text: str) -> str:
    """Attempts to decode text if it's base64 encoded by checks format."""
    cleaned = text.strip().replace("\r\n", "").replace("\n", "").replace(" ", "")
    # Check if looks like pure base64 base string
    if not cleaned or len(cleaned) < 16:
        return text
    try:
        # Pad string correctly
        missing_padding = len(cleaned) % 4
        if missing_padding:
            cleaned += '=' * (4 - missing_padding)
        decoded_bytes = base64.b64decode(cleaned)
        decoded_str = decoded_bytes.decode("utf-8", errors="ignore")
        # Check if contains any protocol references
        if any(pat in decoded_str for pat in ["vless://", "vmess://", "ss://", "ssr://", "trojan://", "hysteria"]):
            return decoded_str
    except Exception:
        pass
    return text

def parse_yaml_configs_safely(text: str) -> list:
    """Fallback manual parser for mihomo.yaml without relying on external pyyaml."""
    extracted = []
    # If standard v2ray URL is embedded inside YAML as text, find it!
    for link in CONFIG_RE.findall(text):
        if link not in extracted:
            extracted.append(link)
    
    # Simple line parsing if it specifies server, port, type, uuid, cipher
    lines = text.split("\n")
    current_proxy = {}
    for line in lines:
        line_strip = line.strip()
        if line_strip.startswith("- name:") or line_strip.startswith("- {name:"):
            # New proxy block
            if current_proxy:
                # Compile a config from current_proxy fields
                config_str = build_v2ray_from_dict(current_proxy)
                if config_str and config_str not in extracted:
                    extracted.append(config_str)
            current_proxy = {}
        
        # Simple extraction of key-values
        if ":" in line_strip:
            parts = line_strip.split(":", 1)
            key = parts[0].strip().replace("-", "").strip()
            value = parts[1].strip().strip("'\"{}")
            if key in ["type", "server", "port", "uuid", "password", "cipher", "sni", "network"]:
                current_proxy[key] = value

    if current_proxy:
        config_str = build_v2ray_from_dict(current_proxy)
        if config_str and config_str not in extracted:
            extracted.append(config_str)

    return extracted

def build_v2ray_from_dict(d: dict) -> str:
    """Builds a rudimentary configuration link from YAML extraction."""
    ptype = d.get("type", "").lower()
    server = d.get("server")
    port = d.get("port")
    if not server or not port:
        return None
    
    # Vless / SS / Trojan
    if ptype == "vless":
        uuid = d.get("uuid", "00000000-0000-0000-0000-000000000000")
        sni = d.get("sni", "")
        return f"vless://{uuid}@{server}:{port}?security=reality&sni={sni}#ClashImported"
    elif ptype == "ss" or ptype == "shadowsocks":
        password = d.get("password", "")
        cipher = d.get("cipher", "aes-256-gcm")
        # base64 cipher:password
        auth = base64.b64encode(f"{cipher}:{password}".encode()).decode()
        return f"ss://{auth}@{server}:{port}#ClashImported"
    elif ptype == "trojan":
        password = d.get("password", "")
        return f"trojan://{password}@{server}:{port}?security=tls#ClashImported"
    
    return None

async def monitor_and_sync():
    """Scrapes both dynamic subscription lists and MTProto Telegram proxy channels, syncing with Database."""
    print("💎 Glassmorphism Sync Engine Initiated!")
    proxy_channels = await fetch_monitored_proxy_channels()
    db_subs = await fetch_db_subscriptions()
    await wipe_prev_data()

    configs_extracted = []
    proxies_extracted = []

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    async with httpx.AsyncClient(headers=headers, follow_redirects=True, timeout=20.0) as client:
        # Part 1. Extract Configs from Supabase-registered subscription lists dynamic pool
        print(f"📥 Loading {len(db_subs)} dynamic VPN subscription lists...")
        for sub_item in db_subs:
            sub_url = sub_item["url"]
            source_name = sub_item["remarks"] or "Sub List"
            try:
                print(f"🔗 Fetching: {sub_url} (Ref: {source_name})")
                res = await client.get(sub_url)
                if res.status_code != 200:
                    print(f"⚠️ Failed to download {sub_url} (HTTP {res.status_code})")
                    continue
                
                raw_text = res.text
                
                # Try Base64 decoding if the file is purely encoded
                decoded_text = try_decode_base64(raw_text)
                
                # Check if YAML
                if ".yaml" in sub_url.lower() or "yaml" in decoded_text[:1000].lower():
                    links = parse_yaml_configs_safely(decoded_text)
                else:
                    links = CONFIG_RE.findall(decoded_text)

                for link in links:
                    # Clean trailing HTML parameters or quotation marks
                    config_clean = link.replace("&amp;", "&").split('"')[0].split("'")[0].split("<")[0].split("\\")[0].strip()
                    if not config_clean:
                        continue

                    # Protocol classifier
                    config_type = "vmess"
                    lower_conf = config_clean.lower()
                    if "vless://" in lower_conf:
                        config_type = "vless"
                    elif "ss://" in lower_conf:
                        config_type = "shadowsocks"
                    elif "hysteria" in lower_conf or "hy2://" in lower_conf:
                        config_type = "hysteria"
                    elif "trojan://" in lower_conf:
                        config_type = "trojan"
                    elif "tuic://" in lower_conf:
                        config_type = "tuic"

                    remarks = f"Ref: {source_name}"

                    if config_clean not in [c["raw_content"] for c in configs_extracted]:
                        configs_extracted.append({
                            "type": config_type,
                            "raw_content": config_clean,
                            "remarks": remarks
                        })

            except Exception as e:
                print(f"❌ Error fetching subscription {sub_url}: {e}")

        # Part 2. Extract MTProto & Dynamic Tunneled Configs from all monitored Telegram channels
        limit = await fetch_messages_count_setting()
        print(f"📥 Scanning last {limit} messages in {len(proxy_channels)} monitored Telegram channels...")
        for channel in proxy_channels:
            url = f"https://t.me/s/{channel}"
            try:
                print(f"📡 Scraping channel: @{channel}")
                response = await client.get(url)
                if response.status_code != 200:
                    continue

                # Fully decode HTML entities and percent-encoded URLs to capture buttons/markdown/inline items
                raw_html = response.text
                decoded_html = unquote(html.unescape(raw_html))

                # Split page into individual message containers to apply precise limits
                message_blocks = re.split(r'<div class="[^"]*tgme_widget_message_wrap', decoded_html)
                posts = message_blocks[1:]
                
                # Slice target check count to comply with Admin wishes
                selected_posts = posts[-limit:] if len(posts) > limit else posts
                
                # Join to build a single text block of exactly N target posts
                content_to_scan = "\n---POST-BOUNDARY---\n".join(selected_posts) if selected_posts else decoded_html

                # Extract MTProto proxies
                for server, port, secret in MTPROTO_RE.findall(content_to_scan):
                    server_clean = server.strip()
                    port_clean = port.strip()
                    secret_clean = secret.strip()
                    tg_link = f"https://t.me/proxy?server={server_clean}&port={port_clean}&secret={secret_clean}"
                    
                    if tg_link not in [p["tg_link"] for p in proxies_extracted]:
                        proxies_extracted.append({
                            "server": server_clean,
                            "port": int(port_clean),
                            "secret": secret_clean,
                            "tg_link": tg_link
                        })

                # Also grab any embedded V2ray/Vless configs listed in channel messages!
                for link in CONFIG_RE.findall(content_to_scan):
                    config_clean = link.replace("&amp;", "&").split('"')[0].split("'")[0].split("<")[0].split("\\")[0].strip()
                    if not config_clean:
                        continue
                    
                    config_type = "vmess"
                    lower_conf = config_clean.lower()
                    if "vless://" in lower_conf:
                        config_type = "vless"
                    elif "ss://" in lower_conf:
                        config_type = "shadowsocks"
                    elif "hysteria" in lower_conf or "hy2://" in lower_conf:
                        config_type = "hysteria"
                    elif "trojan://" in lower_conf:
                        config_type = "trojan"
                    elif "tuic://" in lower_conf:
                        config_type = "tuic"

                    if config_clean not in [c["raw_content"] for c in configs_extracted]:
                        configs_extracted.append({
                            "type": config_type,
                            "raw_content": config_clean,
                            "remarks": f"Ref: @{channel}"
                        })

            except Exception as e:
                print(f"❌ Error scraping channel @{channel}: {e}")

    # Part 3. Push to Supabase with duplicate checks
    if proxies_extracted:
        try:
            print(f"💾 Saving {len(proxies_extracted)} proxies to Supabase...")
            # Supabase free tier sometimes has batch constraints, so we write in chunks of 100
            for i in range(0, len(proxies_extracted), 100):
                chunk = proxies_extracted[i:i+100]
                supabase.table("proxies").insert(chunk).execute()
        except Exception as e:
            print(f"Error inserting proxies to Supabase: {e}")

    if configs_extracted:
        try:
            # Shuffle so we get a diverse mix
            random.shuffle(configs_extracted)
            print(f"💾 Saving {len(configs_extracted)} configs to Supabase...")
            for i in range(0, len(configs_extracted), 100):
                chunk = configs_extracted[i:i+100]
                supabase.table("configs").insert(chunk).execute()
        except Exception as e:
            print(f"Error inserting configs to Supabase: {e}")

    print("✅ Glassmorphism Sync Engine successfully completed iteration.")

if __name__ == "__main__":
    asyncio.run(monitor_and_sync())

