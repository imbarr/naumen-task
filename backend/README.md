## Тестовое задание на стажировку Naumen (Scala)

### Требования

* Java SDK 10
* sbt 1.0

### Инструкции по запуску
1) Измените настройки в `/src/main/resources/application.conf`
2) `sbt run`

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

```
200 OK
```

#### Изменить телефон или имя

```http request
PATCH /phonebook/22

{
  "name": "John",
  "phoneNumber": "88005553535"
}
```

```
200 OK
```