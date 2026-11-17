-- Calcul automatique des surfaces et chants à partir des mesures du composant (second tableau de MEUBLE_HAUT.xlsx)
ALTER TABLE article ALTER COLUMN unit_price TYPE NUMERIC(18,6);

-- Prix et coefficients repris des formules du fichier Excel (mise à jour seulement si la valeur n'a pas été modifiée dans l'administration)
UPDATE article SET unit_price = 10.955077, note = 'Repris de MEUBLE_HAUT.xlsx : 430666 / 2,8 / 2,08 / 5000 / 1,35' WHERE code = 'MEL19-SANIFE' AND unit_price = 10.96;
UPDATE article SET unit_price = 8.375305,  note = 'Repris de MEUBLE_HAUT.xlsx : 329250 / 2,8 / 2,08 / 5000 / 1,35' WHERE code = 'MEL10-FINSA' AND unit_price = 8.38;
UPDATE article SET unit_price = 11.56, note = 'Repris de MEUBLE_HAUT.xlsx' WHERE code = 'MEL19-DECOR-FINSA';
UPDATE article SET approach_coef = 1.45, note = 'Repris de MEUBLE_HAUT.xlsx' WHERE code = 'CHANT-BL' AND approach_coef = 1.46;
UPDATE article SET approach_coef = 1.45, note = 'Repris de MEUBLE_HAUT.xlsx' WHERE code = 'CHANT-DECOR';
UPDATE article SET unit_price = 4.444444, approach_coef = 1.10, note = 'Repris de MEUBLE_HAUT.xlsx : 20000 / 4500, coef 1,1' WHERE code = 'COLLE' AND unit_price = 4.44;

ALTER TABLE template_line ADD COLUMN slot VARCHAR(30);
ALTER TABLE template_line ADD COLUMN quantity_formula VARCHAR(200);

CREATE TABLE component_template (
  id BIGSERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE,
  panel_waste_percent NUMERIC(8,3) NOT NULL DEFAULT 0, chant_waste_percent NUMERIC(8,3) NOT NULL DEFAULT 0);

CREATE TABLE template_piece (
  id BIGSERIAL PRIMARY KEY, template_name VARCHAR(100) NOT NULL, position INT NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL, panel_slot VARCHAR(30), qty_formula VARCHAR(200) NOT NULL DEFAULT 'Q',
  dim1_formula VARCHAR(200) NOT NULL, dim2_formula VARCHAR(200) NOT NULL,
  chant_slot VARCHAR(30), edges_dim1 INT NOT NULL DEFAULT 0, edges_dim2 INT NOT NULL DEFAULT 0);

INSERT INTO app_setting(id,value_numeric,unit,description) VALUES
 ('bom.chant_allowance_mm',50,'mm','Surlongueur ajoutée à chaque bord de chant (le +50 du fichier Excel)');

INSERT INTO component_template(name,panel_waste_percent,chant_waste_percent) VALUES
 ('Caisson',10,0),('Étagère',10,10),('Porte mélaminé',10,0),('Poignées',0,0);

DELETE FROM template_line;

INSERT INTO template_piece(template_name,position,name,panel_slot,qty_formula,dim1_formula,dim2_formula,chant_slot,edges_dim1,edges_dim2) VALUES ('Caisson',1,'Joue','MEL19','2*Q','H','P','CHANT',1,1);
INSERT INTO template_piece(template_name,position,name,panel_slot,qty_formula,dim1_formula,dim2_formula,chant_slot,edges_dim1,edges_dim2) VALUES ('Caisson',2,'Dessous / Dessus','MEL19','2*Q','L-38','P','CHANT',1,0);
INSERT INTO template_piece(template_name,position,name,panel_slot,qty_formula,dim1_formula,dim2_formula,chant_slot,edges_dim1,edges_dim2) VALUES ('Caisson',3,'Fond','MEL10','1*Q','H-28','L-14',NULL,0,0);
INSERT INTO template_piece(template_name,position,name,panel_slot,qty_formula,dim1_formula,dim2_formula,chant_slot,edges_dim1,edges_dim2) VALUES ('Étagère',1,'Étagère','MEL19','Q','L-38','P-25','CHANT',1,0);
INSERT INTO template_piece(template_name,position,name,panel_slot,qty_formula,dim1_formula,dim2_formula,chant_slot,edges_dim1,edges_dim2) VALUES ('Porte mélaminé',1,'Porte','MEL19D','Q','H-10','L/Q-1','CHANT',2,2);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',1,'MEL19-SANIFE',0,'MEL19',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',2,'MEL10-FINSA',0,'MEL10',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',3,'CHANT-BL',0,'CHANT',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',4,'COLLE',0,NULL,'CH/50');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',5,'LAMELLO',0,NULL,'8*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',6,'ATTACHE-BC',0,NULL,'2*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',7,'PROFIL-ATTACHE',0,NULL,'2*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',8,'VIS-5-50',0,NULL,'4*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Caisson',9,'CHEVILLE',0,NULL,'4*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Étagère',1,'MEL19-SANIFE',0,'MEL19',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Étagère',2,'CHANT-BL',0,'CHANT',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Étagère',3,'COLLE',0,NULL,'CH/50');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Étagère',4,'TAQUET',0,NULL,'4*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',1,'MEL19-DECOR-FINSA',0,'MEL19D',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',2,'CHANT-DECOR',0,'CHANT',NULL);
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',3,'COLLE',0,NULL,'CH/50');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',4,'CHARNIERE-STD',0,NULL,'2*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',5,'EMBASE-STD',0,NULL,'2*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Porte mélaminé',6,'VIS-3-16',0,NULL,'8*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Poignées',1,'POIGNEE-ALU',0,NULL,'1*Q');
INSERT INTO template_line(template_name,position,article_code,default_quantity,slot,quantity_formula) VALUES ('Poignées',2,'VIS-4-22',0,NULL,'2*Q');
