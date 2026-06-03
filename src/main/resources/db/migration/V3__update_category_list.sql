ALTER TABLE inspection_observation
DROP CONSTRAINT chk_category;

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
                                        'LOCOMOTIVE INSPECTION',
                                        'SAFETY WALK',
                                        'DLSIC',
                                        'SAC'
                                        )
        );