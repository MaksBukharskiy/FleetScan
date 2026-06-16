const state = {
  apiBase: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  accessToken: localStorage.getItem("fleetscan.driver.accessToken") || "",
  sessionToken: localStorage.getItem("fleetscan.driver.sessionToken") || "",
  chatId: localStorage.getItem("fleetscan.driver.chatId") || "",
  photoPreviewUrl: null,
};

function currentLocale() {
  const saved = localStorage.getItem("fleetscan.locale");
  return saved === "en" ? "en" : "ru";
}

function currentTheme() {
  return localStorage.getItem("fleetscan.theme") === "light" ? "light" : "dark";
}

function applyTheme(theme = currentTheme()) {
  document.documentElement.setAttribute("data-theme", theme);
}

function applyLocale() {
  const dictionary = {
    ru: {
      navOverview: "Обзор",
      navAnalytics: "Диаграммы",
      hintNoSession: "Введите пароль водителя и chat id, затем нажмите вход.",
      hintSessionClosed: "Сессия завершена.",
      hintSelectFile: "Выберите файл фото.",
      loginFailed: "Вход не выполнен",
      error: "Ошибка",
      open: "Открыть",
      photo: "Фото",
      uploadSuccess: "Фото загружено",
      noProfile: "Войдите, чтобы увидеть профиль.",
      noPhotos: "Фото появятся после входа.",
      noDetections: "Проверки появятся после входа.",
      loggedAs: "Вы вошли как",
      missingSession: "Сначала выполните вход по токену водителя.",
    },
    en: {
      navOverview: "Overview",
      navAnalytics: "Charts",
      hintNoSession: "Enter driver token and chat id, then click login.",
      hintSessionClosed: "Session closed.",
      hintSelectFile: "Select a photo file.",
      loginFailed: "Login failed",
      error: "Error",
      open: "Open",
      photo: "Photo",
      uploadSuccess: "Photo uploaded",
      noProfile: "Log in to view profile.",
      noPhotos: "Photos will appear after login.",
      noDetections: "Detections will appear after login.",
      loggedAs: "Logged in as",
      missingSession: "Please log in with driver token first.",
    },
  };
  document.querySelectorAll("[data-i18n]").forEach((node) => {
    const key = node.getAttribute("data-i18n");
    if (key && dictionary[currentLocale()]?.[key]) {
      node.textContent = dictionary[currentLocale()][key];
    }
  });

  const meta = document.querySelector(".topbar-meta");
  if (meta && !document.getElementById("localeSelect")) {
    const wrapper = document.createElement("label");
    wrapper.className = "meta-item";
    wrapper.innerHTML = `
      <span>${currentLocale() === "en" ? "Language" : "Язык"}:</span>
      <select id="localeSelect" style="margin-left:6px; min-width:74px;">
        <option value="ru">RU</option>
        <option value="en">EN</option>
      </select>
    `;
    meta.appendChild(wrapper);
    const select = document.getElementById("localeSelect");
    select.value = currentLocale();
    select.addEventListener("change", () => {
      localStorage.setItem("fleetscan.locale", select.value);
      window.location.reload();
    });

    const themeWrapper = document.createElement("label");
    themeWrapper.className = "meta-item";
    themeWrapper.innerHTML = `
      <span>${currentLocale() === "en" ? "Theme:" : "Тема:"}</span>
      <select id="themeSelect" style="margin-left:6px; min-width:92px;">
        <option value="dark">${currentLocale() === "en" ? "Dark" : "Темная"}</option>
        <option value="light">${currentLocale() === "en" ? "Light" : "Светлая"}</option>
      </select>
    `;
    meta.appendChild(themeWrapper);
    const themeSelect = document.getElementById("themeSelect");
    themeSelect.value = currentTheme();
    themeSelect.addEventListener("change", () => {
      localStorage.setItem("fleetscan.theme", themeSelect.value);
      applyTheme(themeSelect.value);
    });
  }
  applyTheme();
}
function tx(key) {
  const dictionary = {
    ru: {
      hintNoSession: "Введите пароль водителя и chat id, затем нажмите вход.",
      hintSessionClosed: "Сессия завершена.",
      hintSelectFile: "Выберите файл фото.",
      loginFailed: "Вход не выполнен",
      error: "Ошибка",
      open: "Открыть",
      photo: "Фото",
      uploadSuccess: "Фото загружено",
      noProfile: "Войдите, чтобы увидеть профиль.",
      noPhotos: "Фото появятся после входа.",
      noDetections: "Проверки появятся после входа.",
      loggedAs: "Вы вошли как",
      missingSession: "Сначала выполните вход по токену водителя.",
    },
    en: {
      hintNoSession: "Enter driver token and chat id, then click login.",
      hintSessionClosed: "Session closed.",
      hintSelectFile: "Select a photo file.",
      loginFailed: "Login failed",
      error: "Error",
      open: "Open",
      photo: "Photo",
      uploadSuccess: "Photo uploaded",
      noProfile: "Log in to view profile.",
      noPhotos: "Photos will appear after login.",
      noDetections: "Detections will appear after login.",
      loggedAs: "Logged in as",
      missingSession: "Please log in with driver token first.",
    },
  };
  return dictionary[currentLocale()][key] || key;
}

