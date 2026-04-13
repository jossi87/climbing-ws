# About

RESTful Java Web Service (Jersey/JAXB) serving as the API gateway for the Climbing/Bouldering platform.
Used by [https://github.com/jossi87/climbing-web](https://github.com/jossi87/climbing-web) as a gateway to a MySQL database.

## :link: Live sites

- Bouldering: [buldreinfo.com](https://buldreinfo.com)
- Route climbing: [brattelinjer.no](https://brattelinjer.no)
- Ice climbing: [is.brattelinjer.no](https://is.brattelinjer.no)

## :rocket: Onboarding

### 1. Database Setup (MySQL)

Import the [latest database dump](https://github.com/jossi87/climbing-ws/blob/main/20260304.climbing-no-data.sql) and initialize the development environment:
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

### 2. Dependencies
To enable map exports and PDF generation, ensure the **[climbing-leaflet-renderer](https://github.com/jossi87/climbing-leaflet-renderer)** service is running and accessible.

### 3. Docker Deployment

1. **Configure:** Create `src/main/resources/com/buldreinfo/jersey/jaxb/config/buldreinfo.properties` (use `.example` as a template).
2. **Build & Run:**
> ```sh
> docker build -t climbing-ws .
> docker run -d -p 8080:8080 --name climbing-ws climbing-ws
> ```

### :traffic_light: Testing

You should now be able to access the server on:

- <http://localhost:8080/com.buldreinfo.jersey.jaxb/v2/meta>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/openapi.json>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/application.wadl>

## :scroll: License

Distributed under the GNU GENERAL PUBLIC LICENSE (Version 3): https://brattelinjer.no/gpl-3.0.txt

## :handshake: Contact

- Jostein Oeygarden (<jostein.oygarden@gmail.com>)
- Project Link: <https://github.com/jossi87/climbing-ws>

