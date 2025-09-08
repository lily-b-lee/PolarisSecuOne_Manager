// push.js
(function () {
  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  const modal = $('#pushModal');
  if (!modal) return; // 해당 페이지에 모달이 없으면 종료

  const openBtn = $('#openPushModalBtn');
  const closeBtn = $('#closePushModalBtn');
  const cancelBtn = $('#cancelPushBtn');
  const sendBtn = $('#sendPushBtn');
  const targetType = $('#targetType');

  const tokenRow = $('.target-token');
  const tokensRow = $('.target-tokens');
  const topicRow = $('.target-topic');

  const tokenInput = $('#tokenInput');
  const tokensInput = $('#tokensInput');
  const topicInput = $('#topicInput');
  const titleInput = $('#titleInput');
  const bodyInput = $('#bodyInput');
  const priorityInput = $('#priorityInput');
  const kvWrap = $('#kvWrap');
  const addKvBtn = $('#addKvBtn');

  function openModal() {
    modal.style.display = 'flex';
    modal.setAttribute('aria-hidden', 'false');
    setTimeout(() => titleInput.focus(), 0);
  }
  function closeModal() {
    modal.style.display = 'none';
    modal.setAttribute('aria-hidden', 'true');
  }

  openBtn.addEventListener('click', openModal);
  closeBtn.addEventListener('click', closeModal);
  cancelBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

  // 대상 전환
  targetType.addEventListener('change', () => {
    const v = targetType.value;
    tokenRow.style.display = v === 'token' ? '' : 'none';
    tokensRow.style.display = v === 'tokens' ? '' : 'none';
    topicRow.style.display = v === 'topic' ? '' : 'none';
  });

  // KV 추가/삭제
  addKvBtn.addEventListener('click', () => {
    const row = document.createElement('div');
    row.className = 'kv';
    row.innerHTML = `
      <input placeholder="key" class="kv-key">
      <input placeholder="value" class="kv-val">
      <button class="btn" data-action="remove-kv">삭제</button>
    `;
    kvWrap.insertBefore(row, addKvBtn);
  });
  kvWrap.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-action="remove-kv"]');
    if (!btn) return;
    const kv = btn.closest('.kv');
    if (kv) kv.remove();
  });

  function collectDataKV() {
    const data = {};
    $$('.kv').forEach(kv => {
      const k = kv.querySelector('.kv-key')?.value?.trim();
      const v = kv.querySelector('.kv-val')?.value?.trim();
      if (k) data[k] = v ?? '';
    });
    return data;
  }

  function parseTokens(raw) {
    if (!raw) return [];
    return raw
      .split(/[\n,]/g)
      .map(t => t.trim())
      .filter(Boolean);
  }

  async function postJSON(url, payload) {
    const res = await fetch(url, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const text = await res.text().catch(() => '');
      throw new Error(`HTTP ${res.status} ${res.statusText}\n${text}`);
    }
    return res.json().catch(() => ({}));
  }

  sendBtn.addEventListener('click', async () => {
    try {
      const data = collectDataKV();
      const highPriority = priorityInput.value === 'true';
      const base = {
        title: titleInput.value?.trim(),
        body: bodyInput.value?.trim(),
        data, highPriority
      };

      if (!base.title || !base.body) {
        alert('제목과 본문을 입력해 주세요.');
        return;
      }

      const mode = targetType.value;
      let url = '';
      let payload = { ...base };

      if (mode === 'token') {
        const token = tokenInput.value?.trim();
        if (!token) return alert('디바이스 토큰을 입력해 주세요.');
        url = '/api/push/token';
        payload.token = token;
      } else if (mode === 'tokens') {
        const tokens = parseTokens(tokensInput.value);
        if (tokens.length === 0) return alert('디바이스 토큰을 한 개 이상 입력해 주세요.');
        url = '/api/push/tokens';
        payload.tokens = tokens;
      } else if (mode === 'topic') {
        const topic = topicInput.value?.trim();
        if (!topic) return alert('토픽을 입력해 주세요.');
        url = '/api/push/topic';
        payload.topic = topic;
      }

      sendBtn.disabled = true;
      sendBtn.textContent = '전송 중…';

      const result = await postJSON(url, payload);

      if (mode === 'tokens') {
        alert(`완료\n성공: ${result.success ?? '-'} / 실패: ${result.failure ?? '-'}\n유효하지 않은 토큰: ${(result.invalidTokens || []).length}`);
      } else {
        alert(`전송 완료! messageId: ${result.messageId ?? '-'}`);
      }
      closeModal();
    } catch (err) {
      console.error(err);
      alert('전송 실패:\n' + (err?.message || err));
    } finally {
      sendBtn.disabled = false;
      sendBtn.textContent = '보내기';
    }
  });
})();
