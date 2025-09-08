// /js/customers.js
(() => {
  const $ = (s) => document.querySelector(s);

  // API 엔드포인트 (필요시 조정)
  const API = '/api/admin/customers';

  const state = { rows: [], editingId: null };

  // --- auth helpers ---
  const getToken = () => localStorage.getItem('admin_token') || null;
  const authHeaders = () => {
    const h = { 'Content-Type': 'application/json' };
    const t = getToken(); if (t) h['Authorization'] = `Bearer ${t}`;
    return h;
  };

  // --- utils ---
  const nz = (v, d) => (v === null || v === undefined ? d : v);
  const fmtPct = (v) => {
    if (v === null || v === undefined) return '-';
    const n = Number(v);
    return isFinite(n) ? `${n.toFixed(2)}%` : `${v}%`;
  };
  function showErr(msg) { const el = $('#formErr'); if (el) el.textContent = msg || ''; }

  // --- modal helpers ---
  function openModal(title) {
    $('#modalTitle').textContent = title;
    const backdrop = $('#custModal');
    backdrop?.classList.add('show');
    backdrop?.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
  }
  function closeModal() {
    const backdrop = $('#custModal');
    backdrop?.classList.remove('show');
    backdrop?.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    showErr('');
  }

  // --- form helpers ---
  function fillForm(c) {
    $('#custId').value = nz(c?.id, '');
    $('#code').value = nz(c?.code, '');
    $('#name').value = nz(c?.name, '');
    $('#cpiRate').value = nz(c?.cpiRate, 0);
    $('#rsRate').value  = nz(c?.rsRate, 0);
    $('#note').value    = nz(c?.note, '');
  }
  function resetForm() {
    fillForm(null);
    state.editingId = null;
  }

  // --- API calls ---
  async function getList(q) {
    const url = q ? `${API}?q=${encodeURIComponent(q)}` : API;
    const res = await fetch(url, { headers: authHeaders(), credentials: 'same-origin' });
    if (res.status === 401 || res.status === 403) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.href = `/admin/login.html?next=${next}`;
      return [];
    }
    if (!res.ok) {
      console.error('목록 조회 실패:', res.status, await res.text().catch(()=> ''));
      throw new Error('목록 조회 실패');
    }
    return await res.json();
  }
  async function create(payload) {
    const res = await fetch(API, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(payload),
      credentials: 'same-origin',
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) {
      const msg = data?.message || `생성 실패(HTTP ${res.status})`;
      throw new Error(msg);
    }
    return data;
  }
  async function update(id, payload) {
    const res = await fetch(`${API}/${id}`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify(payload),
      credentials: 'same-origin',
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) {
      const msg = data?.message || `수정 실패(HTTP ${res.status})`;
      throw new Error(msg);
    }
    return data;
  }
  async function remove(id) {
    const res = await fetch(`${API}/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
      credentials: 'same-origin',
    });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(`삭제 실패(HTTP ${res.status}) ${t}`);
    }
  }

  // --- render ---
  function renderList(rows) {
    const tb = $('#custTable tbody');
    tb.innerHTML = '';

    if (!rows || rows.length === 0) {
      tb.innerHTML = `<tr><td colspan="5" class="muted" style="text-align:center;padding:24px">데이터가 없습니다.</td></tr>`;
      $('#listInfo').textContent = '-';
      return;
    }

    const frag = document.createDocumentFragment();
    rows.forEach((c) => {
      const id = c.id ?? c.customerId ?? c.code; // 안전한 식별자 추출
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${c.code || '-'}</td>
        <td>${c.name || '-'}</td>
        <td style="text-align:right">${fmtPct(c.cpiRate)}</td>
        <td style="text-align:right">${fmtPct(c.rsRate)}</td>
        <td>
          <div class="quick">
            <button class="btn" data-act="detail" data-id="${id ?? ''}">상세</button>
            <button class="btn" data-act="edit" data-id="${id ?? ''}">수정</button>
            <button class="btn" data-act="del" data-id="${id ?? ''}">삭제</button>
          </div>
        </td>
      `;

      // 행 클릭 → 상세 (버튼 클릭과 구분)
      tr.addEventListener('click', (e) => {
        if (e.target?.dataset?.act) return; // 버튼이면 무시
        if (!id) return;
        location.href = `/customers/detail?id=${encodeURIComponent(id)}`;
      });

      // 각 버튼 이벤트
      tr.querySelectorAll('button[data-act]').forEach((btn) => {
        btn.addEventListener('click', async (e) => {
          e.stopPropagation();
          const act = btn.dataset.act;
          if (act === 'detail') {
            if (!id) return alert('식별자가 없어 상세로 이동할 수 없습니다.');
            location.href = `/customers/detail?id=${encodeURIComponent(id)}`;
          } else if (act === 'edit') {
            state.editingId = id || null;
            fillForm(c);
            $('#code').disabled = true; // 코드 수정 방지
            openModal('고객사 수정');
          } else if (act === 'del') {
            if (!id) return alert('식별자가 없어 삭제할 수 없습니다.');
            if (!confirm('정말 삭제하시겠습니까?')) return;
            try { await remove(id); await refresh(); }
            catch (err) { alert(err.message || '삭제 실패'); }
          }
        });
      });

      frag.appendChild(tr);
    });
    tb.appendChild(frag);
    $('#listInfo').textContent = `${rows.length.toLocaleString()}건`;
  }

  async function refresh() {
    try {
      const q = ($('#q')?.value || '').trim();
      const rows = await getList(q);
      state.rows = rows || [];
      renderList(state.rows);
    } catch (e) {
      console.error(e);
      alert('목록을 불러오지 못했습니다.');
    }
  }

  // --- save ---
  async function onSave() {
    showErr('');
    const id = $('#custId').value || null;
    const payload = {
      code: ($('#code').value || '').trim(),
      name: ($('#name').value || '').trim(),
      cpiRate: $('#cpiRate').value ? Number($('#cpiRate').value) : null,
      rsRate:  $('#rsRate').value  ? Number($('#rsRate').value)  : null,
      note: ($('#note').value || '').trim(),
    };

    if (!id && !payload.code) return showErr('회사 코드를 입력하세요.');
    if (!payload.name) return showErr('회사명을 입력하세요.');

    try {
      if (id) {
        delete payload.code; // 코드 변경 방지
        await update(id, payload);
      } else {
        await create(payload);
      }
      closeModal();
      resetForm();
      $('#code').disabled = false;
      await refresh();
    } catch (e) {
      showErr(e.message || '저장 실패');
    }
  }

  // --- bind & init ---
  document.addEventListener('DOMContentLoaded', () => {
    // 검색/새로고침
    $('#btnSearch')?.addEventListener('click', refresh);
    $('#btnRefresh')?.addEventListener('click', refresh);
    $('#q')?.addEventListener('keydown', (e) => { if (e.key === 'Enter') refresh(); });

    // 새 고객사
    $('#btnNew')?.addEventListener('click', () => {
      resetForm();
      $('#code').disabled = false;
      openModal('고객사 등록');
    });

    // 모달 닫기
    $('#btnCloseModal')?.addEventListener('click', closeModal);
    $('#custModal')?.addEventListener('click', (e) => { if (e.target?.id === 'custModal') closeModal(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

    // 폼
    $('#btnReset')?.addEventListener('click', resetForm);
    $('#btnSave')?.addEventListener('click', onSave);

    // 최초 로드
    refresh();
  });
})();
