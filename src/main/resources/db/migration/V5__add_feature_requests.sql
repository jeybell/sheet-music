create table feature_requests (
    feature_request_id bigserial primary key,
    title varchar(255) not null,
    content text not null,
    author varchar(100),
    status varchar(20) not null default 'PENDING',
    created_at timestamp not null default now(),
    deleted_at timestamp null
);
