# Formulon

![Formulon software logo](/media/logo.svg)

## **About the Project**

**Formulon** is a tiny (<2000 LoC) form builder and storage service with a
(future) focus on privacy.
It provides HTTP REST apis to edit a blueprint of your form and to receive
and validate the answers.
Conditional branching in the form is supported but not yet tested in real
conditions.
It is expected that Formulon will support plugins to add new form field types
with the built-in ones to be :

- text field
- multiple choice field
- single choice field

At moment of writting, this software is not production ready.
I am working on this on my spare time, so it will release when it's ready.

## **Table of Contents**
- [About the Project](#about-the-project)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
- [Usage](#usage)
- [License](#license)

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
- [X] store form answers related to the schema version
- [ ] authorization
- [ ] plugins and plugin advertisement to clients

## **Getting Started**

### **Prerequisites**

- [SBT](https://www.scala-sbt.org/)
- A running PostgreSQL database.

### **Installation**

```bash
# Clone the repository
git clone https://github.com/yourusername/yourproject.git

# Navigate into the project directory
cd yourproject

# Assemble a fatjar
sbt assembly
```

At this point you should be able to move the jar from `target/scala2.13` to your classpath.

Finally, run all migration scripts from `0-*.sql` (no scripts yet for that).

## **Usage**

```bash
Formulon

An HTTP server that allows the creation and serving of dynamic forms.

USAGE: formulon [OPTIONS]

OPTIONS:
--port PORT         Port to listen from (default 8080)
--ip IP             Ipv4 to listen from (default 0.0.0.0)
--db-user USER      The DBMS user to connect as (default "formulon")
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

