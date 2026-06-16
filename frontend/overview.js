import {
  adminState,
  applyDateDefaults,
  saveDateRange,
  restoreSession,
  login,
  logout,
  loadWhoAmI,
  requestJson,
  requestBlob,
  exportFile,
  toast,
  escapeHtml,
  formatDate,
  parseNullableNumber,
  statusClass,
  debounce,
  initPageChrome,
  startAutoRefresh,
  currentLocale,
} from "./admin-common.js";

const filters = {
  plate: "",
  status: "",
  reason: "",
  driverChatId: "",
  confidenceFrom: "",
  confidenceTo: "",
};

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  fromDateInput: document.getElementById("fromDateInput"),
  toDateInput: document.getElementById("toDateInput"),
  metrics: document.getElementById("metrics"),
  detectionsBody: document.getElementById("detectionsBody"),
  hint: document.getElementById("connectionHint"),
  photoModal: document.getElementById("photoModal"),
  photoModalBackdrop: document.getElementById("photoModalBackdrop"),
  photoModalClose: document.getElementById("photoModalClose"),
  photoModalImage: document.getElementById("photoModalImage"),
  photoModalTitle: document.getElementById("photoModalTitle"),
};

let photoUrl = null;
let detectionsAbortController = null;
els.accessTokenInput.value = adminState.accessPassword;
applyDateDefaults(els.fromDateInput, els.toDateInput);
initPageChrome();

function metricCard(label, value, foot, tone = "accent") {
  return `<article class="card metric"><div class="label">${label}</div><p class="value" style="color: var(--${tone})">${value}</p><p class="foot">${foot}</p></article>`;
}

function buildParams() {
  const params = new URLSearchParams();
  if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
  if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
  if (filters.plate) params.set("plate", filters.plate);
  if (filters.status) params.set("status", filters.status);
  if (filters.reason) params.set("decisionReason", filters.reason);
  if (filters.driverChatId) params.set("driverChatId", filters.driverChatId);
  if (filters.confidenceFrom) params.set("confidenceFrom", filters.confidenceFrom);
  if (filters.confidenceTo) params.set("confidenceTo", filters.confidenceTo);
  return params;
}

function closePhoto() {
  els.photoModal.classList.remove("is-open");
  els.photoModal.hidden = true;
  els.photoModalImage.removeAttribute("src");
  if (photoUrl) {
    URL.revokeObjectURL(photoUrl);
    photoUrl = null;
  }
}

async function openPhoto(url) {
  const blob = await requestBlob(url);
  if (photoUrl) {
    URL.revokeObjectURL(photoUrl);
  }
  photoUrl = URL.createObjectURL(blob);
  els.photoModalTitle.textContent = "Фото детекции";
  els.photoModalImage.src = photoUrl;
  els.photoModal.hidden = false;
  els.photoModal.classList.add("is-open");
}

async function loadSummary() {
  els.metrics.innerHTML = "";
  const params = new URLSearchParams();
  if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
  if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
  const data = await requestJson(`/api/admin/analytics/summary?${params.toString()}`);
  els.metrics.innerHTML = [
    metricCard("Всего", data.totalDetections, "Детекции за период"),
    metricCard("ACTIVE", data.activeCount, "Без нарушений", "accent"),
    metricCard("BLOCKED", data.blockedCount, "Критичные", "danger"),
    metricCard("UNDER_REVIEW", data.underReviewCount, "На проверке", "warning"),
  ].join("");
}

async function loadDetections() {
  els.detectionsBody.innerHTML = `<tr><td colspan="6" class="loading-cell">${currentLocale() === "en" ? "Loading..." : "Загрузка..."}</td></tr>`;
  if (detectionsAbortController) {
    detectionsAbortController.abort();
  }
  detectionsAbortController = new AbortController();
  const rows = await requestJson(`/api/admin/analytics/detections?${buildParams().toString()}`, {
    signal: detectionsAbortController.signal,
  });
  if (!rows.length) {
    els.detectionsBody.innerHTML = `<tr><td colspan="6" class="empty">${currentLocale() === "en" ? "No data for current filters." : "Нет данных по текущим фильтрам."}</td></tr>`;
    return;
  }
  els.detectionsBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${Number(row.confidence).toFixed(2)}</td>
      <td>${escapeHtml(row.decisionReason || "")}</td>
      <td>${formatDate(row.detectedAt)}</td>
      <td><button class="btn btn-secondary" data-photo="${escapeHtml(row.photoUrl || "")}" ${row.photoUrl ? "" : "disabled"}>Фото</button></td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-photo]").forEach((button) => {
    button.addEventListener("click", async () => {
      const url = button.getAttribute("data-photo");
      if (!url) return;
      try {
        await openPhoto(url);
      } catch (error) {
        toast("Фото", error.message, "danger");
      }
    });
  });
}

