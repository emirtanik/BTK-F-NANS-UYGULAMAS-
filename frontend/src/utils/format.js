export const ASSET_NAMES = {
  BTC: 'Bitcoin', ETH: 'Ethereum', BNB: 'BNB', SOL: 'Solana', XRP: 'XRP', ADA: 'Cardano',
  GRAM_ALTIN: 'Gram Altın', CEYREK_ALTIN: 'Çeyrek Altın', YARIM_ALTIN: 'Yarım Altın',
  TAM_ALTIN: 'Tam Altın', GRAM_GUMUS: 'Gram Gümüş',
  USD: 'Dolar', EUR: 'Euro', GBP: 'Sterlin',
};

export const ASSET_GROUP = {
  BTC: 'Kripto Para', ETH: 'Kripto Para', BNB: 'Kripto Para', SOL: 'Kripto Para',
  XRP: 'Kripto Para', ADA: 'Kripto Para',
  GRAM_ALTIN: 'Kıymetli Maden', CEYREK_ALTIN: 'Kıymetli Maden', YARIM_ALTIN: 'Kıymetli Maden',
  TAM_ALTIN: 'Kıymetli Maden', GRAM_GUMUS: 'Kıymetli Maden',
  USD: 'Döviz', EUR: 'Döviz', GBP: 'Döviz',
};

export const getAssetName = (s) => ASSET_NAMES[s] || s;
export const getAssetGroup = (s) => ASSET_GROUP[s] || 'Diğer';

export function formatTry(value, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) return '—';
  return Number(value).toLocaleString('tr-TR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

export function formatCompact(value) {
  if (value === null || value === undefined || isNaN(value)) return '—';
  const num = Math.abs(Number(value));
  if (num >= 1e9) return (Number(value) / 1e9).toLocaleString('tr-TR', { maximumFractionDigits: 2 }) + ' Mr';
  if (num >= 1e6) return (Number(value) / 1e6).toLocaleString('tr-TR', { maximumFractionDigits: 2 }) + ' Mn';
  if (num >= 1e3) return (Number(value) / 1e3).toLocaleString('tr-TR', { maximumFractionDigits: 2 }) + ' B';
  return formatTry(value, 2);
}

export function formatPercent(value, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) return '—';
  const num = Number(value);
  const sign = num > 0 ? '+' : '';
  return sign + num.toLocaleString('tr-TR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }) + '%';
}

export function formatAmount(value, symbol) {
  if (value === null || value === undefined || isNaN(value)) return '—';
  const group = getAssetGroup(symbol);
  const decimals = group === 'Kripto Para' ? 6 : 2;
  return Number(value).toLocaleString('tr-TR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: decimals,
  });
}

export function formatDate(date) {
  if (!date) return '—';
  return new Date(date).toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' });
}
