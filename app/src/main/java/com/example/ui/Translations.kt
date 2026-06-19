package com.example.ui

enum class AppLanguage { EN, FA }

object Translations {
    private val en = mapOf(
        "app_title" to "HaveAll",
        "subtitle" to "Ultra low-latency network tunnels",
        "configs" to "Configs",
        "proxies" to "Proxies",
        "admin_panel" to "Admin",
        "db_setup" to "Database Setup",
        "dialog_desc" to "Configure your Supabase endpoints to sync nodes in real time:",
        "apply" to "Apply",
        "cancel" to "Cancel",
        "configs_tip" to "Tap to copy. Import directly into Hiddify.",
        "proxies_tip" to "Connect MTProto proxies instantly to Telegram.",
        "copy" to "Copy",
        "import_btn" to "Import",
        "connect_proxy" to "Connect",
        "active_sync" to "Synced with Supabase",
        "admin_desc" to "Manage monitored channels and subscription sources.",
        "chan_placeholder" to "Channel name (without @)",
        "add_chan" to "Add Channel",
        "monitored_list" to "MONITORED CHANNELS",
        "sync_interval" to "Sync: Every 30 min",
        "empty_db" to "No data yet. Run scraper on bot first.",
        "server" to "Server",
        "port" to "Port",
        "secret" to "Secret",
        "add_sub" to "Add Subscription",
        "sub_url_placeholder" to "https://raw.githubusercontent.com/...txt",
        "sub_label_placeholder" to "Pool label",
        "sub_url_label" to "Subscription URL",
        "sub_label_label" to "Label",
        "sub_list" to "SUBSCRIPTIONS",
        "no_channels" to "No channels added.",
        "no_subs" to "No subscriptions added.",
        "retry" to "Retry",
        "loading" to "Loading..."
    )

    private val fa = mapOf(
        "app_title" to "همه برای تو",
        "subtitle" to "تونل‌های پرسرعت دور زدن فیلترینگ",
        "configs" to "کانفیگ‌ها",
        "proxies" to "پروکسی‌ها",
        "admin_panel" to "مدیریت",
        "db_setup" to "تنظیمات دیتابیس",
        "dialog_desc" to "آدرس و کلید Supabase را برای همگام‌سازی وارد کنید:",
        "apply" to "ذخیره",
        "cancel" to "انصراف",
        "configs_tip" to "برای کپی لمس کنید. ورود مستقیم به هیدیفای.",
        "proxies_tip" to "اتصال فوری پروکسی به تلگرام.",
        "copy" to "کپی",
        "import_btn" to "ورود به هیدیفای",
        "connect_proxy" to "اتصال",
        "active_sync" to "همگام با سوپابیس",
        "admin_desc" to "مدیریت کانال‌ها و منابع اشتراک.",
        "chan_placeholder" to "نام کانال (بدون @)",
        "add_chan" to "افزودن کانال",
        "monitored_list" to "کانال‌های تحت نظارت",
        "sync_interval" to "همگام‌سازی: هر ۳۰ دقیقه",
        "empty_db" to "داده‌ای موجود نیست. ابتدا ربات را اجرا کنید.",
        "server" to "سرور",
        "port" to "پورت",
        "secret" to "سکرت",
        "add_sub" to "افزودن اشتراک",
        "sub_url_placeholder" to "https://raw.githubusercontent.com/...txt",
        "sub_label_placeholder" to "نام منبع",
        "sub_url_label" to "آدرس اشتراک",
        "sub_label_label" to "برچسب",
        "sub_list" to "اشتراک‌ها",
        "no_channels" to "کانالی اضافه نشده.",
        "no_subs" to "اشتراکی اضافه نشده.",
        "retry" to "تلاش مجدد",
        "loading" to "در حال بارگذاری..."
    )

    fun get(key: String, lang: AppLanguage): String {
        return if (lang == AppLanguage.FA) fa[key] ?: en[key] ?: key else en[key] ?: key
    }
}
