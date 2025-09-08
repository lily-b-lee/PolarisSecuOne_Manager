(() => {
  // 날짜 기본값: 최근 5일
  const end = new Date();
  const start = new Date(end); start.setDate(end.getDate() - 4);
  const f = d => d.toISOString().slice(0,10);
  const $ = s => document.querySelector(s);

  $('#startDate').value = f(start);
  $('#endDate').value = f(end);

  $('#applyRange')?.addEventListener('click', () => {
    // 실제 서비스에서는 이곳에서 선택 기간으로 데이터 재조회
    alert(`기간 적용: ${$('#startDate').value} ~ ${$('#endDate').value}`);
  });

  // 샘플 데이터 (월/값)
  const labels = ['5월','6월','7월','8월'];
  const inflow = [1600, 2400, 900, 1500];
  const churn  = [350, 1700, 680, 1200];

  // Chart.js - CSS height를 그대로 쓰도록 설정
  const baseOptions = {
    responsive: true,
    maintainAspectRatio: false,   // ← 핵심: CSS 높이를 존중
    plugins: { legend: { display:false } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } }, x: { grid: { display:false } } }
  };

  const ctx1 = document.getElementById('chartInflow');
  const ctx2 = document.getElementById('chartChurn');

  new Chart(ctx1, {
    type: 'bar',
    data: { labels, datasets: [{ label: '신규 유입', data: inflow }] },
    options: baseOptions
  });

  new Chart(ctx2, {
    type: 'bar',
    data: { labels, datasets: [{ label: '이탈', data: churn }] },
    options: baseOptions
  });
})();
