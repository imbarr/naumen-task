## Тестовое задание на стажировку Naumen (Scala)

### Требования

* Java SDK 10
* sbt 1.0

### Инструкции по запуску
1) Измените настройки в `/src/main/resources/application.conf`
2) `sbt run`

### API

#### Добавление имени и телефона в справочник

```http request
POST /phonebook
```
```json
{
  "name": "Alex",
  "phoneNumber": "+78005553535"
}
```

Заголовок `Location` в ответе будет содержать относительный путь до созданного ресурса.

#### Получение всех записей из справочника

```http request
GET /phonebook
```

#### Поиск записей по подстроке имени

```http request
GET /phonebook?nameSubstring=Doe
```

#### Поиск записей по подстроке номера телефона

```http request
GET /phonebook?phoneSubstring=%2B7922
```

#### Удалить запись по id

```http request
DELETE /phonebook/22
```

#### Изменить телефон или имя

```http request
PATCH /phonebook/22
```

```json
{
  "name": "John",
  "phoneNumber": "88005553535"
}
```