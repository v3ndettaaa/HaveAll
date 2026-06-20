import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { DarkColors } from '../theme/colors';

interface Props {
  onFinished: () => void;
}

export default function SplashScreen({ onFinished }: Props) {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    setVisible(true);
    const timer = setTimeout(onFinished, 2200);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View style={styles.container}>
      <View style={[styles.inner, { opacity: visible ? 1 : 0 }]}>
        <View style={styles.logoWrap}>
          <View style={styles.ring} />
          <View style={styles.glowCircle}>
            <Text style={styles.hubIcon}>⬡</Text>
          </View>
        </View>
        <Text style={styles.title}>HaveAll</Text>
        <Text style={styles.subtitle}>همه برای تو</Text>
        <View style={styles.progressBar}>
          <View style={styles.progressFill} />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: DarkColors.bg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  inner: {
    alignItems: 'center',
  },
  logoWrap: {
    width: 120,
    height: 120,
    justifyContent: 'center',
    alignItems: 'center',
  },
  ring: {
    position: 'absolute',
    width: 120,
    height: 120,
    borderRadius: 60,
    borderWidth: 3,
    borderColor: DarkColors.primary,
    opacity: 0.6,
  },
  glowCircle: {
    width: 82,
    height: 82,
    borderRadius: 41,
    backgroundColor: `${DarkColors.primary}22`,
    justifyContent: 'center',
    alignItems: 'center',
  },
  hubIcon: {
    fontSize: 40,
    color: DarkColors.primary,
  },
  title: {
    fontSize: 28,
    fontWeight: '900',
    color: DarkColors.text,
    fontFamily: 'Vazir-Bold',
    letterSpacing: 2,
    marginTop: 28,
  },
  subtitle: {
    fontSize: 14,
    color: DarkColors.primary,
    fontFamily: 'Vazir-Regular',
    fontWeight: '500',
    marginTop: 4,
  },
  progressBar: {
    width: 120,
    height: 2,
    borderRadius: 1,
    backgroundColor: `${DarkColors.primary}26`,
    marginTop: 48,
    overflow: 'hidden',
  },
  progressFill: {
    width: '60%',
    height: '100%',
    backgroundColor: DarkColors.primary,
    borderRadius: 1,
  },
});
