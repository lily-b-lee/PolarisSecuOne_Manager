(() => {
  const $ = (s, p=document) => p.querySelector(s);

  // ===== BASE URL (context-path 대응) =====
  const APP_BASE = document.querySelector('meta[name="app-base"]')?.content || '/';
  const J = (base, path) => {
    if (!base) return path;
    if (base.endsWith('/') && path.startsWith('/')) return base.slice(0,-1) + path;
    return base + path;
  };

  // ===== DOM =====
  const todayEl = $('#today');
  const fromEl  = $('#from');
  const toEl    = $('#to');
  const quick   = $('#quick');
  const btnLoad = $('#btnLoad');

  // (비즈 KPI는 백엔드 미제공)
  const kNewMonthly   = $('#kNewMonthly');
  const kChurnMonthly = $('#kChurnMonthly');
  const kNewToday     = $('#kNewToday');
  const kChurnToday   = $('#kChurnToday');
  const kSettleMonthly= $('#kSettleMonthly');

  const kMalware = $('#kMalware');
  const kRooting = $('#kRooting');
  const kRemote  = $('#kRemote');

  const rangeTxt = $('#rangeTxt');
  const topInfo  = $('#topInfo');
  const topTable = $('#topTable tbody');

  const chartCanvas = $('#dailyChart');
  let dailyChart = null;

  // ===== Util =====
  const fmtInt = (n) => (n===null || n===undefined) ? '-' : Number(n).toLocaleString();
  const iso = (d) => d.toISOString().slice(0,10);

  function setDefaultDates(days = 30) {
    const now = new Date();
    const from = new Date(now.getTime() - (days-1)*24*3600*1000);
    if (!fromEl.value) fromEl.value = iso(from);
    if (!toEl.value)   toEl.value   = iso(now);
  }

  function getCustomerCode() {
    // 1) <meta name="customer-code">
    const meta = document.querySelector('meta[name="customer-code"]')?.content?.trim();
    if (meta) return meta;
    // 2) URL ?customerCode / ?cc
    const sp = new URLSearchParams(location.search);
    const qp = sp.get('customerCode') || sp.get('cc');
    if (qp) return qp;
    // 3) localStorage
    const ls = localStorage.getItem('customer_code');
    if (ls) return ls;
    // 4) 개발용 임시 (필요할 때만 열어두세요)
    // return 'mg';
    return null;
  }

  function authHeaders() {
    const t = localStorage.getItem('user_token') || localStorage.getItem('admin_token');
    const h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    if (t) h['Authorization'] = `Bearer ${t}`;
    const cc = getCustomerCode();
    if (cc) h['X-Customer-Code'] = cc;   // ✅ 헤더에도 넣기
    return h;
  }

  async function fetchJson(url, init = {}) {
    const headers = { ...authHeaders(), ...(init.headers || {}) };
    const res = await fetch(url, { credentials: 'same-origin', ...init, headers });

    if (res.status === 401 || res.status === 403) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.href = J(APP_BASE, `/login?next=${next}`);
      return null;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : {};
  }

  // ===== API =====
  async function getDaily(from, to, tz='Asia/Seoul') {
    // ✅ 쿼리파라미터에도 customerCode 같이 전달 (컨트롤러가 최우선으로 읽음)
    const cc = getCustomerCode();
    let qs = `?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&tz=${encodeURIComponent(tz)}`;
    if (cc) qs += `&customerCode=${encodeURIComponent(cc)}`;

    const url = J(APP_BASE, `/api/events/report/daily${qs}`);
    console.debug('[reports] GET', url);
    return await fetchJson(url);
  }

  // ===== Renderers =====
  function renderDailyChart(series) {
    const days = Array.isArray(series) ? series : [];
    const labels  = days.map(d => d.date);
    const malware = days.map(d => Number(d.malware || 0));
    const rooting = days.map(d => Number(d.rooting || 0));
    const remote  = days.map(d => Number(d.remote  || 0));

    if (dailyChart) { dailyChart.destroy(); dailyChart = null; }

    dailyChart = new Chart(chartCanvas.getContext('2d'), {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: '악성앱', data: malware, tension: .2, borderWidth: 2, pointRadius: 2 },
          { label: '루팅',   data: rooting, tension: .2, borderWidth: 2, pointRadius: 2 },
          { label: '원격제어', data: remote, tension: .2, borderWidth: 2, pointRadius: 2 },
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { position: 'bottom' } },
        scales: { x: { grid: { display:false } }, y: { beginAtZero:true, ticks:{ precision:0 } } }
      }
    });
  }

  function renderEventKpisFromTotals(totals, series) {
    if (totals) {
      kMalware.textContent = fmtInt(totals.malware);
      kRooting.textContent = fmtInt(totals.rooting);
      kRemote.textContent  = fmtInt(totals.remote);
      return;
    }
    const sum = (Array.isArray(series) ? series : []).reduce((acc, d) => {
      acc.malware += Number(d.malware || 0);
      acc.rooting += Number(d.rooting || 0);
      acc.remote  += Number(d.remote  || 0);
      return acc;
    }, { malware:0, rooting:0, remote:0 });
    kMalware.textContent = fmtInt(sum.malware);
    kRooting.textContent = fmtInt(sum.rooting);
    kRemote.textContent  = fmtInt(sum.remote);
  }

  function renderTopTable(topTypes, topPkgs, totals) {
    const types = Array.isArray(topTypes) ? topTypes : [];
    const pkgs  = Array.isArray(topPkgs)  ? topPkgs  : [];
    const rows  = Math.max(types.length, pkgs.length);

    topTable.innerHTML = '';
    if (rows === 0) {
      topInfo.textContent = '데이터 없음';
      return;
    }

    const frag = document.createDocumentFragment();
    for (let i=0; i<rows; i++){
      const t = types[i];
      const p = pkgs[i];
      const rank = i+1;
      const typeName = t?.type ?? '-';
      const pkgName  = p?.package ?? '-';
      const count    = (t?.count != null) ? t.count : (p?.count != null ? p.count : 0);

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${rank}</td>
        <td>${typeName}</td>
        <td>${pkgName}</td>
        <td class="num">${fmtInt(count)}</td>
      `;
      frag.appendChild(tr);
    }
    topTable.appendChild(frag);

    const totalMal = totals?.malware ?? types.reduce((a,b)=>a+(b.count||0),0);
    topInfo.textContent = `${types.length}개 유형 · ${pkgs.length}개 패키지 · 총 ${fmtInt(totalMal)}건`;
  }

  function renderRange(range) {
    if (!range) return;
    rangeTxt.textContent = `${range.from} ~ ${range.to}`;
  }

  function renderBusinessKpisPlaceholder() {
    [kNewMonthly,kChurnMonthly,kNewToday,kChurnToday,kSettleMonthly].forEach(el => el && (el.textContent='-'));
  }

  // ===== Load =====
  async function loadAll() {
    const from = fromEl.value;
    const to   = toEl.value;

    const data = await getDaily(from, to).catch(()=>null);
    if (!data || data.ok === false) {
      if (data?.message) alert(data.message);
      return;
    }
    renderRange(data.range);
    renderDailyChart(data.series || []);
    renderEventKpisFromTotals(data.totals, data.series);
    renderTopTable(data.malwareTopTypes, data.malwareTopPackages, data.totals);
    renderBusinessKpisPlaceholder();
  }

  function setToday() {
    const now = new Date();
    const days = ['일','월','화','수','목','금','토'];
    todayEl.textContent = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} (${days[now.getDay()]})`;
  }

  // ===== Events =====
  quick?.addEventListener('change', () => {
    const days = Number(quick.value || 30);
    fromEl.value = ''; toEl.value = '';
    setDefaultDates(days);
  });
  btnLoad?.addEventListener('click', loadAll);

  // ===== init =====
  document.addEventListener('DOMContentLoaded', async () => {
    setToday();
    setDefaultDates(30);

    // 디버그: 어떤 고객사 코드로 호출되는지 콘솔에 표시
    console.debug('[reports] customerCode =', getCustomerCode());
    await loadAll();
  });
})();
