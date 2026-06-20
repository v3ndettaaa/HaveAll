import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { SupabaseChannel, SupabaseSubscription, api } from '../api/supabase';
import { Colors } from '../theme/colors';
import { AppLanguage, t } from '../i18n/translations';
import { useLang } from '../i18n/LangContext';

interface Props {
  colors: Colors;
  language: AppLanguage;
  channels: SupabaseChannel[];
  subscriptions: SupabaseSubscription[];
  onRefresh: () => void;
}

export default function AdminPanel({ colors, language, channels, subscriptions, onRefresh }: Props) {
  const { fontFamily } = useLang();
  const [adminTab, setAdminTab] = useState<'channels' | 'subscriptions'>('channels');
  const [channelInput, setChannelInput] = useState('');
  const [subUrl, setSubUrl] = useState('');
  const [subLabel, setSubLabel] = useState('');

  const handleAddChannel = async () => {
    const name = channelInput.replace('@', '').trim();
    if (!name) return;
    try {
      await api.addMonitoredChannel(name);
      setChannelInput('');
      onRefresh();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    }
  };

  const handleDeleteChannel = async (username: string) => {
    try {
      await api.deleteMonitoredChannel(username);
      onRefresh();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    }
  };

  const handleAddSub = async () => {
    if (!subUrl.trim() || !subLabel.trim()) return;
    try {
      await api.addSubscription(subUrl.trim(), subLabel.trim());
      setSubUrl('');
      setSubLabel('');
      onRefresh();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    }
  };

  const handleDeleteSub = async (url: string) => {
    try {
      await api.deleteSubscription(url);
      onRefresh();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={[styles.infoBanner, { backgroundColor: `${colors.primary}14` }]}>
        <Ionicons name="shield-checkmark-outline" size={22} color={colors.primary} />
        <View style={{ marginLeft: 10, flex: 1 }}>
          <Text style={[styles.infoText, { color: colors.text, fontFamily }]}>{t('admin_desc', language)}</Text>
          <Text style={[styles.infoSub, { color: colors.primary, fontFamily }]}>{t('sync_interval', language)}</Text>
        </View>
      </View>

      {/* Sub-toggle */}
      <View style={[styles.toggleRow, { backgroundColor: colors.surface, marginHorizontal: 16 }]}>
        <TouchableOpacity
          style={[styles.toggleBtn, adminTab === 'channels' && { backgroundColor: colors.primary }]}
          onPress={() => setAdminTab('channels')}
        >
          <Ionicons name="radio-outline" size={14} color={adminTab === 'channels' ? colors.bg : colors.subText} />
          <Text style={[styles.toggleText, { color: adminTab === 'channels' ? colors.bg : colors.subText, fontFamily }]}>
            {t('monitored_list', language)}
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.toggleBtn, adminTab === 'subscriptions' && { backgroundColor: colors.primary }]}
          onPress={() => setAdminTab('subscriptions')}
        >
          <Ionicons name="link-outline" size={14} color={adminTab === 'subscriptions' ? colors.bg : colors.subText} />
          <Text style={[styles.toggleText, { color: adminTab === 'subscriptions' ? colors.bg : colors.subText, fontFamily }]}>
            {t('sub_list', language)}
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={{ paddingBottom: 32 }}>
        {adminTab === 'channels' ? (
          <>
            <View style={[styles.inputCard, { backgroundColor: colors.surface, marginTop: 12 }]}>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.border, fontFamily, textAlign: language === 'FA' ? 'right' : 'left' }]}
                placeholder={t('chan_placeholder', language)}
                placeholderTextColor={colors.subText}
                value={channelInput}
                onChangeText={setChannelInput}
              />
              <TouchableOpacity style={[styles.addBtn, { backgroundColor: colors.primary }]} onPress={handleAddChannel}>
                <Ionicons name="add" size={16} color={colors.bg} />
                <Text style={[styles.addBtnText, { color: colors.bg, fontFamily }]}>{t('add_chan', language)}</Text>
              </TouchableOpacity>
            </View>

            {channels.length === 0 ? (
              <Text style={[styles.emptyText, { color: colors.subText, fontFamily }]}>{t('no_channels', language)}</Text>
            ) : (
              channels.map((ch) => (
                <View key={ch.id} style={[styles.listItem, { borderBottomColor: `${colors.border}80` }]}>
                  <Ionicons name="radio-outline" size={18} color={colors.primary} />
                  <View style={{ flex: 1, marginLeft: 10 }}>
                    <Text style={[styles.listTitle, { color: colors.text, fontFamily }]}>@{ch.username}</Text>
                    <Text style={[styles.listSub, { color: colors.subText, fontFamily }]}>ID #{ch.id}</Text>
                  </View>
                  <TouchableOpacity onPress={() => handleDeleteChannel(ch.username)}>
                    <Ionicons name="trash-outline" size={18} color={colors.error} />
                  </TouchableOpacity>
                </View>
              ))
            )}
          </>
        ) : (
          <>
            <View style={[styles.inputCard, { backgroundColor: colors.surface, marginTop: 12 }]}>
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.border, fontFamily, textAlign: language === 'FA' ? 'right' : 'left' }]}
                placeholder={t('sub_url_label', language)}
                placeholderTextColor={colors.subText}
                value={subUrl}
                onChangeText={setSubUrl}
              />
              <TextInput
                style={[styles.input, { color: colors.text, borderColor: colors.border, fontFamily, textAlign: language === 'FA' ? 'right' : 'left', marginTop: 8 }]}
                placeholder={t('sub_label_label', language)}
                placeholderTextColor={colors.subText}
                value={subLabel}
                onChangeText={setSubLabel}
              />
              <TouchableOpacity style={[styles.addBtn, { backgroundColor: colors.primary, marginTop: 8 }]} onPress={handleAddSub}>
                <Ionicons name="add" size={16} color={colors.bg} />
                <Text style={[styles.addBtnText, { color: colors.bg, fontFamily }]}>{t('add_sub', language)}</Text>
              </TouchableOpacity>
            </View>

            {subscriptions.length === 0 ? (
              <Text style={[styles.emptyText, { color: colors.subText, fontFamily }]}>{t('no_subs', language)}</Text>
            ) : (
              subscriptions.map((sub) => (
                <View key={sub.id} style={[styles.listItem, { borderBottomColor: `${colors.border}80` }]}>
                  <Ionicons name="link-outline" size={18} color={colors.primary} />
                  <View style={{ flex: 1, marginLeft: 10 }}>
                    <Text style={[styles.listTitle, { color: colors.text, fontFamily }]} numberOfLines={1}>
                      {sub.remarks || 'Subscription'}
                    </Text>
                    <Text style={[styles.listSub, { color: colors.subText, fontFamily }]} numberOfLines={1}>
                      {sub.url}
                    </Text>
                  </View>
                  <TouchableOpacity onPress={() => handleDeleteSub(sub.url)}>
                    <Ionicons name="trash-outline" size={18} color={colors.error} />
                  </TouchableOpacity>
                </View>
              ))
            )}
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  infoBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    margin: 16,
    borderRadius: 14,
  },
  infoText: { fontSize: 12 },
  infoSub: { fontSize: 11, fontWeight: '700', marginTop: 2 },
  toggleRow: {
    flexDirection: 'row',
    marginHorizontal: 16,
    borderRadius: 10,
    padding: 3,
  },
  toggleBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 8,
    borderRadius: 8,
  },
  toggleText: {
    fontSize: 11,
    fontWeight: '600',
  },
  inputCard: {
    marginHorizontal: 16,
    marginVertical: 4,
    padding: 14,
    borderRadius: 14,
  },
  input: {
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 13,
  },
  addBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 10,
    marginTop: 10,
  },
  addBtnText: { fontSize: 13, fontWeight: '600' },
  emptyText: {
    fontSize: 12,
    marginHorizontal: 20,
    marginVertical: 10,
  },
  listItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderBottomWidth: 0.5,
  },
  listTitle: { fontSize: 13, fontWeight: '600' },
  listSub: { fontSize: 10, marginTop: 2 },
});
