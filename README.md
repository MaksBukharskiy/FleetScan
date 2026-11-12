<p align="center"><img src="frontend/public/logo.svg" alt="Diesel" width="240" /></p>

# Diesel

**AI-powered vehicle inspection for taxi and fleet operators.**

Diesel helps fleet owners collect driver vehicle photos, recognize license plates with AI, check blacklists, and triage incidents — via Telegram and a web dashboard.

---

## English

### The problem

Taxi and fleet operators need regular vehicle checks: photos, plate verification, damage signals, and a shared blacklist. Doing this manually in chats does not scale. Missed plates, inconsistent review, and no audit trail create risk for the fleet.

### The product

Diesel is a multi-tenant inspection platform:

1. A fleet admin creates a fleet in Telegram and invites drivers / observers.
2. Drivers send vehicle photos (front, rear, interior, dashboard, etc.).
3. An AI service reads the plate and estimates basic condition.
4. The backend decides status: **ACTIVE**, **UNDER_REVIEW**, or **BLOCKED** (blacklist, plate mismatch, low confidence, damage).
5. Admins get Telegram alerts and manage incidents, blacklist, drivers, and analytics in the web UI.
6. Drivers can also use a lightweight web portal for profile and photo upload.

### Who it is for

| Role | How they use Diesel |
|------|------------------------|
| **Fleet admin** | Create fleet, invite users, manage blacklist, review incidents, export reports |
| **Driver (operator)** | Send inspection photos via Telegram (or web portal), register vehicle plate/model |
| **Observer** | Limited read access (status / analytics depending on permissions) |

### Core capabilities

