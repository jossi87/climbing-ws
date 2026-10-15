# About

Spring Boot RESTful Web Service serving as the backend API for the Climbing/Bouldering platform.
Used by [https://github.com/jossi87/climbing-web](https://github.com/jossi87/climbing-web) as a gateway to a MySQL database.

## :link: Live sites

- Bouldering: [buldreinfo.com](https://buldreinfo.com)
- Route climbing: [brattelinjer.no](https://brattelinjer.no)
- Ice climbing: [is.brattelinjer.no](https://is.brattelinjer.no)

## :rocket: Onboarding

### 1. Database Setup (MySQL)

Import the [latest database dump](https://github.com/jossi87/climbing-ws/blob/main/climbing-db-no-data.sql) and initialize the development environment:
> ```sh
> CREATE DATABASE IF NOT EXISTS climbing;
> USE climbing;
> SET autocommit=0; SOURCE ./climbing-db-no-data.sql; COMMIT;
> INSERT INTO language (id, code, name) VALUES (1, 'en', 'English');
> INSERT INTO region (id, name, title, description, url, latitude, longitude, default_zoom, emails, language_id) VALUES (1, 'Dev1', 'Title', 'Description', 'http://localhost', 58.72, 6.62, 8, null, 1);
> INSERT INTO discipline (id, name, is_bouldering, is_rope_climbing, is_ice_climbing) VALUES (1, 'Rope climbing', 0, 1, 0);
> INSERT INTO type (id, discipline_id, type, subtype, is_bolted, trad_gear) VALUES (1, 1, 'Route', 'Sport', 1, 0);
> INSERT INTO type (id, discipline_id, type, subtype, is_bolted, trad_gear) VALUES (2, 1, 'Route', 'Trad', 0, 1);
> INSERT INTO region_type (region_id, type_id) VALUES (1, 1);
> INSERT INTO region_type (region_id, type_id) VALUES (1, 2);
> INSERT INTO map (id, name, url, attribution, max_zoom) VALUES (1, 'OpenStreetMap', 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', '&copy; OpenStreetMap', 19);
> INSERT INTO region_map (region_id, map_id, is_default) VALUES (1, 1, 1);
> INSERT INTO grade_system (id, name) VALUES (1, 'Climbing');
> INSERT INTO region_grade_system (region_id, grade_system_id) VALUES (1, 1);
> INSERT INTO grade_color (id, hex_code) VALUES (0, '#CCCCCC');
> INSERT INTO grade_color (id, hex_code) VALUES (6, '#FF0000');
> INSERT INTO grade (id, grade_system_id, grade, weight, label_major, label_compact, grade_color_id) VALUES (1, 1, 'n/a', 0, 'n/a', 'n/a', 0);
> INSERT INTO grade (id, grade_system_id, grade, weight, label_major, label_compact, grade_color_id) VALUES (2, 1, '8 (7b+)', 0, '8', '8', 6);
> COMMIT;
> ```

### 2. Dependencies
To enable map exports in the PDF generation, ensure the **[climbing-leaflet-renderer](https://github.com/jossi87/climbing-leaflet-renderer)** service is running and accessible.

### 3. Application Setup

1. **Configure:** Create `src/main/resources/application-dev.properties` by copying `application-dev.properties.example` and filling in your local secrets. This file is ignored by Git.
2. **Build & Run:** Build & Run: Run the main method in `Application.java` directly from your IDE. The `dev` profile is loaded automatically by default.

### :traffic_light: Testing

You should now be able to access the server on:

- <http://localhost:8080/meta>
- <http://localhost:8080/swagger-ui>
- <http://localhost:8080/v3/api-docs>

## :scroll: License

Distributed under the GNU GENERAL PUBLIC LICENSE (Version 3): https://brattelinjer.no/gpl-3.0.txt

## :handshake: Contact

- Jostein Oeygarden (<jostein.oygarden@gmail.com>)
- Project Link: <https://github.com/jossi87/climbing-ws>

