// /js/auth_ui.js
(() => {
  function getUser() {
    const raw = localStorage.getItem('admin_user');
    if (!raw) return null;
    try { return JSON.parse(raw); } catch { return null; }
  }

  function logout() {
    localStorage.removeItem('admin_user');
    localStorage.removeItem('admin_token');
    location.reload();
  }

  function render() {
    const box = document.getElementById('adminUserBox');
    if (!box) return;

    const u = getUser();
    if (!u) {
      box.innerHTML = `<a class="btn" href="/admin/login.html">관리자 로그인</a>`;
      return;
    }

    const role = (u.role || '').toUpperCase();
    box.innerHTML = `
      <span class="pill" style="padding:6px 10px;border:1px solid #e5e7eb;border-radius:999px;">
        <span style="margin-right:6px">👤</span>
        <b>${u.username}</b>
        <span class="muted" style="margin-left:6px">(${role})</span>
      </span>
      <button class="btn" id="adminLogoutBtn">로그아웃</button>
    `;
    document.getElementById('adminLogoutBtn')?.addEventListener('click', logout);
  }
  function setCookie(name, val, days = 7) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = `${name}=${encodeURIComponent(val)}; Path=/; SameSite=Lax; Expires=${expires}`;
  }

  // 로그인 성공 시 받은 JWT
  function onLoginSuccess(token, role = 'user') {
    // 기존: 로컬스토리지
    if (role === 'admin') {
      localStorage.setItem('admin_token', token);
    } else {
      localStorage.setItem('user_token', token);
    }
    // 추가: 쿠키에도 저장 (페이지 네비게이션용)
    setCookie('access_token', token);       // 일반 사용자
    // 필요하면 관리자 따로: setCookie('admin_token', token);

    // 이동
    location.href = '/manager/reports'; // 또는 next 파라미터 처리
  }

  document.addEventListener('DOMContentLoaded', render);
})();
