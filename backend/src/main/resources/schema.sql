-- MySQL Schema for Ontology Platform
-- Run this script to initialize the database

CREATE DATABASE IF NOT EXISTS ontology_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ontology_platform;

-- Capability Indicator table
CREATE TABLE IF NOT EXISTS capability_indicator (
    id VARCHAR(64) PRIMARY KEY,
    iri VARCHAR(512) NOT NULL,
    name VARCHAR(255) NOT NULL,
    domain VARCHAR(128),
    category VARCHAR(128),
    description TEXT,
    unit VARCHAR(64),
    data_type VARCHAR(64),
    threshold_min DOUBLE,
    threshold_max DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_domain (domain),
    INDEX idx_category (category)
) ENGINE=InnoDB;

-- Ontology Relationship table (reified relationships)
CREATE TABLE IF NOT EXISTS ontology_relationship (
    id VARCHAR(64) PRIMARY KEY,
    source_indicator_id VARCHAR(64) NOT NULL,
    target_indicator_id VARCHAR(64) NOT NULL,
    relationship_type VARCHAR(64) NOT NULL,
    drools_rule_name VARCHAR(255),
    weight DOUBLE DEFAULT 1.0,
    priority INT DEFAULT 0,
    influence_direction VARCHAR(32),
    enabled TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_source (source_indicator_id),
    INDEX idx_target (target_indicator_id)
) ENGINE=InnoDB;

-- Rule Definition table
CREATE TABLE IF NOT EXISTS rule_definition (
    id VARCHAR(64) PRIMARY KEY,
    rule_name VARCHAR(255) NOT NULL,
    drl_content TEXT NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    publish_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_name (rule_name),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Simulation Data table
CREATE TABLE IF NOT EXISTS simulation_data (
    id VARCHAR(64) PRIMARY KEY,
    indicator_id VARCHAR(64) NOT NULL,
    value DOUBLE NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(128),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_indicator (indicator_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB;

-- System User table
CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'VIEWER',
    email VARCHAR(128),
    enabled TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB;

-- Kill Chain Task table
CREATE TABLE IF NOT EXISTS kill_chain_task (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    model_data MEDIUMTEXT,
    status VARCHAR(32) DEFAULT 'DRAFT',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Insert default admin user (password: admin123, BCrypt encoded)
INSERT IGNORE INTO sys_user (id, username, password, role, enabled) VALUES
('1', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'ADMIN', 1);
