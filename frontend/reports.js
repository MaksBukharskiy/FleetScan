import {
  adminState,
  applyDateDefaults,
  saveDateRange,
  restoreSession,
  login,
  logout,
  loadWhoAmI,
  exportFile,
  toast,
  initPageChrome,
  startAutoRefresh,
  currentLocale,
} from "./admin-common.js";

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  fromDateInput: document.getElementById("fromDateInput"),
  toDateInput: document.getElementById("toDateInput"),
  hint: document.getElementById("connectionHint"),
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

async function refresh() {
  try {
    await loadWhoAmI();
    els.hint.textContent = tx("Профиль сессии обновлён.", "Session profile updated.");
  } catch (error) {
    els.hint.textContent = `Ошибка: ${error.message}`;
  }
}

function bind() {
  document.getElementById("saveBtn").addEventListener("click", async () => {
    try {
      saveDateRange(els.fromDateInput, els.toDateInput);
      await login(els.accessTokenInput.value.trim());
      await refresh();
    } catch (error) {
      els.hint.textContent = tx(`Вход не выполнен: ${error.message}`, `Login failed: ${error.message}`);
      toast(tx("Вход", "Login"), error.message, "danger");
    }
  });

  document.getElementById("refreshBtn").addEventListener("click", async () => {
    saveDateRange(els.fromDateInput, els.toDateInput);
    await refresh();
  });

  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await logout();
    els.hint.textContent = tx("Вы вышли из админ-сессии.", "You have logged out from admin session.");
  });

  document.getElementById("detectionsExportBtn").addEventListener("click", async () => {
    try {
      await exportFile("/api/admin/analytics/export", "detections.xlsx", periodParams());
      toast(tx("Экспорт", "Export"), tx("detections.xlsx готов", "detections.xlsx is ready"), "success");
    } catch (error) {
      toast(tx("Экспорт", "Export"), error.message, "danger");
    }
  });

  document.getElementById("incidentsExportBtn").addEventListener("click", async () => {
    try {
      await exportFile("/api/admin/analytics/export/incidents", "incidents.csv", periodParams());
      toast(tx("Экспорт", "Export"), tx("incidents.csv готов", "incidents.csv is ready"), "success");
    } catch (error) {
      toast(tx("Экспорт", "Export"), error.message, "danger");
    }
  });

  document.getElementById("complianceExportBtn").addEventListener("click", async () => {
    try {
      await exportFile("/api/admin/analytics/export/driver-compliance", "driver-compliance.csv");
      toast(tx("Экспорт", "Export"), tx("driver-compliance.csv готов", "driver-compliance.csv is ready"), "success");
    } catch (error) {
      toast(tx("Экспорт", "Export"), error.message, "danger");
    }
  });
}

async function bootstrap() {
  bind();
  const ok = await restoreSession();
  if (!ok) {
    els.hint.textContent = tx("Сессии нет. Введите пароль администратора.", "No session. Enter admin password.");
    return;
  }
  await refresh();
  startAutoRefresh(refresh, 15000);
}

bootstrap();
