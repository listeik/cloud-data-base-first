const elements = {
  authView: document.querySelector("#auth-view"),
  appView: document.querySelector("#app-view"),
  authForm: document.querySelector("#auth-form"),
  authSubmit: document.querySelector("#auth-submit"),
  authError: document.querySelector("#auth-error"),
  signInTab: document.querySelector("#sign-in-tab"),
  signUpTab: document.querySelector("#sign-up-tab"),
  username: document.querySelector("#username"),
  password: document.querySelector("#password"),
  currentUser: document.querySelector("#current-user"),
  signOut: document.querySelector("#sign-out"),
  allFiles: document.querySelector("#all-files"),
  breadcrumbs: document.querySelector("#breadcrumbs"),
  viewTitle: document.querySelector("#view-title"),
  searchForm: document.querySelector("#search-form"),
  searchInput: document.querySelector("#search-input"),
  searchBanner: document.querySelector("#search-banner"),
  searchQuery: document.querySelector("#search-query"),
  clearSearch: document.querySelector("#clear-search"),
  uploadButton: document.querySelector("#upload-button"),
  fileInput: document.querySelector("#file-input"),
  newFolderButton: document.querySelector("#new-folder-button"),
  refreshButton: document.querySelector("#refresh-button"),
  loading: document.querySelector("#loading-state"),
  empty: document.querySelector("#empty-state"),
  resourceList: document.querySelector("#resource-list"),
  resourceRows: document.querySelector("#resource-rows"),
  pagination: document.querySelector("#pagination"),
  previousPage: document.querySelector("#previous-page"),
  nextPage: document.querySelector("#next-page"),
  pageLabel: document.querySelector("#page-label"),
  storageProgress: document.querySelector("#storage-progress"),
  storagePercent: document.querySelector("#storage-percent"),
  storageSummary: document.querySelector("#storage-summary"),
  folderDialog: document.querySelector("#folder-dialog"),
  folderForm: document.querySelector("#folder-form"),
  folderName: document.querySelector("#folder-name"),
  folderError: document.querySelector("#folder-error"),
  moveDialog: document.querySelector("#move-dialog"),
  moveForm: document.querySelector("#move-form"),
  movePath: document.querySelector("#move-path"),
  moveError: document.querySelector("#move-error"),
  toast: document.querySelector("#toast")
};

const state = {
  authMode: "sign-in",
  path: "",
  page: 0,
  size: 20,
  totalPages: 0,
  search: "",
  movingResource: null,
  busy: false
};

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function api(path, options = {}) {
  const { headers = {}, ...requestOptions } = options;
  const response = await fetch(path, {
    credentials: "same-origin",
    ...requestOptions,
    headers: { Accept: "application/json", ...headers }
  });

  if (!response.ok) {
    let message = `Ошибка ${response.status}`;
    try {
      const error = await response.json();
      message = error.message || message;
    } catch {
      // A binary or empty error response still has a useful HTTP status.
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) return null;
  return response.json();
}

function setAuthMode(mode) {
  state.authMode = mode;
  const signingIn = mode === "sign-in";
  elements.signInTab.classList.toggle("active", signingIn);
  elements.signUpTab.classList.toggle("active", !signingIn);
  elements.signInTab.setAttribute("aria-selected", String(signingIn));
  elements.signUpTab.setAttribute("aria-selected", String(!signingIn));
  elements.authSubmit.textContent = signingIn ? "Войти" : "Создать аккаунт";
  elements.password.autocomplete = signingIn ? "current-password" : "new-password";
  elements.authError.textContent = "";
}

function showAuth() {
  elements.appView.hidden = true;
  elements.authView.hidden = false;
  elements.password.value = "";
  setTimeout(() => elements.username.focus(), 0);
}

async function showApp(user) {
  elements.currentUser.textContent = user.username;
  elements.authView.hidden = true;
  elements.appView.hidden = false;
  await Promise.all([loadResources(), loadUsage()]);
}

async function initialize() {
  try {
    const user = await api("/api/user/me");
    await showApp(user);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      showAuth();
      return;
    }
    showAuth();
    notify(error.message || "Не удалось подключиться к серверу", true);
  }
}

