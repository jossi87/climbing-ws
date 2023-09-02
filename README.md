# About
REST Web Service hosted on Tomcat.
Used in [https://github.com/jossi87/climbing-web](https://github.com/jossi87/climbing-web) as a gateway to a MySQL database.

<!-- Product -->
## :link: Product
* Bouldering: [buldreinfo.com](https://buldreinfo.com)
* Route climbing: [brattelinjer.no](https://brattelinjer.no)
* Ice climbing: [is.brattelinjer.no](https://is.brattelinjer.no)

<!-- License -->
## :warning: License
Distributed under the GNU GENERAL PUBLIC LICENSE (Version 3): https://brattelinjer.no/gpl-3.0.txt

<!-- Onboarding -->
## :rocket: Onboarding
1. Database
	- Download the databasedump: https://github.com/jossi87/climbing-ws/blob/main/20230728-100208.buldreinfo.no-data.sql
	- Download and run MySQL Installer (https://dev.mysql.com/downloads/installer/)
	- Choose "Use Legacy Authentication Method (Retain MySQL 5.x Compability)"
	- Run MySQL Command Line Client and type:
	```
	CREATE DATABASE buldreinfo;
	USE buldreinfo;
	SET autocommit=0; SOURCE C:/temp/20230728-100208.buldreinfo.no-data.sql; COMMIT;
	
	-- TODO Remove the following section when database (20230728-100208.buldreinfo.no-data.sql) is updated.
	ALTER TABLE `buldreinfo`.`problem` ADD COLUMN `broken` VARCHAR(255) AFTER `sector_id`;
	ALTER TABLE `buldreinfo`.`area` ADD COLUMN `sun_from_hour` INT NULL AFTER `no_dogs_allowed`, ADD COLUMN `sun_to_hour` INT NULL AFTER `sun_from_hour`;
	ALTER TABLE `buldreinfo`.`sector` ADD COLUMN `wall_direction` VARCHAR(32) NULL AFTER `polygon_coords`;
	CREATE TABLE `buldreinfo`.`coordinate` (
	  `id` INT NOT NULL AUTO_INCREMENT,
	  `latitude` DECIMAL(12,10) NOT NULL,
	  `longitude` DECIMAL(12,10) NOT NULL,
	  `elevation` DECIMAL(6,2) NULL,
	  PRIMARY KEY (`id`),
	  UNIQUE INDEX `coordinate_lat_lng_unique` (`latitude` ASC, `longitude` ASC) VISIBLE);
	CREATE TABLE `buldreinfo`.`sector_outline` (
	  `id` INT NOT NULL AUTO_INCREMENT,
	  `sector_id` INT NOT NULL,
	  `coordinate_id` INT NOT NULL,
	  `sorting` INT NOT NULL,
	  PRIMARY KEY (`id`),
	  INDEX `sector_outline_sector_id_fk_idx` (`sector_id` ASC) VISIBLE,
	  INDEX `sector_outline_coordinate_id_idx` (`coordinate_id` ASC) VISIBLE,
	  CONSTRAINT `sector_outline_sector_id_fk`
		FOREIGN KEY (`sector_id`)
		REFERENCES `buldreinfo`.`sector` (`id`)
		ON DELETE CASCADE
		ON UPDATE CASCADE,
	  CONSTRAINT `sector_outline_coordinate_id`
		FOREIGN KEY (`coordinate_id`)
		REFERENCES `buldreinfo`.`coordinate` (`id`)
		ON DELETE CASCADE
		ON UPDATE CASCADE);
	ALTER TABLE `buldreinfo`.`sector_outline` ADD UNIQUE INDEX `sector_outline_uq` (`sector_id` ASC, `coordinate_id` ASC) VISIBLE;
	
	-- Add dummy data
	INSERT INTO region (id, name, title, description, url, polygon_coords, latitude, longitude, default_zoom, emails) VALUES (1, 'Dev', 'Title', 'Description', 'http://localhost', '58.95852920349744,5.43548583984375;59.139339347998906,5.54534912109375;59.32900841886421,5.990295410156251;59.38780167734329,6.517639160156251;59.139339347998906,7.028503417968751;58.991785092994974,7.033996582031251;58.59547775958452,6.8499755859375;58.26619900311628,6.896667480468751;58.16927656729275,6.594543457031251;58.467870587058236,5.77606201171875;58.729750254584566,5.457458496093751', 58.72, 6.62, 8, null);
	INSERT INTO type VALUES (1, 'Climbing', 'Route', 'Bolt');
	INSERT INTO type  VALUES (2, 'Climbing', 'Route', 'Trad');
	INSERT INTO region_type (region_id, type_id) VALUES (1, 1);
	INSERT INTO region_type (region_id, type_id) VALUES (1, 2);
	INSERT INTO grade VALUES ('CLIMBING', 0, 'n/a', 0, 'n/a');
	INSERT INTO grade VALUES ('CLIMBING', 45, '8 (7b+)', 4, 8);
	```
2. Eclipse
	- Download Eclipse Installer from (https://www.eclipse.org/downloads/)
	- Choose "Eclipse IDE for Enterprise Java and Web Developers"
	- "Checkout projects from Git" (https://github.com/jossi87/climbing-ws)
	- Install Tomcat server: File -> New -> Other -> Tomcat v10.1 Server
	- Edit buldreinfo.properties in "com.buldreinfo.jersey.jaxb.config" (all values are required for the server to run, just add wathever you want to "auth0.secret", "google.apikey" and "vegvesen.auth")
	```
	db.hostname=localhost
	db.database=buldreinfo
	db.username=root
	db.password=YOUR_PASSWORD
	auth0.secret=WhateverYouWant
	google.apikey=WhateverYouWant
	vegvesen.auth=WhateverYouWant
	```
3. Run Project
	- Right click on "com.buldreinfo.jersey.jaxb" in Project Explorer and choose "Run as" -> "Run on server" to start web service.
4. You should now be able to access the server on:
	- http://localhost:8080/com.buldreinfo.jersey.jaxb/v2/meta
	- http://localhost:8080/com.buldreinfo.jersey.jaxb/openapi.json
	- http://localhost:8080/com.buldreinfo.jersey.jaxb/application.wadl

<!-- Contact -->
## :handshake: Contact
* Jostein Oeygarden (jostein.oygarden@gmail.com)
* Project Link: [https://github.com/jossi87/climbing-ws](https://github.com/jossi87/climbing-ws)
