alter table phoneNumbers alter column name varchar(150) not null

alter table phoneNumbers alter column phone varchar(50) not null

alter table phoneNumbers add constraint startsWithPlus check (phone like '+[0-9][0-9]%')