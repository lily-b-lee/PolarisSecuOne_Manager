// static/js/customers.js
(() => {
  const $ = (s) => document.querySelector(s);

  // API 엔드포인트
  const API = '/api/admin/customers';

  const state = { rows: [], editingCode: null };

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
    return Number.isFinite(n) ? `${n.toFixed(2)}%` : `${v}%`;
  };
  const showErr = (msg) => { const el = $('#formErr'); if (el) el.textContent = msg || ''; };

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
    // PK는 code
    $('#custCode').value = nz(c?.code, '');
    $('#code').value     = nz(c?.code, '');
    $('#name').value     = nz(c?.name, '');
    // 서버 필드 우선 + 백워드 호환
    $('#cpiRate').value = nz(c?.cpiValue ?? c?.cpiRate, 0);
    $('#rsRate').value  = nz(c?.rsPercent ?? c?.rsRate, 0);
    $('#note').value    = nz(c?.note, '');
  }
  function resetForm() {
    fillForm(null);
    state.editingCode = null;
  }

  // --- API calls ---
  async function getList(q) {
    const url = q ? `${API}?q=${encodeURIComponent(q)}` : API;
    const res = await fetch(url, { headers: authHeaders(), credentials: 'same-origin' });
    if (res.status === 401 || res.status === 403) {
      const next = encodeURIComponent(location.pathname + location.search);
      // HomeController는 /admin/login (템플릿) 사용
      location.href = `/admin/login?next=${next}`;
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
      if (res.status === 409) throw new Error(data?.message || '이미 존재하는 회사 코드입니다.');
      throw new Error(data?.message || `생성 실패(HTTP ${res.status})`);
    }
    return data;
  }
  async function update(code, payload) {
    const res = await fetch(`${API}/${encodeURIComponent(code)}`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify(payload),
      credentials: 'same-origin',
    });
    const data = await res.json().catch(()=> ({}));
    if (!res.ok) {
      if (res.status === 409) throw new Error(data?.message || '중복/제약으로 수정 실패');
      throw new Error(data?.message || `수정 실패(HTTP ${res.status})`);
    }
    return data;
  }
  async function remove(code) {
    const res = await fetch(`${API}/${encodeURIComponent(code)}`, {
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
      const code = c.code; // PK 확정
      const cpi  = (c.cpiValue ?? c.cpiRate);
      const rs   = (c.rsPercent ?? c.rsRate);

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${c.code || '-'}</td>
        <td>${c.name || '-'}</td>
        <td style="text-align:right">${fmtPct(cpi)}</td>
        <td style="text-align:right">${fmtPct(rs)}</td>
        <td>
          <div class="quick">
            <button class="btn" data-act="detail" data-code="${code ?? ''}">상세</button>
            <button class="btn" data-act="edit" data-code="${code ?? ''}">수정</button>
            <button class="btn" data-act="del" data-code="${code ?? ''}">삭제</button>
          </div>
        </td>
      `;

      // 행 클릭 → 상세 (버튼 클릭과 구분)
      tr.addEventListener('click', (e) => {
        if (e.target?.dataset?.act) return; // 버튼이면 무시
        if (!code) return;
        location.href = `/customers/detail?id=${encodeURIComponent(code)}`;
      });

      // 각 버튼 이벤트
      tr.querySelectorAll('button[data-act]').forEach((btn) => {
        btn.addEventListener('click', async (e) => {
          e.stopPropagation();
          const act = btn.dataset.act;
          const codeAttr = btn.dataset.code;
          if (!codeAttr) return;

          if (act === 'detail') {
            location.href = `/customers/detail?id=${encodeURIComponent(codeAttr)}`;
          } else if (act === 'edit') {
            state.editingCode = codeAttr;
            fillForm(c);
            $('#code').disabled = true; // 코드 수정 방지
            openModal('고객사 수정');
          } else if (act === 'del') {
            if (!confirm('정말 삭제하시겠습니까?')) return;
            try { await remove(codeAttr); await refresh(); }
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
    const editing = state.editingCode; // null이면 생성 모드
    const payload = {
      code: ($('#code').value || '').trim(),
      name: ($('#name').value || '').trim(),
      // 서버 필드명에 맞춰 전송
      cpiValue:  $('#cpiRate').value ? Number($('#cpiRate').value) : null,
      rsPercent: $('#rsRate').value  ? Number($('#rsRate').value)  : null,
      note: ($('#note').value || '').trim(),
    };

    if (!editing && !payload.code) return showErr('회사 코드를 입력하세요.');
    if (!payload.name) return showErr('회사명을 입력하세요.');

    try {
      if (editing) {
        delete payload.code; // 코드 변경 방지
        await update(editing, payload);
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
