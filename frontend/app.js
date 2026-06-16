const state = {
  apiBase: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  accessToken: localStorage.getItem("fleetscan.admin.accessToken") || "1234",
  sessionToken: localStorage.getItem("fleetscan.admin.sessionToken") || "",
  fromDate: localStorage.getItem("fleetscan.admin.fromDate") || "",
  toDate: localStorage.getItem("fleetscan.admin.toDate") || "",
  plate: "",
  status: "",
  decisionReason: "",
  confidenceFrom: "",
  confidenceTo: "",
  driverChatId: "",
  selectedDriverId: null,
  photoPreviewUrl: null,
  whoami: null,
  isReadOnly: false,
};

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  fromDateInput: document.getElementById("fromDateInput"),
  toDateInput: document.getElementById("toDateInput"),
  fleetSelect: document.getElementById("fleetSelect"),
  metrics: document.getElementById("metrics"),
  detectionsBody: document.getElementById("detectionsBody"),
  incidentsBody: document.getElementById("incidentsBody"),
  blacklistBody: document.getElementById("blacklistBody"),
  blacklistStatus: document.getElementById("blacklistStatus"),
  plateFilter: document.getElementById("plateFilter"),
  statusFilter: document.getElementById("statusFilter"),
  reasonFilter: document.getElementById("reasonFilter"),
  driverFilter: document.getElementById("driverFilter"),
  confidenceFromFilter: document.getElementById("confidenceFromFilter"),
  confidenceToFilter: document.getElementById("confidenceToFilter"),
  connectionHint: document.getElementById("connectionHint"),
  driverForm: document.getElementById("driverForm"),
  driverIdInput: document.getElementById("driverIdInput"),
  driverNameInput: document.getElementById("driverNameInput"),
  driverChatIdInput: document.getElementById("driverChatIdInput"),
  driverRoleInput: document.getElementById("driverRoleInput"),
  driverActiveInput: document.getElementById("driverActiveInput"),
  driverPlateInput: document.getElementById("driverPlateInput"),
  driverModelInput: document.getElementById("driverModelInput"),
  driversBody: document.getElementById("driversBody"),
  driverPhotosBody: document.getElementById("driverPhotosBody"),
  selectedDriverPill: document.getElementById("selectedDriverPill"),
  photoModal: document.getElementById("photoModal"),
  photoModalBackdrop: document.getElementById("photoModalBackdrop"),
  photoModalClose: document.getElementById("photoModalClose"),
  photoModalImage: document.getElementById("photoModalImage"),
  photoModalTitle: document.getElementById("photoModalTitle"),
  toastStack: document.getElementById("toastStack"),
  whoamiRole: document.getElementById("whoamiRole"),
  whoamiFleet: document.getElementById("whoamiFleet"),
  whoamiSession: document.getElementById("whoamiSession"),
  auditModeBanner: document.getElementById("auditModeBanner"),
  driverDetailsModal: document.getElementById("driverDetailsModal"),
  driverDetailsBackdrop: document.getElementById("driverDetailsBackdrop"),
  driverDetailsClose: document.getElementById("driverDetailsClose"),
  driverDetailsTitle: document.getElementById("driverDetailsTitle"),
  driverDetailsContent: document.getElementById("driverDetailsContent"),
  driverDetectionsBody: document.getElementById("driverDetectionsBody"),
};

els.accessTokenInput.value = state.accessToken;
els.fromDateInput.value = state.fromDate;
els.toDateInput.value = state.toDate;

const today = new Date();
if (!state.toDate) {
  els.toDateInput.valueAsDate = today;
}
if (!state.fromDate) {
  const weekAgo = new Date();
  weekAgo.setDate(weekAgo.getDate() - 7);
  els.fromDateInput.valueAsDate = weekAgo;
}

bindFilterEvents();
bindActionEvents();