- **Telegram bot** — fleet onboarding, invites, photo intake, blacklist commands, weekly photo reminders
- **AI plate recognition** — FastAPI service (EasyOCR + YOLO plate ROI); Uzbek (`01 A 123 BC`, `01 123 ABC`) and Russian formats, configurable via `PLATE_COUNTRIES`
- **Hybrid damage assessment** — a free local detector (RT-DETRv2, Apache-2.0) checks every photo offline; only unclear cases (low-score or severe findings, plus a 5% audit sample of clean photos) go to Claude Haiku (~$0.0005/photo) with the local findings as hints. See [Damage detection](#damage-detection)
- **Smart decisions** — blacklist hit → blocked; plate mismatch / low confidence / damage → under review
- **Admin web UI** — overview, incidents, blacklist, drivers, reports, analytics
- **Web login via Telegram** — `/web` in the bot issues a one-time 8-character code (5 min, single use); admins/observers get the admin panel, drivers get the portal
- **Notifications** — security alerts to fleet admins on blocked / under-review detections
- **Exports** — Excel reports for detections, incidents, and driver compliance

### Architecture

```
Telegram ──► Spring Boot API (Java 21) ──► MySQL 8
                 │              │
                 │              └──► Photo storage (volume)
                 ├──► AI service (FastAPI :8000)
                 └──► Admin / Driver UI (Vite → nginx :4173)
```

| Layer | Stack |
|-------|--------|
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, WebFlux (AI client) |
| Bot | Telegram Bots Spring Boot Starter |
| Database | MySQL 8 |
| AI | Python 3.11, FastAPI, EasyOCR, OpenCV, Ultralytics YOLO |
| Frontend | Vite 6, vanilla HTML/JS/CSS, nginx |
| Runtime | Docker Compose |

### Repository layout

```
Diesel/
├── docker-compose.yml          # Full stack: db, app, ai, frontend
├── Dockerfile                  # Spring Boot app
├── frontend/                   # Admin + driver static UI
├── src/main/java/com/fleetScan/
│   ├── taxiService/            # Backend (API, bot, domain, services)
│   └── FleetscanAiDetection/   # Python AI service
└── src/main/resources/
    └── application.yml
```

### Quick start

**Requirements:** Docker & Docker Compose.

```bash
git clone <repo-url> Diesel
cd Diesel
cp .env.example .env   # set TELEGRAM_BOT_TOKEN (and optionally ANTHROPIC_API_KEY)
docker compose up --build
```

| Service | URL |
|---------|-----|
| Admin UI | http://localhost:4173/ |
| Driver portal | http://localhost:4173/driver.html |
| Backend API | http://localhost:8080 |
| AI health | http://localhost:8000/ |
| MySQL | `localhost:3307` (db `fleetscan`) |

**Web login:**

| Purpose | Env / default |
|---------|----------------|
| Admin / driver web login | Send `/web` to the bot, enter the one-time code on the site |

Open the Telegram bot configured in your environment, send `/start`, create a fleet, invite a driver, and submit a vehicle photo.

### Configuration

Important environment variables (see `docker-compose.yml`):

| Variable | Description |
|----------|-------------|
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_BOT_NAME` | Bot credentials from BotFather |
| `FLEETSCAN_AI_BASE_URL` | AI service URL (default `http://ai:8000` in Compose) |
| `FLEETSCAN_AI_MIN_CONFIDENCE` | Min OCR confidence for auto-pass (default `0.80`) |
| `ANTHROPIC_API_KEY` | Enables the Claude step of damage assessment (optional) |
| `DAMAGE_MODEL` / `DAMAGE_EFFORT` | Claude model (default `claude-haiku-5-5`) and effort (default `low`) |
| `DAMAGE_DETECTOR_PATH` | Local detector dir (default `fleet_ai/models/damage_detector`) |
| `DAMAGE_LOCAL_SURE` | Local score trusted without Claude (default `0.75`) |
| `DAMAGE_AUDIT_RATE` | Share of clean photos re-checked by Claude (default `0.05`) |
| `DAMAGE_DEFECT_SEVERITIES` | Severities that send a car to review (default `moderate,severe`) |
| `PLATE_COUNTRIES` | Plate formats to accept (default `UZ,RU`) |
| `FLEETSCAN_UPLOAD_DIR` | Photo storage path |
| `FLEETSCAN_CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `FLEETSCAN_API_BASE_URL` | API URL baked into frontend build |

> **Security note:** Do not commit real bot tokens. Prefer env vars / secrets managers. Rotate any token that was ever committed to the repo.

### Detection decision flow

For each Telegram photo (main inspection path):

1. Store image → call AI `/analyze`
2. Active **blacklist** match → `BLOCKED`
3. Registered plate ≠ OCR plate → `UNDER_REVIEW`
4. Photo is not a vehicle / unusable → `UNDER_REVIEW` (`invalid_photo`)
5. Low confidence or damage of configured severity → `UNDER_REVIEW`
5. Otherwise → `ACTIVE` (`auto_passed`)
6. On `BLOCKED` / `UNDER_REVIEW` → notify fleet admins in Telegram

> Driver **web** photo upload currently stores the photo only; full AI inspection runs on the **Telegram** photo flow.

### Damage detection

Each photo goes through:

1. **Local detector** — free, offline, ~0.2 s on CPU. Classes: `scratch`, `dent`, `crack`, `lamp_broken`, `glass_shatter`, `tire_flat`.
2. **Decided locally** if nothing is found, or damage is found with score ≥ `DAMAGE_LOCAL_SURE` and is not severe.
3. **Sent to Claude Haiku** otherwise (and for `DAMAGE_AUDIT_RATE` of clean photos), with the local boxes as hints. Claude also names the damaged part and writes a Russian summary.
4. Fallbacks: no local model → Claude only; no API key → local only; neither → image heuristic (`damage_method: heuristic`, unreliable).

`damage_method` in the result is `local`, `hybrid`, `llm` or `heuristic`.

**Training the local model** (COCO-format dataset, e.g. CarDD or a Roboflow export, plus your own labelled photos):

```bash
cd src/main/java/com/fleetScan/FleetscanAiDetection
pip install -r tools/requirements-train.txt
python tools/train_damage_detector.py --data /path/to/dataset --epochs 40
```

The model is saved to `fleet_ai/models/damage_detector` (git-ignored). Check each dataset's license before commercial use. Note: the plate detector still uses Ultralytics YOLO (AGPL-3.0) — review licensing before a closed commercial launch.

### Main API groups

| Area | Base path |
|------|-----------|
| Admin auth | `/api/auth` |
| Analytics & exports | `/api/admin/analytics` |
| Incidents | `/api/admin/incidents` |
| Blacklist | `/api/admin/blacklist` |
| Drivers | `/api/admin/drivers` |
| Detection media | `/api/admin/detections` |
| Driver auth | `/api/driver/auth` |
| Driver portal | `/api/driver` |

### Frontend local development

```bash
cd frontend
npm install
npm run dev
```

Set `VITE_API_BASE_URL` if the API is not at `http://localhost:8080`.

### Current limitations (honest)

- Auth uses shared access tokens + in-memory sessions (not multi-instance / not OAuth)
- Prototype-stage versioning (`0.0.1-SNAPSHOT`)
- Secrets must be externalized before any public or production deploy

### License / status

Early-stage startup prototype. Product and APIs may change quickly.

---

## Русский

### Проблема

Таксопаркам и автопаркам нужны регулярные проверки машин: фото, сверка номеров, сигналы повреждений и общий чёрный список. Ручной разбор в чатах не масштабируется. Пропущенные номера, разрозненная модерация и отсутствие истории повышают риски для флота.

### Продукт

Diesel — мультиарендная платформа инспекции транспорта:

1. Админ парка создаёт флот в Telegram и приглашает водителей / наблюдателей.
2. Водители присылают фото машины (перед, зад, салон, приборка и т.д.).
3. AI-сервис распознаёт номер и оценивает базовое состояние.
4. Backend выставляет статус: **ACTIVE**, **UNDER_REVIEW** или **BLOCKED** (чёрный список, несовпадение номера, низкая уверенность, повреждения).
5. Админы получают алерты в Telegram и ведут инциденты, blacklist, водителей и аналитику в веб-кабинете.
6. У водителей есть лёгкий веб-портал для профиля и загрузки фото.

### Для кого

| Роль | Как пользуются |
|------|----------------|
| **Админ парка** | Создание флота, инвайты, blacklist, разбор инцидентов, отчёты |
| **Водитель (operator)** | Фото на проверку в Telegram (или веб), регистрация номера/модели |
| **Наблюдатель** | Ограниченный доступ на чтение (статус / аналитика по правам) |

### Возможности

- **Telegram-бот** — онбординг флота, инвайты, приём фото, команды blacklist, еженедельные напоминания
- **AI-распознавание номеров** — FastAPI (EasyOCR + YOLO); узбекские (`01 A 123 BC`, `01 123 ABC`) и российские номера, настройка `PLATE_COUNTRIES`
- **Гибридная оценка повреждений** — бесплатный локальный детектор (RT-DETRv2, Apache-2.0) проверяет каждое фото без интернета; спорные случаи (низкая уверенность, тяжёлые повреждения и 5% «чистых» фото для контроля) уходят в Claude Haiku (~$0.0005 за фото). Обучение: `tools/train_damage_detector.py`, подробности в английском разделе *Damage detection*
- **Авторешения** — blacklist → blocked; несовпадение / низкая уверенность / повреждения → under review
- **Админ UI** — обзор, инциденты, blacklist, водители, отчёты, аналитика
- **Вход на сайт через Telegram** — команда `/web` выдаёт одноразовый код (5 минут); админы и наблюдатели попадают в админку, водители — в кабинет
- **Уведомления** — алерты админам при blocked / under review
- **Экспорт** — Excel: детекции, инциденты, compliance водителей

### Архитектура

```
Telegram ──► Spring Boot API (Java 21) ──► MySQL 8
                 │              │
                 │              └──► Хранилище фото (volume)
                 ├──► AI-сервис (FastAPI :8000)
                 └──► Admin / Driver UI (Vite → nginx :4173)
```

| Слой | Стек |
|------|------|
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, WebFlux |
| Бот | Telegram Bots Spring Boot Starter |
| БД | MySQL 8 |
| AI | Python 3.11, FastAPI, EasyOCR, OpenCV, Ultralytics YOLO |
| Frontend | Vite 6, HTML/JS/CSS, nginx |
| Запуск | Docker Compose |

### Структура репозитория

```
Diesel/
├── docker-compose.yml          # Стек: db, app, ai, frontend
├── Dockerfile                  # Spring Boot
├── frontend/                   # Админка + кабинет водителя
├── src/main/java/com/fleetScan/
│   ├── taxiService/            # Backend
│   └── FleetscanAiDetection/   # Python AI
└── src/main/resources/
    └── application.yml
```

### Быстрый старт

**Нужно:** Docker и Docker Compose.

```bash
git clone <repo-url> Diesel
cd Diesel
cp .env.example .env   # set TELEGRAM_BOT_TOKEN (and optionally ANTHROPIC_API_KEY)
docker compose up --build
```

| Сервис | URL |
|--------|-----|
| Админка | http://localhost:4173/ |
| Кабинет водителя | http://localhost:4173/driver.html |
| Backend API | http://localhost:8080 |
| AI | http://localhost:8000/ |
| MySQL | `localhost:3307` (БД `fleetscan`) |

**Вход на сайт:**

| Назначение | Env / default |
|------------|---------------|
| Вход в админку / кабинет | Отправьте боту `/web`, введите одноразовый код на сайте |

Откройте Telegram-бота из вашей конфигурации: `/start` → создайте флот → пригласите водителя → отправьте фото машины.

### Конфигурация

| Переменная | Назначение |
|------------|------------|
| `TELEGRAM_BOT_TOKEN` / `TELEGRAM_BOT_NAME` | Данные бота из BotFather |
| `FLEETSCAN_AI_BASE_URL` | URL AI-сервиса |
| `FLEETSCAN_AI_MIN_CONFIDENCE` | Порог уверенности OCR (по умолчанию `0.80`) |
| `ANTHROPIC_API_KEY` | Включает оценку повреждений через Claude (опционально) |
| `PLATE_COUNTRIES` | Форматы номеров (по умолчанию `UZ,RU`) |
| `FLEETSCAN_UPLOAD_DIR` | Путь хранения фото |
| `FLEETSCAN_CORS_ALLOWED_ORIGINS` | Разрешённые origin фронтенда |
| `FLEETSCAN_API_BASE_URL` | URL API при сборке фронтенда |

> **Безопасность:** не коммитьте реальные токены бота. Выносите секреты в env / secrets manager. Ротируйте токены, если они уже попадали в репозиторий.

### Логика решений по детекции

Для фото из Telegram (основной путь инспекции):

1. Сохранить файл → вызвать AI `/analyze`
2. Активный **blacklist** → `BLOCKED`
3. Зарегистрированный номер ≠ OCR → `UNDER_REVIEW`
4. Низкая уверенность или признаки повреждений → `UNDER_REVIEW`
5. Иначе → `ACTIVE` (`auto_passed`)
6. При `BLOCKED` / `UNDER_REVIEW` → алерт админам в Telegram

> Загрузка фото через **веб-кабинет водителя** сейчас только сохраняет файл; полный AI-пайплайн работает на фото из **Telegram**.

### Основные группы API

| Область | Базовый путь |
|---------|--------------|
| Auth админа | `/api/auth` |
| Аналитика и экспорт | `/api/admin/analytics` |
| Инциденты | `/api/admin/incidents` |
| Blacklist | `/api/admin/blacklist` |
| Водители | `/api/admin/drivers` |
| Медиа детекций | `/api/admin/detections` |
| Auth водителя | `/api/driver/auth` |
| Портал водителя | `/api/driver` |

### Локальная разработка фронтенда

```bash
cd frontend
npm install
npm run dev
```

При необходимости задайте `VITE_API_BASE_URL`, если API не на `http://localhost:8080`.

### Текущие ограничения

- Auth на shared-токенах и in-memory сессиях (не для multi-instance / не OAuth)
- Стадия прототипа (`0.0.1-SNAPSHOT`)
- Перед публичным или продовым деплоем секреты нужно вынести из кода

### Статус

Ранний прототип стартапа. Продукт и API могут быстро меняться.
