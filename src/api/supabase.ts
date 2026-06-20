export interface SupabaseConfig {
  id: number;
  type: string;
  raw_content: string;
  remarks: string | null;
  created_at: string;
  ping?: number | null;
}

export interface SupabaseProxy {
  id: number;
  server: string;
  port: number;
  secret: string;
  tg_link: string;
  created_at: string;
  ping?: number | null;
}

export interface SupabaseChannel {
  id: number;
  username: string;
  created_at: string;
}

export interface SupabaseSubscription {
  id: number;
  url: string;
  remarks: string | null;
  created_at: string;
}

export function extractServerFromConfig(raw: string): { host: string; port: number } | null {
  try {
    const cleaned = raw.trim();
    const afterProtocol = cleaned.replace(/^(vless|vmess|trojan|ss|ssr|hysteria|tuic):\/\//, '');
    const atIndex = afterProtocol.indexOf('@');
    if (atIndex < 0) return null;
    const hostPort = afterProtocol.substring(atIndex + 1);
    const questionIdx = hostPort.indexOf('?');
    const hp = questionIdx > 0 ? hostPort.substring(0, questionIdx) : hostPort;
    const colonIdx = hp.lastIndexOf(':');
    if (colonIdx < 0) return null;
    const host = hp.substring(0, colonIdx);
    const port = parseInt(hp.substring(colonIdx + 1), 10);
    if (!host || isNaN(port)) return null;
    return { host, port };
  } catch {
    return null;
  }
}

export function measurePing(host: string, port: number, timeoutMs = 4000): Promise<number | null> {
  return new Promise((resolve) => {
    const start = performance.now();
    let done = false;
    const finish = (ms: number) => {
      if (done) return;
      done = true;
      try { ws.close(); } catch {}
      clearTimeout(timer);
      resolve(ms);
    };
    const timer = setTimeout(() => finish(Math.round(performance.now() - start)), timeoutMs);
    const ws = new WebSocket(`wss://${host}:${port}`);
    ws.onopen = () => finish(Math.round(performance.now() - start));
    ws.onerror = () => finish(Math.round(performance.now() - start));
    ws.onclose = () => finish(Math.round(performance.now() - start));
  });
}

class SupabaseClient {
  private baseUrl = '';
  private apiKey = '';

  configure(url: string, key: string) {
    this.baseUrl = url.endsWith('/') ? url : `${url}/`;
    this.apiKey = key;
  }

  private headers() {
    return {
      apikey: this.apiKey,
      Authorization: `Bearer ${this.apiKey}`,
      'Content-Type': 'application/json',
    };
  }

  async getConfigs(limit = 20, offset = 0): Promise<SupabaseConfig[]> {
    const res = await fetch(
      `${this.baseUrl}rest/v1/configs?select=*&limit=${limit}&offset=${offset}&order=created_at.desc`,
      { headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async getProxies(limit = 20, offset = 0): Promise<SupabaseProxy[]> {
    const res = await fetch(
      `${this.baseUrl}rest/v1/proxies?select=*&limit=${limit}&offset=${offset}&order=created_at.desc`,
      { headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async getMonitoredChannels(): Promise<SupabaseChannel[]> {
    const res = await fetch(
      `${this.baseUrl}rest/v1/monitored_channels?select=*&order=username.asc`,
      { headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async addMonitoredChannel(username: string): Promise<SupabaseChannel[]> {
    const res = await fetch(`${this.baseUrl}rest/v1/monitored_channels`, {
      method: 'POST',
      headers: { ...this.headers(), Prefer: 'return=representation' },
      body: JSON.stringify({ username }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async deleteMonitoredChannel(username: string) {
    const res = await fetch(
      `${this.baseUrl}rest/v1/monitored_channels?username=eq.${username}`,
      { method: 'DELETE', headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
  }

  async getSubscriptions(): Promise<SupabaseSubscription[]> {
    const res = await fetch(
      `${this.baseUrl}rest/v1/subscriptions?select=*&order=remarks.asc`,
      { headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async addSubscription(url: string, remarks: string): Promise<SupabaseSubscription[]> {
    const res = await fetch(`${this.baseUrl}rest/v1/subscriptions`, {
      method: 'POST',
      headers: { ...this.headers(), Prefer: 'return=representation' },
      body: JSON.stringify({ url, remarks }),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }

  async deleteSubscription(url: string) {
    const res = await fetch(
      `${this.baseUrl}rest/v1/subscriptions?url=eq.${encodeURIComponent(url)}`,
      { method: 'DELETE', headers: this.headers() }
    );
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
  }
}

export const api = new SupabaseClient();
