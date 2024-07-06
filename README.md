# About

REST Web Service hosted on Tomcat.
Used in [https://github.com/jossi87/climbing-web](https://github.com/jossi87/climbing-web) as a gateway to a MySQL database.

<!-- Product -->

## :link: Product

- Bouldering: [buldreinfo.com](https://buldreinfo.com)
- Route climbing: [brattelinjer.no](https://brattelinjer.no)
- Ice climbing: [is.brattelinjer.no](https://is.brattelinjer.no)

<!-- License -->

## :warning: License

Distributed under the GNU GENERAL PUBLIC LICENSE (Version 3): https://brattelinjer.no/gpl-3.0.txt

<!-- Onboarding -->

## :rocket: Onboarding

### Database

- Download the databasedump: <https://github.com/jossi87/climbing-ws/blob/main/20240706.buldreinfo.no-data.sql>
- Download and run MySQL Installer (<https://dev.mysql.com/downloads/installer/>)
- Choose "Use Legacy Authentication Method (Retain MySQL 5.x Compability)"
- Run MySQL Command Line Client and paste in the contents of `init.sql`.

> **NOTE**: If you're using the command line, you can execute this directly:
>
> ```sh
> mysql -r root -pYOUR_PASSWORD -h 127.0.0.1 < init.sql
> ```

### Eclipse

- Download Eclipse Installer from (<https://www.eclipse.org/downloads/>)
- Choose "Eclipse IDE for Enterprise Java and Web Developers"
- "Checkout projects from Git" (<https://github.com/jossi87/climbing-ws>)
- Install Tomcat server: File -> New -> Other -> Tomcat v10.1 Server
- Edit buldreinfo.properties in "com.buldreinfo.jersey.jaxb.config" (all values are required for the server to run, just add wathever you want to "auth0.secret", "google.apikey" and "vegvesen.auth")

```sh
db.hostname=localhost
db.database=buldreinfo
db.username=root
db.password=YOUR_PASSWORD
auth0.secret=WhateverYouWant
google.apikey=WhateverYouWant
vegvesen.auth=WhateverYouWant
```

### Run Project

- Right click on "com.buldreinfo.jersey.jaxb" in Project Explorer and choose "Run as" -> "Run on server" to start web service.

### Testing

You should now be able to access the server on:

- <http://localhost:8080/com.buldreinfo.jersey.jaxb/v2/meta>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/openapi.json>
- <http://localhost:8080/com.buldreinfo.jersey.jaxb/application.wadl>

## Docker

If you don't want to install MySQL, and you _do_ have [docker] installed, then
you can use that instead:

```sh
docker compose up
```

This will download the necessary images and spin up a MySQL server, including a
storage volume for the database. The database will be initialized with a default
password of `brattelinjer` - **change this before deploying publicly**.

> **NOTE**: You will also need to make sure that the password you use is
> reflected in `buldreinfo.properties`.

[docker]: https://docker.com/

<!-- Contact -->

## :handshake: Contact

- Jostein Oeygarden (<jostein.oygarden@gmail.com>)
- Project Link: <https://github.com/jossi87/climbing-ws>
