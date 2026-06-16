import {
  adminState,
  restoreSession,
  login,
  logout,
  loadWhoAmI,
  requestJson,
  apiFetch,
  toast,
  escapeHtml,
  formatDate,
  parseNullableNumber,
  initPageChrome,
  startAutoRefresh,
  currentLocale,
} from "./admin-common.js";

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  hint: document.getElementById("connectionHint"),
  driverForm: document.getElementById("driverForm"),
  driverIdInput: document.getElementById("driverIdInput"),
  driverNameInput: document.getElementById("driverNameInput"),
  driverChatIdInput: document.getElementById("driverChatIdInput"),
  driverRoleInput: document.getElementById("driverRoleInput"),
  driverActiveInput: document.getElementById("driverActiveInput"),
  driverPlateInput: document.getElementById("driverPlateInput"),
  driverModelInput: document.getElementById("driverModelInput"),
  driverSaveBtn: document.getElementById("driverSaveBtn"),
  driversBody: document.getElementById("driversBody"),
  driverPhotosBody: document.getElementById("driverPhotosBody"),
  selectedDriverPill: document.getElementById("selectedDriverPill"),
};

els.accessTokenInput.value = adminState.accessPassword;
initPageChrome();
let selectedDriverId = null;

function tx(ru, en) {
  return currentLocale() === "en" ? en : ru;
}

function resetForm() {
  els.driverIdInput.value = "";
  els.driverNameInput.value = "";
  els.driverChatIdInput.value = "";
  els.driverRoleInput.value = "OPERATOR";
  els.driverActiveInput.value = "true";
  els.driverPlateInput.value = "";
  els.driverModelInput.value = "";
  els.driverSaveBtn.textContent = tx("Сохранить", "Save");
}

async function loadDriverPhotos(driverId) {
  if (!driverId) {
    els.driverPhotosBody.innerHTML = `<tr><td colspan="4" class="empty">${tx("Фото не выбраны.", "No photos selected.")}</td></tr>`;
    return;
  }
  els.driverPhotosBody.innerHTML = `<tr><td colspan="4" class="loading-cell">${tx("Загрузка...", "Loading...")}</td></tr>`;
  const rows = await requestJson(`/api/admin/drivers/${driverId}/photos`);
  if (!rows.length) {
    els.driverPhotosBody.innerHTML = `<tr><td colspan="4" class="empty">${tx("Фото не найдены.", "No photos found.")}</td></tr>`;
    return;
  }
  els.driverPhotosBody.innerHTML = rows.map((row) => `
    <tr>
      <td>${escapeHtml(row.photoType || "OTHER")}</td>
      <td>${formatDate(row.createdAt)}</td>
      <td>${escapeHtml(row.status || "")}</td>
      <td><a class="btn btn-secondary" href="${escapeHtml(row.photoUrl || "#")}" target="_blank">${tx("Открыть", "Open")}</a></td>
    </tr>
  `).join("");
}

function fillForm(row) {
  els.driverIdInput.value = row.id;
  els.driverNameInput.value = row.name || "";
  els.driverChatIdInput.value = row.chatId ?? "";
  els.driverRoleInput.value = row.role || "OPERATOR";
  els.driverActiveInput.value = row.active ? "true" : "false";
  els.driverPlateInput.value = row.vehiclePlate || "";
  els.driverModelInput.value = row.vehicleModel || "";
  els.driverSaveBtn.textContent = tx("Обновить", "Update");
}

