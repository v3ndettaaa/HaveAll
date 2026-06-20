import React, { createContext, useContext, useState, useCallback } from 'react';
import { AppLanguage } from './translations';

interface LangContextValue {
  language: AppLanguage;
  setLanguage: (lang: AppLanguage) => void;
  fontFamily: string | undefined;
  fontFamilyMedium: string | undefined;
  fontFamilyBold: string | undefined;
}

const LangContext = createContext<LangContextValue>({
  language: 'EN',
  setLanguage: () => {},
  fontFamily: 'Vazir-Regular',
  fontFamilyMedium: 'Vazir-Medium',
  fontFamilyBold: 'Vazir-Bold',
});

export function LangProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguage] = useState<AppLanguage>('EN');
  const value: LangContextValue = {
    language,
    setLanguage,
    fontFamily: 'Vazir-Regular',
    fontFamilyMedium: 'Vazir-Medium',
    fontFamilyBold: 'Vazir-Bold',
  };
  return <LangContext.Provider value={value}>{children}</LangContext.Provider>;
}

export function useLang() {
  return useContext(LangContext);
}