function bindFilterEvents() {
  els.plateFilter.addEventListener("input", (event) => {
    state.plate = event.target.value.trim();
    loadDetections();
  });

  els.statusFilter.addEventListener("change", (event) => {
    state.status = event.target.value;
    loadDetections();
  });

  els.reasonFilter.addEventListener("input", (event) => {
    state.decisionReason = event.target.value.trim();
    loadDetections();
  });

  els.driverFilter.addEventListener("input", (event) => {
    state.driverChatId = event.target.value.trim();
    loadDetections();
  });

  els.confidenceFromFilter.addEventListener("input", (event) => {
    state.confidenceFrom = event.target.value.trim();
    loadDetections();
  });

  els.confidenceToFilter.addEventListener("input", (event) => {
    state.confidenceTo = event.target.value.trim();
    loadDetections();
  });

  document.querySelectorAll("[data-preset]").forEach((button) => {
    button.addEventListener("click", () => {
      applyDatePreset(button.getAttribute("data-preset"));
      refreshAll();
    });
  });
}

function bindActionEvents() {
  document.getElementById("saveBtn").addEventListener("click", async () => {
    state.accessToken = els.accessTokenInput.value.trim();
    state.fromDate = els.fromDateInput.value;
    state.toDate = els.toDateInput.value;
    localStorage.setItem("fleetscan.admin.accessToken", state.accessToken);
    localStorage.setItem("fleetscan.admin.fromDate", state.fromDate);
    localStorage.setItem("fleetscan.admin.toDate", state.toDate);
    await login();
    await refreshAll();
  });

  document.getElementById("refreshBtn").addEventListener("click", refreshAll);
  document.getElementById("exportBtn").addEventListener("click", () => exportFile("/api/admin/analytics/export", "detections.xlsx"));
  document.getElementById("logoutBtn").addEventListener("click", logout);
  document.getElementById("statusCheckBtn").addEventListener("click", checkBlacklistStatus);
  document.getElementById("reportDetectionsBtn").addEventListener("click", () => exportFile("/api/admin/analytics/export", "detections.xlsx"));
  document.getElementById("reportIncidentsBtn").addEventListener("click", () => exportFile("/api/admin/analytics/export/incidents", "incidents.csv"));
  document.getElementById("reportComplianceBtn").addEventListener("click", () => exportFile("/api/admin/analytics/export/driver-compliance", "driver-compliance.csv", false));

  document.getElementById("blacklistForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (state.isReadOnly) {
      return;
    }

    const plateNumber = document.getElementById("blacklistPlateInput").value.trim();
    const reason = document.getElementById("blacklistReasonInput").value.trim();
    const category = document.getElementById("blacklistCategoryInput").value;
    const expiresAtRaw = document.getElementById("blacklistExpiresAtInput").value;
    if (!plateNumber) {
      toast("Чёрный список", "Укажите номер", "warn");
      return;
    }

    await requestJson("/api/admin/blacklist", {
      method: "POST",
      body: JSON.stringify({
        plateNumber,
        reason,
        category,
        expiresAt: expiresAtRaw ? new Date(expiresAtRaw).toISOString().slice(0, 19) : null,
      }),
    });

    document.getElementById("blacklistPlateInput").value = "";
    document.getElementById("blacklistReasonInput").value = "";
    document.getElementById("blacklistExpiresAtInput").value = "";
    await refreshBlacklist();
    toast("Чёрный список", "Запись добавлена", "success");
  });

  els.driverForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (state.isReadOnly) {
      return;
    }

    const payload = {
      name: els.driverNameInput.value.trim(),
      chatId: parseNullableNumber(els.driverChatIdInput.value),
      role: els.driverRoleInput.value,
      vehiclePlate: els.driverPlateInput.value.trim(),
      vehicleModel: els.driverModelInput.value.trim(),
      isActive: els.driverActiveInput.value === "true",
    };

    let saved;
    if (els.driverIdInput.value) {
      saved = await requestJson(`/api/admin/drivers/${els.driverIdInput.value}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
    } else {
      saved = await requestJson("/api/admin/drivers", {
        method: "POST",
        body: JSON.stringify(payload),
      });
    }

    resetDriverForm();
    await refreshDrivers(saved?.id);
    toast("Водители", "Изменения сохранены", "success");
  });

  document.getElementById("driverResetBtn").addEventListener("click", () => {
    resetDriverForm();
    state.selectedDriverId = null;
    els.selectedDriverPill.textContent = "Водитель не выбран";
    renderDriverPhotos([]);
  });

  els.photoModalBackdrop.addEventListener("click", closePhotoPreview);
  els.photoModalClose.addEventListener("click", closePhotoPreview);
  els.driverDetailsBackdrop.addEventListener("click", closeDriverDetails);
  els.driverDetailsClose.addEventListener("click", closeDriverDetails);
}

function applyDatePreset(preset) {
  const to = new Date();
  const from = new Date();
  if (preset === "today") {
    
  } else if (preset === "30d") {
    from.setDate(from.getDate() - 30);
  } else {
    from.setDate(from.getDate() - 7);
  }
  els.fromDateInput.valueAsDate = from;
  els.toDateInput.valueAsDate = to;
  state.fromDate = els.fromDateInput.value;
  state.toDate = els.toDateInput.value;
  localStorage.setItem("fleetscan.admin.fromDate", state.fromDate);
  localStorage.setItem("fleetscan.admin.toDate", state.toDate);
}

function parseNullableNumber(value) {
  const trimmed = String(value || "").trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function toast(title, body, tone = "info") {
  if (!els.toastStack) {
    return;
  }
  const item = document.createElement("div");
  item.className = `toast ${tone === "danger" ? "toast--danger" : tone === "warn" ? "toast--warn" : tone === "success" ? "toast--success" : ""}`;
  item.innerHTML = `<p class="toast-title">${escapeHtml(title)}</p><p class="toast-body">${escapeHtml(body || "")}</p>`;
  els.toastStack.appendChild(item);
  setTimeout(() => item.remove(), 4500);
}

function renderWhoAmI() {
  const data = state.whoami;
  if (!state.sessionToken || !data) {
    els.whoamiRole.textContent = "—";
    els.whoamiFleet.textContent = "—";
    els.whoamiSession.textContent = "—";
    setReadOnly(false);
    return;
  }
  els.whoamiRole.textContent = data.role || "НЕИЗВЕСТНО";
  els.whoamiFleet.textContent = data.fleetName || "—";
  const expires = data.expiresAtEpochMillis ? new Date(data.expiresAtEpochMillis).toLocaleString("ru-RU") : "—";
  els.whoamiSession.textContent = expires;
  els.fleetSelect.innerHTML = `<option>${escapeHtml(data.fleetName || "—")}</option>`;
  setReadOnly(data.role === "OBSERVER");
}

function setReadOnly(enabled) {
  state.isReadOnly = Boolean(enabled);
  els.auditModeBanner.hidden = !state.isReadOnly;
  document.querySelectorAll(".mutating-action").forEach((item) => {
    item.disabled = state.isReadOnly;
    item.classList.toggle("is-disabled", state.isReadOnly);
  });
  document.querySelectorAll("[data-mutating='1']").forEach((item) => {
    item.disabled = state.isReadOnly;
    item.classList.toggle("is-disabled", state.isReadOnly);
  });
}

async function loadWhoAmI() {
  if (!state.sessionToken) {
    state.whoami = null;
    renderWhoAmI();
    return;
  }
  try {
    state.whoami = await requestJson("/api/auth/whoami");
  } catch (error) {
    state.whoami = null;
    toast("Профиль сессии", error.message || "Ошибка", "warn");
  } finally {
    renderWhoAmI();
  }
}

function requireSession() {
  if (!state.sessionToken) {
    els.connectionHint.textContent = "Сначала выполните вход по паролю администратора.";
    throw new Error("missing session token");
  }
}

function authHeaders(extra = {}) {
  requireSession();
  return {
    Authorization: `Bearer ${state.sessionToken}`,
    ...extra,
  };
}

async function apiFetch(url, options = {}) {
  const headers = {
    ...authHeaders(),
    ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
    ...(options.headers || {}),
  };

  const response = await fetch(`${state.apiBase}${url}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed: ${response.status}`);
  }

  return response;
}

async function requestJson(url, options = {}) {
  const response = await apiFetch(url, options);
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function requestBlob(url, options = {}) {
  const response = await apiFetch(url, options);
  return response.blob();
}

async function login() {
  if (!state.accessToken) {
    state.sessionToken = "";
    localStorage.removeItem("fleetscan.admin.sessionToken");
    return;
  }

  const response = await fetch(`${state.apiBase}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ accessToken: state.accessToken }),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "Login failed");
  }

  const data = await response.json();
  state.sessionToken = data.sessionToken;
  localStorage.setItem("fleetscan.admin.sessionToken", state.sessionToken);
}

