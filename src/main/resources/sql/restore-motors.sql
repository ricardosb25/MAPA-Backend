-- Fontes e escopo das variantes: MAPA-Docs/Texts/Motor-Catalog-Sources.md.
INSERT INTO motors (
    manufacturer, name, description,
    displacement_liters, displacement_cc,
    compression_ratio, rpm_cutoff,
    aspiration_type, created_at
)
VALUES
(
    'FORD', 'TI-VCT 1.0 FLEX',
    'Tres cilindros aspirado.',
    1.0, 997, 12.0, 6900,
    'ASPIRADO', CURRENT_TIMESTAMP
),
(
    'FORD', 'ECOBOOST 1.0 GTDI',
    'EcoSport. Tres cilindros turbo.',
    1.0, 999, 10.0, 6500,
    'TURBO', CURRENT_TIMESTAMP
),
(
    'HONDA', 'K20A2 2.0 VTEC',
    'Acura RSX Type-S 2002-2003, mercado norte-americano.',
    2.0, 1998, 11.0, 8100,
    'ASPIRADO', CURRENT_TIMESTAMP
),
(
    'FORD', 'ECOBOOST 2.0 CAF488WQB5',
    'Quatro cilindros turbo. Usa o limite continuo do manual.',
    2.0, 1999, 10.0, 6500,
    'TURBO', CURRENT_TIMESTAMP
),
(
    'NISSAN', 'VQ35DE 3.5 V6 REVUP',
    'Nissan 350Z europeu, ano-modelo 2006, 300 PS. V6 DOHC aspirado; limite de giro de fabrica.',
    3.5, 3498, 10.3, 7000,
    'ASPIRADO', CURRENT_TIMESTAMP
),
(
    'FORD', 'ECOBOOST 2.3 MUSTANG',
    'Mustang S550, manual de 2017. Quatro cilindros turbo.',
    2.3, 2261, 9.5, 6800,
    'TURBO', CURRENT_TIMESTAMP
),
(
    'RENAULT', 'C3G 720 1.2 8V',
    'Twingo 1.2 aspirado com injecao monoponto.',
    1.2, 1239, 9.2, 6000,
    'ASPIRADO', CURRENT_TIMESTAMP
),
(
    'VOLKSWAGEN', 'AP 1.8 8V CFI GASOLINA',
    'Gol G2 1.8 aspirado a gasolina com injecao monoponto.',
    1.8, 1781, 8.5, 6500,
    'ASPIRADO', CURRENT_TIMESTAMP
);
