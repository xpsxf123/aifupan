import { createServer } from "node:http";
import { readFile, writeFile, mkdir, rename } from "node:fs/promises";
import { dirname, join } from "node:path";

const PORT = Number(process.env.PORT || 3100);
const DB_PATH = process.env.DB_PATH || join(process.cwd(), "data", "db.json");
const CONFIG_PATH = process.env.MOCK_CONFIG_PATH || join(process.cwd(), "mock.config.json");

const ok = (data, msg = "请求成功") => ({ code: 0, msg, data: data ?? null, timestamp: Date.now() });
const fail = (code, msg) => ({ code, msg, data: null, timestamp: Date.now() });

const isNonEmptyString = (v) => typeof v === "string" && v.trim().length > 0;

const toInt = (v, fallback) => {
  const n = Number(v);
  if (!Number.isFinite(n)) return fallback;
  return Math.trunc(n);
};

const normalizePath = (p) => {
  const s = String(p || "").trim();
  if (!s) return "/";
  return s.startsWith("/") ? s : `/${s}`;
};

const readJsonBody = async (req) => {
  const contentType = String(req.headers["content-type"] || "");
  if (!contentType.toLowerCase().includes("application/json")) return null;
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const raw = Buffer.concat(chunks).toString("utf8").trim();
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return { __invalid_json: true };
  }
};

const sendJson = (res, status, payload) => {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "Content-Type, Authorization, Token",
    "access-control-allow-methods": "GET,POST,OPTIONS",
  });
  res.end(body);
};

const ensureDbShape = (db) => {
  if (!db || typeof db !== "object") return { _meta: {}, tables: {} };
  db._meta = db._meta && typeof db._meta === "object" ? db._meta : {};
  db.tables = db.tables && typeof db.tables === "object" ? db.tables : {};
  return db;
};

const readDb = async () => {
  try {
    const txt = await readFile(DB_PATH, "utf8");
    return ensureDbShape(JSON.parse(txt));
  } catch (e) {
    if (e && (e.code === "ENOENT" || e.code === "ENOTDIR")) return { _meta: {}, tables: {} };
    throw e;
  }
};

const writeDb = async (db) => {
  const dir = dirname(DB_PATH);
  await mkdir(dir, { recursive: true });
  const tmpPath = join(dir, `.db.${Date.now()}.${Math.random().toString(16).slice(2)}.tmp.json`);
  await writeFile(tmpPath, JSON.stringify(db, null, 2), "utf8");
  await rename(tmpPath, DB_PATH);
};

const ensureConfigShape = (cfg) => {
  const base = cfg && typeof cfg === "object" ? cfg : {};
  const auth = base.auth && typeof base.auth === "object" ? base.auth : {};
  const api = base.api && typeof base.api === "object" ? base.api : {};
  const timestamps = base.timestamps && typeof base.timestamps === "object" ? base.timestamps : {};
  const resources = Array.isArray(base.resources) ? base.resources : [];
  const routes = Array.isArray(base.routes) ? base.routes : [];

  return {
    auth: {
      mode: auth.mode === "token" ? "token" : "none",
      header: isNonEmptyString(auth.header) ? String(auth.header).trim() : "Token",
      anonymous: Array.isArray(auth.anonymous) ? auth.anonymous : [],
    },
    api: {
      successCode: typeof api.successCode === "number" ? api.successCode : 0,
      successMsg: isNonEmptyString(api.successMsg) ? String(api.successMsg) : "请求成功",
      timestamp: api.timestamp !== false,
    },
    timestamps: {
      enabled: timestamps.enabled !== false,
      createField: isNonEmptyString(timestamps.createField) ? String(timestamps.createField) : "createDate",
      updateField: isNonEmptyString(timestamps.updateField) ? String(timestamps.updateField) : "updateDate",
    },
    resources,
    routes,
  };
};

const readConfig = async () => {
  try {
    const txt = await readFile(CONFIG_PATH, "utf8");
    return ensureConfigShape(JSON.parse(txt));
  } catch (e) {
    if (e && (e.code === "ENOENT" || e.code === "ENOTDIR")) return ensureConfigShape({});
    throw e;
  }
};

const validateRequired = (obj, fields) => {
  if (!obj || typeof obj !== "object") return false;
  for (const f of fields) {
    const v = obj[f];
    if (v === undefined || v === null) return false;
    if (typeof v === "string" && v.trim() === "") return false;
  }
  return true;
};

