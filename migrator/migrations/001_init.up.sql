CREATE TABLE content_types (
    type_id integer primary key generated always as identity,
    type_code varchar(16) not null unique,
    display_name varchar(32) not null
);

CREATE TABLE profiles (
    profile_id integer primary key generated always as identity,
    nickname varchar(32) not null unique,
    created_at timestamp default now()
);

CREATE TABLE groups (
    group_id integer primary key generated always as identity,
    name varchar(64) not null,
    invite_code varchar(16) unique,
    age_restriction smallint check (age_restriction between 0 and 21),
    created_at timestamp default now()
);

CREATE TABLE group_members (
    group_id integer references groups(group_id) on delete cascade,
    profile_id integer references profiles(profile_id) on delete cascade,
    role varchar(16) not null default 'member',
    joined_at timestamp default now(),
    primary key (group_id, profile_id)
);

CREATE TABLE genres (
    genre_id integer primary key generated always as identity,
    genre_name varchar(30) not null,
    content_type integer not null references content_types(type_id) on delete cascade,
    unique (genre_name, content_type)
);

CREATE TABLE profile_preferences (
    preference_id integer primary key generated always as identity,
    profile_id integer references profiles(profile_id) on delete cascade,
    genre_id integer references genres(genre_id) on delete cascade,
    weight smallint not null check (weight between 0 and 10),
    updated_at timestamp default now(),
    unique (profile_id, genre_id)
);

CREATE TABLE group_vibes (
    group_id integer references groups(group_id) on delete cascade,
    genre_id integer references genres(genre_id) on delete cascade,
    total_weight integer not null,
    avg_weight numeric(4,2) not null,
    min_weight smallint not null,
    updated_at timestamp default now(),
    primary key (group_id, genre_id)
);