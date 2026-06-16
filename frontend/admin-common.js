export const adminState = {
  apiBase: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  accessPassword: localStorage.getItem("fleetscan.admin.accessToken") || "",
  sessionToken: localStorage.getItem("fleetscan.admin.sessionToken") || "",
  fromDate: localStorage.getItem("fleetscan.admin.fromDate") || "",
  toDate: localStorage.getItem("fleetscan.admin.toDate") || "",
  whoami: null,
  isReadOnly: false,
};

const REFRESH_EVENT = "fleetscan:refresh";

export function debounce(callback, waitMs = 300) {
  let timerId = null;
  return (...args) => {
    if (timerId) {
      clearTimeout(timerId);
    }
    timerId = setTimeout(() => callback(...args), waitMs);
  };
}

export function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

export function formatDate(value) {
  if (!value) {
    return "—";
  }
  return new Date(value).toLocaleString(currentLocale() === "en" ? "en-US" : "ru-RU");
}

export function statusClass(status) {
  if (status === "BLOCKED") return "status-off";
  if (status === "UNDER_REVIEW") return "status-review";
  return "status-on";
}

export function parseNullableNumber(value) {
  const trimmed = String(value || "").trim();
  if (!trimmed) {
    return null;
  }
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? parsed : null;
}

export function toast(title, body, tone = "info") {
  const stack = document.getElementById("toastStack");
  if (!stack) {
    return;
  }
  const item = document.createElement("div");
  item.className = `toast ${tone === "danger" ? "toast--danger" : tone === "warn" ? "toast--warn" : tone === "success" ? "toast--success" : ""}`;
  item.innerHTML = `<p class="toast-title">${escapeHtml(title)}</p><p class="toast-body">${escapeHtml(body || "")}</p>`;
  stack.appendChild(item);
  setTimeout(() => item.remove(), 4500);
}

function authHeaders(extra = {}) {
  if (!adminState.sessionToken) {
    throw new Error("Нет активной сессии администратора");
  }
  return {
    Authorization: `Bearer ${adminState.sessionToken}`,
    ...extra,
  };
}

export async function apiFetch(url, options = {}) {
  const method = String(options.method || "GET").toUpperCase();
  let finalUrl = `${adminState.apiBase}${url}`;
  if (method === "GET") {
    const separator = finalUrl.includes("?") ? "&" : "?";
    finalUrl = `${finalUrl}${separator}_ts=${Date.now()}`;
  }

  const headers = {
    ...authHeaders(),
    ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
    ...(options.headers || {}),
  };

  const response = await fetch(finalUrl, {
    ...options,
    cache: "no-store",
    headers,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed: ${response.status}`);
  }

  if (method !== "GET") {
    try {
      localStorage.setItem(REFRESH_EVENT, String(Date.now()));
    } catch {
      
    }
    window.dispatchEvent(new CustomEvent(REFRESH_EVENT));
  }

  return response;
}

export async function requestJson(url, options = {}) {
  const response = await apiFetch(url, options);
  if (response.status === 204) {
    return null;
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export async function requestBlob(url, options = {}) {
  const response = await apiFetch(url, options);
  return response.blob();
}

export async function login(password) {
  const token = (password ?? "").trim();
  if (!token) {
    throw new Error("Введите пароль администратора");
  }

  const response = await fetch(`${adminState.apiBase}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ accessToken: token }),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "Не удалось выполнить вход");
  }

  const data = await response.json();
  adminState.accessPassword = token;
  adminState.sessionToken = data.sessionToken;
  localStorage.setItem("fleetscan.admin.accessToken", adminState.accessPassword);
  localStorage.setItem("fleetscan.admin.sessionToken", adminState.sessionToken);
}

export async function logout() {
  try {
    if (adminState.sessionToken) {
      await fetch(`${adminState.apiBase}/api/auth/logout`, {
        method: "DELETE",
        headers: authHeaders({}),
      });
    }
  } finally {
    adminState.sessionToken = "";
    localStorage.removeItem("fleetscan.admin.sessionToken");
  }
}

