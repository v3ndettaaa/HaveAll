#!/usr/bin/env bash

# ╔══════════════════════════════════════════════════════════════════════╗
# ║                   HAVEALL PORTAL — INSTALLER                      ║
# ║                     همه برای تو                                    ║
# ╚══════════════════════════════════════════════════════════════════════╝

set -e

# ─── Colors & Symbols ──────────────────────────────────────────────────
R='\033[1;31m'   # Red
G='\033[1;32m'   # Green
Y='\033[1;33m'   # Yellow
B='\033[1;34m'   # Blue
M='\033[1;35m'   # Magenta
C='\033[1;36m'   # Cyan
W='\033[1;37m'   # White
DIM='\033[2m'    # Dim
BOLD='\033[1m'   # Bold
NC='\033[0m'     # Reset

CHECK="${G}✓${NC}"
CROSS="${R}✗${NC}"
ARROW="${C}➜${NC}"
STAR="${M}★${NC}"
LINE="${DIM}────────────────────────────────────────────────${NC}"

banner() {
    clear
    echo ""
    echo -e "${C}${BOLD}"
    echo "  ╔═══════════════════════════════════════════════════════╗"
    echo "  ║                                                       ║"
    echo "  ║     ██╗  ██╗ █████╗ ███████╗██╗  ██╗                 ║"
    echo "  ║     ██║  ██║██╔══██╗██╔════╝██║ ██╔╝                 ║"
    echo "  ║     ███████║███████║███████╗█████╔╝                  ║"
    echo "  ║     ██╔══██║██╔══██║╚════██║██╔═██╗                  ║"
    echo "  ║     ██║  ██║██║  ██║███████║██║  ██╗                 ║"
    echo "  ║     ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝                 ║"
    echo "  ║                                                       ║"
    echo "  ║           همه برای تو — ALL FOR YOU                   ║"
    echo "  ║          VPN Scraper & Proxy Portal                   ║"
    echo "  ║                                                       ║"
    echo "  ╚═══════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
}

section() {
    echo ""
    echo -e "  ${M}${BOLD}┌─── $1 ───┐${NC}"
    echo ""
}

info()    { echo -e "  ${ARROW} ${W}$1${NC}"; }
success() { echo -e "  ${CHECK} ${G}$1${NC}"; }
warn()    { echo -e "  ${Y}⚠  $1${NC}"; }
error()   { echo -e "  ${CROSS} ${R}$1${NC}"; }

prompt() {
    echo -en "  ${C}❯${NC} ${W}$1${NC} "
}

# ─── Main ───────────────────────────────────────────────────────────────
banner

section "SELECT INSTALLATION MODE"
echo -e "  ${DIM}Choose how you want to run the bot:${NC}"
echo ""
echo -e "    ${BOLD}[1]${NC}  ${G}Docker${NC}       — Containerized, isolated, easy updates"
echo -e "    ${BOLD}[2]${NC}  ${BOLD}Native${NC}       — Direct Python, lighter, dev-friendly"
echo ""
prompt "Enter choice (1 or 2): "
read -r INSTALL_MODE

if [[ "$INSTALL_MODE" != "1" && "$INSTALL_MODE" != "2" ]]; then
    error "Invalid choice. Exiting."
    exit 1
fi

echo ""
echo "$LINE"

# ─── Environment Setup ─────────────────────────────────────────────────
section "ENVIRONMENT CONFIGURATION"

ENV_FILE="telegram_bot/.env"

if [ -f "$ENV_FILE" ]; then
    success "Existing .env detected at $ENV_FILE"
    prompt "Overwrite it? (y/N): "
    read -r OVERWRITE
    if [[ "$OVERWRITE" != "y" && "$OVERWRITE" != "Y" ]]; then
        info "Keeping existing .env"
    else
        rm -f "$ENV_FILE"
    fi
fi

if [ ! -f "$ENV_FILE" ]; then
    echo ""
    info "You'll need these from @BotFather and Supabase:"
    echo -e "    ${DIM}• BOT_TOKEN   — from @BotFather → /mybots → API Token${NC}"
    echo -e "    ${DIM}• ADMIN_IDS   — your Telegram user ID (use @UserInfoBot)${NC}"
    echo -e "    ${DIM}• SUPABASE_URL — your project URL${NC}"
    echo -e "    ${DIM}• SUPABASE_KEY — service role or anon key${NC}"
    echo ""

    prompt "Telegram Bot Token: "
    read -r BOT_TOKEN
    prompt "Admin Telegram ID(s) (comma-separated): "
    read -r ADMIN_IDS
    prompt "Supabase Project URL: "
    read -r SUPABASE_URL
    prompt "Supabase API Key: "
    read -r SUPABASE_KEY

    cat <<EOF > "$ENV_FILE"
