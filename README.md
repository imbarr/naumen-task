## Тестовое задание на стажировку Naumen (Scala)

### Требования

* Java SDK 10
* sbt 1.0
* SQL Server 2017 (достаточно Express Edition)
* npm

### Инструкции по запуску
1) Настройте приложение в `backend/src/main/resources/application.conf`.
   Пример настроек смотрите в файле <nobr>`example-application.conf`</nobr>
2) Создайте базу данных для приложения, например, с помощью `sqlcmd`:
   ```
   sqlcmd
   1> create database naumen
   2> go
   ```
3) `sbt run`

#### Веб-приложение
1) Настройте приложение в файле `frontend/src/config.json`
2) ```
   npm install
   npm run start
   ```
3) Дополнительно: заполните таблицу тестовыми данными:
   ```
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
* [Akka](https://akka.io/)
* [Slick](http://slick.lightbend.com/)
* [Circe](https://circe.github.io/circe/)
* [React-Admin](https://marmelab.com/react-admin/)
* [libphonenumber](https://github.com/googlei18n/libphonenumber)