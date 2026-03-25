create table industry_base_response (
    id bigint not null auto_increment primary key,
    fetched_at timestamp not null,
    item_count integer,
    raw_json longtext not null
);

create index idx_industry_base_response_fetched_at
    on industry_base_response (fetched_at desc);

create table stock_pool_response (
    id bigint not null auto_increment primary key,
    page_no integer not null,
    fetched_at timestamp not null,
    item_count integer,
    raw_json longtext not null
);

create index idx_stock_pool_response_page_fetched
    on stock_pool_response (page_no, fetched_at desc);

create table industry_kline_response (
    id bigint not null auto_increment primary key,
    industry_code varchar(32) not null,
    fetched_at timestamp not null,
    raw_json longtext not null
);

create index idx_industry_kline_code_fetched
    on industry_kline_response (industry_code, fetched_at desc);

create table stock_real_response (
    id bigint not null auto_increment primary key,
    stock_code varchar(32) not null,
    fetched_at timestamp not null,
    raw_json longtext not null
);

create index idx_stock_real_code_fetched
    on stock_real_response (stock_code, fetched_at desc);

create table stock_kline_response (
    id bigint not null auto_increment primary key,
    stock_code varchar(32) not null,
    fetched_at timestamp not null,
    raw_json longtext not null
);

create index idx_stock_kline_code_fetched
    on stock_kline_response (stock_code, fetched_at desc);

create table crawl_job_log (
    id bigint not null auto_increment primary key,
    job_name varchar(128) not null,
    target_key varchar(128),
    started_at timestamp not null,
    finished_at timestamp,
    success boolean not null,
    record_count integer,
    message varchar(2000)
);

create index idx_crawl_job_log_started_at
    on crawl_job_log (started_at desc);
