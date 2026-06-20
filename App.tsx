import React, { useState } from 'react';
import { Text, Platform } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { useFonts } from 'expo-font';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';
import MainScreen from './src/screens/MainScreen';

SplashScreen.preventAutoHideAsync();

export default function App() {
  const [fontsLoaded] = useFonts({
    'Vazir-Regular': require('./assets/fonts/Vazir-Regular.ttf'),
    'Vazir-Medium': require('./assets/fonts/Vazir-Medium.ttf'),
    'Vazir-Bold': require('./assets/fonts/Vazir-Bold.ttf'),
  });

  useEffect(() => {
    if (fontsLoaded) {
      SplashScreen.hideAsync();
    }
  }, [fontsLoaded]);

  if (!fontsLoaded) return null;

  return (
    <SafeAreaProvider>
      <MainScreen />
    </SafeAreaProvider>
  );
}