async function logout() {
  try {
    if (state.sessionToken) {
      await fetch(`${state.apiBase}/api/auth/logout`, {
        method: "DELETE",
        headers: authHeaders({}),
      });
    }
  } catch {
    
  } finally {
    state.sessionToken = "";
    state.accessToken = "1234";
    localStorage.removeItem("fleetscan.admin.sessionToken");
    localStorage.setItem("fleetscan.admin.accessToken", state.accessToken);
    els.accessTokenInput.value = state.accessToken;
    els.connectionHint.textContent = "Сессия завершена.";
    await refreshAll();
  }
}

function metricCard(label, value, foot, tone = "accent") {
  return `<article class="card metric"><div class="label">${label}</div><p class="value" style="color: var(--${tone})">${value}</p><p class="foot">${foot}</p></article>`;
}

function statusClass(status) {
  if (status === "BLOCKED") return "status-off";
  if (status === "UNDER_REVIEW") return "status-review";
  return "status-on";
}

function formatDate(value) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString("ru-RU");
}

function openPhotoPreview(url, title = "Фото") {
  if (!url) {
    return;
  }
  requestBlob(url)
    .then((blob) => {
      if (state.photoPreviewUrl) {
        URL.revokeObjectURL(state.photoPreviewUrl);
      }
      state.photoPreviewUrl = URL.createObjectURL(blob);
      els.photoModalTitle.textContent = title;
      els.photoModalImage.src = state.photoPreviewUrl;
      els.photoModal.hidden = false;
      els.photoModal.classList.add("is-open");
    })
    .catch((error) => {
      toast("Фото", `Не удалось открыть: ${error.message}`, "danger");
    });
}

