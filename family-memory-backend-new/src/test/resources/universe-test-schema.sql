-- 仅供 jdbc:h2:mem:universe 测试库使用；禁止指向业务数据库。
DROP ALL OBJECTS;
CREATE TABLE fm_family(id BIGINT PRIMARY KEY, deleted TINYINT DEFAULT 0);
CREATE TABLE fm_user(id BIGINT PRIMARY KEY,username VARCHAR(30),password VARCHAR(100),nickname VARCHAR(30),avatar VARCHAR(500),phone VARCHAR(20),email VARCHAR(100),status TINYINT,created_at TIMESTAMP,updated_at TIMESTAMP,deleted TINYINT DEFAULT 0);
CREATE TABLE fm_memory(id BIGINT AUTO_INCREMENT PRIMARY KEY,family_id BIGINT,author_id BIGINT,type VARCHAR(20),title VARCHAR(120),content TEXT,memory_date DATE,memory_year SMALLINT,location VARCHAR(200),audio_url VARCHAR(500),audio_duration INT,likes_count INT,comments_count INT,created_at TIMESTAMP,updated_at TIMESTAMP,deleted TINYINT DEFAULT 0);
CREATE TABLE fm_memory_media(id BIGINT AUTO_INCREMENT PRIMARY KEY,memory_id BIGINT,media_type VARCHAR(20),url VARCHAR(500),thumbnail_url VARCHAR(500),sort_order INT,created_at TIMESTAMP);
CREATE TABLE fm_memory_tag(id BIGINT AUTO_INCREMENT PRIMARY KEY,memory_id BIGINT,tag_name VARCHAR(30),created_at TIMESTAMP);
CREATE TABLE fm_memory_like(id BIGINT AUTO_INCREMENT PRIMARY KEY,memory_id BIGINT,user_id BIGINT,created_at TIMESTAMP);
CREATE TABLE fm_memory_comment(id BIGINT AUTO_INCREMENT PRIMARY KEY,memory_id BIGINT,user_id BIGINT,content VARCHAR(500),created_at TIMESTAMP,deleted TINYINT DEFAULT 0);
CREATE TABLE fm_time_capsule(id BIGINT AUTO_INCREMENT PRIMARY KEY,family_id BIGINT,creator_id BIGINT,recipient VARCHAR(60),title VARCHAR(120),content TEXT,cover_url VARCHAR(500),open_date DATE,is_opened TINYINT,opened_at TIMESTAMP,created_at TIMESTAMP,updated_at TIMESTAMP,deleted TINYINT DEFAULT 0);
CREATE TABLE fm_universe_index(id BIGINT AUTO_INCREMENT PRIMARY KEY,family_id BIGINT NOT NULL,source_type VARCHAR(16) NOT NULL,source_id BIGINT NOT NULL,node_date DATE NOT NULL,title VARCHAR(120) NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,deleted TINYINT DEFAULT 0,UNIQUE(source_type,source_id));
INSERT INTO fm_family VALUES(1,0);
INSERT INTO fm_user(id,username,nickname,status,deleted) VALUES(2,'test','测试家人',1,0);
