// /static/js/customers.js  (drop-in)
(() => {
  console.log('[customers] script loaded');

  const $ = (s, p=document) => p.querySelector(s);
  const API = '/api/admin/customers';
  const state = { rows: [], editingId: null };

  // ---- auth
  const getToken = () => localStorage.getItem('admin_token') || null;
  const authHeaders = () => {
    const h = { 'Content-Type': 'application/json' };
    const t = getToken(); if (t) h.Authorization = `Bearer ${t}`;
    return h;
  };

  // ---- utils
  const nz = (v, d) => (v === null || v === undefined ? d : v);
  const pct = (v) => (v === null || v === undefined || v === '' ? '-' : `${Number(v).toFixed(2)}%`);
  const num = (v) => (v === null || v === undefined || v === '' ? '-' : String(v));
  const showErr = (m='') => { const el = $('#formErr'); if (el) el.textContent = m; };

  // ---- modal
  function openModal(title) {
    $('#modalTitle').textContent = title || '고객사';
    const wrap = $('#custModal'); if (!wrap) return;
    wrap.classList.add('show','open'); wrap.style.display = 'flex';
    wrap.removeAttribute('aria-hidden'); document.body.style.overflow = 'hidden';
  }
  function closeModal() {
    const wrap = $('#custModal'); if (!wrap) return;
    wrap.classList.remove('show','open'); wrap.style.display = 'none';
    wrap.setAttribute('aria-hidden','true'); document.body.style.overflow = '';
    showErr('');
  }

  // ---- form helpers
  function fillForm(c) {
    $('#custId').value   = nz(c?.id ?? c?.code, '');
    $('#code').value     = nz(c?.code, '');
    $('#name').value     = nz(c?.name, '');
    $('#cpiRate').value  = nz(c?.cpiValue, 0);   // 서버 키에 맞춰 채움
    $('#rsRate').value   = nz(c?.rsPercent, 0);  // 서버 키에 맞춰 채움
    $('#note').value     = nz(c?.note, '');
  }
  function resetForm(){ fillForm(null); state.editingId = null; }

  // ---- API
  async function getList(q) {
    const url = q ? `${API}?q=${encodeURIComponent(q)}` : API;
    const res = await fetch(url, { headers: authHeaders(), credentials: 'same-origin' });
    if (res.status === 401 || res.status === 403) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.href = `/admin/login?next=${next}`;
      return [];
    }
    if (!res.ok) throw new Error(`목록 조회 실패(HTTP ${res.status})`);
    return res.json();
  }
  async function create(payload) {
    const res = await fetch(API, {
      method:'POST', headers:authHeaders(), body:JSON.stringify(payload), credentials:'same-origin'
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) throw new Error(data?.message || `생성 실패(HTTP ${res.status})`);
    return data;
  }
  async function update(id, payload) {
    const res = await fetch(`${API}/${encodeURIComponent(id)}`, {
      method:'PATCH', headers:authHeaders(), body:JSON.stringify(payload), credentials:'same-origin'
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) throw new Error(data?.message || `수정 실패(HTTP ${res.status})`);
    return data;
  }
  async function remove(id) {
    const res = await fetch(`${API}/${encodeURIComponent(id)}`, {
      method:'DELETE', headers:authHeaders(), credentials:'same-origin'
    });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(`삭제 실패(HTTP ${res.status}) ${t}`);
    }
  }

  // ---- render
  function renderList(rows) {
    const tb = $('#custTable tbody'); if (!tb) return;
    tb.innerHTML = '';
    if (!rows?.length) {
      tb.innerHTML = `<tr><td colspan="5" class="muted" style="text-align:center;padding:24px">데이터가 없습니다.</td></tr>`;
      const li = $('#listInfo'); if (li) li.textContent = '-';
      return;
    }
    const frag = document.createDocumentFragment();
    rows.forEach(c => {
      const id = c.code ?? c.id ?? c.customerId;
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${c.code || '-'}</td>
        <td>${c.name || '-'}</td>
        <td class="num">${pct(c.cpiValue)}</td>
        <td class="num">${pct(c.rsPercent)}</td>
        <td>
          <div class="quick">
            <button class="btn" data-act="detail" data-id="${id}" type="button">상세</button>
            <button class="btn" data-act="edit"   data-id="${id}" type="button">수정</button>
            <button class="btn" data-act="del"    data-id="${id}" type="button">삭제</button>
          </div>
        </td>
      `;
      tr.addEventListener('click', e => {
        if (e.target?.dataset?.act) return;
        if (id) location.href = `/customers/detail?id=${encodeURIComponent(id)}`;
      });
      tr.querySelectorAll('button[data-act]').forEach(btn => {
        btn.addEventListener('click', async e => {
          e.stopPropagation();
          const act = btn.dataset.act;
          if (act === 'detail') {
            if (id) location.href = `/customers/detail?id=${encodeURIComponent(id)}`;
          } else if (act === 'edit') {
            state.editingId = id || null;
            fillForm(c); $('#code').disabled = true; openModal('고객사 수정');
          } else if (act === 'del') {
            if (!id) return alert('식별자가 없어 삭제할 수 없습니다.');
            if (!confirm('정말 삭제하시겠습니까?')) return;
            try { await remove(id); await refresh(); } catch (err) { alert(err.message || '삭제 실패'); }
          }
        });
      });
      frag.appendChild(tr);
    });
    tb.appendChild(frag);
    const li = $('#listInfo'); if (li) li.textContent = `${rows.length.toLocaleString()}건`;
  }

  async function refresh() {
    try {
      const q = ($('#q')?.value || '').trim();
      state.rows = await getList(q);
      renderList(state.rows);
    } catch (e) {
      console.error(e); alert('목록을 불러오지 못했습니다.');
    }
  }

  // ---- save
  async function onSave() {
    showErr('');
    const id = $('#custId').value || null;
    const code = ($('#code').value || '').trim();
    const name = ($('#name').value || '').trim();
    const cpi = $('#cpiRate').value ? Number($('#cpiRate').value) : null;
    const rs  = $('#rsRate').value  ? Number($('#rsRate').value)  : null;
    const note = ($('#note').value || '').trim();

    if (!id && !code) return showErr('회사 코드를 입력하세요.');
    if (!name) return showErr('회사명을 입력하세요.');
    // 간단 검증 (필요시 조정)
    if (cpi != null && (isNaN(cpi) || cpi < 0 || cpi > 100)) return showErr('CPI %는 0~100 사이여야 합니다.');
    if (rs  != null && (isNaN(rs)  || rs  < 0 || rs  > 100)) return showErr('RS %는 0~100 사이여야 합니다.');

    // ✅ 서버 DTO 필드명에 맞춰 보냄
    const payload = {
      ...(id ? {} : { code }),
      name,
      cpiValue: cpi,      // <- was cpiRate
      rsPercent: rs,      // <- was rsRate
      note
    };

    try {
      if (id) await update(id, payload);
      else    await create(payload);

      closeModal(); resetForm(); $('#code').disabled = false; await refresh();
    } catch (e) {
      showErr(e.message || '저장 실패');
    }
  }

  // ---- bind & init
  document.addEventListener('DOMContentLoaded', () => {
    $('#btnSearch')?.addEventListener('click', e => { e.preventDefault(); refresh(); });
    $('#btnRefresh')?.addEventListener('click', e => { e.preventDefault(); refresh(); });
    $('#q')?.addEventListener('keydown', e => { if (e.key === 'Enter') refresh(); });

    $('#btnNew')?.addEventListener('click', e => {
      e.preventDefault(); resetForm(); $('#code').disabled = false; openModal('고객사 등록');
    });
    $('#btnCloseModal')?.addEventListener('click', e => { e.preventDefault(); closeModal(); });
    $('#custModal')?.addEventListener('click', e => { if (e.target?.id === 'custModal') closeModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

    $('#btnReset')?.addEventListener('click', e => { e.preventDefault(); resetForm(); });
    $('#btnSave')?.addEventListener('click',  e => { e.preventDefault(); onSave(); });

    refresh().catch(console.error);
  });
})();
