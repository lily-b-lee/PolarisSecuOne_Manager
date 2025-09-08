// /js/nav.js
(() => {
  const $  = (s, p=document) => p.querySelector(s);
  const $$ = (s, p=document) => [...p.querySelectorAll(s)];

  // -----------------------------
  // Cookie helpers
  // -----------------------------
  function setCookie(name, value, { days = 30, path = '/', sameSite = 'Lax', secure = (location.protocol === 'https:') } = {}) {
    const maxAge = days ? `; Max-Age=${days * 24 * 60 * 60}` : '';
    const sec    = secure ? '; Secure' : '';
    document.cookie = `${name}=${encodeURIComponent(value)}; Path=${path}${maxAge}; SameSite=${sameSite}${sec}`;
  }
  function delCookie(name, { path = '/' } = {}) {
    document.cookie = `${name}=; Path=${path}; Max-Age=0; SameSite=Lax`;
  }

  // -----------------------------
  // Auth helpers
  // -----------------------------
  function readAuth() {
    try {
      const type =
        localStorage.getItem('auth_type') ||
        (localStorage.getItem('admin_user') ? 'admin'
         : localStorage.getItem('customer_user') ? 'customer'
         : null);

      let user = null;
      if (type === 'admin') {
        user = JSON.parse(localStorage.getItem('auth_user') || localStorage.getItem('admin_user') || 'null');
      } else if (type === 'customer') {
        user = JSON.parse(localStorage.getItem('auth_user') || localStorage.getItem('customer_user') || 'null');
      }
      return { type, user };
    } catch {
      return { type: null, user: null };
    }
  }
  const isLoggedIn = () => !!readAuth().user;

  // ✅ 핵심: 로컬스토리지 토큰을 쿠키로 동기화 (페이지 이동 시 서버가 인증 가능)
  function syncTokenCookies() {
    const token =
      localStorage.getItem('auth_token')     ||
      localStorage.getItem('admin_token')    ||
      localStorage.getItem('customer_token') ||
      localStorage.getItem('user_token');

    if (token) {
      // 서버 필터가 읽는 이름에 맞춰 두 개 모두 셋업(호환)
      setCookie('access_token', token, { days: 30 });
      setCookie('admin_token',  token, { days: 30 }); // 있으면 사용, 없어도 문제 없음
    } else {
      delCookie('access_token');
      delCookie('admin_token');
    }
  }

  // -----------------------------
  // UI helpers
  // -----------------------------
  function formatCustomerChip(u) {
    if (!u) return '-';
    const email =
      u.email || u.userEmail || u.username || (u.account && u.account.email) || '';
    const code =
      u.customerCode || u.code ||
      (u.customer && (u.customer.code || u.customerCode)) ||
      (u.org && (u.org.code)) || '';
    const company =
      u.companyName || u.customerName || u.company || u.orgName ||
      (u.customer && (u.customer.name || u.customerName)) ||
      (u.org && u.org.name) || '';
    const left = company ? (code ? `${company}(${code})` : company) : (code ? `(${code})` : '');
    if (left && email) return `${left}_${email}`;
    return email || left || '-';
  }

  function syncUserPill() {
    const { type, user } = readAuth();
    const uid  = $('#navUser') || $('#navUserCombined');
    const role = $('#navRole');
    const mail = $('#navEmail');

    if (!uid || !role) return;

    if (!user) {
      uid.textContent  = '-';
      role.textContent = '-';
      if (mail) { mail.textContent = ''; mail.style.display = 'none'; }
      return;
    }

    if (type === 'customer') {
      const label = formatCustomerChip(user);
      uid.textContent  = label || '-';
      role.textContent = 'CUSTOMER';
      if (mail) { mail.textContent = ''; mail.style.display = 'none'; }
    } else {
      const name = user.username || user.email || '';
      uid.textContent  = name || '-';
      role.textContent = (user.role || 'ADMIN').toUpperCase();
      if (mail) {
        mail.textContent   = name;
        mail.style.display = name ? '' : 'none';
      }
    }
  }

  // 메뉴 가드(로그아웃 상태면 /login 으로)
  function guardMenuLinks() {
    [...$$('.sidebar .menu a'), ...$$('.sidebar .nav-menu a')].forEach(a => {
      if (a.dataset.guardBound) return;
      a.dataset.guardBound = '1';
      a.addEventListener('click', (e) => {
        if (!isLoggedIn()) {
          e.preventDefault();
          location.href = '/login';
        } else {
          // 링크 이동 전 쿠키 최신화(탭 간 토큰 변경 대응)
          syncTokenCookies();
        }
      }, true);
    });
  }

  function bindLogout() {
    const btn = $('#btnLogout');
    if (!btn || btn.dataset.bound) return;
    btn.dataset.bound = '1';
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      ['auth_type','auth_user','auth_token','admin_user','admin_token','customer_user','customer_token','user_token']
        .forEach(k => localStorage.removeItem(k));
      delCookie('access_token');
      delCookie('admin_token');
      location.href = '/login';
    });
  }

  // 설정(톱니) 버튼 → /settings or /manager/settings
  function bindSettings() {
    const btn = $('#btnSettings');
    if (!btn || btn.dataset.bound) return;
    btn.dataset.bound = '1';

    const sb   = $('.sidebar');
    const mode = sb?.dataset.mode || (readAuth().type === 'customer' ? 'portal' : 'admin');
    const url  = mode === 'portal' ? '/manager/settings' : '/settings';

    btn.addEventListener('click', (e) => {
      e.preventDefault();
      if (!isLoggedIn()) {
        location.href = '/login?next=' + encodeURIComponent(url);
        return;
      }
      syncTokenCookies(); // 이동 전에 보강
      location.href = url;
    });
  }

  function bindCopyEmail() {
    const btn = $('#btnCopyEmail') || $('.copy-email');
    const src = $('#navEmail') || $('#navUser');
    if (!btn || !src || btn.dataset.bound) return;
    btn.dataset.bound = '1';
    btn.addEventListener('click', () => {
      const text = (src.textContent || '').trim();
      if (!text) return;
      navigator.clipboard?.writeText(text).catch(()=>{});
    });
  }

  function init() {
    // 최초 로드 시 쿠키 동기화(페이지 이동 인증 유지)
    syncTokenCookies();

    const sb = $('.sidebar');
    if (!sb || sb.dataset.navInited) return;
    sb.dataset.navInited = '1';

    syncUserPill();
    guardMenuLinks();
    bindLogout();
    bindSettings();
    bindCopyEmail();
  }

  document.addEventListener('DOMContentLoaded', init);

  // 다른 탭에서 토큰/사용자 정보가 바뀔 때 동기화
  window.addEventListener('storage', (e) => {
    const key = e.key || '';
    if (['auth_type','auth_user','admin_user','customer_user'].includes(key)) {
      syncUserPill();
    }
    if (['auth_token','admin_token','customer_token','user_token'].includes(key)) {
      syncTokenCookies();
    }
  });
})();
