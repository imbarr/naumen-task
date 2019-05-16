## Тестовое задание на стажировку Naumen (Scala)

### Требования

* Java SDK 10
* sbt 1.0
* SQL Server 2017 (достаточно Express Edition)

### Инструкции по запуску
1) Настройте приложение в `/src/main/resources/application.conf`. Пример настроек смотрите в файле `example-application.conf`
2) Создайте базу данных для приложения, например, с помощью `sqlcmd`:
   ```
   sqlcmd
   1> create database naumen
   2> go
   ```
3) `sbt run`

### Примеры использования

#### Добавление имени и телефона в справочник

```http request
POST /phonebook

{
  "name": "Alex",
  "phoneNumber": "+78005553535"
}
```

```
201 Created
Location: /phonebook/22
```

#### Получение всех записей из справочника

```http request
GET /phonebook
```

```
200 OK
[
    {
        "id": 22,
        "name": "John",
        "phoneNumber": "88005553535"
    },
    {
        "id": 33,
        "name": "Jane",
        "phoneNumber": "88005553535"
    },
    ...
]
```

#### Поиск записей по подстроке имени

```http request
GET /phonebook?nameSubstring=Doe
```

```
200 OK
[
    {
        "id": 22,
        "name": "John Doe",
        "phoneNumber": "88005553535"
    },
    {
        "id": 33,
        "name": "Jane Doe",
        "phoneNumber": "88005553535"
    },
    ...
]
```

#### Поиск записей по подстроке номера телефона

```http request
GET /phonebook?phoneSubstring=%2B7922
```

```
200 OK
[
    {
        "id": 22,
        "name": "John Doe",
        "phoneNumber": "+79222222222"
    },
    {
        "id": 33,
        "name": "Jane Doe",
        "phoneNumber": "+79222222292"
    },
    ...
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
  "phoneNumber": "88005553535"
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
  "phoneNumber": "88005553535"
}
```