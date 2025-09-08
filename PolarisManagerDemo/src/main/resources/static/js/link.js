// /js/links.js — UTM: source=회사, medium=WTA/ATA, campaign=사용이유 (추적 없음)
(() => {
  const PKG = 'com.polarisoffice.vguardsecuone';
  const $ = (s) => document.querySelector(s);

  // 소문자/숫자/_-만 남기기 (리포트 가독성용)
  const norm = (v) => (v || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '_')
    .replace(/[^a-z0-9_-]/g, '');

  function buildPlayUrl(company, medium, campaign) {
    const refQuery = new URLSearchParams({
      utm_source: company,   // ★ 회사명(코드)이 그대로 source
      utm_medium: medium,    // WTA/ATA
      utm_campaign: campaign, // vaccine/b2b/b2c 등
      company: company       // 앱에서 추가로 쓰기 쉽게 별도 키도 전달
    }).toString();

    const url = new URL('https://play.google.com/store/apps/details');
    url.searchParams.set('id', PKG);
    url.searchParams.set('referrer', refQuery); // 한 번만 인코딩됨
    return url.toString();
  }

  function setPreview(url) {
    const preview = $('#linkPreview');
    if (preview) preview.value = url;
  }

  function renderItem(listWrap, { company, url }) {
    const li = document.createElement('li');
    li.style.marginBottom = '6px';

    const label = document.createElement('strong');
    label.textContent = `${company}: `;
    li.appendChild(label);

    const a = document.createElement('a');
    a.href = url;
    a.textContent = url;
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    li.appendChild(a);

    const copyBtn = document.createElement('button');
    copyBtn.className = 'btn ghost';
    copyBtn.type = 'button';
    copyBtn.style.marginLeft = '8px';
    copyBtn.title = '링크 복사';
    copyBtn.textContent = '📋';
    copyBtn.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(url);
        copyBtn.textContent = '✅';
        setTimeout(() => (copyBtn.textContent = '📋'), 1200);
      } catch {
        prompt('복사하여 사용하세요:', url);
      }
    });
    li.appendChild(copyBtn);

    listWrap.appendChild(li);
  }

  function makeOne({ listWrap, codeEl, medEl, camEl, preset }) {
    const company  = norm(preset ?? codeEl.value ?? '');
    const medium   = norm(medEl.value || 'wta');          // select 값 WTA/ATA → wta/ata로 정규화
    const campaign = norm(camEl.value || 'vaccine');      // 기본값: vaccine
    if (!company) { codeEl?.focus(); return null; }

    const url = buildPlayUrl(company, medium, campaign);
    setPreview(url);
    renderItem(listWrap, { company, url });
    return url;
  }

  function wirePreviewCopy() {
    const btn = $('#copyPreviewBtn');
    const input = $('#linkPreview');
    if (!btn || !input) return;
    btn.addEventListener('click', async () => {
      const url = input.value?.trim(); if (!url) return;
      try {
        await navigator.clipboard.writeText(url);
        btn.textContent = '✅';
        setTimeout(() => (btn.textContent = '📋'), 1200);
      } catch {
        prompt('복사하여 사용하세요:', url);
      }
    });
  }

  function init() {
    const codeEl   = $('#companyCode');
    const medEl    = $('#utmMedium');
    const camEl    = $('#utmCampaign');
    const makeBtn  = $('#makeLinkBtn');
    const bulkBtn  = $('#bulkMakeBtn');
    const copyAll  = $('#copyAllBtn');
    const listTA   = $('#companyList');
    const listWrap = $('#companyLinks');

    if (!makeBtn || !listWrap) return;

    wirePreviewCopy();

    // 단일 생성
    makeBtn.addEventListener('click', () => {
      listWrap.innerHTML = '';
      makeOne({ listWrap, codeEl, medEl, camEl });
    });

    // 일괄 생성
    bulkBtn?.addEventListener('click', () => {
      const lines = (listTA?.value || '')
        .split(/\r?\n/).map(s => s.trim()).filter(Boolean);
      if (!lines.length) return;
      listWrap.innerHTML = '';
      let last = '';
      lines.forEach(c => {
        const u = makeOne({ listWrap, codeEl, medEl, camEl, preset: c });
        if (u) last = u;
      });
      if (last) setPreview(last);
    });

    // 모든 링크 복사
    copyAll?.addEventListener('click', async () => {
      let urls = [...listWrap.querySelectorAll('a[href^="https://play.google.com/"]')].map(a => a.href);
      if (!urls.length) {
        const u = makeOne({ listWrap, codeEl, medEl, camEl });
        if (!u) return;
        urls = [u];
      }
      try {
        await navigator.clipboard.writeText(urls.join('\n'));
        copyAll.textContent = '복사 완료!';
        setTimeout(() => (copyAll.textContent = '모든 링크 복사'), 1200);
      } catch {
        prompt('복사하여 사용하세요:', urls.join('\n'));
      }
    });
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
