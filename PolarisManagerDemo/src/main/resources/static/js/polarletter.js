// /static/js/polarletter.js
(() => {
  // ---------- helpers ----------
  const $  = (s, p = document) => p.querySelector(s);
  const $$ = (s, p = document) => Array.from(p.querySelectorAll(s));
  const esc = (s) =>
    (s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  const todayYmd = () => {
    const d = new Date(); const z = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())}`;
  };
  const toDot = (ymd) => {
    if (!ymd) return '';
    const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd);
    return m ? `${m[1]}.${m[2]}.${m[3]}` : ymd;
  };
  const toYmd = (dot) => {
    if (!dot) return '';
    const m = /^(\d{4})\.(\d{2})\.(\d{2})$/.exec(dot);
    return m ? `${m[1]}-${m[2]}-${m[3]}` : dot;
  };

  // ---------- elements ----------
  const todayText   = $('#todayText');
  const totalCount  = $('#totalCount');
  const grid        = $('#grid');
  const fab         = $('#fabAdd');

  // modal
  const modal       = $('#plModal');
  const form        = $('#plForm');
  const msg         = $('#plMsg');
  const subTitle    = $('#plModalSub');
  const btnDelete   = $('#btnDelete');
  const btnSave     = $('#btnSave');

  const fId     = $('#mId');
  const fAuthor = $('#mAuthor');
  const fDate   = $('#mDate');
  const fTitle  = $('#mTitle');
  const fCont   = $('#mContent');
  const fUrl    = $('#mUrl');
  const fThumb  = $('#mThumb');

  // ---------- API base auto-detect ----------
  const CANDIDATES = ['/api/polarletters', '/polarletters'];
  let BASE = null;

  async function detectBase() {
    for (const p of CANDIDATES) {
      try {
        const r = await fetch(`${p}/_ping`, { cache: 'no-store' });
        if (r.ok) { BASE = p; return; }
      } catch (_) {}
    }
    throw new Error('Polarletter API endpoint not found (/_ping 실패)');
  }

  // ---------- UI ----------
  function setToday() {
    const d = new Date();
    const opts = { weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit' };
    todayText.textContent = d.toLocaleDateString('ko-KR', opts).replace(/\.\s*/g, '-');
  }

  async function loadList(limit = 24) {
    if (!BASE) throw new Error('BASE not detected');
    try {
      const res = await fetch(`${BASE}?limit=${encodeURIComponent(limit)}`, {
        headers: { 'Accept': 'application/json' },
        cache: 'no-store'
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const items = await res.json();
      renderList(Array.isArray(items) ? items : []);
    } catch (e) {
      console.error('[polarletter] loadList error:', e);
      grid.innerHTML = `<div class="empty">목록을 불러오지 못했습니다. (${esc(e.message)})</div>`;
      totalCount.textContent = '총 0개';
    }
  }

  function renderList(items) {
    totalCount.textContent = `총 ${items.length}개`;
    if (!items.length) {
      grid.innerHTML = '<div class="empty">데이터가 없습니다.</div>';
      return;
    }
    const frag = document.createDocumentFragment();
    items.forEach(x => {
      const el = document.createElement('article');
      el.className = 'news-card';
      el.dataset.id    = x.id ?? '';
      el.dataset.title = x.title ?? '';
      el.dataset.author= x.author ?? '';
      el.dataset.date  = x.createTime ?? x.date ?? '';
      el.dataset.url   = x.url ?? '';
      el.dataset.thumb = x.thumbnail ?? x.thumb ?? '';
      el.dataset.content = x.content ?? '';

      const img = esc(x.thumbnail || x.thumb || '');
      const author = esc(x.author || '');
      const date = esc(x.createTime || x.date || '');
      const title = esc(x.title || '');
      const url = esc(x.url || '');

      el.innerHTML = `
        <div class="thumb-wrap">${img ? `<img src="${img}" alt="" loading="lazy" onerror="this.remove()">` : ''}</div>
        <div class="meta"><span class="cat">${author || '무명'}</span>${date ? ` <span class="dot">·</span> <span class="date">${date}</span>` : ''}</div>
        <h3 class="title">${title}</h3>
        <div class="actions">
          ${url ? `<a class="link" href="${url}" target="_blank" rel="noopener">자세히</a>` : '<span></span>'}
          <span class="id muted">id: ${esc(x.id)}</span>
        </div>
      `;
      // 카드 클릭 → 편집 모달 열기
      el.addEventListener('click', (ev) => {
        // a 링크 클릭은 통과
        if ((ev.target).closest('a')) return;
        openEdit(el.dataset);
      });
      frag.appendChild(el);
    });
    grid.innerHTML = '';
    grid.appendChild(frag);
  }

  // ---------- modal open/close ----------
  function resetForm() {
    form.reset();
    msg.textContent = '';
    fId.value = '';
    btnDelete.style.display = 'none';
  }
  function openCreate() {
    subTitle.textContent = '새 항목 추가';
    resetForm();
    fDate.value = todayYmd();
    modal.classList.add('open');
    modal.setAttribute('aria-hidden','false');
    document.body.style.overflow = 'hidden';
    setTimeout(() => fTitle.focus(), 0);
  }
  function openEdit(data) {
    subTitle.textContent = '항목 수정';
    resetForm();
    fId.value     = data.id || '';
    fAuthor.value = data.author || '';
    fDate.value   = toYmd(data.date || '');
    fTitle.value  = data.title || '';
    fCont.value   = data.content || '';
    fUrl.value    = data.url || '';
    fThumb.value  = data.thumb || '';
    btnDelete.style.display = 'inline-block';

    modal.classList.add('open');
    modal.setAttribute('aria-hidden','false');
    document.body.style.overflow = 'hidden';
    setTimeout(() => fTitle.focus(), 0);
  }
  function closeModal() {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden','true');
    document.body.style.overflow = '';
    msg.textContent = '';
  }

  // ---------- CRUD ----------
  async function createItem() {
    const payload = {
      author:     fAuthor.value.trim() || undefined,
      title:      fTitle.value.trim(),
      content:    fCont.value.trim() || undefined,
      url:        fUrl.value.trim() || undefined,
      thumbnail:  fThumb.value.trim() || undefined,
      createTime: toDot(fDate.value) || undefined
    };
    if (!payload.title) throw new Error('제목은 필수입니다.');

    const res = await fetch(BASE, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify(payload)
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`HTTP ${res.status} ${text || ''}`);
    return text.replace(/"/g, '');
  }

  async function updateItem(id) {
    const payload = {
      author:     fAuthor.value.trim() || undefined,
      title:      fTitle.value.trim() || undefined,
      content:    fCont.value.trim() || undefined,
      url:        fUrl.value.trim() || undefined,
      thumbnail:  fThumb.value.trim() || undefined,
      createTime: toDot(fDate.value) || undefined
    };
    const res = await fetch(`${BASE}/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify(payload)
    });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(`HTTP ${res.status} ${t}`);
    }
  }

  async function deleteItem(id) {
    const res = await fetch(`${BASE}/${encodeURIComponent(id)}`, { method:'DELETE' });
    if (!res.ok && res.status !== 204) {
      const t = await res.text().catch(()=> '');
      throw new Error(`HTTP ${res.status} ${t}`);
    }
  }

  // ---------- events ----------
  fab?.addEventListener('click', openCreate);

  modal?.addEventListener('click', (e) => {
    if (e.target === modal || e.target.hasAttribute('data-close')) closeModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('open')) closeModal();
  });

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      msg.textContent = '저장 중...';
      btnSave.disabled = true;

      if (fId.value) {
        await updateItem(fId.value);
      } else {
        await createItem();
      }
      closeModal();
      await loadList();
    } catch (err) {
      msg.textContent = `실패: ${err.message}`;
    } finally {
      btnSave.disabled = false;
    }
  });

  btnDelete?.addEventListener('click', async () => {
    if (!fId.value) return;
    if (!confirm('정말 삭제할까요?')) return;
    try {
      msg.textContent = '삭제 중...';
      await deleteItem(fId.value);
      closeModal();
      await loadList();
    } catch (err) {
      msg.textContent = `삭제 실패: ${err.message}`;
    }
  });

  // ---------- boot ----------
  window.addEventListener('DOMContentLoaded', async () => {
    setToday();
    try {
      await detectBase();
      await loadList();
    } catch (e) {
      console.error('[polarletter] init error:', e);
      grid.innerHTML = `<div class="empty">API 엔드포인트를 찾지 못했습니다.</div>`;
    }
  });
})();
