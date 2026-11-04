
INSERT INTO motors (
    manufacturer, name, description,
    displacement_liters, displacement_cc,
    compression_ratio, rpm_cutoff,
    aspiration_type, created_at
)
VALUES

-- =========================================
-- DECADA DE 1970
-- =========================================

-- Carros: Volkswagen Fusca 1600, Brasilia 1600 e Variant.
(
    'VOLKSWAGEN', 'BOXER 1600 AE',
    'Boxer 4 cilindros refrigerado a ar, carburado.',
    1.6, 1584, 7.5, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Chevrolet Opala, Caravan e picapes Chevrolet C10.
-- Aplicacoes da familia 250; versoes variam.
(
    'CHEVROLET', '250 4.1 L6',
    'Seis cilindros em linha, comando no bloco.',
    4.1, 4093, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Ford Maverick GT, Mustang, Galaxie e F-100.
-- Aplicacoes da familia Windsor 302.
(
    'FORD', 'WINDSOR 302 V8',
    'Motor V8 OHV de cinco litros.',
    4.9, 4942, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Fiat 128 e Fiat 128 Rally.
-- Confirmar codigo da variante 1297cc.
(
    'FIAT', '1300 OHV',
    'Motor Fiat 1300 carburado.',
    1.3, 1297, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- =========================================
-- DECADA DE 1980
-- =========================================

-- Carros: Volkswagen Gol, Voyage, Parati e Saveiro.
-- Tambem aplicado em versoes do Ford Escort.
(
    'VOLKSWAGEN', 'AP 1.6 8V',
    'Motor EA827/AP, carburado ou injetado.',
    1.6, 1595, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Renault Super 5, Renault 9,
-- Renault 11, Renault 19 e Express.
(
    'RENAULT', 'CLEON C1G 1.2',
    'Motor Cleon-Fonte com comando lateral.',
    1.2, 1237, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Toyota Corolla AE86, Corolla GT,
-- MR2 AW11 e Corolla FX16.
(
    'TOYOTA', '4A-GE 1.6 16V',
    'Motor DOHC 16V de alta rotacao.',
    1.6, 1587, 9.4, 7600,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Nissan Silvia S13, 180SX e 200SX S13.
-- Dependendo do mercado.
(
    'NISSAN', 'CA18DET 1.8 TURBO',
    'Motor DOHC 16V turbo com injecao eletronica.',
    1.8, 1809, 8.5, 7500,
    'TURBO', CURRENT_TIMESTAMP
),

-- Carros: Peugeot 205 GTI 1.9 e 309 GTI 1.9.
(
    'PEUGEOT', 'XU9JA 1.9 8V',
    'Motor 8 valvulas com injecao multiponto.',
    1.9, 1905, 9.6, 6500,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- =========================================
-- DECADA DE 1990
-- =========================================

-- Carros: Renault Twingo I 1.2 (1993-1996),
-- Renault Clio I 1.2 e Renault Express 1.2.
(
    'RENAULT', 'C3G 1.2 8V',
    'Motor Cleon-Fonte 1239cc, comando lateral, corrente de distribuicao e injecao monoponto.',
    1.2, 1239, 9.2, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Fiat Uno Mille e Mille Brio.
(
    'FIAT', 'FIASA 1.0 8V',
    'Motor Fiasa 994cc carburado.',
    1.0, 994, 8.5, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Opel Vectra A, Astra F,
-- Calibra 2.0 8V e Omega A 2.0.
-- Codigo C20NE europeu.
(
    'CHEVROLET', 'C20NE 2.0 8V',
    'Motor GM Familia II, injecao multiponto.',
    2.0, 1998, 9.2, 6200,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Nissan Silvia S13/S14,
-- Nissan 180SX e Nissan Pulsar GTI-R.
-- Aplicacoes SR20DET com diferentes calibracoes.
(
    'NISSAN', 'SR20DET 2.0 TURBO',
    'Motor DOHC 16V turbo.',
    2.0, 1998, 8.5, 7500,
    'TURBO', CURRENT_TIMESTAMP
),

-- Carros: Mitsubishi Lancer Evolution I-IX,
-- Eclipse GSX e Galant VR-4.
-- Variantes distintas do 4G63 turbo.
(
    'MITSUBISHI', '4G63T 2.0 TURBO',
    'Motor DOHC 16V turbo.',
    2.0, 1997, 8.5, 7000,
    'TURBO', CURRENT_TIMESTAMP
),

-- =========================================
-- DECADA DE 2000
-- =========================================

-- Carros: Fiat Uno Mille Fire, Palio,
-- Siena e Novo Uno 1.0.
(
    'FIAT', 'FIRE 1.0 8V',
    'Motor FIRE com injecao multiponto.',
    1.0, 999, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Renault Twingo I 1.2,
-- Clio II 1.2 e Kangoo 1.2.
(
    'RENAULT', 'D7F 1.2 8V',
    'Motor SOHC 8V com injecao eletronica.',
    1.2, 1149, 9.6, 6500,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Honda Civic Type R EP3 europeu
-- e Acura RSX Type-S norte-americano.
(
    'HONDA', 'K20A2 2.0 VTEC',
    'Motor DOHC i-VTEC aspirado.',
    2.0, 1998, 11.0, 8000,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Volkswagen Gol, Fox,
-- Polo, Saveiro e SpaceFox.
-- Aplicacoes de variantes EA111 1.6.
(
    'VOLKSWAGEN', 'EA111 1.6 8V',
    'Motor quatro cilindros com injecao multiponto.',
    1.6, 1598, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Subaru Impreza WRX STI
-- e Subaru WRX STI de geracoes posteriores.
(
    'SUBARU', 'EJ257 2.5 TURBO',
    'Motor boxer turbo com injecao eletronica.',
    2.5, 2457, 8.2, 6700,
    'TURBO', CURRENT_TIMESTAMP
),

-- Carros: Nissan 350Z, Altima 3.5,
-- Maxima 3.5 e Infiniti G35.
(
    'NISSAN', 'VQ35DE 3.5 V6',
    'Motor V6 DOHC aspirado.',
    3.5, 3498, 10.3, 6600,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- =========================================
-- FORD CHT E OUTROS MOTORES BRASILEIROS
-- =========================================

-- Carros: Ford Escort L, Escort GL
-- e algumas versoes do Ford Verona.
(
    'FORD', 'CHT 1.3 GASOLINA',
    'Motor CHT 1341cc carburado.',
    1.3, 1341, 9.0, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Ford Escort, Del Rey, Belina,
-- Corcel II, Pampa, Verona e VW Gol.
-- Aplicacoes variam por ano.
(
    'FORD', 'CHT 1.6 GASOLINA',
    'Motor CHT 1555cc carburado.',
    1.6, 1555, 9.0, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Ford Escort, Del Rey,
-- Belina, Corcel II e Pampa a alcool.
(
    'FORD', 'CHT 1.6 ALCOOL',
    'Motor CHT 1555cc movido a etanol.',
    1.6, 1555, 12.0, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Ford Escort Hobby 1.0,
-- Volkswagen Gol 1000 e Gol 1000i.
-- Tambem conhecido como AE-1000.
(
    'FORD', 'CHT 1.0',
    'Motor AE-1000 de origem CHT.',
    1.0, 997, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Opel Corsa B 1.4,
-- Opel Astra F 1.4 e derivados GM.
-- Confirmar codigo do motor por mercado.
(
    'CHEVROLET', 'FAMILIA I 1.4 8V',
    'Motor GM Familia I de 1389cc.',
    1.4, 1389, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Chevrolet Corsa 1.6,
-- Corsa Sedan, Corsa Wagon e Corsa Pickup.
(
    'CHEVROLET', 'FAMILIA I 1.6 8V',
    'Motor GM Familia I de 1598cc.',
    1.6, 1598, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Fiat Tipo 1.6 i.e.,
-- Fiat Tempra 1.6 em determinados mercados.
-- Confirmar variante Sevel especifica.
(
    'FIAT', 'SEVEL 1.6 8V',
    'Motor Fiat 1581cc da familia Sevel.',
    1.6, 1581, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Ford Escort Zetec 1.8 16V,
-- Escort SW e Ford Mondeo 1.8 16V.
(
    'FORD', 'ZETEC 1.8 16V',
    'Motor DOHC 16 valvulas.',
    1.8, 1796, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Volkswagen Gol GTI 2.0,
-- Santana 2.0, Quantum 2.0,
-- Ford Escort XR3 2.0 e Versailles 2.0.
-- Aplicacoes de variantes EA827/AP 2.0.
(
    'VOLKSWAGEN', 'EA827 2.0 8V',
    'Motor quatro cilindros da familia EA827/AP.',
    2.0, 1984, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
),

-- Carros: Renault Clio 1.6 16V,
-- Megane 1.6 16V, Scenic 1.6 16V,
-- Logan, Sandero e Duster 1.6 16V.
-- Variantes e calibracoes distintas.
(
    'RENAULT', 'K4M 1.6 16V',
    'Motor DOHC 16V com injecao multiponto.',
    1.6, 1598, NULL, NULL,
    'ASPIRADO', CURRENT_TIMESTAMP
);
