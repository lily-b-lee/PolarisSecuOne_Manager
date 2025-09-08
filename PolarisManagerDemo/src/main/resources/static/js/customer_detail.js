// /js/customer_detail.js
(() => {
  const $  = (s, p = document) => p.querySelector(s);
  if (!$('#profileContent') || !$('#profileEmpty')) return;

  const CUST_API_CANDIDATES = [
    '/api/admin/customers',
    '/api/customers',
    '/admin/customers',
    '/customers/api'
  ];
  let BASE = null;

  const titleEl  = $('#custTitle');
  const metaEl   = $('#custMeta');
  const emptyEl  = $('#profileEmpty');
  const contEl   = $('#profileContent');
  const msgEl    = $('#formMsg');

  const profileCard = $('#profileCard');
  const btnToggleProfile = $('#btnToggleProfile');
  const btnLoad  = $('#btnLoadProfile');
  const btnEdit  = $('#btnEdit');
  const btnReset = $('#btnReset');
  const btnSave  = $('#btnSave');
  const btnDel   = $('#btnDelete');

  const fld = {
    id:     $('#custId'),
    code:   $('#code'),
    name:   $('#name'),
    cpiRate:$('#cpiRate'),
    rsRate: $('#rsRate'),
    note:   $('#note'),
  };

  const statDownloads = $('#statDownloads');
  const statDeletes   = $('#statDeletes');
  const statAmount    = $('#statAmount');
  const tblMonthly    = $('#tblMonthly tbody');
  const listInfo      = $('#listInfo');

  // --- Contacts DOM ---
  const contactsTbody  = $('#tblContacts tbody');
  const contactsInfo   = $('#contactsInfo');
  const btnOpenContact = $('#btnOpenContact');

  const contactModal   = $('#contactModal');
  const btnCloseContact= $('#btnCloseContact');
  const btnSaveContact = $('#btnSaveContact');
  const btnContactReset= $('#btnContactReset');

  const fldCM = {
    code:  $('#contactCustomerCode'),
    name:  $('#cmName'),
    email: $('#cmEmail'),
    phone: $('#cmPhone'),
    note:  $('#cmNote'),
  };
  const chkSendInvite = $('#cmSendInvite');
  const contactErr = $('#contactFormErr');
  const contactInfo = $('#contactFormInfo');

  // --- Security Events DOM ---
  const evFrom   = $('#evFrom');
  const evTo     = $('#evTo');
  const evType   = $('#evType');
  const btnEvLoad= $('#btnEvLoad');
  const evTotal  = $('#evTotal');
  const evMalware= $('#evMalware');
  const evRooting= $('#evRooting');
  const evRemote = $('#evRemote');
  const tblEvents= $('#tblEvents tbody');
  const evInfo   = $('#evInfo');
  const evPrev   = $('#evPrev');
  const evNext   = $('#evNext');

  const state = { id: null, profile: null, monthly: [], editing: false };
  const evState = { page: 0, size: 20, totalPages: 0, lastQuery: null };

  const getToken = () => localStorage.getItem('admin_token') || null;
  const authHeaders = () => {
    const h = { 'Content-Type': 'application/json' };
    const t = getToken(); if (t) h['Authorization'] = `Bearer ${t}`;
    return h;
  };
  const nz = (v, d = '') => (v === null || v === undefined ? d : v);
  const fmt = (n) => (n === null || n === undefined ? '-' : Number(n).toLocaleString());
  const fmtWon = (n) => (n === null || n === undefined ? '-' : `₩ ${Number(n).toLocaleString()}`);

  function resolveCustomerId() {
    const sp = new URLSearchParams(location.search);
    const byId = sp.get('id');    if (byId) return byId;
    const byCode = sp.get('code');if (byCode) return byCode;
    const m = location.pathname.match(/\/customers\/([^\/?#]+)/);
    if (m) return decodeURIComponent(m[1]);
    const meta = document.querySelector('meta[name="customer-id"]');
    if (meta?.content) return meta.content;
    return null;
  }

  async function fetchTry(urls, opt) {
    for (const u of urls) {
      try {
        const res = await fetch(u, opt);
        if (res.status === 401 || res.status === 403) {
          location.href = `/admin/login.html?next=${encodeURIComponent(location.pathname + location.search)}`;
          return null;
        }
        if (res.ok) {
          const ct = res.headers.get('content-type') || '';
          return ct.includes('application/json') ? await res.json().catch(()=>null) : {};
        }
      } catch(_) {}
    }
    return null;
  }

  async function detectBase() {
    for (const base of CUST_API_CANDIDATES) {
      const ok = await fetchTry(
        [`${base}/_ping`, `${base}?limit=1`, base],
        { headers: authHeaders(), credentials: 'same-origin' }
      );
      if (ok !== null) { BASE = base; return; }
    }
    throw new Error('고객사 API 엔드포인트를 찾을 수 없습니다.');
  }

  // ---- Customer APIs ----
  async function fetchProfile(idOrCode) {
    const urls = [
      `${BASE}/${encodeURIComponent(idOrCode)}`,
      `${BASE}?id=${encodeURIComponent(idOrCode)}`,
      `${BASE}/by-code/${encodeURIComponent(idOrCode)}`,
      `${BASE}?code=${encodeURIComponent(idOrCode)}`
    ];
    return await fetchTry(urls, { headers: authHeaders(), credentials: 'same-origin' });
  }

  async function fetchMonthly(id) {
    const urls = [
      `${BASE}/${encodeURIComponent(id)}/monthly`,
      `${BASE}/${encodeURIComponent(id)}/months`,
      `${BASE}/${encodeURIComponent(id)}/stats`,
      `${BASE}/${encodeURIComponent(id)}/report`,
    ];
    const data = await fetchTry(urls, { headers: authHeaders(), credentials: 'same-origin' });
    return Array.isArray(data) ? data : [];
  }

  async function updateProfile(id, payload) {
    const res = await fetch(`${BASE}/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify(payload),
      credentials: 'same-origin',
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) throw new Error(data?.message || `수정 실패(HTTP ${res.status})`);
    return data;
  }

  async function deleteCustomer(id) {
    const res = await fetch(`${BASE}/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: authHeaders(),
      credentials: 'same-origin',
    });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(`삭제 실패(HTTP ${res.status}) ${t}`);
    }
  }

  // ---- Contacts API ----
  async function createContact(code, payload) {
    const sendInvite =
      (chkSendInvite ? !!chkSendInvite.checked : (payload.sendInvite ?? !!payload.email));

    const res = await fetch('/api/contacts/upsert', {
      method: 'POST',
      headers: authHeaders(),
      credentials: 'same-origin',
      body: JSON.stringify({
        customerCode: code,
        name:  payload.name,
        email: payload.email,
        phone: payload.phone,
        note:  payload.note,
        sendInvite
      }),
    });

    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('json') ? await res.json().catch(()=> ({})) : {};

    if (!res.ok) {
      const msg = data?.message || data?.error || `HTTP ${res.status}`;
      throw new Error(msg);
    }
    return data;
  }

  async function getContacts(code) {
    const res = await fetch(`/api/contacts?customerCode=${encodeURIComponent(code)}`, {
      method: 'GET',
      headers: authHeaders(),
      credentials: 'same-origin',
    });
    if (!res.ok) return [];
    const ct = res.headers.get('content-type') || '';
    const data = ct.includes('json') ? await res.json().catch(()=> []) : [];
    return Array.isArray(data) ? data : (data?.items || data?.content || []);
  }

  async function deleteContact(_code, id) {
    const res = await fetch(`/api/contacts/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: authHeaders(),
      credentials: 'same-origin',
    });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(t || `HTTP ${res.status}`);
    }
  }

  // ---- Renderers (profile) ----
  function setEditMode(on) {
    state.editing = !!on;
    const dis = !state.editing;
    Object.values(fld).forEach(el => {
      if (!el || el === fld.id) return;
      if (el === fld.code) { el.disabled = true; return; }
      el.disabled = dis;
    });
    btnReset?.classList.toggle('hidden', !state.editing);
    btnSave?.classList.toggle('hidden', !state.editing);
    btnDel?.classList.toggle('hidden', !state.editing);
    btnEdit?.classList.toggle('hidden', state.editing);
  }

  function fillForm(p) {
    fld.id.value    = nz(p?.id);
    fld.code.value  = nz(p?.code);
    fld.name.value  = nz(p?.name);
    fld.cpiRate.value = nz(p?.cpiRate);
    fld.rsRate.value  = nz(p?.rsRate);
    fld.note.value    = nz(p?.note);
  }

  function renderProfile(p) {
    titleEl.textContent = p?.name ? `고객사 상세 · ${p.name}` : '고객사 상세';
    metaEl.textContent  = p ? `코드 ${p.code || '-'}` : '-';
    fillForm(p);
    emptyEl.classList.add('hidden');
    contEl.classList.remove('hidden');
    setEditMode(false);
    msgEl.textContent = '';
  }

  function renderMonthly(arr) {
    const body = tblMonthly; if (!body) return;
    body.innerHTML = '';
    if (!Array.isArray(arr) || arr.length === 0) {
      body.innerHTML = `<tr><td colspan="5" class="muted" style="text-align:center;padding:16px">데이터가 없습니다.</td></tr>`;
      listInfo && (listInfo.textContent = '-');
      statDownloads && (statDownloads.textContent = '-');
      statDeletes && (statDeletes.textContent = '-');
      statAmount && (statAmount.textContent = '-');
      return;
    }
    const normMonth = (m) => String(m || '').replace(/\./g, '-').slice(0, 7);
    arr.forEach(r => r.month = normMonth(r.month || r.yyyymm || r.date || r.period));
    arr.sort((a, b) => String(b.month).localeCompare(String(a.month)));

    let sumD=0, sumX=0, sumA=0;
    const frag = document.createDocumentFragment();
    arr.forEach(r => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${nz(r.month,'-')}</td>
        <td class="cnt">${fmt(r.downloads)}</td>
        <td class="cnt">${fmt(r.deletes)}</td>
        <td>${fmtWon(r.amount)}</td>
        <td>${nz(r.status,'-')}</td>
      `;
      frag.appendChild(tr);
      sumD += Number(r.downloads || 0);
      sumX += Number(r.deletes || 0);
      sumA += Number(r.amount || 0);
    });
    body.appendChild(frag);
    statDownloads && (statDownloads.textContent = fmt(sumD));
    statDeletes && (statDeletes.textContent   = fmt(sumX));
    statAmount && (statAmount.textContent    = fmtWon(sumA));
    listInfo && (listInfo.textContent = `${arr.length.toLocaleString()}건 · 최신월부터`);
  }

  function renderContacts(rows) {
    contactsTbody.innerHTML = '';
    if (!rows || rows.length === 0) {
      contactsTbody.innerHTML = `<tr><td colspan="5" class="muted" style="text-align:center;padding:14px">담당자가 없습니다.</td></tr>`;
      contactsInfo.textContent = '-';
      return;
    }
    const frag = document.createDocumentFragment();
    rows.forEach((r) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${r.name || '-'}</td>
        <td>${r.email || '-'}</td>
        <td>${r.phone || '-'}</td>
        <td>${r.note  || '-'}</td>
        <td><button class="btn" data-act="del" data-id="${r.id ?? ''}">삭제</button></td>
      `;
      tr.querySelector('[data-act="del"]')?.addEventListener('click', async (e) => {
        e.stopPropagation();
        if (!r.id) return alert('식별자가 없습니다.');
        if (!confirm('이 담당자를 삭제할까요?')) return;
        try {
          await deleteContact(state.profile.code, r.id);
          const list = await getContacts(state.profile.code);
          renderContacts(list);
        } catch (err) {
          alert(err.message || '삭제 실패');
        }
      });
      frag.appendChild(tr);
    });
    contactsTbody.appendChild(frag);
    contactsInfo.textContent = `${rows.length.toLocaleString()}명`;
  }

  // ---- Security Events helpers ----
  function ensureDefaultEventDates() {
    if (!evFrom || !evTo) return;
    const today = new Date();
    const to = today.toISOString().slice(0,10);
    const fromDate = new Date(today.getTime() - 6*24*3600*1000);
    const from = fromDate.toISOString().slice(0,10);
    if (!evFrom.value) evFrom.value = from;
    if (!evTo.value)   evTo.value   = to;
  }

  function formatLocal(isoOrMillis) {
    try {
      const d = typeof isoOrMillis === 'string' ? new Date(isoOrMillis) : new Date(isoOrMillis);
      if (isNaN(d.getTime())) return '-';
      const y = d.getFullYear();
      const m = String(d.getMonth()+1).padStart(2,'0');
      const dd= String(d.getDate()).padStart(2,'0');
      const hh= String(d.getHours()).padStart(2,'0');
      const mi= String(d.getMinutes()).padStart(2,'0');
      const ss= String(d.getSeconds()).padStart(2,'0');
      return `${y}-${m}-${dd} ${hh}:${mi}:${ss}`;
    } catch { return '-'; }
  }

  // payload → 악성앱 패키지 추출
  function extractMalwarePackage(payload) {
    if (!payload) return '-';
    try {
      if (typeof payload === 'string' && payload.trim().startsWith('{')) {
        const obj = JSON.parse(payload);
        if (obj && typeof obj === 'object') {
          if (obj.malwarePackage) return String(obj.malwarePackage);
          if (obj.pkg || obj.packageName) return String(obj.pkg || obj.packageName);
        }
      }
    } catch (_) {}
    const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
    const m1 = text.match(/\/(com[\w.]+)[^\/,\s]*\/base\.apk/i);
    if (m1 && m1[1]) return m1[1];
    const m2 = text.match(/\b(com[\w.]+)\b/);
    if (m2 && m2[1]) return m2[1];
    return '-';
  }

  // payload → 악성앱 유형 추출 (예: ",Android.TestVirus\n" → Android.TestVirus)
  function extractMalwareType(payload) {
    if (!payload) return '-';
    try {
      if (typeof payload === 'string' && payload.trim().startsWith('{')) {
        const obj = JSON.parse(payload);
        if (obj && typeof obj === 'object') {
          if (obj.malwareType) return String(obj.malwareType);
          if (obj.type) return String(obj.type);
        }
      }
    } catch (_) {}
    const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
    const m = text.match(/base\.apk\s*,\s*([^\r\n,]+)/i);
    if (m && m[1]) return m[1].trim();
    const cidx = text.lastIndexOf(',');
    if (cidx >= 0 && cidx + 1 < text.length) {
      let t = text.substring(cidx + 1).trim();
      const nl = t.indexOf('\n');
      if (nl >= 0) t = t.substring(0, nl).trim();
      if (t) return t;
    }
    return '-';
  }

  async function fetchSecurityEvents(customerCode, from, to, type, page=0, size=20) {
    const params = new URLSearchParams({
      customerCode, from, to,
      type: type || 'ALL',
      page: String(page),
      size: String(size),
      sort: 'createdAt,DESC'
    });
    const url = `/api/events/security?${params.toString()}`;
    const res = await fetch(url, { headers: authHeaders(), credentials: 'same-origin' });
    if (!res.ok) {
      const t = await res.text().catch(()=> '');
      throw new Error(`이벤트 로드 실패: HTTP ${res.status} ${t}`);
    }
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('json')) return { kpi:{}, items:[], page:{ page:0,totalPages:0,totalElements:0,size } };
    return await res.json();
  }

  function renderEventsKpi(kpi) {
    evTotal.textContent   = fmt(kpi?.total);
    evMalware.textContent = fmt(kpi?.malware);
    evRooting.textContent = fmt(kpi?.rooting);
    evRemote.textContent  = fmt(kpi?.remote);
  }

  function renderEventsTable(items) {
    tblEvents.innerHTML = '';
    if (!Array.isArray(items) || items.length === 0) {
      tblEvents.innerHTML = `<tr><td colspan="6" class="muted" style="text-align:center;padding:14px">데이터가 없습니다.</td></tr>`;
      return;
    }
    const frag = document.createDocumentFragment();
    items.forEach(e => {
      const when = formatLocal(e.createdAt);
      const type = e.type || e.eventType || '-';
      const dev  = e.deviceId || e.objectId || '-';
      const ip   = e.ip || '-';

      const pkg  = e.malwarePackage ?? extractMalwarePackage(e.payload);
      const mtyp = e.malwareType    ?? extractMalwareType(e.payload);

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${when}</td>
        <td>${type}</td>
        <td>${dev}</td>
        <td>${pkg || '-'}</td>
        <td>${mtyp || '-'}</td>
        <td>${ip}</td>
      `;
      frag.appendChild(tr);
    });
    tblEvents.appendChild(frag);
  }

  async function loadSecurityEvents(resetPage=false) {
    if (!state?.profile?.code) return;
    ensureDefaultEventDates();
    const code = state.profile.code;
    const from = evFrom?.value;
    const to   = evTo?.value;
    const type = evType?.value || '';

    if (resetPage) evState.page = 0;

    evState.lastQuery = { code, from, to, type };
    try {
      const data = await fetchSecurityEvents(code, from, to, type, evState.page, evState.size);
      renderEventsKpi(data.kpi || {});
      renderEventsTable(data.items || []);
      const pg = data.page || { page:0, totalPages:1, totalElements:0, size: evState.size };
      evState.page = pg.page ?? evState.page;
      evState.totalPages = pg.totalPages ?? 0;
      evInfo.textContent = `페이지 ${ (evState.page+1).toLocaleString() } / ${ Math.max(evState.totalPages,1).toLocaleString() } · 총 ${ (pg.totalElements??0).toLocaleString() }건`;
      evPrev.disabled = evState.page <= 0;
      evNext.disabled = evState.totalPages === 0 || evState.page >= evState.totalPages - 1;
    } catch (e) {
      console.error('[events] load error:', e);
      evInfo.textContent = e.message || '이벤트 로드 실패';
      tblEvents.innerHTML = `<tr><td colspan="6" class="muted" style="text-align:center;padding:14px">불러오기 실패</td></tr>`;
    }
  }

  // ---- Load (profile/monthly/contacts) ----
  async function loadAll() {
    try {
      msgEl && (msgEl.textContent = '불러오는 중...');
      const p = await fetchProfile(state.id);
      if (!p || (Object.keys(p).length === 0 && p.constructor === Object)) {
        msgEl.textContent = '고객사 정보를 찾을 수 없습니다.';
        emptyEl?.classList.remove('hidden');
        contEl?.classList.add('hidden');
        btnLoad?.classList.remove('hidden');
        return;
      }
      state.profile = p;
      renderProfile(p);

      const m = await fetchMonthly(state.id).catch(() => []);
      state.monthly = m;
      renderMonthly(m);

      const contacts = await getContacts(state.profile.code).catch(()=>[]);
      renderContacts(contacts);

      // 프로필 로드 이후 자동으로 보안 이벤트 조회
      await loadSecurityEvents(true);

      msgEl && (msgEl.textContent = '');
      btnEdit?.classList.remove('hidden');
      btnLoad?.classList.add('hidden');
    } catch (e) {
      console.error('[customer_detail] loadAll error:', e);
      msgEl && (msgEl.textContent = e.message || '불러오기 실패');
      emptyEl?.classList.remove('hidden');
      contEl?.classList.add('hidden');
      btnLoad?.classList.remove('hidden');
    }
  }

  // ---- Profile collapse ----
  if (profileCard && btnToggleProfile) {
    setCollapsed(true);
    btnToggleProfile.addEventListener('click', () => {
      const now = profileCard.dataset.collapsed === 'true';
      setCollapsed(!now);
    });
    function setCollapsed(collapsed) {
      profileCard.dataset.collapsed = collapsed ? 'true' : 'false';
      btnToggleProfile.setAttribute('aria-expanded', (!collapsed).toString());
      const label = btnToggleProfile.querySelector('span > span');
      if (label) label.textContent = collapsed ? '펼치기' : '접기';
    }
  }

  // ---- Profile actions ----
  btnLoad?.addEventListener('click', loadAll);
  btnEdit?.addEventListener('click', () => setEditMode(true));
  btnReset?.addEventListener('click', () => { fillForm(state.profile); msgEl.textContent = ''; });
  btnSave?.addEventListener('click', async () => {
    try {
      msgEl.textContent = '저장 중...';
      const payload = {
        name:   fld.name.value.trim(),
        cpiRate:fld.cpiRate.value ? Number(fld.cpiRate.value) : null,
        rsRate: fld.rsRate.value  ? Number(fld.rsRate.value)  : null,
        note:   fld.note.value.trim(),
      };
      const updated = await updateProfile(state.id, payload);
      state.profile = { ...state.profile, ...updated };
      renderProfile(state.profile);
      setEditMode(false);
      msgEl.textContent = '저장됨';
      setTimeout(() => (msgEl.textContent = ''), 1500);
    } catch (e) {
      msgEl.textContent = e.message || '저장 실패';
    }
  });
  btnDel?.addEventListener('click', async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try { await deleteCustomer(state.id); alert('삭제되었습니다.'); location.href = '/customers'; }
    catch (e) { alert(e.message || '삭제 실패'); }
  });

  // ---- Contact modal ----
  function openContactModal() {
    contactErr.textContent = '';
    if (contactInfo) contactInfo.textContent = '';
    fldCM.name.value=''; fldCM.email.value=''; fldCM.phone.value=''; fldCM.note.value='';
    contactModal.classList.add('show');
    contactModal.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    setTimeout(() => fldCM.name?.focus(), 0);
  }
  function closeContactModal() {
    contactModal.classList.remove('show');
    contactModal.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    contactErr.textContent = '';
    if (contactInfo) contactInfo.textContent = '';
  }

  btnOpenContact?.addEventListener('click', () => {
    if (!state.profile?.code) return alert('고객사 코드가 없습니다. 먼저 고객사 정보를 불러오세요.');
    fldCM.code.value = state.profile.code;
    openContactModal();
  });
  btnCloseContact?.addEventListener('click', closeContactModal);
  contactModal?.addEventListener('click', (e) => { if (e.target?.id === 'contactModal') closeContactModal(); });
  btnContactReset?.addEventListener('click', () => {
    fldCM.name.value=''; fldCM.email.value=''; fldCM.phone.value=''; fldCM.note.value='';
    contactErr.textContent = '';
    if (contactInfo) contactInfo.textContent = '';
  });
  btnSaveContact?.addEventListener('click', async () => {
    contactErr.textContent = '';
    if (contactInfo) contactInfo.textContent = '';
    const code = (fldCM.code.value || '').trim();
    const payload = {
      name:  (fldCM.name.value  || '').trim(),
      email: (fldCM.email.value || '').trim(),
      phone: (fldCM.phone.value || '').trim(),
      note:  (fldCM.note.value  || '').trim(),
    };
    if (!payload.name)  return contactErr.textContent = '이름은 필수입니다.';
    if (payload.email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(payload.email)) {
      return contactErr.textContent = '이메일 형식이 올바르지 않습니다.';
    }
    try {
      const res = await createContact(code, payload);
      if (res?.generatedUsername || res?.tempPassword) {
        const msg = [
          res.generatedUsername ? `계정: ${res.generatedUsername}` : null,
          res.tempPassword ? `임시비번: ${res.tempPassword}` : null,
          '메일함(스팸 포함)을 확인하세요.'
        ].filter(Boolean).join(' · ');
        if (contactInfo) contactInfo.textContent = msg;
        else alert(msg);
      }
      closeContactModal();
      const list = await getContacts(code);
      renderContacts(list);
    } catch (e) {
      contactErr.textContent = e.message || '담당자 추가 실패';
    }
  });

  // ---- Events controls ----
  btnEvLoad?.addEventListener('click', () => loadSecurityEvents(true));
  evPrev?.addEventListener('click', () => {
    if (evState.page > 0) { evState.page -= 1; loadSecurityEvents(false); }
  });
  evNext?.addEventListener('click', () => {
    if (evState.page < evState.totalPages - 1) { evState.page += 1; loadSecurityEvents(false); }
  });

  // ---- init ----
  document.addEventListener('DOMContentLoaded', async () => {
    state.id = resolveCustomerId();
    if (!state.id) {
      msgEl && (msgEl.textContent = '잘못된 접근입니다. id가 없습니다.');
      emptyEl?.classList.remove('hidden');
      contEl?.classList.add('hidden');
      return;
    }
    try {
      await detectBase();
      ensureDefaultEventDates(); // 먼저 기본기간 세팅
      await loadAll();           // 프로필 로드 → 이벤트 자동 조회
    } catch (e) {
      console.error(e);
      msgEl && (msgEl.textContent = e.message || 'API 탐지 실패');
    }
  });
})();