const els = {
  accessTokenInput: document.getElementById("accessTokenInput"),
  chatIdInput: document.getElementById("chatIdInput"),
  metrics: document.getElementById("metrics"),
  profileCard: document.getElementById("profileCard"),
  photosBody: document.getElementById("photosBody"),
  detectionsBody: document.getElementById("detectionsBody"),
  uploadForm: document.getElementById("uploadForm"),
  uploadFileInput: document.getElementById("uploadFileInput"),
  uploadTypeInput: document.getElementById("uploadTypeInput"),
  uploadNoteInput: document.getElementById("uploadNoteInput"),
  connectionHint: document.getElementById("connectionHint"),
  photoModal: document.getElementById("photoModal"),
  photoModalBackdrop: document.getElementById("photoModalBackdrop"),
  photoModalClose: document.getElementById("photoModalClose"),
  photoModalImage: document.getElementById("photoModalImage"),
  photoModalTitle: document.getElementById("photoModalTitle"),
  toastStack: document.getElementById("toastStack"),
};

els.accessTokenInput.value = state.accessToken;
els.chatIdInput.value = state.chatId;
applyLocale();

document.getElementById("saveBtn").addEventListener("click", async () => {
  state.accessToken = els.accessTokenInput.value.trim();
  state.chatId = els.chatIdInput.value.trim();
  localStorage.setItem("fleetscan.driver.accessToken", state.accessToken);
  localStorage.setItem("fleetscan.driver.chatId", state.chatId);
  await login();
  await refreshAll();
});

document.getElementById("refreshBtn").addEventListener("click", refreshAll);
document.getElementById("logoutBtn").addEventListener("click", logout);
els.uploadForm.addEventListener("submit", uploadPhoto);
els.photoModalBackdrop.addEventListener("click", closePhotoPreview);
els.photoModalClose.addEventListener("click", closePhotoPreview);

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

function requireSession() {
  if (!state.sessionToken) {
    els.connectionHint.textContent = tx("missingSession");
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
  if (!state.accessToken || !state.chatId) {
    state.sessionToken = "";
    localStorage.removeItem("fleetscan.driver.sessionToken");
    return;
  }

  const response = await fetch(`${state.apiBase}/api/driver/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ accessToken: state.accessToken, chatId: Number(state.chatId) }),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "Login failed");
  }

  const data = await response.json();
  state.sessionToken = data.sessionToken;
  localStorage.setItem("fleetscan.driver.sessionToken", state.sessionToken);
}

async function logout() {
  try {
    if (state.sessionToken) {
      await fetch(`${state.apiBase}/api/driver/auth/logout`, {
        method: "DELETE",
        headers: authHeaders({}),
      });
    }
  } catch {
    
  } finally {
    state.sessionToken = "";
    state.accessToken = "";
    localStorage.removeItem("fleetscan.driver.sessionToken");
    localStorage.removeItem("fleetscan.driver.accessToken");
    els.accessTokenInput.value = "";
    els.connectionHint.textContent = tx("hintSessionClosed");
    await refreshAll();
  }
}

function metricCard(label, value, foot, tone = "accent") {
  return `<article class="card metric"><div class="label">${label}</div><p class="value" style="color: var(--${tone})">${value}</p><p class="foot">${foot}</p></article>`;
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleString(currentLocale() === "en" ? "en-US" : "ru-RU");
}

function statusClass(status) {
  if (status === "BLOCKED") return "status-off";
  if (status === "UNDER_REVIEW") return "status-review";
  return "status-on";
}

function openPhotoPreview(url, title = "Фото") {
  if (!url) return;
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
      toast(tx("photo"), `Не удалось открыть: ${error.message}`, "danger");
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

function renderProfile(profile) {
  if (!profile) {
    els.profileCard.innerHTML = `<div class="empty">${tx("noProfile")}</div>`;
    els.metrics.innerHTML = "";
    return;
  }

  els.profileCard.innerHTML = `
    <div class="row"><div class="pill">Имя: <strong>${escapeHtml(profile.name)}</strong></div><div class="pill">Fleet: <strong>${escapeHtml(profile.fleetName || "—")}</strong></div></div>
    <div class="row"><div class="pill">Роль: <strong>${escapeHtml(profile.role)}</strong></div><div class="pill">Chat ID: <strong>${profile.chatId ?? "—"}</strong></div></div>
    <div class="row"><div class="pill">Номер: <strong>${escapeHtml(profile.vehiclePlate || "—")}</strong></div><div class="pill">Модель: <strong>${escapeHtml(profile.vehicleModel || "—")}</strong></div></div>
  `;

  els.metrics.innerHTML = [
    metricCard("Проверки", profile.detectionCount, "История AI-срабатываний"),
    metricCard("Фото", profile.photoCount, "Загруженные фото"),
    metricCard("Статус", profile.role, "Текущая роль"),
    metricCard("Машина", profile.vehiclePlate || "—", "Привязанный номер"),
  ].join("");
}

function renderPhotos(rows) {
  if (!rows.length) {
    els.photosBody.innerHTML = `<tr><td colspan="4" class="empty">${tx("noPhotos")}</td></tr>`;
    return;
  }

  els.photosBody.innerHTML = rows.map((row) => `
    <tr>
      <td><span class="badge soft">${escapeHtml(row.photoType || "OTHER")}</span><div class="hint">${escapeHtml(row.note || "")}</div></td>
      <td><span class="badge soft">${escapeHtml(row.status || "")}</span></td>
      <td>${formatDate(row.createdAt)}</td>
      <td class="action-cell"><button class="btn btn-secondary" data-photo-preview="${escapeHtml(row.photoUrl || "")}" ${row.photoUrl ? "" : "disabled"}>${tx("open")}</button></td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-photo-preview]").forEach((button) => {
    button.addEventListener("click", () => {
      const url = button.getAttribute("data-photo-preview");
      if (url) {
        openPhotoPreview(url, tx("photo"));
      }
    });
  });
}