async function refreshAll() {
  try {
    await Promise.all([loadWhoAmI(), loadSummary(), loadDetections()]);
    els.hint.textContent = "Данные обновлены.";
  } catch (error) {
    if (String(error?.name) === "AbortError") {
      return;
    }
    els.hint.textContent = `Ошибка: ${error.message}`;
    toast("Обзор", error.message, "danger");
  }
}

function bind() {
  document.getElementById("saveBtn").addEventListener("click", async () => {
    try {
      saveDateRange(els.fromDateInput, els.toDateInput);
      await login(els.accessTokenInput.value.trim());
      await refreshAll();
    } catch (error) {
      els.hint.textContent = `Вход не выполнен: ${error.message}`;
      toast("Вход", error.message, "danger");
    }
  });

  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await logout();
    els.hint.textContent = "Вы вышли из админ-сессии.";
  });

  document.getElementById("refreshBtn").addEventListener("click", async () => {
    saveDateRange(els.fromDateInput, els.toDateInput);
    await refreshAll();
  });

  document.getElementById("exportBtn").addEventListener("click", async () => {
    try {
      await exportFile("/api/admin/analytics/export", "detections.xlsx", buildParams());
      toast("Экспорт", "Файл detections.xlsx готов", "success");
    } catch (error) {
      toast("Экспорт", error.message, "danger");
    }
  });

  const scheduleDetectionsLoad = debounce(() => {
    loadDetections().catch((error) => {
      if (String(error?.name) !== "AbortError") {
        toast("Обзор", error.message, "danger");
      }
    });
  }, 320);
  document.getElementById("plateFilter").addEventListener("input", (e) => { filters.plate = e.target.value.trim(); scheduleDetectionsLoad(); });
  document.getElementById("statusFilter").addEventListener("change", (e) => { filters.status = e.target.value; scheduleDetectionsLoad(); });
  document.getElementById("reasonFilter").addEventListener("input", (e) => { filters.reason = e.target.value.trim(); scheduleDetectionsLoad(); });
  document.getElementById("driverFilter").addEventListener("input", (e) => { filters.driverChatId = parseNullableNumber(e.target.value) ?? ""; scheduleDetectionsLoad(); });
  document.getElementById("confidenceFromFilter").addEventListener("input", (e) => { filters.confidenceFrom = e.target.value.trim(); scheduleDetectionsLoad(); });
  document.getElementById("confidenceToFilter").addEventListener("input", (e) => { filters.confidenceTo = e.target.value.trim(); scheduleDetectionsLoad(); });

  document.querySelectorAll("[data-preset]").forEach((button) => {
    button.addEventListener("click", async () => {
      const preset = button.getAttribute("data-preset");
      const to = new Date();
      const from = new Date();
      if (preset === "30d") from.setDate(from.getDate() - 30);
      else if (preset === "7d") from.setDate(from.getDate() - 7);
      els.fromDateInput.valueAsDate = from;
      els.toDateInput.valueAsDate = to;
      saveDateRange(els.fromDateInput, els.toDateInput);
      await refreshAll();
    });
  });

  els.photoModalBackdrop.addEventListener("click", closePhoto);
  els.photoModalClose.addEventListener("click", closePhoto);
}

async function bootstrap() {
  bind();
  const ok = await restoreSession();
  if (!ok) {
    els.hint.textContent = "Сессии нет. Введите пароль администратора.";
    return;
  }
  await refreshAll();
  startAutoRefresh(refreshAll, 15000);
}

bootstrap();
