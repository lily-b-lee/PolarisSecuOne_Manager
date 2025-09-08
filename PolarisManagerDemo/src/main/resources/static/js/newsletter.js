// /static/js/newsletter.js
(function () {
  console.log('[newsletter] script loaded');

  const $  = (sel, p = document) => p.querySelector(sel);
  const $$ = (sel, p = document) => [...p.querySelectorAll(sel)];
  const esc = s => (s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  const todayText  = $('#todayText');
  const totalCount = $('#totalCount');
  const grid       = $('#grid');

  // 엔드포인트 자동 탐색
  const CANDIDATES = ['/newsletters', '/secu-news', '/api/newsletters', '/api/secu-news'];
  let BASE = null;

  async function detectBase() {
    for (const p of CANDIDATES) {
      try {
        const r = await fetch(`${p}/_ping`, { cache: 'no-store' });
        if (r.ok) { BASE = p; console.log('[newsletter] BASE =', BASE); return; }
      } catch (_) {}
    }
    throw new Error('No newsletter API endpoint found (/_ping 실패)');
  }

  function setToday() {
    const d = new Date();
    const opts = { weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit' };
    todayText.textContent = d.toLocaleDateString('ko-KR', opts).replace(/\.\s*/g, '-');
  }

  // 목록/캐시
  let cache = []; // [{id,title,category,date,thumbnail,url}, ...]

  async function loadList(limit = 12) {
    if (!BASE) throw new Error('BASE not detected');
    try {
      const res = await fetch(`${BASE}?limit=${encodeURIComponent(limit)}`, {
        headers: { Accept: 'application/json' },
        cache: 'no-store',
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const items = await res.json();
      cache = Array.isArray(items) ? items : [];
      renderList(cache);
    } catch (e) {
      console.error('[newsletter] loadList error:', e);
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
      const card = document.createElement('article');
      card.className   = 'news-card';
      card.tabIndex    = 0;                       // 접근성/키보드 포커스
      card.dataset.id  = x.id || '';
      const img = esc(x.thumbnail || '');
      const cat = esc(x.category || '');
      const date = esc(x.date || x.createTime || '');
      const title = esc(x.title || '');
      const url = esc(x.url || '');

      card.innerHTML = `
        <div class="thumb-wrap">${img ? `<img src="${img}" alt="" loading="lazy" onerror="this.remove()">` : ''}</div>
        <div class="meta"><span class="cat">${cat}</span>${date ? ` <span class="dot">·</span> <span class="date">${date}</span>` : ''}</div>
        <h3 class="title">${title}</h3>
        <div class="actions">
          ${url ? `<a class="link" href="${url}" target="_blank" rel="noopener">원문 보기</a>` : '<span></span>'}
          <span class="id muted">id: ${esc(x.id)}</span>
        </div>
      `;
      frag.appendChild(card);
    });
    grid.innerHTML = '';
    grid.appendChild(frag);
  }

  /* ===========================
   * 모달 (추가/수정/삭제)
   * =========================== */
  const modal  = $('#newsModal');
  const form   = $('#modalForm');
  const msg    = $('#modalMsg');
  const fab    = $('#fabAdd');
  const tInput = $('#mTitle');
  const dInput = $('#mDate');
  const cInput = $('#mCategory');
  const uInput = $('#mUrl');
  const thInput= $('#mThumb');
  const titleEl= $('#newsModalTitle');
  const subEl  = modal?.querySelector('.subtitle');
  const btnDelete = $('#btnDelete');

  let current = null; // 현재 편집중 항목(없으면 생성 모드)

  const todayYmd = () => {
    const d = new Date(); const z = n => String(n).padStart(2,'0');
    return `${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())}`;
  };

  function openModalCreate(){
    current = null;
    modal.classList.add('open');
    modal.setAttribute('aria-hidden','false');
    document.body.style.overflow = 'hidden';
    titleEl.textContent = '시큐뉴스';
    subEl.textContent   = '새 항목 추가';

    // 기본값
    dInput.value  = todayYmd();
    tInput.value  = '';
    cInput.value  = '';
    uInput.value  = '';
    thInput.value = '';
    btnDelete.hidden = true;

    setTimeout(() => tInput?.focus(), 0);
  }

  function openModalEdit(item){
    current = item;
    modal.classList.add('open');
    modal.setAttribute('aria-hidden','false');
    document.body.style.overflow = 'hidden';
    titleEl.textContent = '시큐뉴스';
    subEl.textContent   = '항목 수정';

    dInput.value  = (item.date || item.createTime || '').replace(/\./g,'-') || todayYmd();
    tInput.value  = item.title || '';
    cInput.value  = item.category || '';
    uInput.value  = item.url || '';
    thInput.value = item.thumbnail || '';
    btnDelete.hidden = !item.id;

    setTimeout(() => tInput?.focus(), 0);
  }

  function closeModal(){
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden','true');
    document.body.style.overflow = '';
    msg.textContent = '';
    form.reset();
    current = null;
  }

  // 열기: FAB = 생성
  fab?.addEventListener('click', openModalCreate);

  // 닫기: 배경 클릭 / X 버튼
  modal?.addEventListener('click', (e) => {
    if (e.target === modal || e.target.closest('[data-close]')) closeModal();
  });

  // ESC 닫기
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modal.classList.contains('open')) closeModal();
  });

  // 카드 클릭 → 수정 모달
  grid?.addEventListener('click', (e) => {
    const card = e.target.closest('.news-card');
    if (!card) return;
    const id = card.dataset.id;
    const item = cache.find(it => String(it.id) === String(id));
    if (!item) return;
    openModalEdit(item);
  });

  // 키보드 접근: Enter로 열기
  grid?.addEventListener('keydown', (e) => {
    if (e.key !== 'Enter') return;
    const card = e.target.closest('.news-card');
    if (!card) return;
    const id = card.dataset.id;
    const item = cache.find(it => String(it.id) === String(id));
    if (!item) return;
    openModalEdit(item);
  });

  // 저장(생성 or 수정)
  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!BASE) { msg.textContent = 'API 미탐지'; return; }

    const payload = {
      title:     tInput.value.trim(),
      category:  cInput.value.trim(),
      date:      dInput.value || todayYmd(),
      url:       uInput.value.trim(),
      thumbnail: thInput.value.trim(),
    };
    if (!payload.title){ msg.textContent = '제목은 필수입니다.'; return; }

    msg.textContent = current ? '수정 중...' : '저장 중...';

    try{
      let res, text;
      if (current?.id) {
        // 수정
        res  = await fetch(`${BASE}/${encodeURIComponent(current.id)}`, {
          method: 'PUT', headers: {'Content-Type':'application/json'},
          body: JSON.stringify(payload)
        });
        text = await res.text();
        if (!res.ok) throw new Error(`HTTP ${res.status} ${text||''}`);
      } else {
        // 생성
        res  = await fetch(BASE, {
          method:'POST', headers:{'Content-Type':'application/json'},
          body: JSON.stringify(payload)
        });
        text = await res.text();
        if (!res.ok) throw new Error(`HTTP ${res.status} ${text||''}`);
      }

      closeModal();
      await loadList();
    }catch(err){
      msg.textContent = `실패: ${err.message}`;
    }
  });

  // 삭제
  btnDelete?.addEventListener('click', async () => {
    if (!current?.id || !BASE) return;
    if (!confirm('정말 삭제하시겠어요?')) return;

    try{
      const res = await fetch(`${BASE}/${encodeURIComponent(current.id)}`, { method:'DELETE' });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      closeModal();
      await loadList();
    }catch(err){
      msg.textContent = `삭제 실패: ${err.message}`;
    }
  });

  // 초기화
  window.addEventListener('DOMContentLoaded', async () => {
    setToday();
    try {
      await detectBase();
      await loadList(12);
    } catch (e) {
      console.error('[newsletter] init error:', e);
      grid.innerHTML = `<div class="empty">API 엔드포인트를 찾지 못했습니다.</div>`;
    }
  });
})();
