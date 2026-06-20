import * as FileSystem from 'expo-file-system';

const STORAGE_FILE = `${FileSystem.documentDirectory}haveall_config.json`;

interface StoredConfig {
  supabaseUrl: string;
  supabaseKey: string;
}

export async function loadStoredConfig(): Promise<StoredConfig | null> {
  try {
    const info = await FileSystem.getInfoAsync(STORAGE_FILE);
    if (!info.exists) return null;
    const content = await FileSystem.readAsStringAsync(STORAGE_FILE);
    return JSON.parse(content);
  } catch {
    return null;
  }
}

export async function saveStoredConfig(url: string, key: string): Promise<void> {
  try {
    const data: StoredConfig = { supabaseUrl: url, supabaseKey: key };
    await FileSystem.writeAsStringAsync(STORAGE_FILE, JSON.stringify(data));
  } catch {}
}
