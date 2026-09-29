CREATE TABLE rescue_centers (
    id     BIGSERIAL PRIMARY KEY,
    code   VARCHAR(50)  NOT NULL,
    name   VARCHAR(150) NOT NULL,
    city   VARCHAR(100) NOT NULL,
    CONSTRAINT uk_rescue_centers_code UNIQUE (code)
);

CREATE TABLE rescue_cases (
    id               BIGSERIAL PRIMARY KEY,
    case_code        VARCHAR(50)  NOT NULL,
    rescue_date      DATE         NOT NULL,
    rescue_location  VARCHAR(200) NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    rescue_center_id BIGINT       NOT NULL,
    CONSTRAINT uk_rescue_cases_case_code UNIQUE (case_code),
    CONSTRAINT fk_rescue_cases_center
        FOREIGN KEY (rescue_center_id) REFERENCES rescue_centers (id),
    CONSTRAINT ck_rescue_cases_status
        CHECK (status IN (
            'ADMITTED',
            'UNDER_EVALUATION',
            'IN_REHABILITATION',
            'READY_FOR_RELEASE',
            'RELEASED',
            'CLOSED'
        ))
);

CREATE TABLE animals (
    id               BIGSERIAL PRIMARY KEY,
    animal_code      VARCHAR(50)  NOT NULL,
    common_name      VARCHAR(150) NOT NULL,
    scientific_name  VARCHAR(150) NOT NULL,
    sex              VARCHAR(20)  NOT NULL,
    rescue_case_id   BIGINT       NOT NULL,
    CONSTRAINT uk_animals_animal_code UNIQUE (animal_code),
    CONSTRAINT uk_animals_rescue_case_id UNIQUE (rescue_case_id),
    CONSTRAINT fk_animals_rescue_case
        FOREIGN KEY (rescue_case_id) REFERENCES rescue_cases (id)
);

CREATE TABLE medical_records (
    id                 BIGSERIAL PRIMARY KEY,
    animal_id          BIGINT        NOT NULL,
    initial_weight     DECIMAL(6,2),
    initial_condition  VARCHAR(150),
    injuries           TEXT,
    observations       TEXT,
    CONSTRAINT uk_medical_records_animal_id UNIQUE (animal_id),
    CONSTRAINT fk_medical_records_animal
        FOREIGN KEY (animal_id) REFERENCES animals (id)
);

-- 5) specialists
CREATE TABLE specialists (
    id                 BIGSERIAL PRIMARY KEY,
    professional_code  VARCHAR(50)  NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    email              VARCHAR(150) NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_specialists_professional_code UNIQUE (professional_code),
    CONSTRAINT uk_specialists_email UNIQUE (email)
);

CREATE TABLE expertise (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    CONSTRAINT uk_expertise_name UNIQUE (name)
);

CREATE TABLE specialist_expertise (
    specialist_id  BIGINT NOT NULL,
    expertise_id   BIGINT NOT NULL,
    CONSTRAINT pk_specialist_expertise PRIMARY KEY (specialist_id, expertise_id),
    CONSTRAINT fk_specialist_expertise_specialist
        FOREIGN KEY (specialist_id) REFERENCES specialists (id),
    CONSTRAINT fk_specialist_expertise_expertise
        FOREIGN KEY (expertise_id) REFERENCES expertise (id)
);

CREATE TABLE treatments (
    id             BIGSERIAL PRIMARY KEY,
    animal_id      BIGINT      NOT NULL,
    specialist_id  BIGINT      NOT NULL,
    performed_at   TIMESTAMP   NOT NULL,
    type           VARCHAR(50) NOT NULL,
    description    TEXT,
    CONSTRAINT fk_treatments_animal
        FOREIGN KEY (animal_id) REFERENCES animals (id),
    CONSTRAINT fk_treatments_specialist
        FOREIGN KEY (specialist_id) REFERENCES specialists (id)
);

CREATE INDEX idx_rescue_cases_rescue_center_id ON rescue_cases (rescue_center_id);
CREATE INDEX idx_rescue_cases_status           ON rescue_cases (status);
CREATE INDEX idx_rescue_cases_rescue_date       ON rescue_cases (rescue_date);

CREATE INDEX idx_treatments_animal_id      ON treatments (animal_id);
CREATE INDEX idx_treatments_specialist_id  ON treatments (specialist_id);
CREATE INDEX idx_treatments_performed_at   ON treatments (performed_at);
