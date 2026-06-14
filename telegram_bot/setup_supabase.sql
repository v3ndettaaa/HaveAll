-- ====================================================================
-- SUPABASE SCHEMA SETUP FOR HAVEALL (HAVE ALL)
-- Run this script in your Supabase SQL Editor (https://supabase.com)
-- ====================================================================

-- 1. Create Monitored Channels Table
CREATE TABLE IF NOT EXISTS public.monitored_channels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.monitored_channels ENABLE ROW LEVEL SECURITY;

-- Create Public Access Policies to allow reading (drop first if exists to prevent errors)
DROP POLICY IF EXISTS "Allow Public Access to Monitored Channels" ON public.monitored_channels;
CREATE POLICY "Allow Public Access to Monitored Channels" 
ON public.monitored_channels FOR SELECT USING (true);

-- Create Policy to allow editing (for the bot with Service Role or Anon key override)
DROP POLICY IF EXISTS "Allow Anonymous Insert/Update/Delete" ON public.monitored_channels;
CREATE POLICY "Allow Anonymous Insert/Update/Delete" 
ON public.monitored_channels FOR ALL USING (true) WITH CHECK (true);


-- 2. Create Proxies Table
CREATE TABLE IF NOT EXISTS public.proxies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    server TEXT NOT NULL,
    port INTEGER NOT NULL,
    secret TEXT NOT NULL,
    tg_link TEXT UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.proxies ENABLE ROW LEVEL SECURITY;

-- Create Public Access Policies to allow reading
DROP POLICY IF EXISTS "Allow Public Access to Proxies" ON public.proxies;
CREATE POLICY "Allow Public Access to Proxies" 
ON public.proxies FOR SELECT USING (true);

-- Create Policy to allow editing
DROP POLICY IF EXISTS "Allow All Actions on Proxies" ON public.proxies;
CREATE POLICY "Allow All Actions on Proxies" 
ON public.proxies FOR ALL USING (true) WITH CHECK (true);


-- 3. Create Configs Table (V2Ray, VLESS, VMess, Hysteria, Shadowsocks, etc.)
CREATE TABLE IF NOT EXISTS public.configs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type TEXT NOT NULL, -- 'vless', 'vmess', 'ss', 'trojan', 'hysteria', etc.
    raw_content TEXT UNIQUE NOT NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.configs ENABLE ROW LEVEL SECURITY;

-- Create Public Access Policies to allow reading
DROP POLICY IF EXISTS "Allow Public Access to Configs" ON public.configs;
CREATE POLICY "Allow Public Access to Configs" 
ON public.configs FOR SELECT USING (true);

-- Create Policy to allow editing
DROP POLICY IF EXISTS "Allow All Actions on Configs" ON public.configs;
CREATE POLICY "Allow All Actions on Configs" 
ON public.configs FOR ALL USING (true) WITH CHECK (true);


-- 4. Insert Default Channels to Monitor (Seeds)
INSERT INTO public.monitored_channels (username)
VALUES 
    ('ProxyMTProto'),
    ('Masir_Sefid'),
    ('v2ray_outlinefree'),
    ('ProxyDaemi')
ON CONFLICT (username) DO NOTHING;


-- 5. Create Bot Users Tracker
CREATE TABLE IF NOT EXISTS public.bot_users (
    id BIGINT PRIMARY KEY,
    username TEXT,
    first_name TEXT,
    last_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

ALTER TABLE public.bot_users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow Public Access to Bot Users" ON public.bot_users;
CREATE POLICY "Allow Public Access to Bot Users" 
ON public.bot_users FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow Anonymous All Access on Bot Users" ON public.bot_users;
CREATE POLICY "Allow Anonymous All Access on Bot Users" 
ON public.bot_users FOR ALL USING (true) WITH CHECK (true);


-- 6. Create Settings Table
CREATE TABLE IF NOT EXISTS public.settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

ALTER TABLE public.settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow Public Access to Settings" ON public.settings;
CREATE POLICY "Allow Public Access to Settings" 
ON public.settings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow Anonymous All Access on Settings" ON public.settings;
CREATE POLICY "Allow Anonymous All Access on Settings" 
ON public.settings FOR ALL USING (true) WITH CHECK (true);

INSERT INTO public.settings (key, value) 
VALUES ('messages_count', '10') 
ON CONFLICT (key) DO NOTHING;


-- 7. Create Subscriptions Table
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    url TEXT UNIQUE NOT NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow Public Access to Subscriptions" ON public.subscriptions;
CREATE POLICY "Allow Public Access to Subscriptions" 
ON public.subscriptions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow Anonymous All Access on Subscriptions" ON public.subscriptions;
CREATE POLICY "Allow Anonymous All Access on Subscriptions" 
ON public.subscriptions FOR ALL USING (true) WITH CHECK (true);

INSERT INTO public.subscriptions (url, remarks)
VALUES 
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile.txt', 'Russian Mobile List'),
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/Vless-Reality-White-Lists-Rus-Mobile-2.txt', 'Russian Mobile List 2'),
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS_mobile.txt', 'Black Vless Rus Mobile'),
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/WHITE-CIDR-RU-checked.txt', 'White Cidr Ru Checked'),
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_VLESS_RUS.txt', 'Black Vless Rus'),
    ('https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/refs/heads/main/BLACK_SS+All_RUS.txt', 'Black SS All Rus'),
    ('https://raw.githubusercontent.com/Mosifree/-FREE2CONFIG/refs/heads/main/FRAGMENT', 'Fragment List'),
    ('https://raw.githubusercontent.com/ThomasJasperthecat/sub/main/sublist1.txt', 'Jasper Cat List'),
    ('https://raw.githubusercontent.com/masir-sefid/Sub/main/@Masir_Sefid.txt', 'Masir_Sefid'),
    ('https://sub.iampedi5.live/sub/base64.txt', 'Base64 Static list'),
    ('https://sub.whitedns.one/sub/mihomo.yaml', 'WhiteDNS Mihomo YAML')
ON CONFLICT (url) DO NOTHING;

