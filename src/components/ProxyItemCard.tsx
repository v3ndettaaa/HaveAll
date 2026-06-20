import React, { useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SupabaseProxy } from '../api/supabase';
import { Colors } from '../theme/colors';
import { useLang } from '../i18n/LangContext';
import { t } from '../i18n/translations';

interface Props {
  proxy: SupabaseProxy;
  colors: Colors;
  index: number;
  onCopy: () => void;
  onConnect: () => void;
}

export default function ProxyItemCard({ proxy, colors, index, onCopy, onConnect }: Props) {
  const { language, fontFamily, fontFamilyMedium } = useLang();
  const ping = proxy.ping ?? null;
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(30)).current;

  useEffect(() => {
    const delay = Math.min(index, 8) * 80;
    Animated.parallel([
      Animated.timing(fadeAnim, { toValue: 1, duration: 400, delay, useNativeDriver: true }),
      Animated.timing(slideAnim, { toValue: 0, duration: 400, delay, useNativeDriver: true }),
    ]).start();
  }, []);

  const getPingColor = () => {
    if (ping === null) return colors.subText;
    if (ping < 200) return '#10B981';
    if (ping < 500) return '#F59E0B';
    return '#EF4444';
  };

  return (
    <Animated.View style={{ opacity: fadeAnim, transform: [{ translateY: slideAnim }] }}>
      <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.header}>
          <View style={styles.headerLeft}>
            <View style={[styles.iconBox, { backgroundColor: `${colors.primary}1F` }]}>
              <Ionicons name="radio-outline" size={18} color={colors.primary} />
            </View>
            <Text style={[styles.title, { color: colors.text, fontFamily: fontFamilyMedium }]}>MTProto</Text>
          </View>
          <View style={styles.headerRight}>
            <View style={[styles.pingBadge, { backgroundColor: `${getPingColor()}20` }]}>
              <View style={[styles.pingDot, { backgroundColor: getPingColor() }]} />
              <Text style={[styles.pingText, { color: getPingColor(), fontFamily }]}>
                {ping !== null ? `${ping} ${t('ping_ms', language)}` : t('ping_timeout', language)}
              </Text>
            </View>
            <View style={[styles.idBadge, { backgroundColor: `${colors.primary}1F` }]}>
              <Text style={[styles.idText, { color: colors.primary, fontFamily }]}>#{proxy.id}</Text>
            </View>
          </View>
        </View>

        <View style={[styles.details, { backgroundColor: colors.bg }]}>
          <DetailRow label={t('server', language)} value={proxy.server} colors={colors} fontFamily={fontFamily} />
          <DetailRow label={t('port', language)} value={String(proxy.port)} colors={colors} fontFamily={fontFamily} />
          <DetailRow label={t('secret', language)} value={proxy.secret} colors={colors} fontFamily={fontFamily} />
        </View>

        <View style={styles.actions}>
          <TouchableOpacity
            style={[styles.btn, { borderColor: colors.border }]}
            onPress={onCopy}
          >
            <Ionicons name="copy-outline" size={14} color={colors.text} />
            <Text style={[styles.btnText, { color: colors.text, fontFamily }]}>{t('copy', language)}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.btn, { backgroundColor: colors.primary }]}
            onPress={onConnect}
          >
            <Ionicons name="send-outline" size={14} color={colors.bg} />
            <Text style={[styles.btnText, { color: colors.bg, fontFamily }]}>Telegram</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Animated.View>
  );
}

function DetailRow({ label, value, colors, fontFamily }: { label: string; value: string; colors: Colors; fontFamily?: string }) {
  return (
    <View style={styles.detailRow}>
      <Text style={[styles.detailLabel, { color: colors.subText, fontFamily }]}>{label}</Text>
      <Text
        style={[styles.detailValue, { color: colors.text, fontFamily }]}
        numberOfLines={1}
        ellipsizeMode="tail"
      >
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: 16,
    borderWidth: 1,
    padding: 16,
    marginHorizontal: 16,
    marginBottom: 10,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  iconBox: {
    width: 32,
    height: 32,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    fontSize: 14,
    fontWeight: '700',
  },
  pingBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  pingDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  pingText: {
    fontSize: 10,
    fontWeight: '600',
  },
  idBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  idText: {
    fontSize: 10,
    fontWeight: '700',
  },
  details: {
    borderRadius: 10,
    padding: 12,
    marginTop: 12,
    gap: 6,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  detailLabel: {
    fontSize: 11,
    fontWeight: '500',
  },
  detailValue: {
    fontSize: 11,
    maxWidth: 180,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 12,
  },
  btn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 5,
    paddingVertical: 9,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  btnText: {
    fontSize: 12,
  },
});