export async function ensureSession() {
  if (!adminState.sessionToken) {
    return false;
  }
  try {
    const response = await fetch(`${adminState.apiBase}/api/auth/me`, {
      method: "GET",
      headers: authHeaders({}),
    });
    if (!response.ok) {
      adminState.sessionToken = "";
      localStorage.removeItem("fleetscan.admin.sessionToken");
      return false;
    }
    const data = await response.json();
    if (!data?.authenticated) {
      adminState.sessionToken = "";
      localStorage.removeItem("fleetscan.admin.sessionToken");
      return false;
    }
    return true;
  } catch {
    adminState.sessionToken = "";
    localStorage.removeItem("fleetscan.admin.sessionToken");
    return false;
  }
}

export async function restoreSession() {
  const hasSession = await ensureSession();
  if (hasSession) {
    return true;
  }
  if (!adminState.accessPassword) {
    return false;
  }
  try {
    await login(adminState.accessPassword);
    return await ensureSession();
  } catch {
    return false;
  }
}

export function applyDateDefaults(fromInput, toInput) {
  if (adminState.fromDate) {
    fromInput.value = adminState.fromDate;
  }
  if (adminState.toDate) {
    toInput.value = adminState.toDate;
  }

  const today = new Date();
  if (!toInput.value) {
    toInput.valueAsDate = today;
  }
  if (!fromInput.value) {
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    fromInput.valueAsDate = weekAgo;
  }

  adminState.fromDate = fromInput.value;
  adminState.toDate = toInput.value;
  localStorage.setItem("fleetscan.admin.fromDate", adminState.fromDate);
  localStorage.setItem("fleetscan.admin.toDate", adminState.toDate);
}

export function saveDateRange(fromInput, toInput) {
  adminState.fromDate = fromInput.value;
  adminState.toDate = toInput.value;
  localStorage.setItem("fleetscan.admin.fromDate", adminState.fromDate);
  localStorage.setItem("fleetscan.admin.toDate", adminState.toDate);
}

export async function loadWhoAmI() {
  adminState.whoami = await requestJson("/api/auth/whoami");
  adminState.isReadOnly = adminState.whoami?.role === "OBSERVER";
  renderWhoAmI();
}

const I18N = {
  ru: {
    navOverview: "Обзор",
    navIncidents: "Инциденты",
    navBlacklist: "Черный список",
    navDrivers: "Водители",
    navReports: "Отчеты",
    navAnalytics: "Диаграммы",
    navDriver: "Портал водителя",
    lang: "Язык",
    placeholderAdminPassword: "Введите пароль",
  },
  en: {
    navOverview: "Overview",
    navIncidents: "Incidents",
    navBlacklist: "Blacklist",
    navDrivers: "Drivers",
    navReports: "Reports",
    navAnalytics: "Charts",
    navDriver: "Driver Portal",
    lang: "Language",
    placeholderAdminPassword: "Enter password",
  },
};

export function currentLocale() {
  const saved = localStorage.getItem("fleetscan.locale");
  return saved === "en" ? "en" : "ru";
}

export function t(key) {
  const locale = currentLocale();
  return I18N[locale]?.[key] ?? I18N.ru[key] ?? key;
}

function applyCommonNavLocale() {
  document.querySelectorAll("[data-i18n]").forEach((node) => {
    const key = node.getAttribute("data-i18n");
    if (!key) {
      return;
    }
    node.textContent = t(key);
  });
  document.querySelectorAll("[data-i18n-placeholder]").forEach((node) => {
    const key = node.getAttribute("data-i18n-placeholder");
    if (!key) {
      return;
    }
    node.setAttribute("placeholder", t(key));
  });
  document.querySelectorAll("[data-i18n-title]").forEach((node) => {
    const key = node.getAttribute("data-i18n-title");
    if (!key) {
      return;
    }
    node.setAttribute("title", t(key));
  });
}