function closePhotoPreview() {
  els.photoModal.classList.remove("is-open");
  els.photoModal.hidden = true;
  els.photoModalImage.removeAttribute("src");
  if (state.photoPreviewUrl) {
    URL.revokeObjectURL(state.photoPreviewUrl);
    state.photoPreviewUrl = null;
  }
}

function closeDriverDetails() {
  els.driverDetailsModal.classList.remove("is-open");
  els.driverDetailsModal.hidden = true;
}

function resetDriverForm() {
  els.driverIdInput.value = "";
  els.driverNameInput.value = "";
  els.driverChatIdInput.value = "";
  els.driverRoleInput.value = "OPERATOR";
  els.driverActiveInput.value = "true";
  els.driverPlateInput.value = "";
  els.driverModelInput.value = "";
  els.selectedDriverPill.textContent = "Водитель не выбран";
  document.getElementById("driverSaveBtn").textContent = "Сохранить";
}

async function loadSummary() {
  if (!state.sessionToken) {
    els.metrics.innerHTML = "";
    return;
  }
  const params = new URLSearchParams();
  if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
  if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
  const data = await requestJson(`/api/admin/analytics/summary?${params.toString()}`);

  els.metrics.innerHTML = [
    metricCard("Всего детекций", data.totalDetections, "Сводка по выбранному периоду"),
    metricCard("ACTIVE", data.activeCount, "Нормальные машины", "accent"),
    metricCard("BLOCKED", data.blockedCount, "Требуют внимания", "danger"),
    metricCard("UNDER_REVIEW", data.underReviewCount, "На ручной проверке", "warning"),
  ].join("");
}

