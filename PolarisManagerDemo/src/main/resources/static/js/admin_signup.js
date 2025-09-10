// /js/admin_signup.js
(() => {
  const $ = (s) => document.querySelector(s);

  // BASE 보정 (서브패스 배포 대비)
  const BASE = (document.querySelector('meta[name="app-base"]')?.content || '/')
               .replace(/\/+$/, '') || '';
  const API  = `${BASE}/api/admin/auth`.replace(/\/+/g,'/');

  function showErr(msg){ const el = $('#err'); if (el) el.textContent = msg || ''; }

  async function postJson(url, data){
    const res = await fetch(url, {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(data),
      credentials: 'same-origin',
    });
    let payload=null; try { payload = await res.json(); } catch {}
    return { ok: res.ok, status: res.status, data: payload };
  }

  function disableDuring(fn, btn){
    return async (...args) => {
      if (!btn) return fn(...args);
      const orig = btn.textContent;
      btn.disabled = true; btn.textContent = '처리 중...';
      try { return await fn(...args); }
      finally { btn.disabled = false; btn.textContent = orig; }
    };
  }

  document.addEventListener('DOMContentLoaded', () => {
    const btn = $('#signupBtn');
    const username= $('#username');
    const pwd = $('#password');
    const pwd2= $('#password2');
    const role= $('#role');
    const secret=$('#secret');

    const handler = disableDuring(async () => {
      showErr('');
      const u = (username?.value || '').trim();
      const p1=  pwd?.value || '';
      const p2=  pwd2?.value || '';
      const r = (role?.value || 'EDITOR').trim();
      const s = (secret?.value || '').trim();

      if (!u) return showErr('아이디를 입력하세요.');
      if (p1.length < 8) return showErr('비밀번호는 8자 이상이어야 합니다.');
      if (p1 !== p2) return showErr('비밀번호 확인이 일치하지 않습니다.');

      const payload = { username:u, password:p1, role:r, secret: s || null };
      const { ok, status, data } = await postJson(`${API}/signup`, payload);

      if (!ok) {
        if (status === 409) return showErr('이미 존재하는 아이디입니다.');
        if (status === 401) return showErr('가입 시크릿이 올바르지 않습니다.');
        if (status === 400) return showErr(data?.message || '입력 값을 확인하세요.');
        return showErr(data?.message || `오류가 발생했습니다. (HTTP ${status})`);
      }

      alert('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.');
      // 로그인 화면을 관리자 탭으로 열기
      location.href = `${BASE}/login?mode=admin`.replace(/\/+/g,'/');
    }, btn);

    btn?.addEventListener('click', handler);
    [username, pwd, pwd2, role, secret].forEach(el => {
      el?.addEventListener('keydown', (e)=>{ if (e.key === 'Enter') handler(); });
    });
  });
})();
