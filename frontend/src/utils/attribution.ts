const ATTRIBUTION_KEY = 'wuxing_attribution';
const DEFAULT_SHARED_CAMPAIGN = 'shared-result';

export interface Attribution {
  channel?: string;
  campaign?: string;
}

export function getAttribution(): Attribution {
  const current = captureAttributionFromLocation();
  if (current.channel || current.campaign) {
    return current;
  }
  return readStoredAttribution();
}

export function withShareAttribution(url: string): string {
  return withAttribution(url, {
    channel: 'share',
    campaign: 'result-card',
  });
}

function captureAttributionFromLocation(): Attribution {
  if (typeof window === 'undefined') {
    return {};
  }
  const params = new URLSearchParams(window.location.search);
  const fromShortLink = params.has('sc');
  const channel = normalize(params.get('channel') ?? params.get('utm_source') ?? params.get('source'));
  const campaign = normalize(params.get('campaign') ?? params.get('utm_campaign'));
  const attribution: Attribution = {
    channel: channel ?? (fromShortLink ? 'shortlink' : undefined),
    campaign: campaign ?? (fromShortLink ? DEFAULT_SHARED_CAMPAIGN : undefined),
  };
  if (attribution.channel || attribution.campaign) {
    localStorage.setItem(ATTRIBUTION_KEY, JSON.stringify(attribution));
  }
  return attribution;
}

function readStoredAttribution(): Attribution {
  try {
    const raw = localStorage.getItem(ATTRIBUTION_KEY);
    return raw ? (JSON.parse(raw) as Attribution) : {};
  } catch {
    return {};
  }
}

function withAttribution(url: string, attribution: Required<Attribution>) {
  try {
    const target = new URL(url, window.location.origin);
    target.searchParams.set('channel', attribution.channel);
    target.searchParams.set('campaign', attribution.campaign);
    return target.toString();
  } catch {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}channel=${encodeURIComponent(attribution.channel)}&campaign=${encodeURIComponent(attribution.campaign)}`;
  }
}

function normalize(value: string | null): string | undefined {
  if (!value) {
    return undefined;
  }
  const normalized = value.trim().replace(/\s+/g, '-').toLowerCase();
  if (!normalized) {
    return undefined;
  }
  return normalized.slice(0, 64);
}
