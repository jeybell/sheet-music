create table users (
    user_id bigserial primary key,
    username varchar(50) not null unique,
    password_hash varchar(100) not null,
    created_at timestamp not null default now()
);
