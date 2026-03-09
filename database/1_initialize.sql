CREATE TABLE event_streams (
  id SERIAL PRIMARY KEY,
  persisted_at BIGINT NOT NULL,
  type_name VARCHAR(64) NOT NULL,
  stream_name VARCHAR(128) NOT NULL,
  stream_index INTEGER NOT NULL,
  event_type_name VARCHAR(64) NOT NULL,
  content JSON NOT NULL,
  UNIQUE (type_name, stream_name, stream_index)
);
