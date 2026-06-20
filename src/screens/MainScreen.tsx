import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  Alert,
  Linking,
  Clipboard,
  Modal,
  TextInput,
  ActivityIndicator,
  Platform,
  StatusBar,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  SupabaseConfig,
  SupabaseProxy,
  SupabaseChannel,
  SupabaseSubscription,
  api,
  measurePing,
  extractServerFromConfig,
} from '../api/supabase';
import { loadStoredConfig, saveStoredConfig } from '../api/storage';
import { DarkColors, LightColors, Colors } from '../theme/colors';
import { t } from '../i18n/translations';
import { LangProvider, useLang } from '../i18n/LangContext';
import ConfigItemCard from '../components/ConfigItemCard';
import ProxyItemCard from '../components/ProxyItemCard';
import AdminPanel from '../components/AdminPanel';
import SplashScreen from '../components/SplashScreen';

type Tab = 'configs' | 'proxies' | 'admin';
const PAGE_SIZE = 20;

export default function MainScreen() {
  const [showSplash, setShowSplash] = useState(true);

  return (
    <LangProvider>
      {showSplash
        ? <SplashScreen onFinished={() => setShowSplash(false)} />
        : <MainContent />}
    </LangProvider>
  );
}

function MainContent() {
  const { language, setLanguage, fontFamily, fontFamilyBold } = useLang();
  const [darkMode, setDarkMode] = useState(true);
  const [tab, setTab] = useState<Tab>('configs');
  const [showSettings, setShowSettings] = useState(false);

  const [supabaseUrl, setSupabaseUrl] = useState('');
  const [supabaseKey, setSupabaseKey] = useState('');
  const [settingsUrl, setSettingsUrl] = useState('');
  const [settingsKey, setSettingsKey] = useState('');

  const [configs, setConfigs] = useState<SupabaseConfig[]>([]);
  const [proxies, setProxies] = useState<SupabaseProxy[]>([]);
  const [channels, setChannels] = useState<SupabaseChannel[]>([]);
  const [subscriptions, setSubscriptions] = useState<SupabaseSubscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [configOffset, setConfigOffset] = useState(0);
  const [proxyOffset, setProxyOffset] = useState(0);
  const [configHasMore, setConfigHasMore] = useState(true);
  const [proxyHasMore, setProxyHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);

  const colors: Colors = darkMode ? DarkColors : LightColors;
  const insets = useSafeAreaInsets();

  useEffect(() => {
    (async () => {
      const stored = await loadStoredConfig();
      if (stored && stored.supabaseUrl && stored.supabaseKey) {
        setSupabaseUrl(stored.supabaseUrl);
        setSupabaseKey(stored.supabaseKey);
      } else {
        setLoading(false);
      }
    })();
  }, []);

  const loadData = useCallback(async (url?: string, key?: string) => {
    const u = url || supabaseUrl;
    const k = key || supabaseKey;
    if (!u || !k) {
      setLoading(false);
      setError(t('empty_db', language));
      return;
    }
    setLoading(true);
    setError(null);
    setConfigOffset(0);
    setProxyOffset(0);
    setConfigHasMore(true);
    setProxyHasMore(true);
    try {
      api.configure(u, k);
      const [c, p, ch, s] = await Promise.all([
        api.getConfigs(PAGE_SIZE, 0),
        api.getProxies(PAGE_SIZE, 0),
        api.getMonitoredChannels(),
        api.getSubscriptions(),
      ]);
      setConfigs(c.filter((v, i, a) => a.findIndex(x => x.id === v.id) === i));
      setProxies(p.filter((v, i, a) => a.findIndex(x => x.id === v.id) === i));
      setChannels(ch);
      setSubscriptions(s);
      setConfigHasMore(c.length >= PAGE_SIZE);
      setProxyHasMore(p.length >= PAGE_SIZE);

      Promise.all(
        p.map(async (proxy) => {
          const ms = await measurePing(proxy.server, proxy.port);
          return { ...proxy, ping: ms };
        })
      ).then((pinged) => {
        setProxies((prev) => {
          const sorted = prev.map((p) => {
            const found = pinged.find((pp) => pp.id === p.id);
            return found ? { ...p, ping: found.ping } : p;
          });
          return [...sorted].sort((a, b) => (a.ping ?? Infinity) - (b.ping ?? Infinity));
        });
      });

      Promise.all(
        c.map(async (config) => {
          const srv = extractServerFromConfig(config.raw_content);
          if (!srv) return { ...config, ping: null as number | null };
          const ms = await measurePing(srv.host, srv.port);
          return { ...config, ping: ms };
        })
      ).then((pinged) => {
        setConfigs((prev) => {
          const sorted = prev.map((p) => {
            const found = pinged.find((pp) => pp.id === p.id);
            return found ? { ...p, ping: found.ping } : p;
          });
          return [...sorted].sort((a, b) => (a.ping ?? Infinity) - (b.ping ?? Infinity));
        });
      });
    } catch (e: any) {
      setError(e.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, [supabaseUrl, supabaseKey, language]);

  useEffect(() => {
    if (supabaseUrl && supabaseKey) {
      loadData(supabaseUrl, supabaseKey);
    }
  }, [supabaseUrl, supabaseKey]);

  const loadMoreConfigs = useCallback(async () => {
    if (loadingMore || !configHasMore) return;
    setLoadingMore(true);
    try {
      const nextOffset = configOffset + PAGE_SIZE;
      const more = await api.getConfigs(PAGE_SIZE, nextOffset);
      setConfigs((prev) => { const ids = new Set(prev.map(x => x.id)); return [...prev, ...more.filter(x => !ids.has(x.id))]; });
      setConfigOffset(nextOffset);
      if (more.length < PAGE_SIZE) setConfigHasMore(false);

      Promise.all(
        more.map(async (config) => {
          const srv = extractServerFromConfig(config.raw_content);
          if (!srv) return { ...config, ping: null as number | null };
          const ms = await measurePing(srv.host, srv.port);
          return { ...config, ping: ms };
        })
      ).then((pinged) => {
        setConfigs((prev) => {
          const updated = prev.map((p) => {
            const found = pinged.find((pp) => pp.id === p.id);
            return found ? { ...p, ping: found.ping } : p;
          });
          return [...updated].sort((a, b) => (a.ping ?? Infinity) - (b.ping ?? Infinity));
        });
      });
    } catch {}
    setLoadingMore(false);
  }, [configOffset, configHasMore, loadingMore]);

  const loadMoreProxies = useCallback(async () => {
    if (loadingMore || !proxyHasMore) return;
    setLoadingMore(true);
    try {
      const nextOffset = proxyOffset + PAGE_SIZE;
      const more = await api.getProxies(PAGE_SIZE, nextOffset);
      setProxies((prev) => { const ids = new Set(prev.map(x => x.id)); return [...prev, ...more.filter(x => !ids.has(x.id))]; });
      setProxyOffset(nextOffset);
      if (more.length < PAGE_SIZE) setProxyHasMore(false);

      Promise.all(
        more.map(async (proxy) => {
          const ms = await measurePing(proxy.server, proxy.port);
          return { ...proxy, ping: ms };
        })
      ).then((pinged) => {
        setProxies((prev) => {
          const updated = prev.map((p) => {
            const found = pinged.find((pp) => pp.id === p.id);
            return found ? { ...p, ping: found.ping } : p;
          });
          return [...updated].sort((a, b) => (a.ping ?? Infinity) - (b.ping ?? Infinity));
        });
      });
    } catch {}
    setLoadingMore(false);
  }, [proxyOffset, proxyHasMore, loadingMore]);

  const handleApplySettings = async () => {
    const u = settingsUrl.trim();
    const k = settingsKey.trim();
    setSupabaseUrl(u);
    setSupabaseKey(k);
    setShowSettings(false);
    await saveStoredConfig(u, k);
  };

  const openSettings = () => {
    setSettingsUrl(supabaseUrl);
    setSettingsKey(supabaseKey);
    setShowSettings(true);
  };

  const currentLang = language;

  const tabs: { key: Tab; label: string; icon: keyof typeof Ionicons.glyphMap }[] = [
    { key: 'configs', label: t('configs', currentLang), icon: 'layers-outline' },
    { key: 'proxies', label: t('proxies', currentLang), icon: 'key-outline' },
    { key: 'admin', label: t('admin_panel', currentLang), icon: 'shield-outline' },
  ];

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.bg }]} edges={['top']}>
      <StatusBar barStyle={darkMode ? 'light-content' : 'dark-content'} />

      <View style={[styles.topBar, { backgroundColor: colors.bg }]}>
        <View style={[styles.logoIcon, { backgroundColor: `${colors.primary}1A` }]}>
          <Ionicons name="git-network-outline" size={20} color={colors.primary} />
        </View>
        <View style={{ flex: 1, marginLeft: 12 }}>
          <Text style={[styles.appTitle, { color: colors.text, fontFamily: fontFamilyBold }]}>{t('app_title', currentLang)}</Text>
          <Text style={[styles.appSubtitle, { color: colors.subText, fontFamily }]}>{t('subtitle', currentLang)}</Text>
        </View>
        <TouchableOpacity
          onPress={() => setLanguage(currentLang === 'EN' ? 'FA' : 'EN')}
          style={styles.actionBtn}
        >
          <Ionicons name="language-outline" size={20} color={colors.primary} />
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setDarkMode(!darkMode)} style={styles.actionBtn}>
          <Ionicons name={darkMode ? 'sunny-outline' : 'moon-outline'} size={20} color={colors.primary} />
        </TouchableOpacity>
        <TouchableOpacity onPress={openSettings} style={styles.actionBtn}>
          <Ionicons name="settings-outline" size={20} color={colors.primary} />
        </TouchableOpacity>
      </View>

      <View style={styles.content}>
        {loading ? (
          <View style={styles.centered}>
            <ActivityIndicator size="large" color={colors.primary} />
          </View>
        ) : error ? (
          <View style={styles.centered}>
            <Ionicons name="alert-circle-outline" size={48} color={colors.error} />
            <Text style={[styles.errorText, { color: colors.text, fontFamily }]}>{error}</Text>
            <TouchableOpacity
              style={[styles.retryBtn, { backgroundColor: colors.primary }]}
              onPress={() => loadData()}
            >
              <Text style={{ color: colors.bg, fontWeight: '600', fontFamily }}>{t('retry', currentLang)}</Text>
            </TouchableOpacity>
          </View>
        ) : tab === 'configs' ? (
          <FlatList
            data={configs}
            keyExtractor={(item) => `config_${item.id}`}
            renderItem={({ item, index }) => (
              <ConfigItemCard
                config={item}
                colors={colors}
                index={index}
                onCopy={() => {
                  Clipboard.setString(item.raw_content);
                  Alert.alert('Copied!');
                }}
                onImport={() => {
                  Linking.openURL(`hiddify://import/#${item.raw_content}`).catch(() =>
                    Alert.alert('Hiddify not installed')
                  );
                }}
              />
            )}
            contentContainerStyle={{ paddingVertical: 8 }}
            ListHeaderComponent={
              <TipBanner text={t('configs_tip', currentLang)} icon="information-circle-outline" color={colors.primary} fontFamily={fontFamily} />
            }
            ListFooterComponent={
              configHasMore ? (
                <TouchableOpacity
                  style={[styles.loadMoreBtn, { borderColor: colors.border }]}
                  onPress={loadMoreConfigs}
                  disabled={loadingMore}
                >
                  {loadingMore ? (
                    <ActivityIndicator size="small" color={colors.primary} />
                  ) : (
                    <Text style={[styles.loadMoreText, { color: colors.primary, fontFamily }]}>
                      {t('load_more', currentLang)}
                    </Text>
                  )}
                </TouchableOpacity>
              ) : configs.length > 0 ? (
                <Text style={[styles.endText, { color: colors.subText, fontFamily }]}>{t('end_of_list', currentLang)}</Text>
              ) : null
            }
            ListEmptyComponent={
              <Text style={[styles.emptyText, { color: colors.subText, fontFamily }]}>{t('empty_db', currentLang)}</Text>
            }
          />
        ) : tab === 'proxies' ? (
          <FlatList
            data={proxies}
            keyExtractor={(item) => `proxy_${item.id}`}
            renderItem={({ item, index }) => (
              <ProxyItemCard
                proxy={item}
                colors={colors}
                index={index}
                onCopy={() => {
                  const link = `tg://proxy?server=${item.server}&port=${item.port}&secret=${item.secret}`;
                  Clipboard.setString(link);
                  Alert.alert('Copied!');
                }}
                onConnect={() => {
                  const link = `tg://proxy?server=${item.server}&port=${item.port}&secret=${item.secret}`;
                  Linking.openURL(link).catch(() =>
                    Alert.alert('Telegram not installed')
                  );
                }}
              />
            )}
            contentContainerStyle={{ paddingVertical: 8 }}
            ListHeaderComponent={
              <TipBanner text={t('proxies_tip', currentLang)} icon="flash-outline" color={colors.primary} fontFamily={fontFamily} />
            }
            ListFooterComponent={
              proxyHasMore ? (
                <TouchableOpacity
                  style={[styles.loadMoreBtn, { borderColor: colors.border }]}
                  onPress={loadMoreProxies}
                  disabled={loadingMore}
                >
                  {loadingMore ? (
                    <ActivityIndicator size="small" color={colors.primary} />
                  ) : (
                    <Text style={[styles.loadMoreText, { color: colors.primary, fontFamily }]}>
                      {t('load_more', currentLang)}
                    </Text>
                  )}
                </TouchableOpacity>
              ) : proxies.length > 0 ? (
                <Text style={[styles.endText, { color: colors.subText, fontFamily }]}>{t('end_of_list', currentLang)}</Text>
              ) : null
            }
            ListEmptyComponent={
              <Text style={[styles.emptyText, { color: colors.subText, fontFamily }]}>{t('empty_db', currentLang)}</Text>
            }
          />
        ) : (
          <AdminPanel
            colors={colors}
            language={currentLang}
            channels={channels}
            subscriptions={subscriptions}
            onRefresh={loadData}
          />
        )}
      </View>

      <View style={[styles.bottomBar, { backgroundColor: colors.surface, borderTopColor: colors.border, paddingBottom: insets.bottom || 8 }]}>
        {tabs.map((t) => (
          <TouchableOpacity
            key={t.key}
            style={styles.tabItem}
            onPress={() => setTab(t.key)}
          >
            <Ionicons
              name={tab === t.key ? t.icon.replace('-outline', '') : t.icon}
              size={22}
              color={tab === t.key ? colors.primary : colors.subText}
            />
            <Text style={[styles.tabLabel, { color: tab === t.key ? colors.primary : colors.subText, fontFamily }]}>
              {t.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Modal visible={showSettings} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.card }]}>
            <Ionicons name="server-outline" size={28} color={colors.primary} style={{ alignSelf: 'center' }} />
            <Text style={[styles.modalTitle, { color: colors.text, fontFamily: fontFamilyBold }]}>{t('db_setup', currentLang)}</Text>
            <Text style={[styles.modalDesc, { color: colors.subText, fontFamily }]}>{t('dialog_desc', currentLang)}</Text>
            <TextInput
              style={[styles.modalInput, { color: colors.text, borderColor: colors.border, fontFamily }]}
              placeholder="Supabase URL"
              placeholderTextColor={colors.subText}
              value={settingsUrl}
              onChangeText={setSettingsUrl}
              autoCapitalize="none"
            />
            <TextInput
              style={[styles.modalInput, { color: colors.text, borderColor: colors.border, fontFamily }]}
              placeholder="API Key"
              placeholderTextColor={colors.subText}
              value={settingsKey}
              onChangeText={setSettingsKey}
              autoCapitalize="none"
              secureTextEntry
            />
            <View style={styles.modalActions}>
              <TouchableOpacity
                style={[styles.modalBtn, { borderWidth: 1, borderColor: colors.border }]}
                onPress={() => setShowSettings(false)}
              >
                <Text style={{ color: colors.text, fontFamily }}>{t('cancel', currentLang)}</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalBtn, { backgroundColor: colors.primary }]}
                onPress={handleApplySettings}
              >
                <Text style={{ color: colors.bg, fontWeight: '600', fontFamily }}>{t('apply', currentLang)}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

