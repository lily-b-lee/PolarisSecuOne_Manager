// /static/js/overview.js
(() => {
  const $ = (s, p = document) => p.querySelector(s);
  const $$ = (s, p = document) => [...p.querySelectorAll(s)];

  /* =========================
   * 공통/초기 세팅
   * ========================= */
  function setToday() {
    const d = new Date();
    const opt = { weekday: 'long', year: 'numeric', month: '2-digit', day: '2-digit' };
    const t = d.toLocaleDateString('ko-KR', opt).replace(/\.\s*/g, '-');
    const el = $('#todayText');
    if (el) el.textContent = t;
  }

  function setDefaultRange() {
    const from = $('#fromDate');
    const to = $('#toDate');
    if (!from || !to) return;

    const d = new Date();
    const z = (n) => String(n).padStart(2, '0');
    const toYmd = `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;

    const d2 = new Date(d);
    d2.setDate(d.getDate() - 6);
    const fromYmd = `${d2.getFullYear()}-${z(d2.getMonth() + 1)}-${z(d2.getDate())}`;

    if (!from.value) from.value = fromYmd;
    if (!to.value) to.value = toYmd;
  }

  /* =========================
   * 차트 (Chart.js)
   * ========================= */
  function makeBarChart(ctxId, labels, data, labelText) {
    if (!window.Chart) {
      console.warn('[overview] Chart.js not loaded, skip charts.');
      return null;
    }
    const ctx = document.getElementById(ctxId);
    if (!ctx) return null;

    return new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{ label: labelText, data }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { enabled: true }
        },
        scales: {
          x: { ticks: { autoSkip: true, maxRotation: 0 } },
          y: { beginAtZero: true, ticks: { precision: 0 } }
        }
      }
    });
  }

  function initCharts() {
    const labels = ['MG', 'ACME', 'ZEN', 'NOVO', 'BORA'];
    const newUsers = [32, 18, 27, 8, 15];
    const churn = [5, 2, 4, 1, 2];

    makeBarChart('chartNewUsers', labels, newUsers, 'New Users');
    makeBarChart('chartChurn', labels, churn, 'Churn Users');
  }

  /* =========================
   * 공지사항: 로드 & 렌더
   * ========================= */
  const NOTICE_CANDIDATES = ['/api/notices', '/notices', '/api/notice', '/notice'];

  async function detectNoticeBase() {
    for (const p of NOTICE_CANDIDATES) {
      try {
        const r = await fetch(`${p}/_ping`, { cache: 'no-store' });
        if (r.ok) return p;
      } catch(_) {}
    }
    return null;
  }

  function dateToKey(v) {
    const s = String(v || '').replace(/\D/g, '');
    return s.length >= 8 ? +s.slice(0, 8) : 0;
  }

  function catLabel(cat) {
    switch ((cat || '').toUpperCase()) {
      case 'EVENT': return '이벤트';
      case 'EMERGENCY': return '긴급';
      case 'SERVICE_GUIDE': return '서비스안내';
      case 'UPDATE': return '업데이트';
      default: return cat || '기타';
    }
  }
  function catColor(cat) {
    switch ((cat || '').toUpperCase()) {
      case 'EVENT':         return '#93c5fd'; // 파랑(blue-300)
      case 'EMERGENCY':     return '#fca5a5'; // 빨강(red-300)
      case 'SERVICE_GUIDE': return '#a5b4fc'; // 인디고(indigo-300)
      case 'UPDATE':        return '#86efac'; // 초록(green-300)
      default:              return '#d1d5db'; // 회색(gray-300)
    }
  }
  // ===== 실제 고객사 수 로드해서 #metricCust에 표시 =====
  async function loadCustomerCount() {
    const $ = (s, p=document) => p.querySelector(s);
    const metric = $('#metricCust');
    if (!metric) return;

    const getToken = () => localStorage.getItem('admin_token') || null;
    const authHeaders = () => {
      const h = { 'Accept': 'application/json' };
      const t = getToken();
      if (t) h['Authorization'] = `Bearer ${t}`;
      return h;
    };

    // 1) 가능한 경우: /api/admin/customers/count → 숫자 또는 {count:n}
    try {
      const r1 = await fetch('/api/admin/customers/count', {
        headers: authHeaders(),
        credentials: 'same-origin'
      });
      if (r1.ok) {
        // 숫자 텍스트 or JSON 둘 다 케어
        const txt = await r1.clone().text().catch(()=>null);
        const numFromText = Number(txt);
        if (Number.isFinite(numFromText)) {
          metric.textContent = numFromText.toLocaleString();
          return;
        }
        const j = await r1.json().catch(()=> null);
        if (j && typeof j.count === 'number') {
          metric.textContent = j.count.toLocaleString();
          return;
        }
      } else if (r1.status === 401 || r1.status === 403) {
        location.href = '/login?next=' + encodeURIComponent(location.pathname);
        return;
      }
    } catch (_) { /* fallthrough */ }

    // 2) 폴백: /api/admin/customers → 배열 길이/total 사용
    try {
      const r2 = await fetch('/api/admin/customers', {
        headers: authHeaders(),
        credentials: 'same-origin'
      });
      if (r2.status === 401 || r2.status === 403) {
        location.href = '/login?next=' + encodeURIComponent(location.pathname);
        return;
      }
      if (!r2.ok) throw new Error('HTTP ' + r2.status);
      const data = await r2.json().catch(()=> []);
      let count = 0;
      if (Array.isArray(data)) count = data.length;
      else if (typeof data?.total === 'number') count = data.total;
      else if (typeof data?.length === 'number') count = data.length;

      metric.textContent = count.toLocaleString();
    } catch (e) {
      console.warn('[overview] 고객사 수 로드 실패:', e);
      metric.textContent = '-';
    }
  }

  function renderNotices(list) {
    const ul = $('#noticeList');
    if (!ul) return;
    ul.innerHTML = '';

    if (!Array.isArray(list) || list.length === 0) {
      ul.innerHTML = `<li class="muted">공지사항이 없습니다.</li>`;
      return;
    }

    const sorted = list.slice().sort((a, b) => dateToKey(b?.date) - dateToKey(a?.date));

    const frag = document.createDocumentFragment();
    sorted.slice(0, 6).forEach((n) => {
      const li = document.createElement('li');

      const badge = document.createElement('span');
      badge.className = 'badge';
      badge.textContent = catLabel(n.category);
      badge.style.background = catColor(n.category);

      const left = document.createElement('span');
      left.appendChild(badge);
      left.appendChild(document.createTextNode(' ' + (n.title || '(제목 없음)')));

      const right = document.createElement('span');
      right.className = 'muted';
      right.textContent = n.date || '';

      li.appendChild(left);
      li.appendChild(right);
      frag.appendChild(li);
    });

    ul.appendChild(frag);
  }

  async function loadNotices() {
    const base = await detectNoticeBase();
    if (!base) {
      console.warn('[overview] notice endpoint not found');
      renderNotices([]);
      return;
    }
    try {
      const res = await fetch(`${base}?limit=20`, {
        headers: { Accept: 'application/json' },
        cache: 'no-store'
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const json = await res.json();
      renderNotices(Array.isArray(json) ? json : []);
    } catch (e) {
      console.error('[overview] loadNotices error:', e);
      renderNotices([]);
    }
  }

  /* =========================
   * Quick Action: 고객사 추가 모달
   * ========================= */
  const CustModal = (() => {
    const API = '/api/admin/customers';

    const getToken = () => localStorage.getItem('admin_token') || null;
    const authHeaders = () => {
      const h = { 'Content-Type': 'application/json' };
      const t = getToken();
      if (t) h['Authorization'] = `Bearer ${t}`;
      return h;
    };

    const sel = {
      backdrop: '#custModalOv',
      title: '#ovModalTitle',
      code: '#ovCode',
      name: '#ovName',
      cName: '#ovContactName',
      cEmail: '#ovContactEmail',
      cPhone: '#ovContactPhone',
      cpi: '#ovCpiRate',
      rs: '#ovRsRate',
      note: '#ovNote',
      err: '#ovFormErr',
      btnOpen: '#qaAddCustomer',
      btnClose: '#ovBtnClose',
      btnSave: '#ovBtnSave',
      btnReset: '#ovBtnReset'
    };

    const q = (k) => $(sel[k]);

    function showErr(m) { const el = q('err'); if (el) el.textContent = m || ''; }
    function resetForm() {
      if (!q('code')) return;
      q('code').value = '';
      q('name').value = '';
      q('cName').value = '';
      q('cEmail').value = '';
      q('cPhone').value = '';
      q('cpi').value = '0';
      q('rs').value = '0';
      q('note').value = '';
      showErr('');
    }
    function open() {
      const m = q('backdrop');
      if (!m) return;
      m.classList.add('show');
      m.setAttribute('aria-hidden', 'false');
      document.body.style.overflow = 'hidden';
      setTimeout(() => q('code')?.focus(), 0);
    }
    function close() {
      const m = q('backdrop');
      if (!m) return;
      m.classList.remove('show');
      m.setAttribute('aria-hidden', 'true');
      document.body.style.overflow = '';
      showErr('');
    }

    async function create(payload) {
      const res = await fetch(API, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(payload),
        credentials: 'same-origin'
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        const msg = data?.message || `생성 실패(HTTP ${res.status})`;
        throw new Error(msg);
      }
      return data;
    }

    async function onSave() {
      showErr('');
      const code = (q('code')?.value || '').trim();
      const name = (q('name')?.value || '').trim();
      if (!code) return showErr('회사 코드를 입력하세요.');
      if (!name) return showErr('회사명을 입력하세요.');

      const payload = {
        code,
        name,
        contactName: (q('cName')?.value || '').trim(),
        contactEmail: (q('cEmail')?.value || '').trim(),
        contactPhone: (q('cPhone')?.value || '').trim(),
        cpiRate: q('cpi')?.value ? Number(q('cpi').value) : null,
        rsRate:  q('rs')?.value ? Number(q('rs').value) : null,
        note: (q('note')?.value || '').trim()
      };

      try {
        await create(payload);
        close();
        resetForm();
        alert('고객사가 등록되었습니다.');
      } catch (e) {
        showErr(e.message || '저장 실패');
      }
    }

    function bind() {
      q('btnOpen')?.addEventListener('click', () => { resetForm(); open(); });
      q('btnClose')?.addEventListener('click', close);
      q('btnReset')?.addEventListener('click', resetForm);
      q('btnSave')?.addEventListener('click', onSave);

      // 배경 클릭/ESC 닫기
      document.addEventListener('click', (e) => {
        const m = q('backdrop');
        if (!m || !m.classList.contains('show')) return;
        if (e.target === m) close();
      });
      document.addEventListener('keydown', (e) => {
        const m = q('backdrop');
        if (e.key === 'Escape' && m?.classList.contains('show')) close();
      });
    }

    return { bind };
  })();

  /* =========================
   * Quick Action: UTM 링크 생성 모달
   *  - 기본 URL 고정:
   *    https://play.google.com/store/apps/details?id=com.polarisoffice.vguardsecuone
   *  - utm_source = "회사명(코드)"
   *  - utm_medium = 'ata' | 'wta'
   *  - utm_campaign = '연동기능'
   * ========================= */
  const UtmModal = (() => {
    const DEFAULT_BASE = 'https://play.google.com/store/apps/details?id=com.polarisoffice.vguardsecuone';

    const sel = {
      backdrop: '#utmModal',
      btnOpen: '#qaUtm',
      btnClose: '#utmBtnClose',
      btnCopy: '#utmCopy',
      btnOpenNew: '#utmOpen',
      btnReset: '#utmReset',

      base: '#utmBase',
      source: '#utmSource',
      medium: '#utmMedium',
      campaign: '#utmCampaign',
      term: '#utmTerm',
      content: '#utmContent',
      preview: '#utmPreview',
      err: '#utmErr',

      compName: '#utmCompanyName',
      compCode: '#utmCompanyCode',
      mediumRadios: 'input[name="utmMediumChoice"]',
      mediumSelect: '#utmMediumSelect',
      campaignSelect: '#utmCampaignSelect'
    };
    const q = (k) => $(sel[k]);

    const showErr = (m) => { const el = q('err'); if (el) el.textContent = m || ''; };
    const slugAscii = (s) => (s||'').trim().toLowerCase()
      .replace(/\s+/g,'-').replace(/[^a-z0-9_\-./]/g,'');

    function readMedium() {
      const checked = document.querySelector(sel.mediumRadios + ':checked');
      if (checked && (checked.value === 'ata' || checked.value === 'wta')) return checked.value;
      const selEl = q('mediumSelect');
      if (selEl && (selEl.value === 'ata' || selEl.value === 'wta')) return selEl.value;
      const txt = (q('medium')?.value || '').trim().toLowerCase();
      return (txt === 'wta') ? 'wta' : 'ata';
    }

    function readCampaign() {
      const fixed = '';
      const selEl = q('campaignSelect');
      if (selEl && selEl.value) return selEl.value;
      if (q('campaign') && !q('campaign').value) q('campaign').value = fixed;
      return (q('campaign')?.value || fixed);
    }

    function readSource() {
      const name = (q('compName')?.value || '').trim();
      const code = (q('compCode')?.value || '').trim();
      if (name || code) return code ? `${name}(${code})` : name;
      return (q('source')?.value || '').trim();
    }

    function open() {
      const m = q('backdrop');
      if (!m) return;
      m.classList.add('show');
      m.setAttribute('aria-hidden','false');
      document.body.style.overflow = 'hidden';

      // 기본 URL 고정: 표시용 입력이 있어도 값/placeholder 세팅 후 읽기전용 처리
      if (q('base')) {
        q('base').value = DEFAULT_BASE;
        q('base').placeholder = DEFAULT_BASE;
        q('base').setAttribute('readonly','readonly');
      }
      if (q('medium')) q('medium').value = '';
      if (q('campaign')) q('campaign').value = '';
      updatePreview();
      setTimeout(()=>q('compName')?.focus() || q('source')?.focus(), 0);
    }
    function close() {
      const m = q('backdrop');
      if (!m) return;
      m.classList.remove('show');
      m.setAttribute('aria-hidden','true');
      document.body.style.overflow = '';
      showErr('');
    }

    function buildUtmUrl(params) {
      try {
        const url = new URL(DEFAULT_BASE); // 🔒 기본 URL 고정
        const sp = url.searchParams;
        Object.entries(params).forEach(([k,v]) => {
          const s = (v ?? '').toString().trim();
          if (s) sp.set(k, s);
        });
        url.search = sp.toString();
        return url.toString();
      } catch {
        return '';
      }
    }

    function values() {
      return {
        base: DEFAULT_BASE,                        // 🔒 항상 고정
        source: readSource(),                      // "회사명(코드)"
        medium: readMedium(),                      // 'ata' | 'wta'
        campaign: readCampaign(),                  // '연동기능'
        term: slugAscii(q('term')?.value || ''),
        content: slugAscii(q('content')?.value || '')
      };
    }

    function updatePreview() {
      showErr('');
      const v = values();
      if (!q('preview')) return;

      const params = {
        utm_source: v.source,
        utm_medium: v.medium,
        utm_campaign: v.campaign
      };
      if (v.term) params.utm_term = v.term;
      if (v.content) params.utm_content = v.content;

      const out = buildUtmUrl(params);
      q('preview').textContent = out || '-';
    }

    async function copyPreview() {
      const t = q('preview')?.textContent || '';
      if (!t || t === '-') return showErr('생성된 링크가 없습니다.');
      try {
        await navigator.clipboard.writeText(t);
        showErr('복사되었습니다.');
        setTimeout(() => showErr(''), 1200);
      } catch {
        showErr('클립보드 복사 실패');
      }
    }

    function openPreview() {
      const t = q('preview')?.textContent || '';
      if (!t || t === '-') return showErr('생성된 링크가 없습니다.');
      window.open(t, '_blank', 'noopener');
    }

    function resetForm() {
      if (q('compName')) q('compName').value = '';
      if (q('compCode')) q('compCode').value = '';
      if (q('source')) q('source').value = '';
      if (q('medium')) q('medium').value = 'ata';
      const checked = document.querySelector(sel.mediumRadios + ':checked');
      if (checked) checked.checked = false;
      if (q('mediumSelect')) q('mediumSelect').value = 'ata';
      if (q('campaign')) q('campaign').value = '';
      if (q('campaignSelect')) q('campaignSelect').value = '';
      if (q('term')) q('term').value = '';
      if (q('content')) q('content').value = '';
      // base 필드는 항상 고정값으로 표시
      if (q('base')) {
        q('base').value = DEFAULT_BASE;
        q('base').placeholder = DEFAULT_BASE;
        q('base').setAttribute('readonly','readonly');
      }
      showErr('');
      updatePreview();
    }

    function bind() {
      q('btnOpen')?.addEventListener('click', open);
      q('btnClose')?.addEventListener('click', close);
      q('btnCopy')?.addEventListener('click', copyPreview);
      q('btnOpenNew')?.addEventListener('click', openPreview);
      q('btnReset')?.addEventListener('click', resetForm);

      // 입력 변화 시 미리보기 갱신
      ['source','campaign','term','content'].forEach(k => {
        q(k)?.addEventListener('input', updatePreview);
        q(k)?.addEventListener('change', updatePreview);
      });
      ['compName','compCode'].forEach(k=>{
        q(k)?.addEventListener('input', updatePreview);
        q(k)?.addEventListener('change', updatePreview);
      });
      $$(sel.mediumRadios).forEach(r => r.addEventListener('change', updatePreview));
      q('mediumSelect')?.addEventListener('change', updatePreview);
      q('medium')?.addEventListener('input', updatePreview);

      q('campaignSelect')?.addEventListener('change', updatePreview);

      // 배경 클릭/ESC 닫기
      document.addEventListener('click', (e) => {
        const m = q('backdrop');
        if (!m || !m.classList.contains('show')) return;
        if (e.target === m) close();
      });
      document.addEventListener('keydown', (e) => {
        const m = q('backdrop');
        if (e.key === 'Escape' && m?.classList.contains('show')) close();
      });
    }

    return { bind };
  })();

  /* =========================
   * 기타 Quick Action
   * ========================= */
  function bindQuickActionsMisc() {
    $('#qaPush')?.addEventListener('click', () => {
      alert('푸시 보내기는 공지/고객사 화면에서 테스트해 주세요.');
    });
    // qaAddCustomer, qaUtm 은 각 모달 모듈에서 바인딩
  }

  /* =========================
   * 초기 구동
   * ========================= */
  document.addEventListener('DOMContentLoaded', () => {
    setToday();
    setDefaultRange();
    initCharts();
    loadNotices();
    CustModal.bind();
    UtmModal.bind();
    bindQuickActionsMisc();
	loadCustomerCount();
  });
})();