const paginate = (items, page, limit) => {
  const p = Math.max(1, toInt(page, 1));
  const l = Math.max(1, toInt(limit, 10));
  const totalCount = items.length;
  const totalPage = Math.max(1, Math.ceil(totalCount / l));
  const start = (p - 1) * l;
  const list = items.slice(start, start + l);
  return { currPage: p, pageSize: l, totalCount, totalPage, list };
};

const nextId = (db, collectionKey) => {
  const k = `${collectionKey}_last_id`;
  db._meta[k] = toInt(db._meta[k], 0) + 1;
  return db._meta[k];
};

const findById = (arr, idField, id) => arr.find((x) => Number(x?.[idField]) === Number(id));
const removeById = (arr, idField, id) => {
  const idx = arr.findIndex((x) => Number(x?.[idField]) === Number(id));
  if (idx < 0) return false;
  arr.splice(idx, 1);
  return true;
};

const buildOptions = (items, keyword, idField, labelField) => {
  const kw = isNonEmptyString(keyword) ? keyword.trim() : "";
  const filtered = kw
    ? items.filter((x) => isNonEmptyString(x?.[labelField]) && String(x[labelField]).includes(kw))
    : items;
  return filtered.map((x) => ({ key: Number(x?.[idField]), label: String(x?.[labelField] ?? "") }));
};

const getHeaderValue = (req, headerName) => {
  const k = String(headerName || "").toLowerCase();
  if (!k) return undefined;
  return req.headers[k];
};

const isAnonymous = (anonymousList, method, path) =>
  anonymousList.some((x) => String(x?.method || "").toUpperCase() === method && normalizePath(x?.path) === path);

const resolveEndpoints = (r) => {
  const e = r && typeof r.endpoints === "object" ? r.endpoints : {};
  return {
    add: normalizePath(e.add || "/add"),
    update: normalizePath(e.update || "/update"),
    delete: normalizePath(e.delete || "/delete"),
    detail: normalizePath(e.detail || "/detail"),
    page: normalizePath(e.page || "/page"),
    list: normalizePath(e.list || "/list"),
    option: normalizePath(e.option || "/option"),
  };
};