BOT_TOKEN=$BOT_TOKEN
ADMIN_IDS=$ADMIN_IDS
SUPABASE_URL=$SUPABASE_URL
SUPABASE_KEY=$SUPABASE_KEY
EOF

    success "Environment saved to $ENV_FILE"
fi

echo ""
echo "$LINE"

# ─── Docker Installation ───────────────────────────────────────────────
if [ "$INSTALL_MODE" == "1" ]; then
    section "DOCKER INSTALLATION"

    # Check Docker
    if ! command -v docker &> /dev/null; then
        warn "Docker not found. Attempting install..."
        if command -v apt-get &> /dev/null; then
            sudo apt-get update -qq
            sudo apt-get install -y -qq docker.io docker-compose-plugin
            sudo systemctl enable docker
            sudo systemctl start docker
            success "Docker installed"
        else
            error "Please install Docker manually: https://docs.docker.com/engine/install/"
            exit 1
        fi
    else
        success "Docker found: $(docker --version)"
    fi

    # Check Docker Compose
    if ! docker compose version &> /dev/null; then
        if command -v docker-compose &> /dev/null; then
            COMPOSE_CMD="docker-compose"
        else
            error "Docker Compose not found. Please install: https://docs.docker.com/compose/install/"
            exit 1
        fi
    else
        COMPOSE_CMD="docker compose"
    fi

    echo ""
    info "Stopping existing containers (if any)..."
    cd telegram_bot
    $COMPOSE_CMD down 2>/dev/null || true
    cd ..

    echo ""
    info "Building and starting bot..."
    cd telegram_bot
    $COMPOSE_CMD up -d --build
    cd ..

    echo ""
    success "Bot is running in Docker!"
    echo ""
    info "Useful commands:"
    echo -e "    ${DIM}docker logs -f haveall_telegram_bot${NC}    — View live logs"
    echo -e "    ${DIM}docker restart haveall_telegram_bot${NC}    — Restart bot"
    echo -e "    ${DIM}docker stop haveall_telegram_bot${NC}        — Stop bot"

# ─── Native Installation ──────────────────────────────────────────────
else
    section "NATIVE INSTALLATION"

    # Check Python
    PYTHON_CMD=""
    for cmd in python3.12 python3.11 python3.10 python3; do
        if command -v "$cmd" &> /dev/null; then
            PYTHON_CMD="$cmd"
            break
        fi
    done

    if [ -z "$PYTHON_CMD" ]; then
        error "Python 3.10+ not found. Please install Python first."
        exit 1
    fi
    success "Python found: $($PYTHON_CMD --version)"

    # Check uv
    if ! command -v uv &> /dev/null; then
        warn "uv not found. Installing..."
        curl -LsSf https://astral.sh/uv/install.sh | sh
        export PATH="$HOME/.local/bin:$PATH"
        if [ -f "$HOME/.cargo/env" ]; then
            source "$HOME/.cargo/env"
        fi
        success "uv installed"
    else
        success "uv found: $(uv --version)"
    fi

    # Create venv
    if [ ! -d ".venv" ]; then
        info "Creating virtual environment..."
        uv venv .venv
    fi
    source .venv/bin/activate

    # Install dependencies
    info "Installing dependencies..."
    uv pip install \
        python-telegram-bot[job-queue]==21.3 \
        supabase==2.5.0 \
        httpx==0.27.0
    success "Dependencies installed"

    # Start bot
    echo ""
    info "Starting bot..."
    python telegram_bot/telegram_bot.py
fi

echo ""
echo -e "  ${C}${BOLD}═══════════════════════════════════════════════════════${NC}"
echo -e "  ${G}${BOLD}  HaveAll Portal is live! ${NC}${DIM}— همه برای تو${NC}"
echo -e "  ${C}${BOLD}═══════════════════════════════════════════════════════${NC}"
echo ""