export function initLocaleSwitcher() {
  const meta = document.querySelector(".topbar-meta");
  if (!meta || document.getElementById("localeSelect")) {
    applyCommonNavLocale();
    return;
  }

  const wrapper = document.createElement("label");
  wrapper.className = "meta-item";
  wrapper.innerHTML = `
    <span data-i18n="lang">${t("lang")}:</span>
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
    applyCommonNavLocale();
    window.location.reload();
  });

  applyCommonNavLocale();
}

export function currentTheme() {
  return localStorage.getItem("fleetscan.theme") === "light" ? "light" : "dark";
}

export function applyTheme(theme = currentTheme()) {
  document.documentElement.setAttribute("data-theme", theme);
}

function initThemeSwitcher() {
  const meta = document.querySelector(".topbar-meta");
  if (!meta || document.getElementById("themeSelect")) {
    applyTheme();
    return;
  }

  const wrapper = document.createElement("label");
  wrapper.className = "meta-item";
  wrapper.innerHTML = `
    <span>${currentLocale() === "en" ? "Theme:" : "Тема:"}</span>
    <select id="themeSelect" style="margin-left:6px; min-width:92px;">
      <option value="dark">${currentLocale() === "en" ? "Dark" : "Темная"}</option>
      <option value="light">${currentLocale() === "en" ? "Light" : "Светлая"}</option>
    </select>
  `;
  meta.appendChild(wrapper);
  const select = document.getElementById("themeSelect");
  select.value = currentTheme();
  select.addEventListener("change", () => {
    localStorage.setItem("fleetscan.theme", select.value);
    applyTheme(select.value);
  });
  applyTheme();
}

export function initPageChrome() {
  initLocaleSwitcher();
  initThemeSwitcher();
  const current = window.location.pathname.split("/").pop() || "overview.html";
  document.querySelectorAll(".anchor-nav a[href]").forEach((link) => {
    try {
      const href = link.getAttribute("href") || "";
      const target = href.split("/").pop();
      link.classList.toggle("is-active", target === current);
    } catch {
      
    }
  });
}

export function startAutoRefresh(refreshFn, intervalMs = 15000) {
  if (typeof refreshFn !== "function") {
    return () => {};
  }
  const timerId = setInterval(() => {
    refreshFn().catch(() => {
      
    });
  }, intervalMs);
  const onStorage = (event) => {
    if (event.key === REFRESH_EVENT) {
      refreshFn().catch(() => {});
    }
  };
  const onRefresh = () => {
    refreshFn().catch(() => {});
  };
  window.addEventListener("storage", onStorage);
  window.addEventListener(REFRESH_EVENT, onRefresh);
  return () => {
    clearInterval(timerId);
    window.removeEventListener("storage", onStorage);
    window.removeEventListener(REFRESH_EVENT, onRefresh);
  };
}

export function renderWhoAmI() {
  const roleEl = document.getElementById("whoamiRole");
  const fleetEl = document.getElementById("whoamiFleet");
  const sessionEl = document.getElementById("whoamiSession");
  const auditBanner = document.getElementById("auditModeBanner");

  if (!roleEl || !fleetEl || !sessionEl) {
    return;
  }

  if (!adminState.sessionToken || !adminState.whoami) {
    roleEl.textContent = "—";
    fleetEl.textContent = "—";
    sessionEl.textContent = "—";
    if (auditBanner) {
      auditBanner.hidden = true;
    }
    return;
  }

  roleEl.textContent = adminState.whoami.role || "НЕИЗВЕСТНО";
  fleetEl.textContent = adminState.whoami.fleetName || "—";
  sessionEl.textContent = adminState.whoami.expiresAtEpochMillis
    ? new Date(adminState.whoami.expiresAtEpochMillis).toLocaleString(currentLocale() === "en" ? "en-US" : "ru-RU")
    : "—";

  if (auditBanner) {
    auditBanner.hidden = !adminState.isReadOnly;
  }

  document.querySelectorAll("[data-mutating='1']").forEach((item) => {
    item.disabled = adminState.isReadOnly;
  });
}

export async function exportFile(path, fileName, params = new URLSearchParams()) {
  const response = await apiFetch(`${path}?${params.toString()}`, { method: "GET" });
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}