const nowText = () => {
  const d = new Date();
  const p2 = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())} ${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`;
};

const handle = async (req, res) => {
  if (req.method === "OPTIONS") {
    res.writeHead(204, {
      "access-control-allow-origin": "*",
      "access-control-allow-headers": "Content-Type, Authorization, Token",
      "access-control-allow-methods": "GET,POST,OPTIONS",
    });
    res.end();
    return;
  }

  const cfg = await readConfig();
  const url = new URL(req.url || "/", "http://localhost");
  const path = url.pathname;
  const method = String(req.method || "GET").toUpperCase();
  const body = method === "POST" ? await readJsonBody(req) : null;

  if (method === "POST" && body && body.__invalid_json) {
    sendJson(res, 200, fail(2004, "数据格式错误"));
    return;
  }

  if (method === "GET" && path === "/health") {
    sendJson(res, 200, ok(null, cfg.api.successMsg));
    return;
  }

  if (cfg.auth.mode === "token") {
    const headerValue = getHeaderValue(req, cfg.auth.header);
    if (!isAnonymous(cfg.auth.anonymous, method, path) && !isNonEmptyString(String(headerValue || ""))) {
      sendJson(res, 200, fail(401, "未授权访问"));
      return;
    }
  }

  for (const rt of cfg.routes) {
    const m = String(rt?.method || "").toUpperCase();
    const p = normalizePath(rt?.path);
    if (m === method && p === path) {
      const type = String(rt?.type || "static");
      if (type === "static") {
        sendJson(res, 200, ok(rt?.data ?? null, rt?.msg ?? cfg.api.successMsg));
        return;
      }
      sendJson(res, 200, fail(405, "不支持"));
      return;
    }
  }

  const db = await readDb();

  for (const r of cfg.resources) {
    const base = normalizePath(r?.base);
    const collectionKey = isNonEmptyString(r?.collection) ? String(r.collection).trim() : null;
    if (!collectionKey || !base || base === "/") continue;

    const endpoints = resolveEndpoints(r);
    const idField = isNonEmptyString(r?.idField) ? String(r.idField).trim() : "id";
    const labelField = isNonEmptyString(r?.labelField) ? String(r.labelField).trim() : "name";
    const required = r?.required && typeof r.required === "object" ? r.required : {};
    const addRequired = Array.isArray(required.add) ? required.add : [];
    const updateRequired = Array.isArray(required.update) ? required.update : [];
    const filters = Array.isArray(r?.filters) ? r.filters : [];
    const search = r?.search && typeof r.search === "object" ? r.search : {};
    const searchField = isNonEmptyString(search.field) ? String(search.field) : null;

    db.tables[collectionKey] = Array.isArray(db.tables[collectionKey]) ? db.tables[collectionKey] : [];
    const list = db.tables[collectionKey];

    if (method === "POST" && path === `${base}${endpoints.add}`) {
      if (!validateRequired(body, addRequired)) {
        sendJson(res, 200, fail(2001, "参数校验未通过"));
        return;
      }
      const id = nextId(db, collectionKey);
      const rec = { ...(body || {}), [idField]: id };
      if (cfg.timestamps.enabled) {
        const t = nowText();
        rec[cfg.timestamps.createField] = t;
        rec[cfg.timestamps.updateField] = t;
      }
      list.push(rec);
      await writeDb(db);
      sendJson(res, 200, ok(null, cfg.api.successMsg));
      return;
    }

    if (method === "POST" && path === `${base}${endpoints.update}`) {
      if (!validateRequired(body, [idField, ...updateRequired])) {
        sendJson(res, 200, fail(2001, "参数校验未通过"));
        return;
      }
      const rec = findById(list, idField, body[idField]);
      if (!rec) {
        sendJson(res, 200, fail(404, "未找到"));
        return;
      }
      Object.assign(rec, body);
      if (cfg.timestamps.enabled) rec[cfg.timestamps.updateField] = nowText();
      await writeDb(db);
      sendJson(res, 200, ok(null, cfg.api.successMsg));
      return;
    }

    if (method === "POST" && path === `${base}${endpoints.delete}`) {
      if (!validateRequired(body, [idField])) {
        sendJson(res, 200, fail(2001, "参数校验未通过"));
        return;
      }
      const removed = removeById(list, idField, body[idField]);
      if (!removed) {
        sendJson(res, 200, fail(404, "未找到"));
        return;
      }
      await writeDb(db);
      sendJson(res, 200, ok(null, cfg.api.successMsg));
      return;
    }

    if (method === "GET" && path === `${base}${endpoints.detail}`) {
      const id = url.searchParams.get(idField) || url.searchParams.get("id");
      if (!isNonEmptyString(String(id || ""))) {
        sendJson(res, 200, fail(402, "必填项缺失"));
        return;
      }
      const rec = findById(list, idField, id);
      if (!rec) {
        sendJson(res, 200, fail(404, "未找到"));
        return;
      }
      sendJson(res, 200, ok(rec, cfg.api.successMsg));
      return;
    }

    if (method === "POST" && (path === `${base}${endpoints.page}` || path === `${base}${endpoints.list}`)) {
      const page = body?.page ?? 1;
      const limit = body?.limit ?? 10;
      let items = [...list];
      if (searchField && isNonEmptyString(body?.[searchField])) {
        const kw = String(body[searchField]).trim();
        items = items.filter((x) => isNonEmptyString(String(x?.[labelField] || "")) && String(x[labelField]).includes(kw));
      }
      for (const f of filters) {
        const v = body?.[f];
        if (v !== undefined && v !== null && v !== "" && v !== 0) items = items.filter((x) => Number(x?.[f]) === Number(v));
      }
      sendJson(res, 200, ok(paginate(items, page, limit), cfg.api.successMsg));
      return;
    }

    if (method === "GET" && path === `${base}${endpoints.option}`) {
      const keyword = url.searchParams.get("keyword") || url.searchParams.get("name") || "";
      sendJson(res, 200, ok(buildOptions(list, keyword, idField, labelField), cfg.api.successMsg));
      return;
    }
  }

  sendJson(res, 200, fail(404, "未找到"));
};

const server = createServer((req, res) => {
  handle(req, res).catch((e) => {
    sendJson(res, 200, fail(500, e?.message || "失败 / 系统出小差啦"));
  });
});

server.listen(PORT, () => {
  console.log(`mock-backend-json listening on http://localhost:${PORT}`);
  console.log(`DB_PATH=${DB_PATH}`);
  console.log(`MOCK_CONFIG_PATH=${CONFIG_PATH}`);
});