async function handleAuth(event) {
  event.preventDefault();
  elements.authError.textContent = "";
  elements.authSubmit.disabled = true;
  try {
    const user = await api(`/api/auth/${state.authMode}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: elements.username.value.trim(), password: elements.password.value })
    });
    state.path = "";
    state.page = 0;
    state.search = "";
    await showApp(user);
  } catch (error) {
    elements.authError.textContent = error.message;
  } finally {
    elements.authSubmit.disabled = false;
  }
}

async function handleSignOut() {
  try {
    await api("/api/auth/sign-out", { method: "POST" });
  } catch (error) {
    if (!(error instanceof ApiError && error.status === 401)) notify(error.message, true);
  }
  showAuth();
}

async function loadResources() {
  if (state.busy) return;
  state.busy = true;
  setResourceState("loading");
  renderNavigation();
  try {
    const endpoint = state.search
      ? `/api/resource/search/page?query=${encodeURIComponent(state.search)}&page=${state.page}&size=${state.size}`
      : `/api/directory/page?path=${encodeURIComponent(state.path)}&page=${state.page}&size=${state.size}`;
    const result = await api(endpoint);
    state.totalPages = result.totalPages;
    renderResources(result.content);
    renderPagination(result);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      showAuth();
    } else {
      setResourceState("empty");
      notify(error.message, true);
    }
  } finally {
    state.busy = false;
  }
}

function setResourceState(view) {
  elements.loading.hidden = view !== "loading";
  elements.empty.hidden = view !== "empty";
  elements.resourceList.hidden = view !== "list";
  if (view !== "list") elements.pagination.hidden = true;
}

function renderResources(resources) {
  elements.resourceRows.replaceChildren();
  if (resources.length === 0) {
    setResourceState("empty");
    return;
  }

  const fragment = document.createDocumentFragment();
  resources.forEach(resource => fragment.append(createResourceRow(resource)));
  elements.resourceRows.append(fragment);
  setResourceState("list");
}

function createResourceRow(resource) {
  const fullPath = `${resource.path}${resource.name}${resource.type === "DIRECTORY" ? "/" : ""}`;
  const row = document.createElement("div");
  row.className = "resource-row";

  const nameCell = document.createElement("div");
  nameCell.className = "resource-name";
  const icon = document.createElement("span");
  icon.className = `resource-icon ${resource.type === "DIRECTORY" ? "directory" : ""}`;
  icon.textContent = resource.type === "DIRECTORY" ? "D" : fileExtension(resource.name);
  icon.setAttribute("aria-hidden", "true");

  const name = document.createElement("button");
  name.type = "button";
  name.className = `resource-name-button ${resource.type === "DIRECTORY" ? "directory" : ""}`;
  name.textContent = resource.name;
  name.title = fullPath;
  if (resource.type === "DIRECTORY") {
    name.addEventListener("click", () => openDirectory(fullPath));
  } else {
    name.addEventListener("click", () => downloadResource(fullPath));
  }
  nameCell.append(icon, name);

  const size = document.createElement("span");
  size.className = "resource-size";
  size.textContent = resource.type === "DIRECTORY" ? "—" : formatBytes(resource.size);

  const type = document.createElement("span");
  type.className = "resource-type";
  type.textContent = resource.type === "DIRECTORY" ? "Папка" : "Файл";

  const actions = document.createElement("div");
  actions.className = "resource-actions";
  actions.append(
    actionButton("Скачать", () => downloadResource(fullPath)),
    actionButton("Изменить", () => openMoveDialog(resource, fullPath)),
    actionButton("Удалить", () => deleteResource(resource, fullPath), "danger")
  );

  row.append(nameCell, size, type, actions);
  return row;
}

function actionButton(label, handler, kind = "ghost") {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `button ${kind}`;
  button.textContent = label;
  button.addEventListener("click", handler);
  return button;
}

function renderNavigation() {
  elements.breadcrumbs.replaceChildren();
  const root = document.createElement("button");
  root.type = "button";
  root.className = "breadcrumb-button";
  root.textContent = "Мои файлы";
  root.addEventListener("click", () => navigateTo(""));
  elements.breadcrumbs.append(root);

  let currentPath = "";
  state.path.split("/").filter(Boolean).forEach(segment => {
    currentPath += `${segment}/`;
    elements.breadcrumbs.append(document.createTextNode("/"));
    const button = document.createElement("button");
    button.type = "button";
    button.className = "breadcrumb-button";
    button.textContent = segment;
    const target = currentPath;
    button.addEventListener("click", () => navigateTo(target));
    elements.breadcrumbs.append(button);
  });

  elements.viewTitle.textContent = state.path
    ? state.path.split("/").filter(Boolean).at(-1)
    : "Все файлы";
  elements.searchBanner.hidden = !state.search;
  elements.searchQuery.textContent = state.search;
}

function renderPagination(result) {
  elements.pagination.hidden = result.totalPages <= 1;
  elements.previousPage.disabled = result.page <= 0;
  elements.nextPage.disabled = result.page + 1 >= result.totalPages;
  elements.pageLabel.textContent = result.totalPages === 0
    ? "Нет страниц"
    : `${result.page + 1} из ${result.totalPages}`;
}

function navigateTo(path) {
  state.path = path;
  state.search = "";
  state.page = 0;
  elements.searchInput.value = "";
  loadResources();
}

function openDirectory(path) {
  navigateTo(path);
}

async function loadUsage() {
  try {
    const usage = await api("/api/storage/usage");
    const percent = usage.quotaBytes === 0 ? 0 : Math.min(100, Math.round(usage.usedBytes / usage.quotaBytes * 100));
    elements.storageProgress.style.width = `${percent}%`;
    elements.storageProgress.parentElement.setAttribute("aria-valuenow", String(percent));
    elements.storagePercent.textContent = `${percent}%`;
    elements.storageSummary.textContent = `${formatBytes(usage.usedBytes)} из ${formatBytes(usage.quotaBytes)}`;
    elements.fileInput.title = `Максимум ${formatBytes(usage.maxFileSizeBytes)} на файл`;
  } catch (error) {
    elements.storageSummary.textContent = "Не удалось получить статистику";
  }
}

async function uploadFiles() {
  const files = Array.from(elements.fileInput.files || []);
  if (files.length === 0) return;
  const body = new FormData();
  files.forEach(file => body.append("files", file));
  elements.uploadButton.disabled = true;
  try {
    await api(`/api/resource?path=${encodeURIComponent(state.path)}`, { method: "POST", body });
    notify(files.length === 1 ? "Файл загружен" : `Загружено файлов: ${files.length}`);
    state.page = 0;
    await Promise.all([loadResources(), loadUsage()]);
  } catch (error) {
    notify(error.message, true);
  } finally {
    elements.uploadButton.disabled = false;
    elements.fileInput.value = "";
  }
}

function openFolderDialog() {
  elements.folderForm.reset();
  elements.folderError.textContent = "";
  elements.folderDialog.showModal();
  elements.folderName.focus();
}

async function createFolder(event) {
  event.preventDefault();
  const name = elements.folderName.value.trim().replaceAll("\\", "/");
  if (!name || name.includes("/")) {
    elements.folderError.textContent = "Укажите название без символа /";
    return;
  }
  try {
    await api(`/api/directory?path=${encodeURIComponent(`${state.path}${name}/`)}`, { method: "POST" });
    elements.folderDialog.close();
    notify("Папка создана");
    state.page = 0;
    await loadResources();
  } catch (error) {
    elements.folderError.textContent = error.message;
  }
}

function openMoveDialog(resource, fullPath) {
  state.movingResource = { resource, fullPath };
  elements.moveError.textContent = "";
  elements.movePath.value = fullPath;
  elements.moveDialog.showModal();
  elements.movePath.focus();
  elements.movePath.select();
}

async function moveResource(event) {
  event.preventDefault();
  const target = elements.movePath.value.trim();
  if (!target) return;
  if (state.movingResource.resource.type === "DIRECTORY" && !target.endsWith("/")) {
    elements.moveError.textContent = "Путь папки должен заканчиваться символом /";
    return;
  }
  try {
    await api(`/api/resource/move?from=${encodeURIComponent(state.movingResource.fullPath)}&to=${encodeURIComponent(target)}`);
    elements.moveDialog.close();
    notify("Ресурс обновлён");
    await loadResources();
  } catch (error) {
    elements.moveError.textContent = error.message;
  }
}

async function deleteResource(resource, fullPath) {
  const confirmed = window.confirm(`Удалить ${resource.type === "DIRECTORY" ? "папку" : "файл"} «${resource.name}»?`);
  if (!confirmed) return;
  try {
    await api(`/api/resource?path=${encodeURIComponent(fullPath)}`, { method: "DELETE" });
    notify("Ресурс удалён");
    await Promise.all([loadResources(), loadUsage()]);
  } catch (error) {
    notify(error.message, true);
  }
}

async function downloadResource(path) {
  try {
    const response = await fetch(`/api/resource/download?path=${encodeURIComponent(path)}`, { credentials: "same-origin" });
    if (!response.ok) throw new ApiError(response.status, `Не удалось скачать ресурс (${response.status})`);
    const blob = await response.blob();
    const disposition = response.headers.get("Content-Disposition") || "";
    const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
    const simpleName = disposition.match(/filename="?([^";]+)"?/i)?.[1];
    const fallback = path.split("/").filter(Boolean).at(-1) || "download";
    const fileName = encodedName ? decodeURIComponent(encodedName) : (simpleName || fallback);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) {
    notify(error.message, true);
  }
}

function handleSearch(event) {
  event.preventDefault();
  const query = elements.searchInput.value.trim();
  if (!query) return;
  state.search = query;
  state.page = 0;
  loadResources();
}

function clearSearch() {
  state.search = "";
  state.page = 0;
  elements.searchInput.value = "";
  loadResources();
}

function changePage(offset) {
  const target = state.page + offset;
  if (target < 0 || target >= state.totalPages) return;
  state.page = target;
  loadResources();
}

function formatBytes(bytes) {
  if (bytes === 0) return "0 Б";
  if (bytes == null) return "—";
  const units = ["Б", "КБ", "МБ", "ГБ", "ТБ"];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** index;
  return `${new Intl.NumberFormat("ru-RU", { maximumFractionDigits: index === 0 ? 0 : 1 }).format(value)} ${units[index]}`;
}

function fileExtension(name) {
  const extension = name.includes(".") ? name.split(".").at(-1) : "F";
  return extension.slice(0, 3).toUpperCase();
}

let toastTimer;
function notify(message, error = false) {
  clearTimeout(toastTimer);
  elements.toast.textContent = message;
  elements.toast.classList.toggle("error", error);
  elements.toast.classList.add("visible");
  toastTimer = setTimeout(() => elements.toast.classList.remove("visible"), 3500);
}

elements.signInTab.addEventListener("click", () => setAuthMode("sign-in"));
elements.signUpTab.addEventListener("click", () => setAuthMode("sign-up"));
elements.authForm.addEventListener("submit", handleAuth);
elements.signOut.addEventListener("click", handleSignOut);
elements.allFiles.addEventListener("click", () => navigateTo(""));
elements.searchForm.addEventListener("submit", handleSearch);
elements.clearSearch.addEventListener("click", clearSearch);
elements.uploadButton.addEventListener("click", () => elements.fileInput.click());
elements.fileInput.addEventListener("change", uploadFiles);
elements.newFolderButton.addEventListener("click", openFolderDialog);
elements.refreshButton.addEventListener("click", () => Promise.all([loadResources(), loadUsage()]));
elements.folderForm.addEventListener("submit", createFolder);
elements.moveForm.addEventListener("submit", moveResource);
elements.previousPage.addEventListener("click", () => changePage(-1));
elements.nextPage.addEventListener("click", () => changePage(1));
document.querySelectorAll(".dialog-close").forEach(button => {
  button.addEventListener("click", () => button.closest("dialog").close());
});

initialize();
