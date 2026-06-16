# FleetScan Frontend

Отдельный статический сайт для FleetScan.

## Страницы

- `/` - админка
- `/driver.html` - кабинет водителя

## Запуск локально

```bash
cd frontend
npm install
npm run dev
```

После этого откройте:

- `http://localhost:4173/`
- `http://localhost:4173/driver.html`

## Настройка

Админка использует:

- `Web access token`

Кабинет водителя использует:

- `Driver access token`
- `Chat ID`

Оба экрана обращаются к backend API:

- `/api/auth/login`
- `/api/auth/me`
- `/api/auth/logout`
- `/api/admin/analytics/summary`
- `/api/admin/analytics/detections`
- `/api/admin/analytics/export`
- `/api/admin/blacklist`
- `/api/admin/drivers`
- `/api/driver/auth/login`
- `/api/driver/auth/me`
- `/api/driver/auth/logout`
- `/api/driver/profile`
- `/api/driver/photos`
- `/api/driver/detections`

### API URL

По умолчанию фронтенд использует `http://localhost:8080`.
Для другого окружения задайте `VITE_API_BASE_URL` при сборке.
