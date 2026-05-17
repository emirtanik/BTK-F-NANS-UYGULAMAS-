export const getApiErrorMessage = (err, fallback = 'Bir hata oluştu.') => {
  if (!err?.response) {
    return 'Sunucuya bağlanılamadı. Backend çalışıyor mu kontrol edin.';
  }

  const { status, data } = err.response;

  if (status === 401 || status === 403) {
    return data?.message || 'Oturumunuz sona erdi. Lütfen tekrar giriş yapın.';
  }

  if (data?.details?.length) {
    return data.details.join(', ');
  }

  if (data?.message) {
    return data.message;
  }

  return fallback;
};
