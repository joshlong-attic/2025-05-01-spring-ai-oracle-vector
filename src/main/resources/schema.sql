create table if not exists dog
(
    id          integer primary key,
    name        varchar(255) not null,
    owner       varchar(255) null,
    description varchar(255) not null
);

CREATE TABLE if not exists ai_chat_memory
(
    conversation_id VARCHAR2(36 CHAR) NOT NULL,
    content         CLOB              NOT NULL,
    type            VARCHAR2(10 CHAR) NOT NULL,
    "timestamp"     TIMESTAMP         NOT NULL,
    CONSTRAINT ai_chat_memory_type_chk CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);


CREATE INDEX if not exists ai_chat_memory_conversation_id_timestamp_idx
    ON ai_chat_memory(conversation_id, "timestamp")
