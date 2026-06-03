ALTER TABLE inspection_observation
    DROP CONSTRAINT chk_category;

ALTER TABLE inspection_observation
    DROP CONSTRAINT chk_department;

ALTER TABLE inspection_observation
    ADD CONSTRAINT chk_category CHECK ("category" IN
                                       (
                                        'GENERAL INSPECTION',
                                        'SAFETY MONITORING',
                                        'AUDIO VISUAL INSPECTION',
                                        'CENTRAL CABLE GALLERY INSPECTION',
                                        'CONVEYOR GALLERY INSPECTION',
                                        'EOT CRANE INSPECTION',
                                        'ILLUMINATION INSPECTION',
                                        'NIGHT INSPECTION',
                                        'RAIL ROAD CROSSING INSPECTION',
                                        'SAFETY WALK',
                                        'DLSIC',
                                        'SAC'
                                        )
    );

ALTER TABLE inspection_observation
    ADD CONSTRAINT chk_department CHECK ("department" IN
                                       (
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
                                        'PROJECTS',
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
                                        )
    );