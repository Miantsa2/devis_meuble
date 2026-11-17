CREATE TABLE article (
  id BIGSERIAL PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL,
  category VARCHAR(30) NOT NULL, unit VARCHAR(20) NOT NULL,
  unit_price NUMERIC(14,4) NOT NULL DEFAULT 0, approach_coef NUMERIC(8,4) NOT NULL DEFAULT 1,
  active BOOLEAN NOT NULL DEFAULT TRUE, note VARCHAR(500));

CREATE TABLE template_line (
  id BIGSERIAL PRIMARY KEY, template_name VARCHAR(100) NOT NULL, position INT NOT NULL DEFAULT 0,
  article_code VARCHAR(50) NOT NULL, default_quantity NUMERIC(14,4) NOT NULL DEFAULT 0);

CREATE TABLE bom_quote (
  id BIGSERIAL PRIMARY KEY, reference VARCHAR(30), customer_name VARCHAR(200), customer_contact VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL, created_by VARCHAR(100), input_json TEXT NOT NULL, result_json TEXT NOT NULL,
  total_ht NUMERIC(14,2), total_ttc NUMERIC(14,2), currency VARCHAR(10));

INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('MEL19-SANIFE','Mélaminé 19 mm blanc SANIFE','PANNEAU','M2',10.96,1.45,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('MEL10-FINSA','Mélaminé 10 mm blanc FINSA','PANNEAU','M2',8.38,1.45,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('MEL19-DECOR-FINSA','Mélaminé 19 mm décor FINSA','PANNEAU','M2',11.56,1.45,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('CHANT-BL','Chant blanc','CHANT','ML',0.22,1.46,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('CHANT-DECOR','Chant décor','CHANT','ML',0.50,1.45,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('COLLE','Colle','COLLE','U',4.44,1.10,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('LAMELLO','Lamello','QUINCAILLERIE','PCE',0.05,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('ATTACHE-BC','Attache BC','QUINCAILLERIE','PCE',3.51,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('PROFIL-ATTACHE','Profil attache','QUINCAILLERIE','PCE',0.32,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('VIS-5-50','Vis 5/50','QUINCAILLERIE','PCE',0.04,1.00,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('CHEVILLE','Cheville','QUINCAILLERIE','PCE',0.04,1.00,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('TAQUET','Taquet','QUINCAILLERIE','PCE',0.05,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('CHARNIERE-STD','Charnière standard + amortisseur','QUINCAILLERIE','PCE',2.74,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('EMBASE-STD','Embase standard','QUINCAILLERIE','PCE',0.47,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('VIS-3-16','Vis 3/16','QUINCAILLERIE','PCE',0.01,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('POIGNEE-ALU','Poignée alu','QUINCAILLERIE','PCE',2.00,1.00,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO article(code,name,category,unit,unit_price,approach_coef,note) VALUES ('VIS-4-22','Vis 4/22','QUINCAILLERIE','PCE',0.01,1.60,'Exemple tiré du devis Excel : vérifier prix et coefficient');
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',1,'MEL19-SANIFE',0.99);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',2,'MEL10-FINSA',0.47);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',3,'CHANT-BL',3.68);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',4,'COLLE',0.07);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',5,'LAMELLO',8);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',6,'ATTACHE-BC',2);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',7,'PROFIL-ATTACHE',2);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',8,'VIS-5-50',4);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Caisson',9,'CHEVILLE',4);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Étagère',1,'MEL19-SANIFE',0.16);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Étagère',2,'CHANT-BL',0.56);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Étagère',3,'COLLE',0.011);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Étagère',4,'TAQUET',4);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',1,'MEL19-DECOR-FINSA',0.49);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',2,'CHANT-DECOR',2.98);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',3,'COLLE',0.06);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',4,'CHARNIERE-STD',2);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',5,'EMBASE-STD',2);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Porte mélaminé',6,'VIS-3-16',8);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Poignées',1,'POIGNEE-ALU',1);
INSERT INTO template_line(template_name,position,article_code,default_quantity) VALUES ('Poignées',2,'VIS-4-22',2);
