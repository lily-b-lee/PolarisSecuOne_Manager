// manager_setting.js — 설정 화면 + 비밀번호 변경 모달
(() => {
  const $  = (s, p=document) => p.querySelector(s);
  const $$ = (s, p=document) => [...p.querySelectorAll(s)];
  const BASE = (document.querySelector('meta[name="app-base"]')?.content || '/').replace(/\/+$/, '') || '';

  // ---------- 토스트 ----------
  function showToast(msg, ms=2200){
    const el = $('#toast'); if(!el) return;
    el.textContent = msg || '';
    el.style.display = 'block';
    setTimeout(()=>{ el.style.display='none'; }, ms);
  }

  // ---------- 읽기/쓰기 모드 전환 ----------
  function setReadOnlyMode(isReadOnly){
    const ids = ['name','phone'];
    ids.forEach(id=>{
      const el = $('#'+id);
      if(!el) return;
      el.readOnly = isReadOnly;
      el.setAttribute('aria-readonly', isReadOnly ? 'true' : 'false');
      if (isReadOnly) el.classList.remove('editing'); else el.classList.add('editing');
    });

    // 버튼 토글
    $('#btnEdit')?.style.setProperty('display', isReadOnly ? '' : 'none');
    $('#btnSave')?.style.setProperty('display', isReadOnly ? 'none' : '');
    $('#btnCancel')?.style.setProperty('display', isReadOnly ? 'none' : '');
  }

  // ---------- auth 읽기 ----------
  function readAuth() {
    try {
      const type =
        localStorage.getItem('auth_type') ||
        (localStorage.getItem('admin_user') ? 'admin' :
         localStorage.getItem('customer_user') ? 'customer' : null);

      let user = null;
      if (type === 'admin') {
        user = JSON.parse(localStorage.getItem('auth_user') || localStorage.getItem('admin_user') || 'null');
      } else if (type === 'customer') {
        user = JSON.parse(localStorage.getItem('auth_user') || localStorage.getItem('customer_user') || 'null');
      }
      return { type, user };
    } catch { return { type: null, user: null }; }
  }

  // ---------- 표준화 ----------
  function normalize(u = {}) {
    const name =
      u.name || u.fullName || u.realName || u.displayName ||
      (u.profile && (u.profile.name || u.profile.fullName)) || '';

    const email =
      u.email || u.userEmail || (u.account && u.account.email) || u.username || '';

    const phone =
      u.phone || u.phoneNumber || u.mobile || u.tel ||
      (u.profile && (u.profile.phone || u.profile.mobile)) || '';

    const code =
      u.customerCode || u.orgCode || u.code ||
      (u.customer && (u.customer.code || u.customerCode)) ||
      (u.org && u.org.code) || '';

    const company =
      u.companyName || u.customerName || u.company || u.orgName ||
      (u.customer && (u.customer.name || u.customerName)) ||
      (u.org && u.org.name) || '';

    return { name, email, phone, code, company };
  }

  // ---------- 서버에서 me 가져와 병합 ----------
  async function fetchMe() {
    const candidates = ['/api/customer/auth/me', '/api/me', '/api/users/me'];
    for (const url of candidates) {
      try {
        const r = await fetch(url, { credentials: 'same-origin' });
        if (r.ok) return await r.json().catch(()=>null);
      } catch {}
    }
    return null;
  }

  function setValue(id, val) {
    const el = $('#'+id); if(!el) return;
    if (val != null && val !== '') el.value = val;
  }

  // ---------- 초기화(보기 모드 + 값 세팅) ----------
  async function initFormValues() {
    const { user } = readAuth();
    let info = normalize(user);

    const remote = await fetchMe();
    if (remote) info = { ...info, ...normalize(remote) };

    setValue('name',    info.name);
    setValue('phone',   info.phone);
    setValue('email',   info.email);
    setValue('code',    info.code);
    setValue('company', info.company);

    setReadOnlyMode(true);
  }

  // ---------- 저장(연락처 upsert) ----------
  async function saveProfile() {
    const name  = ($('#name')?.value || '').trim();
    const phone = ($('#phone')?.value || '').trim();
    const email = ($('#email')?.value || '').trim();
    const code  = ($('#code')?.value || '').trim();

    if (!email || !code) {
      showToast('고객사 코드/이메일이 없습니다. 다시 로그인 해주세요.');
      return;
    }
    if (!name) { showToast('이름을 입력해 주세요.'); return; }

    // 업데이트가 되도록 id 를 함께 보낼 수 있으면 더 안전하지만,
    // 서버가 (customerCode, email)로 upsert 하도록 되어 있음.
    const payload = {
      customerCode: code,
      name,
      email,    // 읽기전용이지만 키로 사용
      phone,
      note: ''
    };

    const r = await fetch('/api/contacts/upsert', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      credentials: 'same-origin',
      body: JSON.stringify(payload)
    });

    if (!r.ok) {
      const msg = (await r.json().catch(()=>null))?.message || '저장 실패';
      showToast(msg);
      return;
    }
    showToast('저장되었습니다.');
    setReadOnlyMode(true);
    // 최신값 반영 위해 다시 me 호출(선택)
    setTimeout(initFormValues, 300);
  }

  // ---------- 편집/취소 ----------
  function bindEditButtons() {
    $('#btnEdit')?.addEventListener('click', () => setReadOnlyMode(false));
    $('#btnCancel')?.addEventListener('click', () => {
      setReadOnlyMode(true);
      initFormValues(); // 값 원복
    });
    $('#btnSave')?.addEventListener('click', saveProfile);
  }

  // ============================================================
  // ================   비밀번호 변경 모달 로직   ================
  // ============================================================
  const MODAL = {
    el: null, err: null, cur: null, nw: null, nw2: null, submit: null, cancel: null
  };

  function openPwModal() {
    if (!MODAL.el) return;
    MODAL.cur.value = '';
    MODAL.nw.value = '';
    MODAL.nw2.value = '';
    MODAL.err.textContent = '';
    MODAL.el.hidden = false;
    MODAL.el.setAttribute('aria-hidden', 'false');
    MODAL.cur.focus();
  }

  function closePwModal() {
    if (!MODAL.el) return;
    MODAL.el.hidden = true;
    MODAL.el.setAttribute('aria-hidden', 'true');
  }

  function validateNewPw(pw, username) {
    if (!pw || pw.length < 8 || pw.includes(' ')) return false;
    if (username && pw.toLowerCase().includes(String(username).toLowerCase())) return false;
    const hasDigit = /\d/.test(pw);
    const hasAlpha = /[A-Za-z]/.test(pw);
    const hasPunct = /[!@#$%^&*()[\]{}<>?/\\|~`_\-+=.:;,'"]/.test(pw);
    return (hasDigit?1:0) + (hasAlpha?1:0) + (hasPunct?1:0) >= 2;
  }

  async function changePassword() {
    const cur = MODAL.cur.value || '';
    const nw  = MODAL.nw.value || '';
    const nw2 = MODAL.nw2.value || '';
    MODAL.err.textContent = '';

    if (!cur) { MODAL.err.textContent = '현재 비밀번호를 입력해 주세요.'; return; }
    if (!nw)  { MODAL.err.textContent = '새 비밀번호를 입력해 주세요.'; return; }
    if (nw !== nw2) { MODAL.err.textContent = '새 비밀번호가 일치하지 않습니다.'; return; }

    // username 은 검증용(아이디 포함 금지)
    const { user } = readAuth();
    const username = user?.username || user?.email || '';
    if (!validateNewPw(nw, username)) {
      MODAL.err.textContent = '8자 이상, 공백 없음, 아이디/이메일 불포함, 숫자/문자/특수 중 2종류 이상을 포함해야 합니다.';
      return;
    }

    MODAL.submit.disabled = true;
    MODAL.submit.textContent = '변경 중…';

    try {
      const r = await fetch('/api/customer/auth/change-password', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        credentials: 'same-origin',
        body: JSON.stringify({ currentPassword: cur, newPassword: nw })
      });

      const ct = (r.headers.get('content-type') || '').toLowerCase();
      let data = null;
      if (ct.includes('application/json')) { try { data = await r.json(); } catch{} }

      if (!r.ok) {
        const msg =
          data?.message ||
          (r.status === 401 ? '인증이 만료되었습니다. 다시 로그인 해주세요.' :
           r.status === 400 ? '요청이 올바르지 않습니다.' :
           '비밀번호 변경에 실패했습니다.');
        MODAL.err.textContent = msg;
        if (r.status === 401) {
          // 세션 만료 시 로그인 화면으로
          setTimeout(()=>{ location.href = '/login'; }, 1000);
        }
        return;
      }

      // 서버는 비번 변경 후 세션 무효화하므로 로컬 스토리지도 정리
      showToast(data?.message || '비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
      try {
        ['auth_type','auth_user','auth_token','admin_user','admin_token','customer_user','customer_token']
          .forEach(k => localStorage.removeItem(k));
      } catch {}
      closePwModal();
      setTimeout(()=>{ location.href = '/login'; }, 800);

    } catch (e) {
      MODAL.err.textContent = e?.message || '네트워크 오류입니다.';
    } finally {
      MODAL.submit.disabled = false;
      MODAL.submit.textContent = '변경';
    }
  }

  function bindPwModal() {
    MODAL.el     = $('#pwModal');
    MODAL.err    = $('#pwErr');
    MODAL.cur    = $('#curPw');
    MODAL.nw     = $('#newPw');
    MODAL.nw2    = $('#newPw2');
    MODAL.submit = $('#pwSubmit');
    MODAL.cancel = $('#pwCancel');

    $('#btnPwOpen')?.addEventListener('click', openPwModal);
    MODAL.cancel?.addEventListener('click', closePwModal);
    MODAL.submit?.addEventListener('click', changePassword);

    // 배경 클릭으로 닫기
    MODAL.el?.addEventListener('click', (e)=>{
      if (e.target?.dataset?.close) closePwModal();
    });

    // ESC로 닫기
    document.addEventListener('keydown', (e)=>{
      if (e.key === 'Escape' && MODAL.el && !MODAL.el.hidden) closePwModal();
    });
  }

  // ---------- 부트스트랩 ----------
  async function init() {
    await initFormValues();
    bindEditButtons();
    bindPwModal();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