function buildDetectionQuery() {
  const params = new URLSearchParams();
  if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
  if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
  if (state.plate) params.set("plate", state.plate);
  if (state.status) params.set("status", state.status);
  if (state.decisionReason) params.set("decisionReason", state.decisionReason);
  if (state.driverChatId) params.set("driverChatId", state.driverChatId);
  if (state.confidenceFrom) params.set("confidenceFrom", state.confidenceFrom);
  if (state.confidenceTo) params.set("confidenceTo", state.confidenceTo);
  return params;
}

async function loadDetections() {
  if (!state.sessionToken) {
    els.detectionsBody.innerHTML = `<tr><td colspan="7" class="empty">Войдите по паролю, чтобы загрузить данные.</td></tr>`;
    return;
  }
  const rows = await requestJson(`/api/admin/analytics/detections?${buildDetectionQuery().toString()}`);

  if (!rows.length) {
    els.detectionsBody.innerHTML = `<tr><td colspan="7" class="empty">Данных нет.</td></tr>`;
    return;
  }

  els.detectionsBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${Number(row.confidence).toFixed(2)}</td>
      <td>${escapeHtml(row.condition)}</td>
      <td>${escapeHtml(row.decisionReason)}</td>
      <td>${formatDate(row.detectedAt)}</td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-preview="${escapeHtml(row.photoUrl || "")}" ${row.photoUrl ? "" : "disabled"}>Фото</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-preview]").forEach((button) => {
    button.addEventListener("click", () => {
      const url = button.getAttribute("data-preview");
      if (url) {
        openPhotoPreview(url, "Фото детекции");
      }
    });
  });
}

async function loadIncidents() {
  if (!state.sessionToken) {
    els.incidentsBody.innerHTML = `<tr><td colspan="5" class="empty">Войдите по паролю, чтобы загрузить данные.</td></tr>`;
    return;
  }

  const rows = await requestJson("/api/admin/incidents");
  if (!rows.length) {
    els.incidentsBody.innerHTML = `<tr><td colspan="5" class="empty">Инцидентов нет.</td></tr>`;
    return;
  }

  els.incidentsBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${escapeHtml(row.decisionReason || "")}</td>
      <td>${formatDate(row.detectedAt)}</td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-incident-action="ACTIVE" data-incident-id="${row.id}" data-mutating="1">Принять</button>
        <button class="btn btn-secondary" data-incident-action="UNDER_REVIEW" data-incident-id="${row.id}" data-mutating="1">На проверке</button>
        <button class="btn btn-secondary" data-incident-action="BLOCKED" data-incident-id="${row.id}" data-mutating="1">Блок</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-incident-action]").forEach((button) => {
    button.disabled = state.isReadOnly;
    button.addEventListener("click", async () => {
      if (state.isReadOnly) {
        return;
      }
      const id = Number(button.getAttribute("data-incident-id"));
      const status = button.getAttribute("data-incident-action");
      const comment = window.prompt("Комментарий/причина (опционально):", "") || "";
      await requestJson(`/api/admin/incidents/${id}`, {
        method: "PUT",
        body: JSON.stringify({ status, comment }),
      });
      toast("Инциденты", `Инцидент #${id} обновлён`, "success");
      await Promise.all([loadIncidents(), loadDetections(), loadSummary()]);
    });
  });
}

