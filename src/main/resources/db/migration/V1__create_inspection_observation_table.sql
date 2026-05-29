-- V1__create_inspection_observation_table.sql

-- Create table
CREATE TABLE inspection_observation (
                                        "observation_id"                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                                        "user_id"                       UUID,
                                        "inspection_date"               DATE            NOT NULL,
                                        "category"                      VARCHAR(100)    NOT NULL,
                                        "department"                    VARCHAR(50)     NOT NULL,
                                        "sub_department"                VARCHAR(255),
                                        "location"                      VARCHAR(255),
                                        "observation"                   TEXT,
                                        "compliance_status"             VARCHAR(50),
                                        "target_date"                   DATE,
                                        "to_be_included_in_dispatcher"  VARCHAR(255),
                                        "recommendations"               TEXT,
                                        "discussed_with"                VARCHAR(255),
                                        "inspection_photo_url"          TEXT[],
                                        "complied_photo_url"            TEXT[],
                                        "photo_upload_status"           VARCHAR(50),
                                        "is_deleted"                    BOOLEAN         DEFAULT FALSE,
                                        "created_at"                    TIMESTAMPTZ     NOT NULL,
                                        "updated_at"                    TIMESTAMPTZ,
                                        "observation_hash"              VARCHAR(255),

                                        CONSTRAINT chk_category CHECK ("category" IN (
                                                                                      'GENERAL INSPECTION',
                                                                                      'SAFETY MONITORING',
                                                                                      'AUDIO VISUAL INSPECTION',
                                                                                      'CENTRAL CABLE GALLERY INSPECTION',
                                                                                      'CONVEYOR GALLERY INSPECTION',
                                                                                      'EOT CRANE INSPECTION',
                                                                                      'ILLUMINATION INSPECTION',
                                                                                      'NIGHT INSPECTION',
                                                                                      'RAIL ROAD CROSSING INSPECTION',
                                                                                      'SAFETY WALK'
                                            )),

                                        CONSTRAINT chk_department CHECK ("department" IN (
                                                                                          'BF',
                                                                                          'CED',
                                                                                          'CME',
                                                                                          'CMM',
                                                                                          'CO&CC',
                                                                                          'CRM-1&2',
                                                                                          'CRM-3',
                                                                                          'DNW',
                                                                                          'EMD',
                                                                                          'GU',
                                                                                          'HRCF',
                                                                                          'HSM',
                                                                                          'I&A',
                                                                                          'MRD',
                                                                                          'RCL',
                                                                                          'RED',
                                                                                          'RGBS',
                                                                                          'RMHP',
                                                                                          'RMP',
                                                                                          'SHOPS & FOUNDRY',
                                                                                          'SMS NEW',
                                                                                          'SMS-2 & CCS',
                                                                                          'SP',
                                                                                          'TBS',
                                                                                          'TRAFFIC',
                                                                                          'WMD',
                                                                                          'OTHERS'
                                            )),

                                        CONSTRAINT chk_compliance_status CHECK ("compliance_status" IN (
                                                                                                        'COMPLIED',
                                                                                                        'NOT COMPLIED',
                                                                                                        'GOOD POINT'
                                            ))
);

-- Indexes for common query patterns
CREATE INDEX idx_inspection_observation_date       ON inspection_observation ("inspection_date");
CREATE INDEX idx_inspection_observation_category   ON inspection_observation ("category");
CREATE INDEX idx_inspection_observation_department ON inspection_observation ("department");
CREATE INDEX idx_inspection_observation_isDeleted  ON inspection_observation ("is_deleted");