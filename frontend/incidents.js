import {
  adminState,
  restoreSession,
  login,
  logout,
  loadWhoAmI,
  requestJson,
  exportFile,
  toast,
  escapeHtml,
  formatDate,
  statusClass,
  initPageChrome,
  startAutoRefresh,
  currentLocale,
} from "./admin-common.js";

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  hint: document.getElementById("connectionHint"),
  incidentsBody: document.getElementById("incidentsBody"),
  incidentModal: document.getElementById("incidentModal"),
  incidentModalBackdrop: document.getElementById("incidentModalBackdrop"),
  incidentModalClose: document.getElementById("incidentModalClose"),
  incidentModalCancel: document.getElementById("incidentModalCancel"),
  incidentModalConfirm: document.getElementById("incidentModalConfirm"),
  incidentCommentInput: document.getElementById("incidentCommentInput"),
  incidentModalTitle: document.getElementById("incidentModalTitle"),
};

els.accessTokenInput.value = adminState.accessPassword;
initPageChrome();
let pendingAction = null;

function tx(ru, en) {
  return currentLocale() === "en" ? en : ru;
}

function openIncidentModal(id, status) {
  pendingAction = { id, status };
  els.incidentCommentInput.value = "";
  els.incidentModalTitle.textContent = tx(`Обновить инцидент #${id}`, `Update incident #${id}`);
  els.incidentModal.hidden = false;
  els.incidentModal.classList.add("is-open");
}

function closeIncidentModal() {
  pendingAction = null;
  els.incidentModal.classList.remove("is-open");
  els.incidentModal.hidden = true;
}

async function confirmIncidentAction() {
  if (!pendingAction || adminState.isReadOnly) {
    return;
  }
  const comment = els.incidentCommentInput.value.trim();
  const { id, status } = pendingAction;
  await requestJson(`/api/admin/incidents/${id}`, {
    method: "PUT",
    body: JSON.stringify({ status, comment }),
  });
  toast(tx("Инциденты", "Incidents"), tx(`Инцидент #${id} обновлён`, `Incident #${id} updated`), "success");
  closeIncidentModal();
  await loadIncidents();
}

async function loadIncidents() {
  els.incidentsBody.innerHTML = `<tr><td colspan="5" class="loading-cell">${tx("Загрузка...", "Loading...")}</td></tr>`;
  const rows = await requestJson("/api/admin/incidents");
  if (!rows.length) {
    els.incidentsBody.innerHTML = `<tr><td colspan="5" class="empty">${tx("Инцидентов нет.", "No incidents.")}</td></tr>`;
    return;
  }

  els.incidentsBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${escapeHtml(row.decisionReason || "")}</td>
      <td>${formatDate(row.detectedAt)}</td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-id="${row.id}" data-status="ACTIVE" data-mutating="1">${tx("Принять", "Approve")}</button>
        <button class="btn btn-secondary" data-id="${row.id}" data-status="UNDER_REVIEW" data-mutating="1">${tx("На проверке", "Review")}</button>
        <button class="btn btn-secondary" data-id="${row.id}" data-status="BLOCKED" data-mutating="1">${tx("Блок", "Block")}</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-id][data-status]").forEach((button) => {
    button.disabled = adminState.isReadOnly;
    button.addEventListener("click", async () => {
      if (adminState.isReadOnly) {
        return;
      }
      const id = Number(button.getAttribute("data-id"));
      const status = button.getAttribute("data-status");
      openIncidentModal(id, status);
    });
  });
}

async function refreshAll() {
  try {
    await Promise.all([loadWhoAmI(), loadIncidents()]);
    els.hint.textContent = tx("Инциденты обновлены.", "Incidents updated.");
  } catch (error) {
    els.hint.textContent = `Ошибка: ${error.message}`;
    toast(tx("Инциденты", "Incidents"), error.message, "danger");
  }
}

function bind() {
  document.getElementById("saveBtn").addEventListener("click", async () => {
    try {
      await login(els.accessTokenInput.value.trim());
      await refreshAll();
    } catch (error) {
      els.hint.textContent = tx(`Вход не выполнен: ${error.message}`, `Login failed: ${error.message}`);
    }
  });

  document.getElementById("refreshBtn").addEventListener("click", refreshAll);

  document.getElementById("exportBtn").addEventListener("click", async () => {
    try {
      await exportFile("/api/admin/analytics/export/incidents", "incidents.csv");
      toast(tx("Экспорт", "Export"), tx("Файл incidents.csv готов", "incidents.csv is ready"), "success");
    } catch (error) {
      toast(tx("Экспорт", "Export"), error.message, "danger");
    }
  });

  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await logout();
    els.hint.textContent = tx("Вы вышли из админ-сессии.", "You have logged out from admin session.");
  });

  els.incidentModalBackdrop.addEventListener("click", closeIncidentModal);
  els.incidentModalClose.addEventListener("click", closeIncidentModal);
  els.incidentModalCancel.addEventListener("click", closeIncidentModal);
  els.incidentModalConfirm.addEventListener("click", async () => {
    try {
      await confirmIncidentAction();
    } catch (error) {
      toast(tx("Инциденты", "Incidents"), error.message, "danger");
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
  await refreshAll();
  startAutoRefresh(refreshAll, 15000);
}

bootstrap();