async function loadBlacklist() {
  if (!state.sessionToken) {
    els.blacklistBody.innerHTML = `<tr><td colspan="6" class="empty">Войдите по паролю, чтобы загрузить данные.</td></tr>`;
    return;
  }
  const rows = await requestJson("/api/admin/blacklist");

  if (!rows.length) {
    els.blacklistBody.innerHTML = `<tr><td colspan="6" class="empty">Пока пусто.</td></tr>`;
    return;
  }

  els.blacklistBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td>${escapeHtml(row.reason || "")}</td>
      <td>${escapeHtml(row.category || "OTHER")}</td>
      <td>${formatDate(row.expiresAt)}</td>
      <td>${row.active ? "Да" : "Нет"}</td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-remove="${escapeHtml(row.plateNumber)}" data-mutating="1">Удалить</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-remove]").forEach((button) => {
    button.disabled = state.isReadOnly;
    button.addEventListener("click", async () => {
      if (state.isReadOnly) {
        return;
      }
      const plateNumber = button.getAttribute("data-remove");
      await apiFetch(`/api/admin/blacklist/${encodeURIComponent(plateNumber)}`, { method: "DELETE" });
      await refreshBlacklist();
      toast("Чёрный список", `${plateNumber} удалён`, "success");
    });
  });
}

async function checkBlacklistStatus() {
  const plateNumber = document.getElementById("blacklistPlateInput").value.trim();
  if (!plateNumber) return;

  const params = new URLSearchParams({ plateNumber });
  const status = await requestJson(`/api/admin/blacklist/status?${params.toString()}`);
  els.blacklistStatus.textContent = status.active
    ? `Статус: ${status.plateNumber} в blacklist. Причина: ${status.reason}`
    : `Статус: ${status.plateNumber} не в blacklist.`;
}

function renderDrivers(rows) {
  if (!rows.length) {
    els.driversBody.innerHTML = `<tr><td colspan="5" class="empty">Пока нет водителей.</td></tr>`;
    return;
  }

  els.driversBody.innerHTML = rows.map((row) => `
    <tr data-driver-row="${row.id}" class="${state.selectedDriverId === row.id ? "selected-row" : ""}">
      <td>
        <strong>${escapeHtml(row.name)}</strong>
        <div class="hint">${escapeHtml(row.vehiclePlate || "")} ${escapeHtml(row.vehicleModel || "")}</div>
      </td>
      <td><span class="badge soft">${escapeHtml(row.role)}</span></td>
      <td>${row.chatId ?? "—"}</td>
      <td><div class="hint">${row.photoCount} фото</div><div class="hint">${row.detectionCount} детекций</div></td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-select-driver="${row.id}">Открыть</button>
        <button class="btn btn-secondary" data-driver-card="${row.id}">Карточка</button>
        <button class="btn btn-secondary" data-edit-driver="${row.id}" data-mutating="1">Редактировать</button>
        <button class="btn btn-secondary" data-delete-driver="${row.id}" data-mutating="1">Удалить</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-select-driver]").forEach((button) => {
    button.addEventListener("click", async () => {
      const driverId = Number(button.getAttribute("data-select-driver"));
      state.selectedDriverId = driverId;
      await loadDriverPhotos(driverId);
      await highlightDriverRows();
    });
  });

  document.querySelectorAll("[data-driver-card]").forEach((button) => {
    button.addEventListener("click", async () => {
      const driverId = Number(button.getAttribute("data-driver-card"));
      await openDriverDetails(driverId, rows.find((item) => item.id === driverId));
    });
  });

  document.querySelectorAll("[data-edit-driver]").forEach((button) => {
    button.disabled = state.isReadOnly;
    button.addEventListener("click", () => {
      const driverId = Number(button.getAttribute("data-edit-driver"));
      const row = rows.find((item) => item.id === driverId);
      if (!row) return;
      fillDriverForm(row);
    });
  });

  document.querySelectorAll("[data-delete-driver]").forEach((button) => {
    button.disabled = state.isReadOnly;
    button.addEventListener("click", async () => {
      if (state.isReadOnly) {
        return;
      }
      const driverId = Number(button.getAttribute("data-delete-driver"));
      await apiFetch(`/api/admin/drivers/${driverId}`, { method: "DELETE" });
      if (state.selectedDriverId === driverId) {
        state.selectedDriverId = null;
        renderDriverPhotos([]);
      }
      await refreshDrivers();
      toast("Водители", `Водитель #${driverId} деактивирован`, "success");
    });
  });
}

