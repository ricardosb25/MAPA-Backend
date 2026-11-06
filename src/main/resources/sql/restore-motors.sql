-- Fontes e escopo das variantes: MAPA-Docs/Texts/Motor-Catalog-Sources.md.
INSERT INTO motors (
    manufacturer, name, description,
    displacement_liters, displacement_cc,
    compression_ratio, rpm_cutoff,
    aspiration_type, created_at
)
VALUES
(
    'HONDA', 'K20A2 2.0 VTEC',
    'Acura RSX Type-S 2002-2003, mercado norte-americano. DOHC i-VTEC aspirado; corte de combustivel em movimento.',
    2.0, 1998, 11.0, 8100,
    'ASPIRADO', CURRENT_TIMESTAMP
),
(
    'NISSAN', 'VQ35DE 3.5 V6 REVUP',
    'Nissan 350Z europeu, ano-modelo 2006, 300 PS. V6 DOHC aspirado; limite de giro de fabrica.',
    3.5, 3498, 10.3, 7000,
    'ASPIRADO', CURRENT_TIMESTAMP
);
