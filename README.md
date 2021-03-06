## Тестовое задание на стажировку Naumen (Scala)

![Build Status](https://travis-ci.org/imbarr/naumen-task.svg?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/7ca23f56b8954ddeb3c2dbe94e717a7c)](https://www.codacy.com/app/imbarr/naumen-task?utm_source=github.com&utm_medium=referral&utm_content=imbarr/naumen-task&utm_campaign=Badge_Coverage)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ca23f56b8954ddeb3c2dbe94e717a7c)](https://www.codacy.com/app/imbarr/naumen-task?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=imbarr/naumen-task&amp;utm_campaign=Badge_Grade)

[Ссылка на репозиторий](https://github.com/imbarr/naumen-task)

*  [Функционал](#функционал)
*  [Требования](#требования)
*  [Инструкции по запуску](#инструкции-по-запуску)
*  [Примеры использования](#примеры-использования)
   *  [Добавление имени и телефона в справочник](#добавление-имени-и-телефона-в-справочник)
   *  [Запросить данные из справочника](#запросить-данные-из-справочника)
   *  [Удалить запись по id](#удалить-запись-по-id)
   *  [Изменить телефон или имя](#изменить-телефон-или-имя)
   *  [Сохранение данных справочника на жесткий диск](#сохранение-данных-справочника-на-жесткий-диск)
*  [Используемые технологии](#используемые-технологии)
*  [Возможные улучшения](#возможные-улучшения)

### Функционал
*   Хранение данных в РБД
*   Клиентское веб-приложение
*   Сохранение данных на жесткий диск
*   Валидация номера телефона и запрет на дубликаты
*   Автоматическое удаление старых записей
*   Кэширование запрашиваемых данных (**механизм не самописный**)

### Требования

*   Java SDK 10
*   sbt 1.0
*   [SQL Server 2017](https://docs.microsoft.com/en-us/sql/linux/quickstart-install-connect-ubuntu?view=sql-server-2017) (достаточно Express Edition)
*   npm

### Инструкции по запуску
1) Настройте приложение в `backend/src/main/resources/application.conf`.
   Пример настроек смотрите в файле <nobr>`example-application.conf`</nobr>
2) Создайте базу данных для приложения, например, с помощью [sqlcmd](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-2017#ubuntu):
   ```bash
   sqlcmd -U sa -P password
   1> create database naumen
   2> go
   ```
3) ```bash
   sbt run
   ```

#### Веб-приложение
1) Настройте приложение в файле `frontend/src/config.json`
2) ```bash
   npm install
   npm run start
   ```
3) Дополнительно: заполните таблицу тестовыми данными:
   ```bash
   sqlcmd -d naumen -i database/fill.sql
   ```

### Примеры использования

#### Добавление имени и телефона в справочник

```http request
POST /phonebook
{
  "name": "Alex",
  "phone": "+78005553535"
}

201 Created
Location: /phonebook/22
```

#### Запросить данные из справочника
**Важно:** при изменении данных в справочнике напрямую (не через API приложения) не произойдет инвалидации кеша.

Параметры *nameSubstring*, *phoneSubstring*, *start*, *end* можно комбинировать
произвольным образом, но если есть *start*, то должен быть и *end*

```http request
GET /phonebook?nameSubstring=Doe&phoneSubstring=%2B7-922+600

200 OK
[
    {
        "id": 22,
        "name": "John Doe",
        "phone": "+79226005555"
    },
    {
        "id": 33,
        "name": "Jane Doe",
        "phone": "+79226001111"
    },
    {
```

```http request
GET /phonebook?nameSubstring=Doe&start=10&end=11

200 OK
X-Total-Count: 20
[
    {
        "id": 22,
        "name": "John Doe",
        "phone": "+79226005555"
    },
    {
        "id": 33,
        "name": "Jane DOe",
        "phone": "+79226001111"
    }
]
```

#### Удалить запись по id

```http request
DELETE /phonebook/22
```

#### Изменить телефон или имя

```http request
PATCH /phonebook/22
{
  "name": "John",
  "phone": "+78005553535"
}
```

```http request
PATCH /phonebook/22
{
  "name": "John"
}
```

```http request
PATCH /phonebook/22
{
  "phone": "+29022158"
}
```

#### Сохранение данных справочника на жесткий диск

```http request
POST /files

202 Accepted
Location: /tasks/12
```

```http request
GET /tasks/12

200 OK
{
  "status": "in progress"
}
```

### Используемые технологии
*   [Akka](https://akka.io/)
*   [Slick](http://slick.lightbend.com/)
*   [Circe](https://circe.github.io/circe/)
*   [React-Admin](https://marmelab.com/react-admin/)
*   [libphonenumber](https://github.com/googlei18n/libphonenumber)

### Возможные улучшения
1) Интеграционные тесты через Docker (в первую очередь, взаимодействие с БД)
2) Запрос данных из БД через reactive streams
3) Добавление функции сохранения данных на жесткий диск в веб-приложение