async function openDriverDetails(driverId, driverRow) {
  els.driverDetailsTitle.textContent = `Детали водителя #${driverId}`;
  els.driverDetailsContent.innerHTML = `
    <div class="pill">Имя: <strong>${escapeHtml(driverRow?.name || "—")}</strong></div>
    <div class="pill">Роль: <strong>${escapeHtml(driverRow?.role || "—")}</strong></div>
    <div class="pill">Номер: <strong>${escapeHtml(driverRow?.vehiclePlate || "—")}</strong></div>
    <div class="pill">Модель: <strong>${escapeHtml(driverRow?.vehicleModel || "—")}</strong></div>
  `;

  els.driverDetectionsBody.innerHTML = `<tr><td colspan="4" class="empty">Загрузка истории детекций...</td></tr>`;
  els.driverDetailsModal.hidden = false;
  els.driverDetailsModal.classList.add("is-open");

  const rows = await requestJson(`/api/admin/drivers/${driverId}/detections`);
  if (!rows.length) {
    els.driverDetectionsBody.innerHTML = `<tr><td colspan="4" class="empty">История детекций отсутствует.</td></tr>`;
    return;
  }

  els.driverDetectionsBody.innerHTML = rows.slice(0, 50).map((row) => `
    <tr>
      <td>${escapeHtml(row.plateNumber)}</td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${escapeHtml(row.decisionReason || "")}</td>
      <td>${formatDate(row.detectedAt)}</td>
    </tr>
  `).join("");
}

async function highlightDriverRows() {
  document.querySelectorAll("[data-driver-row]").forEach((row) => {
    row.classList.toggle("selected-row", Number(row.getAttribute("data-driver-row")) === state.selectedDriverId);
  });
}

function fillDriverForm(row) {
  els.driverIdInput.value = row.id;
  els.driverNameInput.value = row.name || "";
  els.driverChatIdInput.value = row.chatId ?? "";
  els.driverRoleInput.value = row.role || "OPERATOR";
  els.driverActiveInput.value = row.active ? "true" : "false";
  els.driverPlateInput.value = row.vehiclePlate || "";
  els.driverModelInput.value = row.vehicleModel || "";
  els.selectedDriverPill.textContent = `${row.name}${row.vehiclePlate ? ` • ${row.vehiclePlate}` : ""}`;
  document.getElementById("driverSaveBtn").textContent = "Обновить";
  state.selectedDriverId = row.id;
  loadDriverPhotos(row.id);
}

async function loadDrivers(preferredId = null) {
  if (!state.sessionToken) {
    els.driversBody.innerHTML = `<tr><td colspan="5" class="empty">Войдите по паролю, чтобы загрузить водителей.</td></tr>`;
    els.selectedDriverPill.textContent = "Водитель не выбран";
    renderDriverPhotos([]);
    return [];
  }

  const rows = await requestJson("/api/admin/drivers");
  renderDrivers(rows);

  if (preferredId) {
    state.selectedDriverId = preferredId;
  }

  if (!state.selectedDriverId && rows.length) {
    state.selectedDriverId = rows[0].id;
  }

  if (state.selectedDriverId) {
    await loadDriverPhotos(state.selectedDriverId);
  }
  await highlightDriverRows();
  return rows;
}

