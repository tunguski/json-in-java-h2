DROP TABLE IF EXISTS TBL_STRINGS;

CREATE TABLE TBL_STRINGS (
  id INT AUTO_INCREMENT PRIMARY KEY,
  text VARCHAR NOT NULL
);

CREATE INDEX idx_strings ON tbl_strings(text, id);



DROP SEQUENCE IF EXISTS seq_properties;

CREATE SEQUENCE seq_properties START WITH 1;

DROP TABLE IF EXISTS tbl_ref_properties;

CREATE TABLE tbl_ref_properties (
  id INT DEFAULT NEXTVAL('seq_properties') PRIMARY KEY,
  name INT NOT NULL,
  type TINYINT NOT NULL,
  propertyValue INT NOT NULL
);

CREATE INDEX idx_tbl_ref_properties ON tbl_ref_properties(name, type, propertyValue, id);

DROP TABLE IF EXISTS tbl_fixed_properties;

CREATE TABLE tbl_fixed_properties (
  id INT DEFAULT NEXTVAL('seq_properties') PRIMARY KEY,
  name INT NOT NULL,
  type TINYINT NOT NULL
);

CREATE INDEX idx_tbl_fixed_properties ON tbl_fixed_properties(name, type, id);

DROP TABLE IF EXISTS tbl_raw_properties;

CREATE TABLE tbl_raw_properties (
  id INT DEFAULT NEXTVAL('seq_properties') PRIMARY KEY,
  name INT NOT NULL,
  type TINYINT NOT NULL,
  propertyValue BINARY(8) NOT NULL
);

CREATE INDEX idx_tbl_raw_properties ON tbl_raw_properties(name, type, propertyValue, id);



DROP SEQUENCE IF EXISTS seq_values;

CREATE SEQUENCE seq_values START WITH 1;

DROP TABLE IF EXISTS tbl_ref_values;

CREATE TABLE tbl_ref_values (
  id INT DEFAULT NEXTVAL('seq_values') PRIMARY KEY,
  type TINYINT NOT NULL,
  propertyValue INT NOT NULL
);

CREATE INDEX idx_tbl_ref_values ON tbl_ref_values(type, propertyValue, id);

DROP TABLE IF EXISTS tbl_fixed_values;

CREATE TABLE tbl_fixed_values (
  id INT DEFAULT NEXTVAL('seq_values') PRIMARY KEY,
  type TINYINT NOT NULL
);

CREATE INDEX idx_tbl_fixed_values ON tbl_fixed_values(type, id);

DROP TABLE IF EXISTS tbl_raw_values;

CREATE TABLE tbl_raw_values (
  id INT DEFAULT NEXTVAL('seq_values') PRIMARY KEY,
  type TINYINT NOT NULL,
  propertyValue BINARY(8) NOT NULL
);

CREATE INDEX idx_tbl_raw_values ON tbl_raw_values(type, propertyValue, id);



DROP TABLE IF EXISTS TBL_OBJECTS;

CREATE TABLE TBL_OBJECTS (
  id INT AUTO_INCREMENT PRIMARY KEY,
  properties INT ARRAY
);

CREATE INDEX idx_objects ON tbl_objects(properties, id);



DROP TABLE IF EXISTS tbl_arrays;

CREATE TABLE tbl_arrays (
  id INT AUTO_INCREMENT PRIMARY KEY,
  valuesArray INT ARRAY
);

CREATE INDEX idx_arrays ON tbl_arrays(valuesArray, id);
