export function extractErrorMessage(error, fallback = 'Bir hata oluştu') {
  if (!error) return fallback;
  if (error.response?.data?.message) return error.response.data.message;
  if (error.response?.data?.details?.length) return error.response.data.details.join(', ');
  if (error.message) return error.message;
  return fallback;
}
