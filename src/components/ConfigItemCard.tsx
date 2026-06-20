import React, { useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Animated } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SupabaseConfig } from '../api/supabase';
import { Colors } from '../theme/colors';
import { useLang } from '../i18n/LangContext';
import { t } from '../i18n/translations';

interface Props {
  config: SupabaseConfig;
  colors: Colors;
  index: number;
  onCopy: () => void;
  onImport: () => void;
}

const protocolColors: Record<string, string> = {
  vmess: '#6366F1',
  vless: '#0EA5E9',
  trojan: '#EC4899',
  shadowsocks: '#F59E0B',
  hysteria: '#10B981',
  tuic: '#8B5CF6',
};

export default function ConfigItemCard({ config, colors, index, onCopy, onImport }: Props) {
  const { language, fontFamily, fontFamilyMedium, fontFamilyBold } = useLang();
  const protoColor = protocolColors[config.type?.toLowerCase()] || '#6366F1';
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
    if (config.ping === null || config.ping === undefined) return colors.subText;
    if (config.ping < 200) return '#10B981';
    if (config.ping < 500) return '#F59E0B';
    return '#EF4444';
  };

  return (
    <Animated.View style={{ opacity: fadeAnim, transform: [{ translateY: slideAnim }] }}>
      <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
        <View style={styles.header}>
          <View style={[styles.badge, { backgroundColor: `${protoColor}22` }]}>
            <Text style={[styles.badgeText, { color: protoColor, fontFamily: fontFamilyMedium }]}>{config.type?.toUpperCase()}</Text>
          </View>
          <View style={styles.headerRight}>
            <View style={[styles.pingBadge, { backgroundColor: `${getPingColor()}20` }]}>
              <View style={[styles.pingDot, { backgroundColor: getPingColor() }]} />
              <Text style={[styles.pingText, { color: getPingColor(), fontFamily }]}>
                {config.ping !== null && config.ping !== undefined ? `${config.ping} ${t('ping_ms', language)}` : '—'}
              </Text>
            </View>
            <Text style={[styles.remarks, { color: colors.subText, fontFamily }]}>
              {config.remarks ? `${config.remarks}` : `#${config.id}`}
            </Text>
          </View>
        </View>

        <View style={[styles.contentBox, { backgroundColor: colors.bg }]}>
          <Text
            style={[styles.content, { color: `${colors.text}A6`, fontFamily }]}
            numberOfLines={2}
            ellipsizeMode="tail"
          >
            {config.raw_content}
          </Text>
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
            onPress={onImport}
          >
            <Ionicons name="download-outline" size={14} color={colors.bg} />
            <Text style={[styles.btnText, { color: colors.bg, fontFamily }]}>Hiddify</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Animated.View>
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
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '700',
    letterSpacing: 1,
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
  remarks: {
    fontSize: 11,
  },
  contentBox: {
    borderRadius: 10,
    padding: 12,
    marginTop: 12,
  },
  content: {
    fontSize: 10,
    lineHeight: 15,
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
