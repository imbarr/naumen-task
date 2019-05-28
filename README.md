## Тестовое задание на стажировку Naumen (Scala)

![Build Status](https://travis-ci.org/imbarr/naumen-task.svg?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ca23f56b8954ddeb3c2dbe94e717a7c)](https://www.codacy.com/app/imbarr/naumen-task?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=imbarr/naumen-task&amp;utm_campaign=Badge_Grade)

### Функционал
*   <span style="color:green">Хранение данных в РБД</span>
*   <span style="color:green">Клиентское веб-приложение</span>
*   <span style="color:green">Сохранение данных на жесткий диск</span>
*   <span style="color:green">Валидация номера телефона и запрет на дубликаты</span>
*   <span style="color:green">Автоматическое удаление старых записей</span>
*   <span style="color:yellow">Кэширование запрашиваемых данных (механизм не самописный)</span>

### Требования

*   Java SDK 10
*   sbt 1.0
*   SQL Server 2017 (достаточно Express Edition)
*   npm

### Инструкции по запуску
1) Настройте приложение в `backend/src/main/resources/application.conf`.
   Пример настроек смотрите в файле <nobr>`example-application.conf`</nobr>
2) Создайте базу данных для приложения, например, с помощью [sqlcmd](https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-setup-tools?view=sql-server-2017#ubuntu):
   ```bash
   sqlcmd
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

### Используемые технологии
*   [Akka](https://akka.io/)
*   [Slick](http://slick.lightbend.com/)
*   [Circe](https://circe.github.io/circe/)
*   [React-Admin](https://marmelab.com/react-admin/)
*   [libphonenumber](https://github.com/googlei18n/libphonenumber)