CREATE TABLE event_streams (
  id SERIAL PRIMARY KEY,
  type_name VARCHAR(50) NOT NULL,
  stream_name VARCHAR(100) NOT NULL,
  stream_index INTEGER,
  content JSON,
  UNIQUE (type_name, stream_name, stream_index)
);
