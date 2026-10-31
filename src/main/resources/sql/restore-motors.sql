INSERT INTO motors (manufacturer, name, description, displacement_liters, displacement_cc, compression_ratio, rpm_cutoff, aspiration_type, created_at) 
VALUES 
('VOLKSWAGEN', 'AP 1.8 8V', 'Motor didático clássico. Ideal para a primeira calibração de ignição.', 1.8, 1781, 9.2, 6500, 'ASPIRADO', CURRENT_TIMESTAMP),
('VOLKSWAGEN', 'EA888 2.0 TSI', 'Injeção direta e turbo twin-scroll. Cuidado com detonação em carga alta.', 2.0, 1984, 9.6, 7000, 'TURBO', CURRENT_TIMESTAMP),
('TOYOTA', '2JZ-GTE 3.0', 'Seis em linha lendário. Muita margem de boost, muita chance de fundir.', 3.0, 2997, 8.5, 7600, 'TURBO', CURRENT_TIMESTAMP),
('HONDA', 'K20A VTEC', 'Alta compressão e giro alto. Avanço agressivo demais destrói pistão.', 2.0, 1998, 11.5, 8600, 'ASPIRADO', CURRENT_TIMESTAMP),
('CHEVROLET', 'LS3 6.2 V8', 'Torque desde a marcha lenta. Excelente para estudar curva de carga.', 6.2, 6162, 10.7, 6600, 'ASPIRADO', CURRENT_TIMESTAMP),
('BMW', 'B58 3.0 Turbo', 'Turbo moderno com alta compressão. Exige mistura rica no topo.', 3.0, 2998, 11.0, 7000, 'TURBO', CURRENT_TIMESTAMP);
