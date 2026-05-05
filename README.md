# Travel Planner

Travel Planner — це клієнт-серверний додаток (Android + Node.js) для планування подорожей, управління маршрутами та спільного доступу до поїздок.

## Сутності (Entities)

Основні моделі даних, що використовуються в системі:
- **User (Користувач)**: `id`, `displayName`, `email`, `password`, `homeCity`, `preferredCurrency`, `registeredAt`.
- **Trip (Поїздка)**: `id`, `ownerId`, `title`, `destination`, `startDate`, `endDate`, `totalBudget`, `currencyCode`, `coverImageUrl`, `notes`.
- **RoutePoint (Точка маршруту)**: `id`, `tripId`, `name`, `address`, `latitude`, `longitude`, `arrivalDate`, `durationDays`, `estimatedCost`, `currencyCode`, `isVisited`, `category`, `notes`.
- **Invite (Запрошення)**: `token`, `tripId`, `createdAt`.
- **UserTrip (Зв'язок користувача з поїздкою)**: Таблиця зв'язку `userId` та `tripId` для спільного доступу.

## Ендпоїнти (REST API)

Бекенд (Node.js/Express) надає наступні ендпоїнти:

### Базові
- `GET /health` та `GET /` — перевірка статусу сервера.

### Користувачі
- `GET /users`, `GET /users/:id` — отримання користувачів.
- `POST /users` — реєстрація.
- `PUT /users/:id` — оновлення профілю.
- `DELETE /users/:id` — видалення.

### Поїздки
- `GET /trips`, `GET /trips/:id` — отримання поїздок.
- `POST /trips` — створення нової поїздки.
- `PUT /trips/:id` — оновлення поїздки.
- `DELETE /trips/:id` — видалення поїздки.
- `GET /users/:userId/trips` — поїздки конкретного користувача.
- `PUT /users/:userId/trips/:tripId` — приєднання до поїздки.
- `DELETE /users/:userId/trips/:tripId` — вихід з поїздки.

### Точки маршруту
- `GET /trips/:tripId/route-points`
- `GET /trips/:tripId/route-points/:pointId`
- `POST /trips/:tripId/route-points`
- `PUT /trips/:tripId/route-points/:pointId`
- `DELETE /trips/:tripId/route-points/:pointId`

### Запрошення
- `POST /trips/:tripId/invite` — створення токена для запрошення.
- `GET /invites/:token/preview` — перегляд інформації про поїздку по токену.
- `POST /invites/:token/accept` — прийняття запрошення.

## Вебсокет (WebSocket)

Вебсокет-сервер доступний за адресою `/ws` і відповідає за синхронізацію даних у реальному часі між усіма учасниками:
- Бродкастить події: `connected`, `created`, `updated`, `deleted`, `accepted` для сутностей (`trip`, `route_point`, `user`, `invite`).
- На клієнті існують екрани `RealtimeEventsScreen` та `RealtimeStatusScreen` для моніторингу статусу підключення та відображення живих оновлень.

## Сторінки (Екрани Android додатку)

Додаток використовує Jetpack Compose. Основні екрани:
- **Авторизація**: `LoginScreen`, `RegisterScreen`.
- **Профіль**: `ProfileScreen`, `EditProfileScreen`.
- **Поїздки**: `TripListScreen` (список поїздок), `TripDetailScreen` (деталі маршруту та бюджету), `AddTripScreen` (створення), `AddRoutePointScreen` (додавання точок).
- **Спільний доступ**: `InviteScreen` (прийняття запрошення по діплінку).
- **Публічні екрани**: `PublicScreen`, `PublicTripScreen`.
- **Безпека**: `SecuritySettingsScreen`, `CriticalActionScreen`.
- **Системні/Налагодження**: `DebugDeepLinkScreen`, `DeferredOnboardingScreen`.

## Вхід по біометрії

Додаток підтримує вхід за допомогою відбитка пальця:
- Реалізовано через `AndroidBiometricManager` (використовує AndroidX Biometric).
- Логіка шифрування та зберігання токенів працює через `SecurityPreferences` із використанням апаратного Keystore.
- Налаштовується в `SecuritySettingsScreen`.

## Діплінки (Deep Links)

Система навігації підтримує зовнішні посилання (https та кастомні схеми):
- Обробка посилань здійснюється через `DeepLinkRouter`. Підтримуються схеми `myapp://` та `https://myapp.com/`.

Доступні маршрути (діплінки):
- `myapp://home` (або `https://myapp.com/home`) — Головний екран (Home)
- `myapp://items/{id}` — Деталі поїздки/маршруту
- `myapp://catalog` (або з параметром `?filter=new` / `?filter=old`) — Каталог поїздок з можливістю фільтрації
- `myapp://invite/{token}` — Прийняти запрошення до поїздки
- `myapp://notifications` — Екран сповіщень
- `myapp://public` — Публічний екран
- `myapp://debug` — Екран для тестування (DebugDeepLinkScreen)

Додатково:
- Передбачено обробку "холодного" (cold start) та "гарячого" (warm start) стартів.
- Відкладений діплінкінг (Deferred Onboarding) для нових користувачів.
- Деякі маршрути вимагають авторизації, в такому випадку користувач перенаправляється на екран логіну зі збереженням початкового посилання (pending destination).

## Тести

Проєкт містить модульні тести для перевірки критичних компонентів (у каталозі `app/src/test/`):

- **Навігація та діплінки (`DeepLinkRouterTest.kt`)**: 
  - Перевіряє коректність парсингу різних схем (`myapp://` та `https://`).
  - Тестує логіку маршрутизації, включно з вилученням параметрів (наприклад `?filter=new`).
  - Перевіряє відкладену навігацію (pending destination): якщо користувач переходить за діплінком без авторизації, система має перенаправити його на екран логіну та зберегти оригінальне посилання.

- **Безпека (`BiometricManagerTest.kt`)**: 
  - Юніт-тести для перевірки всіх станів датчика біометрії (успішна авторизація, скасування користувачем, відсутність сенсора, системні помилки).
  - Тестування `SecurityViewModel` та перевірка коректності зміни UI-станів (`Authenticating`, `Success`, `Failed`).
  - Перевірка правильності збереження та зчитування налаштувань безпеки (наприклад, таймаути автоблокування).

- **Реалтайм синхронізація (`data/realtime/`)**:
  - `SocketEventParserTest.kt`: Перевірка парсингу JSON-подій, що надходять через WebSocket (trip, route_point, user, invite).
  - `SocketManagerTest.kt`: Перевірка життєвого циклу з'єднання, перепідключення та маршрутизації подій у додаток.
