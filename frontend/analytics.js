import {
  adminState,
  applyDateDefaults,
  saveDateRange,
  restoreSession,
  login,
  logout,
  loadWhoAmI,
  requestJson,
  initPageChrome,
  startAutoRefresh,
  currentLocale,
  toast,
} from "./admin-common.js";

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  fromDateInput: document.getElementById("fromDateInput"),
  toDateInput: document.getElementById("toDateInput"),
  hint: document.getElementById("connectionHint"),
  metrics: document.getElementById("metrics"),
  statusBars: document.getElementById("statusBars"),
  incidentBars: document.getElementById("incidentBars"),
};

els.accessTokenInput.value = adminState.accessPassword;
applyDateDefaults(els.fromDateInput, els.toDateInput);
initPageChrome();

function tx(ru, en) {
  return currentLocale() === "en" ? en : ru;
}

function periodParams() {
  const params = new URLSearchParams();
  if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
  if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
  return params;
}

function metricCard(label, value, foot, tone = "accent") {
  return `<article class="card metric"><div class="label">${label}</div><p class="value" style="color: var(--${tone})">${value}</p><p class="foot">${foot}</p></article>`;
}

function barItem(label, value, total, tone = "accent") {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  return `
    <div>
      <div class="row" style="justify-content:space-between;"><strong>${label}</strong><span class="hint">${value} (${pct}%)</span></div>
      <div style="height:10px; border:1px solid var(--line); border-radius:999px; overflow:hidden; background:rgba(255,255,255,0.04);">
        <div style="height:100%; width:${pct}%; background:var(--${tone});"></div>
      </div>
    </div>
  `;
}

async function loadCharts() {
  els.statusBars.innerHTML = `<div class="loading-cell">${tx("Загрузка...", "Loading...")}</div>`;
  els.incidentBars.innerHTML = `<div class="loading-cell">${tx("Загрузка...", "Loading...")}</div>`;
  const params = periodParams();
  const [summary, incidentRows] = await Promise.all([
    requestJson(`/api/admin/analytics/summary?${params.toString()}`),
    requestJson("/api/admin/incidents"),
  ]);

  const total = Number(summary.totalDetections || 0);
  els.metrics.innerHTML = [
    metricCard(tx("Всего детекций", "Total detections"), total, tx("За выбранный период", "For selected period")),
    metricCard("ACTIVE", summary.activeCount, tx("Без нарушений", "No violations"), "accent"),
    metricCard("BLOCKED", summary.blockedCount, tx("Критичные", "Critical"), "danger"),
    metricCard("UNDER_REVIEW", summary.underReviewCount, tx("На проверке", "Under review"), "warning"),
  ].join("");

  els.statusBars.innerHTML = [
    barItem("ACTIVE", Number(summary.activeCount || 0), total, "accent"),
    barItem("BLOCKED", Number(summary.blockedCount || 0), total, "danger"),
    barItem("UNDER_REVIEW", Number(summary.underReviewCount || 0), total, "warning"),
  ].join("");

  const blocked = incidentRows.filter((item) => item.status === "BLOCKED").length;
  const review = incidentRows.filter((item) => item.status === "UNDER_REVIEW").length;
  const incidentTotal = blocked + review;
  els.incidentBars.innerHTML = [
    barItem("BLOCKED", blocked, incidentTotal, "danger"),
    barItem("UNDER_REVIEW", review, incidentTotal, "warning"),
  ].join("");
}

async function refreshAll() {
  try {
    await Promise.all([loadWhoAmI(), loadCharts()]);
    els.hint.textContent = tx("Диаграммы обновлены.", "Charts updated.");
  } catch (error) {
    els.hint.textContent = `Ошибка: ${error.message}`;
    toast(tx("Диаграммы", "Charts"), error.message, "danger");
  }
}

function bind() {
  document.getElementById("saveBtn").addEventListener("click", async () => {
    try {
      saveDateRange(els.fromDateInput, els.toDateInput);
      await login(els.accessTokenInput.value.trim());
      await refreshAll();
    } catch (error) {
      els.hint.textContent = tx(`Вход не выполнен: ${error.message}`, `Login failed: ${error.message}`);
    }
  });
  document.getElementById("refreshBtn").addEventListener("click", async () => {
    saveDateRange(els.fromDateInput, els.toDateInput);
    await refreshAll();
  });
  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await logout();
    els.hint.textContent = tx("Вы вышли из админ-сессии.", "You have logged out from admin session.");
  });
}

async function bootstrap() {
  bind();
  const ok = await restoreSession();
  if (!ok) {
    els.hint.textContent = tx("Сессии нет. Введите пароль администратора.", "No session. Enter admin password.");
    return;
  }
  await refreshAll();
  startAutoRefresh(refreshAll, 15000);
}

bootstrap();
