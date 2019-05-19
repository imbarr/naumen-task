create login ${user} with password='${password}', default_database=${database}, check_policy=off;
create user ${user} for login ${user};