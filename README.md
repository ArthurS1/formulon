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
- [Skunk](https://typelevel.org/skunk/)
- [Cats](https://typelevel.org/cats/) & [Cats effect](https://typelevel.org/cats-effect/)
- [Circe](https://circe.github.io/circe/)

## **Features**

*temporary list of features to be implemented*

- [X] CRUD form schemas for administrators
- [X] maintain an history of the schema versions
- [X] serve the forms
- [ ] store form answers related to the schema version
- [ ] serve form answers to the responsible organization
- [ ] make sure an organization can only edit their schema
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
Formulon

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: formulon [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--db-user USER      The DBMS user to connect as (default "form")
--db-password PASS  The password for this DBMS user (default "test")
--db-host HOST      The host of the DBMS (default "localhost")
--db-port PASS      The port of the DBMS (default "5432")
--db-database DB    The database to connect to (default "formulon")
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

