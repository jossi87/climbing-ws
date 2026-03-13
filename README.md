# About

REST Web Service hosted on Tomcat.
Used in [https://github.com/jossi87/climbing-web](https://github.com/jossi87/climbing-web) as a gateway to a MySQL database.

## :link: Product

- Bouldering: [buldreinfo.com](https://buldreinfo.com)
- Route climbing: [brattelinjer.no](https://brattelinjer.no)
- Ice climbing: [is.brattelinjer.no](https://is.brattelinjer.no)

## :warning: License

Distributed under the GNU GENERAL PUBLIC LICENSE (Version 3): https://brattelinjer.no/gpl-3.0.txt

## :rocket: Onboarding

### Database

- Download the databasedump: <https://github.com/jossi87/climbing-ws/blob/main/20260304.climbing-no-data.sql>
- Download and run MySQL Installer (<https://dev.mysql.com/downloads/installer/>)
- Choose "Use Legacy Authentication Method (Retain MySQL 5.x Compability)"
- Connect to the database and run these commands:
> ```sh
> CREATE DATABASE IF NOT EXISTS climbing;
> USE climbing;
> SET autocommit=0; SOURCE ./20260304.climbing-no-data.sql; COMMIT;
> INSERT INTO region (id, name, title, description, url, polygon_coords, latitude, longitude, default_zoom, emails) VALUES (1, 'Dev1', 'Title', 'Description', 'http://localhost', '58.95852920349744,5.43548583984375;59.139339347998906,5.54534912109375;59.32900841886421,5.990295410156251;59.38780167734329,6.517639160156251;59.139339347998906,7.028503417968751;58.991785092994974,7.033996582031251;58.59547775958452,6.8499755859375;58.26619900311628,6.896667480468751;58.16927656729275,6.594543457031251;58.467870587058236,5.77606201171875;58.729750254584566,5.457458496093751', 58.72, 6.62, 8, null);
> INSERT INTO type VALUES (1, 'Climbing', 'Route', 'Bolt');
> INSERT INTO type VALUES (2, 'Climbing', 'Route', 'Trad');
> INSERT INTO region_type (region_id, type_id) VALUES (1, 1);
> INSERT INTO region_type (region_id, type_id) VALUES (1, 2);
> INSERT INTO grade VALUES ('CLIMBING', 0, 'n/a', 0, 'n/a');
> INSERT INTO grade VALUES ('CLIMBING', 45, '8 (7b+)', 4, 8);
> COMMIT;
> ```

### Eclipse

- Download Eclipse Installer from (<https://www.eclipse.org/downloads/>)
- Choose "Eclipse IDE for Enterprise Java and Web Developers"
- "Checkout projects from Git" (<https://github.com/jossi87/climbing-ws>)
- Install Tomcat server: File -> New -> Other -> Tomcat v10.1 Server

### Configuration

- Create a file named `buldreinfo.properties` in `src/main/resources/com/buldreinfo/jersey/jaxb/config/`. 
- You can use `buldreinfo.properties.example` as a template.
- This file is ignored by Git to prevent accidental leaks of secrets.
- All values are required for the server to run.

### Sidecar Services

This project relies on the following internal microservices:

* **[climbing-leaflet-renderer](https://github.com/jossi87/climbing-leaflet-renderer)**: 
  Handles Puppeteer-based map rendering for PDF and image exports.

### Run Project

- Right click on "com.buldreinfo.jersey.jaxb" in Project Explorer and choose "Run as" -> "Run on server" to start web service.

### Testing

You should now be able to access the server on:

- <http://localhost:8080/com.buldreinfo.jersey.jaxb/v2/meta>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/openapi.json>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/application.wadl>

<!-- Contact -->

## :handshake: Contact

- Jostein Oeygarden (<jostein.oygarden@gmail.com>)
- Project Link: <https://github.com/jossi87/climbing-ws>