async function loadDrivers() {
  els.driversBody.innerHTML = `<tr><td colspan="5" class="loading-cell">${tx("Загрузка...", "Loading...")}</td></tr>`;
  const rows = await requestJson("/api/admin/drivers");
  if (!rows.length) {
    els.driversBody.innerHTML = `<tr><td colspan="5" class="empty">${tx("Нет водителей.", "No drivers.")}</td></tr>`;
    return;
  }
  els.driversBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.name)}</strong><div class="hint">${escapeHtml(row.vehiclePlate || "")}</div></td>
      <td>${escapeHtml(row.role || "")}</td>
      <td>${row.chatId ?? "—"}</td>
      <td><div class="hint">${tx("Детекций", "Detections")}: ${row.detectionCount}</div><div class="hint">${tx("Фото", "Photos")}: ${row.photoCount}</div></td>
      <td class="action-cell">
        <button class="btn btn-secondary" data-open="${row.id}">${tx("Открыть", "Open")}</button>
        <button class="btn btn-secondary" data-edit="${row.id}" data-mutating="1">${tx("Редактировать", "Edit")}</button>
        <button class="btn btn-secondary" data-delete="${row.id}" data-mutating="1">${tx("Удалить", "Delete")}</button>
      </td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-open]").forEach((button) => {
    button.addEventListener("click", async () => {
      const id = Number(button.getAttribute("data-open"));
      selectedDriverId = id;
      els.selectedDriverPill.textContent = tx(`Водитель #${id}`, `Driver #${id}`);
      await loadDriverPhotos(id);
    });
  });

  document.querySelectorAll("[data-edit]").forEach((button) => {
    button.disabled = adminState.isReadOnly;
    button.addEventListener("click", () => {
      if (adminState.isReadOnly) return;
      const id = Number(button.getAttribute("data-edit"));
      const row = rows.find((item) => item.id === id);
      if (!row) return;
      fillForm(row);
      selectedDriverId = id;
      els.selectedDriverPill.textContent = tx(`Водитель #${id}`, `Driver #${id}`);
    });
  });

  document.querySelectorAll("[data-delete]").forEach((button) => {
    button.disabled = adminState.isReadOnly;
    button.addEventListener("click", async () => {
      if (adminState.isReadOnly) return;
      const id = Number(button.getAttribute("data-delete"));
      await apiFetch(`/api/admin/drivers/${id}`, { method: "DELETE" });
      toast(tx("Водители", "Drivers"), tx(`Водитель #${id} деактивирован`, `Driver #${id} deactivated`), "success");
      await loadDrivers();
    });
  });
}

async function refreshAll() {
  try {
    await Promise.all([loadWhoAmI(), loadDrivers()]);
    if (selectedDriverId) {
      await loadDriverPhotos(selectedDriverId);
    }
    els.hint.textContent = tx("Данные по водителям обновлены.", "Driver data updated.");
  } catch (error) {
    els.hint.textContent = `Ошибка: ${error.message}`;
    toast(tx("Водители", "Drivers"), error.message, "danger");
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
  document.getElementById("logoutBtn").addEventListener("click", async () => {
    await logout();
    els.hint.textContent = tx("Вы вышли из админ-сессии.", "You have logged out from admin session.");
  });

  document.getElementById("driverResetBtn").addEventListener("click", () => {
    resetForm();
    selectedDriverId = null;
    els.selectedDriverPill.textContent = tx("Водитель не выбран", "No driver selected");
    els.driverPhotosBody.innerHTML = `<tr><td colspan="4" class="empty">${tx("Фото не выбраны.", "No photos selected.")}</td></tr>`;
  });

  els.driverForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (adminState.isReadOnly) {
      return;
    }

    const payload = {
      name: els.driverNameInput.value.trim(),
      chatId: parseNullableNumber(els.driverChatIdInput.value),
      role: els.driverRoleInput.value,
      isActive: els.driverActiveInput.value === "true",
      vehiclePlate: els.driverPlateInput.value.trim(),
      vehicleModel: els.driverModelInput.value.trim(),
    };

    const id = els.driverIdInput.value;
    if (id) {
      await requestJson(`/api/admin/drivers/${id}`, { method: "PUT", body: JSON.stringify(payload) });
    } else {
      await requestJson("/api/admin/drivers", { method: "POST", body: JSON.stringify(payload) });
    }

    resetForm();
    toast(tx("Водители", "Drivers"), tx("Изменения сохранены", "Changes saved"), "success");
    await loadDrivers();
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