function TipBanner({ text, icon, color, fontFamily }: { text: string; icon: keyof typeof Ionicons.glyphMap; color: string; fontFamily?: string }) {
  return (
    <View style={[styles.tipBanner, { backgroundColor: `${color}12` }]}>
      <Ionicons name={icon} size={16} color={color} />
      <Text style={[styles.tipText, { color: `${color}BF`, fontFamily }]}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  logoIcon: {
    width: 40,
    height: 40,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  appTitle: { fontSize: 20, fontWeight: '900' },
  appSubtitle: { fontSize: 11 },
  actionBtn: {
    width: 36,
    height: 36,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
    marginLeft: 4,
  },
  content: { flex: 1 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center', gap: 12 },
  errorText: { fontSize: 14, textAlign: 'center', maxWidth: 280 },
  retryBtn: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 10, marginTop: 8 },
  emptyText: { textAlign: 'center', marginTop: 40, fontSize: 14 },
  tipBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 16,
    marginBottom: 8,
    padding: 12,
    borderRadius: 12,
    gap: 8,
  },
  tipText: { fontSize: 12, flex: 1 },
  loadMoreBtn: {
    marginHorizontal: 16,
    marginTop: 8,
    marginBottom: 16,
    paddingVertical: 12,
    borderRadius: 10,
    borderWidth: 1,
    alignItems: 'center',
  },
  loadMoreText: { fontSize: 13, fontWeight: '600' },
  endText: { textAlign: 'center', fontSize: 11, marginTop: 8, marginBottom: 16 },
  bottomBar: {
    flexDirection: 'row',
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingTop: 8,
  },
  tabItem: {
    flex: 1,
    alignItems: 'center',
    gap: 2,
  },
  tabLabel: { fontSize: 11 },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    padding: 24,
  },
  modalCard: {
    borderRadius: 16,
    padding: 24,
    gap: 12,
  },
  modalTitle: { fontSize: 18, fontWeight: '700', textAlign: 'center' },
  modalDesc: { fontSize: 12, textAlign: 'center', marginBottom: 4 },
  modalInput: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
  },
  modalActions: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 4,
  },
  modalBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    alignItems: 'center',
  },
});
