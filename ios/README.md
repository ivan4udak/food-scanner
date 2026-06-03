# Food Scanner — iOS (SwiftUI)

Минималистичный нативный клиент к backend `food-scanner` (`/api/v1`).

## Запуск

1. Подними backend: `./scripts/start.sh` (Spring Boot на `:8080` + Postgres).
2. Открой `ios/FoodScanner.xcodeproj` в Xcode 16+.
3. Выбери симулятор iPhone → ⌘R.

> Симулятор видит хост-машину как `localhost`. Для запуска на реальном
> устройстве нажми на адрес сервера на экране входа и укажи IP машины
> в локальной сети (напр. `http://192.168.1.10:8080`).

Сборка из консоли:

```bash
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
xcodebuild -project ios/FoodScanner.xcodeproj -scheme FoodScanner \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

## Флоу

| Экран            | Endpoint                                 |
|------------------|------------------------------------------|
| Регистрация      | `POST /contributors`                     |
| Сканирование     | `POST /scan` → `NEW` (зелёный) / `EXISTS` (красный) |
| Сбор 6 фото      | `POST /drafts/{id}/photos`               |
| Завершение       | `POST /drafts/{id}/complete`             |
| Просмотр записи  | `GET  /entries/{barcode}`                |

Шесть обязательных типов фото (`PhotoSlot`) зеркалят
`PhotoType` / `CatalogCompletionPolicy.REQUIRED_TYPES` на бэкенде.
Фото отправляются как `storageKey` (Этап 1 — без реальной загрузки бинарей),
локальное превью показывается для наглядности.

## Структура

```
FoodScanner/
  App/            точка входа + роутинг (NavigationStack)
  DesignSystem/   палитра, метрики, переиспользуемые компоненты
  Networking/     APIClient (async/await) + Codable-модели DTO
  Session/        AppState (профиль, адрес сервера), PhotoSlot
  Features/
    Onboarding/   регистрация контрибьютора + настройка сервера
    Scan/         ввод штрихкода
    Draft/        сетка из 6 фото-слотов + прогресс-кольцо
    Result/       экраны «уже в каталоге» и «готово»
    Lookup/       просмотр существующей записи
```

Проект использует file-system-synchronized группу Xcode 16 —
новые `.swift` файлы в `FoodScanner/` подхватываются автоматически,
править `.pbxproj` не нужно.
