(function(){
  const $ = (s,p=document)=>p.querySelector(s);
  const $$ = (s,p=document)=>[...p.querySelectorAll(s)];

  function setToday(){
    const d=new Date(), opt={weekday:'long',year:'numeric',month:'2-digit',day:'2-digit'};
    $('#todayText').textContent = d.toLocaleDateString('ko-KR',opt).replace(/\.\s*/g,'-');
  }

  function switchTab(key){
    $$('.tabbar .tab').forEach(b=>{
      const active = b.dataset.tab===key;
      b.classList.toggle('active', active);
      b.setAttribute('aria-selected', String(active));
    });
    $$('.tab-panel').forEach(p=>{
      p.classList.toggle('hidden', p.id !== `tab-${key}`);
      p.setAttribute('aria-hidden', String(p.id !== `tab-${key}`));
    });
  }

  function readRange(){
    const s = $('#startAt').value, e = $('#endAt').value;
    return { startAt:s || null, endAt:e || null };
  }

  async function loadOverview(range){
    // TODO: 실제 API 연결
    $('#chartDownloads').textContent = `그래프(경로별 다운로드) — ${range.startAt||'시작 미지정'} ~ ${range.endAt||'종료 미지정'}`;
    $('#chartConversion').textContent = `그래프(삭제 전환율) — ${range.startAt||'시작 미지정'} ~ ${range.endAt||'종료 미지정'}`;
    $('#chartProtection').textContent = `그래프(보호 조치) — ${range.startAt||'시작 미지정'} ~ ${range.endAt||'종료 미지정'}`;
  }

  async function loadEvents(range){
    // TODO: 실제 API 연결
    const rows = [
      {ts:'2025-08-25 10:21', user:'alice@corp', event:'download', target:'/docs/A.pdf', detail:'성공'},
      {ts:'2025-08-25 10:25', user:'bob@corp',   event:'delete',   target:'/docs/B.pdf', detail:'차단됨'}
    ];
    const tbody = $('#eventsTbody');
    tbody.innerHTML = rows.map(r=>`
      <tr>
        <td>${r.ts}</td><td>${r.user}</td><td>${r.event}</td><td>${r.target}</td><td>${r.detail}</td>
      </tr>`).join('');
    $('#eventsEmpty').hidden = rows.length>0;
  }

  async function loadUsers(range){
    // TODO: 실제 API 연결
    $('#userSummary').innerHTML = `
      <li>총 파일 1,240개</li>
      <li>보호됨 1,002개</li>
      <li>주의 필요 57개</li>
    `;
    $('#issueFiles').innerHTML = `
      <li>/exports/log-2025-08-25.csv — 외부 공유 시도</li>
      <li>/docs/plan.xlsx — 만료 임박</li>
    `;
  }

  async function refreshAll(){
    const range = readRange();
    const active = $('.tabbar .tab.active')?.dataset.tab || 'overview';
    if (active==='overview') await loadOverview(range);
    if (active==='events')   await loadEvents(range);
    if (active==='users')    await loadUsers(range);
  }

  // 초기화
  window.addEventListener('DOMContentLoaded', async ()=>{
    setToday();

    // 탭 전환
    $$('.tabbar .tab').forEach(btn=>{
      btn.addEventListener('click', ()=>{
        switchTab(btn.dataset.tab);
        refreshAll();
      });
    });

    // 새로고침
    $('#btnRefresh').addEventListener('click', refreshAll);

    // 기본값(최근 하루) 세팅
    const now = new Date();
    const z = n=>String(n).padStart(2,'0');
    const end = `${now.getFullYear()}-${z(now.getMonth()+1)}-${z(now.getDate())}T${z(now.getHours())}:${z(now.getMinutes())}`;
    const startDate = new Date(now.getTime()-24*60*60*1000);
    const start = `${startDate.getFullYear()}-${z(startDate.getMonth()+1)}-${z(startDate.getDate())}T${z(startDate.getHours())}:${z(startDate.getMinutes())}`;
    $('#startAt').value = start;
    $('#endAt').value = end;

    switchTab('overview');
    await refreshAll();
  });
})();
