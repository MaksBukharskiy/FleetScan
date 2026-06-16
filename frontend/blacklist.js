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
  initPageChrome,
  startAutoRefresh,
  currentLocale,
} from "./admin-common.js";

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  hint: document.getElementById("connectionHint"),
  form: document.getElementById("blacklistForm"),
  plateInput: document.getElementById("plateInput"),
  reasonInput: document.getElementById("reasonInput"),
  categoryInput: document.getElementById("categoryInput"),
  expiresAtInput: document.getElementById("expiresAtInput"),
  checkStatusBtn: document.getElementById("checkStatusBtn"),
  statusPill: document.getElementById("statusPill"),
  blacklistBody: document.getElementById("blacklistBody"),
};

els.accessTokenInput.value = adminState.accessPassword;
initPageChrome();

function tx(ru, en) {
  return currentLocale() === "en" ? en : ru;
}

async function loadBlacklist() {
  els.blacklistBody.innerHTML = `<tr><td colspan="6" class="loading-cell">${tx("Загрузка...", "Loading...")}</td></tr>`;
  const rows = await requestJson("/api/admin/blacklist");
  if (!rows.length) {
    els.blacklistBody.innerHTML = `<tr><td colspan="6" class="empty">${tx("Нет записей.", "No records.")}</td></tr>`;
    return;
  }

  els.blacklistBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td>${escapeHtml(row.reason || "")}</td>
      <td>${escapeHtml(row.category || "OTHER")}</td>
      <td>${formatDate(row.expiresAt)}</td>
      <td>${row.active ? tx("Да", "Yes") : tx("Нет", "No")}</td>
      <td><button class="btn btn-secondary" data-remove="${escapeHtml(row.plateNumber)}" data-mutating="1">${tx("Удалить", "Remove")}</button></td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-remove]").forEach((button) => {
    button.disabled = adminState.isReadOnly;
    button.addEventListener("click", async () => {
      if (adminState.isReadOnly) return;
      const plate = button.getAttribute("data-remove");
      try {
        await apiFetch(`/api/admin/blacklist/${encodeURIComponent(plate)}`, { method: "DELETE" });
        toast(tx("Чёрный список", "Blacklist"), tx(`${plate} удалён`, `${plate} removed`), "success");
        await loadBlacklist();
      } catch (error) {
        toast(tx("Чёрный список", "Blacklist"), error.message, "danger");
      }
    });
  });
}

async function checkStatus() {
  const plate = els.plateInput.value.trim();
  if (!plate) {
    return;
  }
  const data = await requestJson(`/api/admin/blacklist/status?plateNumber=${encodeURIComponent(plate)}`);
  els.statusPill.textContent = data.active
    ? tx(`Статус: ${data.plateNumber} в чёрном списке (${data.reason || "без причины"})`, `Status: ${data.plateNumber} is blacklisted (${data.reason || "no reason"})`)
    : tx(`Статус: ${data.plateNumber} не в чёрном списке`, `Status: ${data.plateNumber} is not blacklisted`);
}

async function refreshAll() {
  try {
    await Promise.all([loadWhoAmI(), loadBlacklist()]);
    els.hint.textContent = tx("Чёрный список обновлён.", "Blacklist updated.");
  } catch (error) {
    els.hint.textContent = `Ошибка: ${error.message}`;
    toast(tx("Чёрный список", "Blacklist"), error.message, "danger");
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

  els.checkStatusBtn.addEventListener("click", async () => {
    try {
      await checkStatus();
    } catch (error) {
      toast(tx("Проверка", "Check"), error.message, "danger");
    }
  });

  els.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (adminState.isReadOnly) {
      return;
    }
    const plateNumber = els.plateInput.value.trim();
    if (!plateNumber) {
      toast(tx("Чёрный список", "Blacklist"), tx("Укажите номер", "Enter plate number"), "warn");
      return;
    }
    try {
      await requestJson("/api/admin/blacklist", {
        method: "POST",
        body: JSON.stringify({
          plateNumber,
          reason: els.reasonInput.value.trim(),
          category: els.categoryInput.value,
          expiresAt: els.expiresAtInput.value ? new Date(els.expiresAtInput.value).toISOString().slice(0, 19) : null,
        }),
      });
      els.form.reset();
      toast(tx("Чёрный список", "Blacklist"), tx("Запись добавлена", "Record added"), "success");
      await loadBlacklist();
    } catch (error) {
      toast(tx("Чёрный список", "Blacklist"), error.message, "danger");
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
