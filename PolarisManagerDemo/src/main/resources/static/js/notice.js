// /static/js/notice.js
(function () {
  const $  = (s, p=document) => p.querySelector(s);
  const $$ = (s, p=document) => [...p.querySelectorAll(s)];
  const esc = s => (s ?? '').toString().replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const todayText = $('#todayText'), totalCount = $('#totalCount'), tbody = $('#tbody'), empty = $('#empty');
  const fab = $('#fabAdd'), modal = $('#noticeModal'), form = $('#noticeForm'), msg = $('#modalMsg'), rowMenu = $('#rowMenu');
  const mId=$('#mId'), mTitle=$('#mTitle'), mContent=$('#mContent'), mAuthor=$('#mAuthor'), mCategory=$('#mCategory'), mDate=$('#mDate');
  const btnDelete = $('#btnDelete');
  // 푸시 체크박스(없어도 동작)
  const mSendPush = $('#mSendPush') || { checked: false };
  const mPushTest = $('#mPushTest') || { checked: false };

  const CANDIDATES = ['/api/notices','/notices','/api/notice','/notice'];
  let BASE = null;         // 탐지된 엔드포인트
  let DATA = [];           // 목록 캐시
  let menuTargetId = null; // 행 메뉴 대상 id

  // 유틸
  const toDot = ymd => /^\d{4}-\d{2}-\d{2}$/.test(ymd) ? ymd.replaceAll('-', '.') : (ymd || '');
  const toInputDate = s => {
    if (!s) return '';
    if (/^\d{4}\.\d{2}\.\d{2}$/.test(s)) return s.replaceAll('.', '-');
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;
    return '';
  };
  const toKey = d => +((d || '').replaceAll('.', '').slice(0, 8)) || 0; // 2025.07.21 -> 20250721

  function setToday() {
    const d = new Date(), opt = { weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit' };
    todayText.textContent = d.toLocaleDateString('ko-KR', opt).replace(/\.\s*/g, '-');
  }

  async function detectBase() {
    for (const p of CANDIDATES) {
      try {
        const r = await fetch(`${p}/_ping`, { cache: 'no-store' });
        if (r.ok) { BASE = p; return; }
      } catch (_) {}
    }
    throw new Error('No NOTICE API endpoint found');
  }

  // 목록 로드(백엔드 정렬 보강을 위해 클라에서도 추가 정렬)
  async function loadList(limit = 20) {
    if (!BASE) throw new Error('BASE not set');
    const res = await fetch(`${BASE}?limit=${encodeURIComponent(limit)}`, {
      headers: { Accept: 'application/json' },
      cache: 'no-store'
    });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const list = await res.json();
    DATA = (Array.isArray(list) ? list : []).sort((a, b) => toKey(b.date) - toKey(a.date));
    renderTable(DATA);
  }

  // 카테고리 칩 스타일
  function chipCls(v) {
    const s = (v || '').toString().toUpperCase();
    if (s.includes('EMERGENCY')) return 'chip emergency';
    if (s.includes('EVENT'))     return 'chip event';
    if (s.includes('SERVICE') || s.includes('GUIDE')) return 'chip guide';
    if (s.includes('UPDATE'))    return 'chip update';
    return 'chip';
  }

  // 테이블 렌더
  function renderTable(items) {
    totalCount.textContent = items.length;
    if (!items.length) {
      tbody.innerHTML = '';
      empty.hidden = false;
      return;
    }
    empty.hidden = true;

    const frag = document.createDocumentFragment();
    items.forEach(it => {
      const tr = document.createElement('tr');
      tr.dataset.id = it.id;

      const title = esc(it.title);
      const content = esc(it.content || '');
      const author = esc(it.author || '');
      const category = esc(it.category || '');
      const date = esc(it.date || it.createTime || '');

      tr.innerHTML = `
        <td class="truncate">${title}</td>
        <td class="truncate small" title="${content}">${content}</td>
        <td>${author}</td>
        <td><span class="${chipCls(category)}">${category || '—'}</span></td>
        <td>${date}</td>
        <td><button type="button" class="more" aria-label="행 메뉴">⋯</button></td>
      `;
      frag.appendChild(tr);
    });
    tbody.innerHTML = '';
    tbody.appendChild(frag);
  }

  // 모달 열기/닫기
  function openModal(mode, item) {
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';
    msg.textContent = '';

    if (mode === 'create') {
      $('#modalSub').textContent = '새 항목 추가';
      mId.value = '';
      mTitle.value = '';
      mContent.value = '';
      mAuthor.value = '';
      if (mCategory) {
        // select면 기본 첫 옵션, input이면 빈 값
        if (mCategory.tagName === 'SELECT') mCategory.selectedIndex = 0; else mCategory.value = '';
      }
      const d = new Date(); const z = n => String(n).padStart(2, '0');
      mDate.value = `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
      btnDelete.style.display = 'none';
      if ('checked' in mSendPush) mSendPush.checked = false;
      if ('checked' in mPushTest) mPushTest.checked = false;
    } else {
      $('#modalSub').textContent = '항목 수정';
      mId.value = item.id || '';
      mTitle.value = item.title || '';
      mContent.value = item.content || '';
      mAuthor.value = item.author || '';
      if (mCategory) {
        const val = (item.category || '').toString();
        if (mCategory.tagName === 'SELECT') {
          // 옵션 중 일치 항목이 없으면 그대로(브라우저가 무시)
          mCategory.value = val;
        } else {
          mCategory.value = val;
        }
      }
      const dt = item.date || item.createTime || '';
      mDate.value = toInputDate(dt);
      btnDelete.style.display = 'inline-flex';
      if ('checked' in mSendPush) mSendPush.checked = false;
      if ('checked' in mPushTest) mPushTest.checked = false;
    }
  }
  function closeModal() {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
    msg.textContent = '';
    form.reset();
  }

  // 행 메뉴
  function openRowMenu(btn, id) {
    const rect = btn.getBoundingClientRect();
    rowMenu.style.left = `${Math.min(window.innerWidth - 160, rect.left)}px`;
    rowMenu.style.top  = `${rect.bottom + 6}px`;
    rowMenu.hidden = false;
    menuTargetId = id;
  }
  function closeRowMenu() { rowMenu.hidden = true; menuTargetId = null; }

  // 저장
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!BASE) return;

    const payload = {
      title:   mTitle.value.trim(),
      content: mContent.value.trim(),
      author:  mAuthor.value.trim(),
      category: (mCategory?.tagName === 'SELECT' ? mCategory.value : mCategory?.value?.trim()) || '',
      date:    toDot(mDate.value) || '' // "yyyy.MM.dd" 로 통일
    };
    if (!payload.title) { msg.textContent = '제목은 필수입니다.'; return; }

    // 푸시 옵션 쿼리
    const qs = new URLSearchParams({
      sendPush: mSendPush.checked ? 'true' : 'false',
      test:     mPushTest.checked ? 'true' : 'false'
    }).toString();

    try {
      let res, text;
      if (mId.value) { // update
        res = await fetch(`${BASE}/${encodeURIComponent(mId.value)}?${qs}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('HTTP ' + res.status);
      } else { // create
        res = await fetch(`${BASE}?${qs}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        text = await res.text();
        if (!res.ok) throw new Error(`HTTP ${res.status} ${text || ''}`);
      }
      closeModal();
      await loadList();
    } catch (err) {
      msg.textContent = `실패: ${err.message}`;
    }
  });

  // 삭제
  btnDelete.addEventListener('click', async () => {
    if (!mId.value || !BASE) return;
    if (!confirm('정말 삭제할까요?')) return;
    try {
      const res = await fetch(`${BASE}/${encodeURIComponent(mId.value)}`, { method: 'DELETE' });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      closeModal();
      await loadList();
    } catch (err) {
      msg.textContent = `삭제 실패: ${err.message}`;
    }
  });

  // 이벤트 바인딩
  fab.addEventListener('click', () => openModal('create'));
  modal.addEventListener('click', (e) => { if (e.target === modal || e.target.dataset.close !== undefined) closeModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && modal.classList.contains('open')) closeModal(); });

  // 테이블 더보기 메뉴
  tbody.addEventListener('click', (e) => {
    const btn = e.target.closest('.more');
    if (!btn) return;
    const tr = btn.closest('tr'); const id = tr?.dataset.id;
    if (!id) return;
    openRowMenu(btn, id);
  });
  rowMenu.addEventListener('click', async (e) => {
    const act = e.target.dataset.action;
    if (!act || !menuTargetId) return;
    const item = DATA.find(x => String(x.id) === String(menuTargetId));
    closeRowMenu();
    if (!item) return;

    if (act === 'edit') {
      openModal('edit', item);
    } else if (act === 'delete') {
      if (!confirm('정말 삭제할까요?')) return;
      try {
        const res = await fetch(`${BASE}/${encodeURIComponent(menuTargetId)}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('HTTP ' + res.status);
        await loadList();
      } catch (err) { alert('삭제 실패: ' + err.message); }
    }
  });
  document.addEventListener('click', (e) => {
    if (!rowMenu.hidden && !rowMenu.contains(e.target) && !e.target.closest('.more')) closeRowMenu();
  });
  window.addEventListener('resize', closeRowMenu);
  window.addEventListener('scroll', closeRowMenu, true);

  // 초기화
  (async function init() {
    try {
      setToday();
      await detectBase();
      await loadList();
    } catch (err) {
      console.error('[notice] init error:', err);
      tbody.innerHTML = '';
      empty.hidden = false;
      empty.textContent = 'API 엔드포인트를 찾지 못했습니다.';
    }
  })();
})();
