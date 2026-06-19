#!/usr/bin/env bash

set -e

R='\033[1;31m'
G='\033[1;32m'
Y='\033[1;33m'
M='\033[1;35m'
C='\033[1;36m'
W='\033[1;37m'
DIM='\033[2m'
BOLD='\033[1m'
NC='\033[0m'

CHECK="${G}✓${NC}"
CROSS="${R}✗${NC}"
ARROW="${C}➜${NC}"

print_line() {
    echo -e "  ${DIM}────────────────────────────────────────────────${NC}"
}

banner() {
    clear
    echo ""
    echo -e "${C}${BOLD}"
    echo -e "  ╔═══════════════════════════════════════════════════╗"
    echo -e "  ║                                                   ║"
    echo -e "  ║   ██╗  ██╗ █████╗ ███████╗██╗  ██╗              ║"
    echo -e "  ║   ██║  ██║██╔══██╗██╔════╝██║ ██╔╝              ║"
    echo -e "  ║   ███████║███████║███████╗█████╔╝               ║"
    echo -e "  ║   ██╔══██║██╔══██║╚════██║██╔═██╗               ║"
    echo -e "  ║   ██║  ██║██║  ██║███████║██║  ██╗              ║"
    echo -e "  ║   ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝              ║"
    echo -e "  ║                                                   ║"
    echo -e "  ║        همه برای تو — ALL FOR YOU                 ║"
    echo -e "  ║       VPN Scraper & Proxy Portal                 ║"
    echo -e "  ║                                                   ║"
    echo -e "  ╚═══════════════════════════════════════════════════╝"
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

require_input() {
    local var_name="$1"
    local label="$2"
    local val=""
    while [ -z "$val" ]; do
        prompt "$label: "
        read -r val
        if [ -z "$val" ]; then
            error "This field is required."
        fi
    done
    eval "$var_name='$val'"
}

stop_existing() {
    if command -v docker &> /dev/null; then
        if docker ps -q -f name=haveall_telegram_bot 2>/dev/null | grep -q .; then
            info "Stopping existing Docker container..."
            (cd telegram_bot 2>/dev/null && docker compose down 2>/dev/null) || true
            success "Docker container stopped"
        fi
    fi

    local PIDS
    PIDS=$(pgrep -f "python.*telegram_bot.py" 2>/dev/null || true)
    if [ -n "$PIDS" ]; then
        info "Stopping existing bot process (PID: $PIDS)..."
        kill "$PIDS" 2>/dev/null || true
        sleep 1
        kill -9 "$PIDS" 2>/dev/null || true
        success "Bot process stopped"
    fi
}

# ─── Main ───────────────────────────────────────────────────────────────
banner

if [ ! -d "telegram_bot" ]; then
    error "Run this script from the project root (where telegram_bot/ folder is)."
    exit 1
fi

section "SELECT INSTALLATION MODE"
echo -e "  ${DIM}Choose how you want to run the bot:${NC}"
echo ""
echo -e "    ${BOLD}[1]${NC}  ${G}Docker${NC}       — Containerized, isolated, easy updates"
echo -e "    ${BOLD}[2]${NC}  ${W}Native${NC}       — Direct Python, lighter, dev-friendly"
echo ""
prompt "Enter choice (1 or 2): "
read -r INSTALL_MODE

if [[ "$INSTALL_MODE" != "1" && "$INSTALL_MODE" != "2" ]]; then
    error "Invalid choice. Exiting."
    exit 1
fi

echo ""
print_line

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

    require_input BOT_TOKEN "Telegram Bot Token"
    require_input ADMIN_IDS "Admin Telegram ID(s) (comma-separated)"
    require_input SUPABASE_URL "Supabase Project URL"
    require_input SUPABASE_KEY "Supabase API Key"

    cat <<EOF > "$ENV_FILE"
BOT_TOKEN=$BOT_TOKEN
ADMIN_IDS=$ADMIN_IDS
SUPABASE_URL=$SUPABASE_URL
SUPABASE_KEY=$SUPABASE_KEY
EOF

    success "Environment saved to $ENV_FILE"
fi

echo ""
print_line

stop_existing

echo ""
print_line

if [ "$INSTALL_MODE" == "1" ]; then
    section "DOCKER INSTALLATION"

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

    if ! docker info &> /dev/null; then
        warn "Docker daemon not running. Starting..."
        sudo systemctl start docker 2>/dev/null || sudo service docker start 2>/dev/null || true
        sleep 2
        if ! docker info &> /dev/null; then
            error "Could not start Docker daemon. Please start it manually."
            exit 1
        fi
        success "Docker daemon started"
    fi

    COMPOSE_CMD=""
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        error "Docker Compose not found. Please install: https://docs.docker.com/compose/install/"
        exit 1
    fi
    success "Compose found: $COMPOSE_CMD"

    echo ""
    info "Building and starting bot..."
    (cd telegram_bot && $COMPOSE_CMD up -d --build)

    echo ""
    success "Bot is running in Docker!"
    echo ""
    info "Useful commands:"
    echo -e "    ${DIM}docker logs -f haveall_telegram_bot${NC}    — View live logs"
    echo -e "    ${DIM}docker restart haveall_telegram_bot${NC}    — Restart bot"
    echo -e "    ${DIM}docker stop haveall_telegram_bot${NC}        — Stop bot"

else
    section "NATIVE INSTALLATION"

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

    if [ ! -d ".venv" ]; then
        info "Creating virtual environment..."
        uv venv .venv
    fi
    source .venv/bin/activate

    info "Installing dependencies..."
    uv pip install \
        python-telegram-bot[job-queue]==21.3 \
        supabase==2.5.0 \
        httpx==0.27.0
    success "Dependencies installed"

    echo ""
    info "Starting bot... (Ctrl+C to stop)"
    python telegram_bot/telegram_bot.py
fi

echo ""
echo -e "  ${C}${BOLD}═════════════════════════════════════════════════════${NC}"
echo -e "  ${G}${BOLD}  HaveAll Portal is live! ${NC}${DIM}— همه برای تو${NC}"
echo -e "  ${C}${BOLD}═════════════════════════════════════════════════════${NC}"
echo ""
