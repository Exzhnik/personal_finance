# Личные финансы

Мой итоговый проект по ООП: консольное приложение на Java 21 для учёта доходов, расходов и бюджетов. Поддерживаются несколько пользователей, переводы между ними, сохранение данных и CSV-импорт/экспорт.

## Запуск

Нужны JDK 21 и Maven 3.9+. Зависимости Maven скачает автоматически.

```bash
mvn clean package
java -cp target/classes personalfinance.Main
```

Доступные команды выводятся столбцом с краткими описаниями. Команда `help` показывает полную справку и примеры. Для сохранения данных завершайте работу командой `exit`. При завершении ввода (EOF) данные также сохраняются, даже если ввод команды не закончен; незавершённая операция не добавляется.

## Команды

| Команда | Назначение |
|---|---|
| `register` | Регистрация |
| `login` | Вход и загрузка кошелька |
| `income` | Добавить доход |
| `expense` | Добавить расход |
| `budget` | Установить или изменить бюджет |
| `rename-category` | Переименовать категорию |
| `report` | Баланс, суммы и бюджеты по категориям |
| `summary` | Сумма по нескольким категориям |
| `transfer` | Перевод другому пользователю |
| `export` | Экспорт кошелька в CSV |
| `import` | Импорт кошелька из CSV |
| `logout` | Выйти из аккаунта |
| `help` | Справка и примеры |
| `exit` | Сохранить данные и выйти |

## Пример

Команды и ответы на вопросы приложения вводятся построчно:

```text
register
alice
secret
login
alice
secret
income
Зарплата
60000
expense
Еда
800
budget
Еда
4000
report
exit
```

Отчёт покажет доход `60000`, расход `800`, баланс `59200` и остаток бюджета `3200`.

## Данные

- Во время работы данные хранятся в памяти, при `exit` сохраняются в `data/<логин>.properties` и загружаются при следующем входе.
- CSV содержит операции и бюджеты. Импорт заменяет текущий кошелёк только после успешной проверки всего файла.
- При входе и импорте приложение проверяет бюджеты и баланс: предупреждает о перерасходе, исчерпании и использовании от 80% лимита, а также о нулевом балансе.
- Это учебная версия: пароли хранятся без шифрования.

## Архитектура

| Пакет | Ответственность |
|---|---|
| `model` | `User`, `Wallet`, `Operation`, расчёты и бюджеты |
| `core` | `UserService`: авторизация, переводы и сохранение |
| `infra` | Интерфейс `WalletStorage`, Properties и CSV-хранилища |
| `cli` | `ConsoleApp`: цикл команд, ввод, вывод и оповещения |

Для денег используется `BigDecimal`. Хранилище передаётся в сервис через интерфейс.

## Тесты и проверка

В проекте 43 unit- и интеграционных тестов, включая сквозные CLI-сценарии.

```bash
mvn test    # тесты
mvn verify  # тесты, сборка и Checkstyle
```

GitHub Actions запускает `mvn -B verify` для каждого push и pull request.

## Структура проекта

```text
src/main/java/personalfinance/
├── Main.java
├── cli/ConsoleApp.java
├── core/UserService.java
├── infra/
│   ├── CsvWalletStorage.java
│   ├── PropertiesWalletStorage.java
│   └── WalletStorage.java
└── model/
    ├── Operation.java
    ├── OperationType.java
    ├── User.java
    └── Wallet.java

src/test/java/personalfinance/
├── cli/ConsoleAppIntegrationTest.java
├── core/UserServiceTest.java
├── infra/CsvWalletStorageIntegrationTest.java
├── infra/PropertiesWalletStorageIntegrationTest.java
└── model/
    ├── OperationTest.java
    └── WalletTest.java
```
