// /js/admin_api.js
export async function adminFetch(url, options={}) {
  const token = localStorage.getItem('admin_token');
  const headers = Object.assign({'Content-Type':'application/json'}, options.headers||{});
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const r = await fetch(url, {...options, headers});
  if (r.status === 401) {
    alert('세션이 만료되었거나 권한이 없습니다. 다시 로그인해 주세요.');
    location.href = '/admin/login.html';
    return Promise.reject(new Error('Unauthorized'));
  }
  return r;
}
