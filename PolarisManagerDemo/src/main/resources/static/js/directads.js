// /static/js/directads.js
(function () {
  /* ========= helpers ========= */
  const $  = (s, p=document) => p.querySelector(s);
  const $$ = (s, p=document) => Array.from(p.querySelectorAll(s));
  const esc = s => (s ?? '').toString().replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

  const todayText = () => {
    const d = new Date(), z = n => String(n).padStart(2,'0');
    const wd = d.toLocaleDateString('ko-KR',{weekday:'long'});
    return `${wd} ${d.getFullYear()}-${z(d.getMonth()+1)}-${z(d.getDate())}`;
  };

  // datetime-local <-> ISO
  const toLocalInput = iso => {
    if (!iso) return '';
    const d = new Date(iso); if (Number.isNaN(+d)) return '';
    const d2 = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
    return d2.toISOString().slice(0,16);
  };
  const toIsoOrNull = v => { if (!v) return null; const d = new Date(v); return Number.isNaN(+d) ? null : d.toISOString(); };
  const csvToArray = v => !v ? null : v.split(',').map(s=>s.trim()).filter(Boolean);
  const arrayToCsv = a => Array.isArray(a) ? a.join(',') : '';

  /* ========= endpoint candidates ========= */
  const META_BASE = ($('meta[name="directads-endpoint"]')?.content || '').trim();
  const CANDIDATES = [ META_BASE || '', '/api/directads', '/directads', '/api/polarisdirectads', '/api/polaris_direct_ads' ]
    .filter(Boolean);

  /* ========= global state ========= */
  const STATE = { all: [], byId: new Map(), base: null };
  window.STATE = STATE; // 디버깅용

  /* ========= network ========= */
  async function tryListOn(base, limit=200) {
    const url = `${base}?limit=${encodeURIComponent(limit)}`;
    const r = await fetch(url, { credentials:'include', cache:'no-store', headers:{Accept:'application/json'} });
    if (!r.ok) throw new Error(`${r.status} ${r.statusText} @ ${url}`);
    const ct = r.headers.get('content-type') || '';
    if (!ct.includes('application/json')) throw new Error(`Not JSON (${ct}) @ ${url}`);
    const data = await r.json();
    if (!Array.isArray(data)) throw new Error(`Not Array JSON @ ${url}`);
    return data;
  }

  async function fetchListAny(limit=200) {
    let lastErr;
    for (const base of CANDIDATES) {
      try {
        const list = await tryListOn(base, limit);
        STATE.base = base;
        return list;
      } catch (e) { lastErr = e; }
    }
    throw lastErr || new Error('No endpoint reachable');
  }

  // CRUD (확정된 base 사용)
  const apiCreate = async payload => {
    const r = await fetch(`${STATE.base}`, {
      method:'POST', credentials:'include',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify(payload)
    });
    const t = await r.text();
    if (!r.ok) throw new Error(`HTTP ${r.status} ${t||''}`);
    return t;
  };
  const apiUpdate = async (id, payload) => {
    const r = await fetch(`${STATE.base}/${encodeURIComponent(id)}`, {
      method:'PUT', credentials:'include',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify(payload)
    });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
  };
  const apiDelete = async id => {
    const r = await fetch(`${STATE.base}/${encodeURIComponent(id)}`, { method:'DELETE', credentials:'include' });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
  };

  /* ========= render ========= */
  function splitByType(list) {
    const out = { BANNER:[], BOTTOM:[], EVENT:[], OTHERS:[] };
    (list||[]).forEach(ad => {
      const t = (ad.adType || '').toString().toUpperCase();
      if (t === 'BANNER') out.BANNER.push(ad);
      else if (t === 'BOTTOM') out.BOTTOM.push(ad);
      else if (t === 'EVENT' || t === 'EVENT_FAB') out.EVENT.push(ad); // EVENT + EVENT_FAB 묶기
      else out.OTHERS.push(ad);
    });
    return out;
  }

  function cardTemplate(ad) {
    const dateStr = (ad.publishedAt || ad.createdAt)
      ? new Date(ad.publishedAt || ad.createdAt).toISOString().slice(0,10) : '-';
    return `
      <div class="ad-card" data-id="${esc(ad.id)}" role="button" tabindex="0" aria-label="광고 수정">
        <div class="thumb-wrap"><img class="ad-thumb" src="${esc(ad.imageUrl||'')}" alt="" loading="lazy"></div>
        <div class="ad-row">
          <div class="ad-title">${esc(ad.advertiserName||'-')}</div>
          <div class="ad-kpi">노출수 | ${ad.viewCount ?? 0} · 클릭수 | ${ad.clickCount ?? 0}</div>
        </div>
        <div class="ad-foot"><span>${esc(dateStr)}</span><span>›</span></div>
      </div>`;
  }

  function renderGrid(containerId, list){
    const el = document.querySelector(`#${containerId}`);
    if (!el) return;
    if (!list || list.length === 0) { el.innerHTML = `<div class="muted" style="padding:12px 6px">표시할 항목이 없습니다.</div>`; return; }
    el.innerHTML = list.map(cardTemplate).join('');
    bindCardEvents(el);
  }

  function bindCardEvents(scope=document) {
    const cards = Array.from(scope.querySelectorAll('.ad-card'));
    cards.forEach(card => {
      if (card.dataset.clickBound) return;
      card.dataset.clickBound = '1';

      card.addEventListener('click', (e) => {
        e.preventDefault(); e.stopPropagation();
        const id = card.getAttribute('data-id');
        openEditModal(STATE.byId.get(id) || null);
      }, true);

      card.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          const id = card.getAttribute('data-id');
          openEditModal(STATE.byId.get(id) || null);
        }
      });
    });
  }

  /* ========= modal ========= */
  const modal   = $('#adModal');
  const form    = $('#adForm');
  const msgEl   = $('#adMsg');
  const btnDel  = $('#btnDelete');
  const fab     = $('#fabAdd');

  const f = {
    id:             $('#mId'),
    adType:         $('#mAdType'),
    advertiserName: $('#mAdvertiserName'),
    backgroundColor:$('#mBackgroundColor'),
    imageUrl:       $('#mImageUrl'),
    targetUrl:      $('#mTargetUrl'),
    status:         $('#mStatus'),
    locales:        $('#mLocales'),
    minAppVersion:  $('#mMinAppVersion'),
    maxAppVersion:  $('#mMaxAppVersion'),
    publishedAt:    $('#mPublishedAt'),
    startAt:        $('#mStartAt'),
    endAt:          $('#mEndAt')
  };

  function openEditModal(ad) {
    if (!modal) return;
    msgEl && (msgEl.textContent = '');
    modal.setAttribute('aria-hidden','false');

    if (!ad) {
      $('#adModalTitle') && ($('#adModalTitle').textContent = '광고 • 새 항목 추가');
      f.id && (f.id.value='');
      f.adType && (f.adType.value='BANNER');
      f.advertiserName && (f.advertiserName.value='');
      f.backgroundColor && (f.backgroundColor.value='#FFFFFF');
      f.imageUrl && (f.imageUrl.value='');
      f.targetUrl && (f.targetUrl.value='');
      f.status && (f.status.value='ACTIVE');
      f.locales && (f.locales.value='');
      f.minAppVersion && (f.minAppVersion.value='');
      f.maxAppVersion && (f.maxAppVersion.value='');
      f.publishedAt && (f.publishedAt.value='');
      f.startAt && (f.startAt.value='');
      f.endAt && (f.endAt.value='');
      btnDel && (btnDel.hidden = true);
    } else {
      $('#adModalTitle') && ($('#adModalTitle').textContent = '광고 • 항목 수정');
      f.id && (f.id.value = ad.id || '');
      f.adType && (f.adType.value = (ad.adType || 'BANNER').toString().toUpperCase());
      f.advertiserName && (f.advertiserName.value = ad.advertiserName || '');
      f.backgroundColor && (f.backgroundColor.value = ad.backgroundColor || '#FFFFFF');
      f.imageUrl && (f.imageUrl.value = ad.imageUrl || '');
      f.targetUrl && (f.targetUrl.value = ad.targetUrl || '');
      f.status && (f.status.value = (ad.status || 'ACTIVE').toString().toUpperCase());
      f.locales && (f.locales.value = arrayToCsv(ad.locales || []));
      f.minAppVersion && (f.minAppVersion.value = ad.minAppVersion || '');
      f.maxAppVersion && (f.maxAppVersion.value = ad.maxAppVersion || '');
      f.publishedAt && (f.publishedAt.value = toLocalInput(ad.publishedAt));
      f.startAt && (f.startAt.value     = toLocalInput(ad.startAt));
      f.endAt && (f.endAt.value         = toLocalInput(ad.endAt));
      btnDel && (btnDel.hidden = false);
    }
    setTimeout(() => (f.advertiserName || modal).focus(), 10);
  }

  function closeModal(){ if (!modal) return; modal.setAttribute('aria-hidden','true'); msgEl && (msgEl.textContent=''); }

  if (modal) {
    $$('[data-close]', modal).forEach(b => b.addEventListener('click', closeModal));
    modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
  }
  window.addEventListener('keydown', e => { if (e.key==='Escape' && modal?.getAttribute('aria-hidden')==='false') closeModal(); });

  form && form.addEventListener('submit', async (e) => {
    e.preventDefault(); msgEl && (msgEl.textContent='');
    const payload = {
      adType:        f.adType?.value || 'BANNER',
      advertiserName:f.advertiserName?.value?.trim(),
      backgroundColor:f.backgroundColor?.value?.trim(),
      imageUrl:      f.imageUrl?.value?.trim(),
      targetUrl:     f.targetUrl?.value?.trim(),
      status:        f.status?.value || 'ACTIVE',
      locales:       csvToArray(f.locales?.value),
      minAppVersion: f.minAppVersion?.value?.trim() || null,
      maxAppVersion: f.maxAppVersion?.value?.trim() || null,
      publishedAt:   toIsoOrNull(f.publishedAt?.value),
      startAt:       toIsoOrNull(f.startAt?.value),
      endAt:         toIsoOrNull(f.endAt?.value),
      meta:          null
    };
    try {
      if (f.id?.value) await apiUpdate(f.id.value, payload);
      else             await apiCreate(payload);
      closeModal();
      await boot();
    } catch (err) { msgEl && (msgEl.textContent = err.message || '저장 실패'); }
  });

  btnDel && btnDel.addEventListener('click', async () => {
    if (!f.id?.value) return;
    if (!confirm('정말 삭제할까요?')) return;
    try { await apiDelete(f.id.value); closeModal(); await boot(); }
    catch (err) { msgEl && (msgEl.textContent = err.message || '삭제 실패'); }
  });

  fab && fab.addEventListener('click', () => openEditModal(null));

  /* ========= boot ========= */
  async function boot() {
    $('#todayText') && ($('#todayText').textContent = todayText());
    $('#apiBaseHint') && ($('#apiBaseHint').textContent = STATE.base || '');

    try {
      const list = STATE.base ? await tryListOn(STATE.base, 200) : await fetchListAny(200);
      STATE.base && ($('#apiBaseHint') && ($('#apiBaseHint').textContent = STATE.base));
      STATE.all = Array.isArray(list) ? list : [];
      STATE.byId.clear(); STATE.all.forEach(x => STATE.byId.set(String(x.id), x));

      const { BANNER, BOTTOM, EVENT, OTHERS } = splitByType(STATE.all);

      $('#totalBanner') && ($('#totalBanner').textContent = `BANNER`);
      $('#totalBottom') && ($('#totalBottom').textContent = `BOTTOM`);
      $('#totalEvent')  && ($('#totalEvent').textContent  = `EVENT`);
      $('#totalOthers') && ($('#totalOthers').textContent = `기타`);

      renderGrid('gridBanner', BANNER);
      renderGrid('gridBottom', BOTTOM);
      renderGrid('gridEvent',  EVENT);
      renderGrid('gridOthers', OTHERS);
    } catch (err) {
      console.error('[directads] list failed', err);
      const msg = esc(err.message || '로드 실패');
      $('#gridBanner') && ($('#gridBanner').innerHTML = `<div class="muted" style="padding:12px 6px">에러: ${msg}</div>`);
      $('#gridBottom') && ($('#gridBottom').innerHTML = '');
      $('#gridEvent')  && ($('#gridEvent').innerHTML  = '');
      $('#gridOthers') && ($('#gridOthers').innerHTML = '');
    }
  }

  // ===== init =====
  // 모달을 body 바로 아래로 이동(레이어/포지셔닝 문제 방지)
  if (modal && modal.parentElement !== document.body) {
    document.body.appendChild(modal);
  }

  (async function init(){ await boot(); })();
})();