function renderDetections(rows) {
  if (!rows.length) {
    els.detectionsBody.innerHTML = `<tr><td colspan="4" class="empty">${tx("noDetections")}</td></tr>`;
    return;
  }

  els.detectionsBody.innerHTML = rows.map((row) => `
    <tr>
      <td><strong>${escapeHtml(row.plateNumber)}</strong></td>
      <td><span class="${statusClass(row.status)}">${escapeHtml(row.status)}</span></td>
      <td>${formatDate(row.detectedAt)}</td>
      <td class="action-cell"><button class="btn btn-secondary" data-detection-preview="${escapeHtml(row.photoUrl || "")}" ${row.photoUrl ? "" : "disabled"}>${tx("photo")}</button></td>
    </tr>
  `).join("");

  document.querySelectorAll("[data-detection-preview]").forEach((button) => {
    button.addEventListener("click", () => {
      const url = button.getAttribute("data-detection-preview");
      if (url) {
        openPhotoPreview(url, tx("photo"));
      }
    });
  });
}

async function loadProfile() {
  if (!state.sessionToken) {
    renderProfile(null);
    return null;
  }
  const profile = await requestJson("/api/driver/profile");
  renderProfile(profile);
  return profile;
}

async function loadPhotos() {
  if (!state.sessionToken) {
    renderPhotos([]);
    return;
  }
  const rows = await requestJson("/api/driver/photos");
  renderPhotos(rows);
}

async function loadDetections() {
  if (!state.sessionToken) {
    renderDetections([]);
    return;
  }
  const rows = await requestJson("/api/driver/detections");
  renderDetections(rows);
}

async function uploadPhoto(event) {
  event.preventDefault();
  requireSession();

  const file = els.uploadFileInput.files?.[0];
  if (!file) {
    els.connectionHint.textContent = tx("hintSelectFile");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  formData.append("photoType", els.uploadTypeInput.value);
  formData.append("note", els.uploadNoteInput.value.trim());

  await apiFetch("/api/driver/photos", { method: "POST", body: formData });

  els.uploadForm.reset();
  els.uploadTypeInput.value = "OTHER";
  await refreshAll();
  toast("Upload", tx("uploadSuccess"), "success");
}

async function refreshAll() {
  if (!state.sessionToken) {
    els.connectionHint.textContent = tx("hintNoSession");
    await Promise.all([loadProfile(), loadPhotos(), loadDetections()]);
    return;
  }

  try {
    const [profile] = await Promise.all([loadProfile(), loadPhotos(), loadDetections()]);
    if (profile) {
      els.connectionHint.textContent = `Вы вошли как ${profile.name}.`;
      if (currentLocale() === "en") {
        els.connectionHint.textContent = `${tx("loggedAs")} ${profile.name}.`;
      }
    }
  } catch (error) {
    els.connectionHint.textContent = `${tx("error")}: ${error.message}`;
    toast(tx("error"), error.message, "danger");
  }
}

async function bootstrap() {
  try {
    if (state.sessionToken) {
      const me = await requestJson("/api/driver/auth/me");
      if (!me.authenticated) {
        state.sessionToken = "";
        localStorage.removeItem("fleetscan.driver.sessionToken");
      }
    } else if (state.accessToken && state.chatId) {
      await login();
    }
  } catch (error) {
    state.sessionToken = "";
    localStorage.removeItem("fleetscan.driver.sessionToken");
    els.connectionHint.textContent = `${tx("loginFailed")}: ${error.message}`;
  }

  await refreshAll();
  setInterval(() => {
    if (state.sessionToken) {
      refreshAll().catch(() => {});
    }
  }, 15000);
}

bootstrap();
