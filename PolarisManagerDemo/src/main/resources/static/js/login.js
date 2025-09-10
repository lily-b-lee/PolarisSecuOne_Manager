// /js/login.js
(() => {
  const $  = (s, p=document) => p.querySelector(s);

  const BASE = (document.querySelector('meta[name="app-base"]')?.content || '/')
    .replace(/\/+$/, '') || '';

  const ADMIN_LOGIN_URL    = `${BASE}/api/admin/auth/login`.replace(/\/+/g,'/');
  const CUSTOMER_LOGIN_URL = `${BASE}/api/customer/auth/login`.replace(/\/+/g,'/');

  let MODE = 'admin';
  let BUSY = false;

  const errEl = $('#err');
  const showErr = (m) => { if (errEl) errEl.textContent = m || ''; };
  const setBusy = (b) => {
    BUSY = b;
    const btn = $('#loginBtn');
    if (btn) { btn.disabled = b; btn.textContent = b ? '로그인 중…' : '로그인'; }
  };

  function renderMode() {
    const isCustomer = (MODE === 'customer');

    $('#tabAdmin')?.classList.toggle('active', !isCustomer);
    $('#tabAdmin')?.setAttribute('aria-selected', (!isCustomer).toString());
    $('#tabCustomer')?.classList.toggle('active', isCustomer);
    $('#tabCustomer')?.setAttribute('aria-selected', isCustomer.toString());

    $('#customerCodeRow')?.style && ($('#customerCodeRow').style.display = isCustomer ? '' : 'none');

    const label = $('#usernameLabel');
    const user = $('#username');
    if (label) label.textContent = isCustomer ? '이메일' : '아이디';
    if (user) {
      user.placeholder = isCustomer ? '이메일 주소' : '아이디';
      user.type = isCustomer ? 'email' : 'text';
      user.autocomplete = isCustomer ? 'email' : 'username';
    }

    // 고객사 탭에서는 회원가입 숨김
    const signupWrap = $('#signupWrap');
    if (signupWrap) signupWrap.style.display = isCustomer ? 'none' : '';

    // 비밀번호 표시 상태 초기화
    const pw = $('#password');
    const toggle = $('#pwToggle');
    if (pw && toggle) {
      pw.type = 'password';
      toggle.setAttribute('data-show','false');
      toggle.setAttribute('aria-pressed','false');
      toggle.setAttribute('title','비밀번호 보기');
    }

    showErr('');
  }

  function bindTabs() {
    const seg = document.querySelector('.seg');
    if (!seg || seg.dataset.bound) return;
    seg.dataset.bound = '1';
    seg.addEventListener('click', (e) => {
      const btn = e.target.closest('.seg-btn');
      if (!btn) return;
      const mode = btn.getAttribute('data-mode');
      if (!mode) return;
      MODE = mode; // 'admin' | 'customer'
      renderMode();
    }, true);
  }

  function bindPwToggle() {
    const btn = $('#pwToggle');
    const pw  = $('#password');
    if (!btn || !pw || btn.dataset.bound) return;
    btn.dataset.bound = '1';
    btn.addEventListener('click', () => {
      const showing = btn.getAttribute('data-show') === 'true';
      const next = !showing;
      pw.type = next ? 'text' : 'password';
      btn.setAttribute('data-show', String(next));
      btn.setAttribute('aria-pressed', String(next));
      btn.setAttribute('title', next ? '비밀번호 숨기기' : '비밀번호 보기');
    });
  }

  async function postJson(url, body) {
    const res = await fetch(url, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      credentials: 'same-origin',
      body: JSON.stringify(body),
    });
    const ct = (res.headers.get('content-type') || '').toLowerCase();
    let data=null, text='';
    if (ct.includes('application/json')) { try { data = await res.json(); } catch {} }
    else { try { text = await res.text(); } catch {} }
    return { res, data, text, ct };
  }

  const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

  async function doLogin(e) {
    if (e) { e.preventDefault(); e.stopPropagation(); }
    if (BUSY) return;
    showErr('');

    const username = ($('#username')?.value || '').trim();
    const password = ($('#password')?.value || '');
    const customerCode = ($('#customerCode')?.value || '').trim();

    if (!username || !password) { showErr('아이디/비밀번호를 입력해 주세요.'); return; }

    if (MODE === 'customer') {
      if (!isEmail(username)) { showErr('이메일 주소 형식이 올바르지 않습니다.'); return; }
      if (!customerCode)      { showErr('고객사 코드를 입력해 주세요.'); return; }
    }

    const url = (MODE === 'customer') ? CUSTOMER_LOGIN_URL : ADMIN_LOGIN_URL;
    const payload = (MODE === 'customer')
      ? { username, password, customerCode }
      : { username, password };

    setBusy(true);
    try {
      const { res, data, text, ct } = await postJson(url, payload);

      if (!res.ok) {
        const serverMsg = data?.message || data?.error || text || '';
        const msg =
          res.status === 0   ? '네트워크 오류입니다. 연결을 확인해 주세요.' :
          res.status === 400 ? (serverMsg || '요청 형식이 올바르지 않습니다.') :
          res.status === 401 ? (serverMsg || '아이디 또는 비밀번호가 올바르지 않습니다.') :
          res.status === 403 ? (serverMsg || '접근 권한이 없습니다. 관리자에게 문의해 주세요.') :
          res.status >= 500  ? (serverMsg || '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.') :
          (serverMsg || `로그인 실패 (HTTP ${res.status})`);
        showErr(msg);
        return;
      }

      if (ct.includes('text/html')) {
        showErr(`서버가 JSON 대신 HTML을 응답했습니다. 서버 매핑을 확인하세요. • 요청: ${url}`);
        return;
      }

      const typeGuess  = (MODE === 'customer') ? 'customer' : 'admin';
      const type  = (data?.type || typeGuess);
      const user  = data?.user || data;
      const token = data?.token || null;

      try {
        localStorage.setItem('auth_type', type);
        localStorage.setItem('auth_user', JSON.stringify(user));
        if (token) localStorage.setItem('auth_token', token);

        if (type === 'admin') {
          localStorage.setItem('admin_user', JSON.stringify(user));
          if (token) localStorage.setItem('admin_token', token);
          localStorage.removeItem('customer_user');
          localStorage.removeItem('customer_token');
        } else {
          localStorage.setItem('customer_user', JSON.stringify(user));
          if (token) localStorage.setItem('customer_token', token);
          localStorage.removeItem('admin_user');
          localStorage.removeItem('admin_token');
        }
      } catch {}

      const next = new URLSearchParams(location.search).get('next')
                 || (type === 'admin' ? '/overview' : '/manager/overview');
      location.href = next;

    } catch (err) {
      showErr(err?.message || '알 수 없는 오류가 발생했습니다.');
    } finally {
      setBusy(false);
    }
  }

  function bindForm() {
    $('#loginBtn')?.addEventListener('click', doLogin);
    $('#loginForm')?.addEventListener('submit', doLogin);
    $('#username')?.addEventListener('keydown', (e)=>{ if (e.key === 'Enter') doLogin(e); });
    $('#password')?.addEventListener('keydown', (e)=>{ if (e.key === 'Enter') doLogin(e); });
    $('#signupBtn')?.setAttribute('href', '/admin/signup'); // 관리자만 노출
  }

  function init() {
    bindTabs();
    bindPwToggle();
    bindForm();
    renderMode();

    // URL ?tab=customer 로 바로 고객사 탭 열기 지원
    const tab = new URLSearchParams(location.search).get('tab');
    if (tab === 'customer') { MODE = 'customer'; renderMode(); }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