async function loadDriverPhotos(driverId) {
  if (!state.sessionToken || !driverId) {
    renderDriverPhotos([]);
    return;
  }

  const rows = await requestJson(`/api/admin/drivers/${driverId}/photos`);
  els.selectedDriverPill.textContent = `Водитель #${driverId}`;
  renderDriverPhotos(rows);
}

function renderDriverPhotos(rows) {
  if (!rows.length) {
    els.driverPhotosBody.innerHTML = `<tr><td colspan="4" class="empty">Фото не найдены.</td></tr>`;
    return;
  }

  els.driverPhotosBody.innerHTML = rows.map((row) => `
    <tr>
      <td><span class="badge soft">${escapeHtml(row.photoType || "OTHER")}</span><div class="hint">${escapeHtml(row.note || "")}</div></td>
      <td>${formatDate(row.createdAt)}</td>
      <td>${escapeHtml(row.status || "")}</td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-photo-preview="${escapeHtml(row.photoUrl || "")}" ${row.photoUrl ? "" : "disabled"}>Открыть</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-photo-preview]").forEach((button) => {
    button.addEventListener("click", () => {
      const url = button.getAttribute("data-photo-preview");
      if (url) {
        openPhotoPreview(url, "Фото водителя");
      }
    });
  });
}

async function refreshSummary() {
  await loadSummary();
}

async function refreshDetections() {
  await loadDetections();
}

async function refreshBlacklist() {
  await loadBlacklist();
}

async function refreshDrivers(preferredId = null) {
  await loadDrivers(preferredId);
}

async function refreshAll() {
  if (!state.sessionToken) {
    els.connectionHint.textContent = "Введите единый пароль администратора и нажмите вход.";
    await Promise.all([loadSummary(), loadDetections(), loadIncidents(), loadBlacklist()]);
    await loadDrivers();
    renderWhoAmI();
    return;
  }
  try {
    await Promise.all([
      loadWhoAmI(),
      refreshSummary(),
      refreshDetections(),
      loadIncidents(),
      refreshBlacklist(),
      refreshDrivers(state.selectedDriverId),
    ]);
    els.connectionHint.textContent = "Данные обновлены.";
  } catch (error) {
    els.connectionHint.textContent = `Ошибка: ${error.message}`;
    toast("Ошибка", error.message, "danger");
  }
}

async function exportFile(path, fileName, includeDetectionFilters = true) {
  try {
    requireSession();
    const params = new URLSearchParams();
    if (els.fromDateInput.value) params.set("fromDate", els.fromDateInput.value);
    if (els.toDateInput.value) params.set("toDate", els.toDateInput.value);
    if (includeDetectionFilters) {
      if (state.plate) params.set("plate", state.plate);
      if (state.status) params.set("status", state.status);
      if (state.decisionReason) params.set("decisionReason", state.decisionReason);
      if (state.driverChatId) params.set("driverChatId", state.driverChatId);
      if (state.confidenceFrom) params.set("confidenceFrom", state.confidenceFrom);
      if (state.confidenceTo) params.set("confidenceTo", state.confidenceTo);
    }

    const response = await apiFetch(`${path}?${params.toString()}`, { method: "GET" });
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
    toast("Экспорт", `Файл ${fileName} подготовлен`, "success");
  } catch (error) {
    els.connectionHint.textContent = `Экспорт не выполнен: ${error.message}`;
    toast("Экспорт", error.message, "danger");
  }
}

async function bootstrap() {
  try {
    if (state.sessionToken) {
      const me = await requestJson("/api/auth/me");
      if (!me.authenticated) {
        state.sessionToken = "";
        localStorage.removeItem("fleetscan.admin.sessionToken");
      }
    } else if (state.accessToken) {
      await login();
    }
  } catch (error) {
    state.sessionToken = "";
    localStorage.removeItem("fleetscan.admin.sessionToken");
    els.connectionHint.textContent = `Вход не выполнен: ${error.message}`;
  }

  await refreshAll();
}

bootstrap();
