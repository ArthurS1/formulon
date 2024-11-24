# Formulon

## **Table of Contents**
- [About the Project](#about-the-project)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Usage](#usage)
- [License](#license)

## **About the Project**

**Formulon** is a form builder and storage service.
In summary, it is a backend service that allows administrators to build forms with conditional logic and users to fill those forms before saving them.
This project originates from the need of at home services to have way for their customers to request their services in a customizable way.
It is provided under a non-commercial license meaning it can only be used for educational, research, or personal purposes.

### **Tech stack overview**

- Scala 2
- [Http4s](https://http4s.org/)
- [Slick](https://scala-slick.org/)
- [Cats](https://typelevel.org/cats/) & [Cats effect](https://typelevel.org/cats-effect/)
- [Circe](https://circe.github.io/circe/)

## **Features**

*temporary list of features to be implemented*

- [ ] CRUD form schemas for administrators
- [ ] maintain an history of the schema versions
- [ ] make sure an organization can only edit their schema
- [ ] serve the forms
- [ ] store form answers related to the schema version
- [ ] serve form answers to the responsible organization
- [ ] advertise the different kinds of schema blocks available

## **Getting Started**

### **Prerequisites**

- [SBT](https://www.scala-sbt.org/)
- A running PostgreSQL database. (migration scripts not done yet)

### **Installation**

```bash
# Clone the repository
git clone https://github.com/yourusername/yourproject.git

# Navigate into the project directory
cd yourproject

# Assemble a fatjar
sbt assembly
```

Then you should just be able to move the jar from `target/scala2.13` to your classpath.

## **Usage**

```bash
Form service

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: form-service [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--jdbcurl URL       The JDBC url to connect to the postgres database (default jdbc:postgresql://localhost/konexii)
--db-user USER      The database user to connect as (default "form")
--db-password PASS  The password for this database user (default "test")
--help or -h        Shows this message
```

Example:

```bash
java -jar formulon.jar
```

## **License**

This project is licensed under a **Non-Commercial License**.
You are free to:
- Use, copy, and modify the project for **personal, educational, or research purposes**.

You are not allowed to:
- Use this project or its derivatives for **commercial purposes**.

See the [LICENSE](LICENSE) file for full details.

