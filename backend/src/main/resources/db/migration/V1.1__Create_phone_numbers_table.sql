create table phoneNumbers (
  id int primary key identity,
  name varchar(100),
  phone varchar(20)
)

grant select, insert, update, delete on phoneNumbers to ${user}