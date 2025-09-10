// /js/customer_signup.js
(() => {
  const $ = (s)=>document.querySelector(s);
  const BASE = (document.querySelector('meta[name="app-base"]')?.content || '/')
               .replace(/\/+$/, '') || '';
  const API  = `${BASE}/api/customer/auth`.replace(/\/+/g,'/');

  function showErr(msg){ const el = $('#err'); if (el) el.textContent = msg || ''; }
  async function postJson(url, data){
    const res = await fetch(url, {
      method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify(data), credentials:'same-origin'
    });
    let payload=null; try{ payload = await res.json(); }catch{}
    return { ok: res.ok, status: res.status, data: payload };
  }
  function disableDuring(fn, btn){
    return async (...a)=>{ if(!btn) return fn(...a);
      const t=btn.textContent; btn.disabled=true; btn.textContent='처리 중...';
      try{ return await fn(...a);} finally{ btn.disabled=false; btn.textContent=t; }
    };
  }

  document.addEventListener('DOMContentLoaded', () => {
    const btn=$('#signupBtn'), cc=$('#customerCode'), u=$('#username'),
          p1=$('#password'), p2=$('#password2'), em=$('#email');

    const handler = disableDuring(async () => {
      showErr('');
      const customerCode=(cc?.value||'').trim();
      const username=(u?.value||'').trim();
      const password=p1?.value||'';
      const confirm =p2?.value||'';
      const email   =(em?.value||'').trim();

      if(!customerCode) return showErr('고객사 코드를 입력하세요.');
      if(!username)     return showErr('아이디를 입력하세요.');
      if(password.length<8) return showErr('비밀번호는 8자 이상이어야 합니다.');
      if(password!==confirm) return showErr('비밀번호 확인이 일치하지 않습니다.');

      const payload={ customerCode, username, password, email: email || null };
      const { ok, status, data } = await postJson(`${API}/signup`, payload);

      if(!ok){
        if(status===409) return showErr('이미 존재하는 아이디입니다.');
        if(status===404) return showErr('해당 고객사 코드를 찾을 수 없습니다.');
        if(status===400) return showErr(data?.message || '입력 값을 확인하세요.');
        return showErr(data?.message || `오류가 발생했습니다. (HTTP ${status})`);
      }

      alert('회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.');
      location.href = `${BASE}/login?mode=customer`.replace(/\/+/g,'/');
    }, btn);

    btn?.addEventListener('click', handler);
    [cc,u,p1,p2,em].forEach(el=>el?.addEventListener('keydown',e=>{ if(e.key==='Enter') handler(); }));
  });
})();
