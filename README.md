# DevOps Calculator Web — JUnit Starter

This is the **starting version** for the progressive DevOps Fundamentals lab.

## Included at the start
- Existing Java Calculator Web application and JUnit 5 tests
- Maven WAR packaging
- Jenkins pipeline with Checkout, Verify Tools, Build and Test, Verify WAR
- Jenkins publishes JUnit test results and archives the WAR file

## Deliberately not configured yet
- SonarQube / JaCoCo coverage reporting
- Nexus repository publishing
- EC2/Tomcat deployment
- Ansible deployment

Students will add those capabilities one stage at a time during the lab.

## Requirements
- Java 21
- Maven 3.x
- Jenkins with Pipeline, Git, JUnit, and artifact archiving support

## Local verification
From the project root, run:

```bash
mvn -B clean verify
```

Expected WAR output:

```text
target/calculator.war
```

## Important
The Jenkinsfile's `JAVA_HOME` is set for the lab Jenkins agent environment. If your agent uses a different Java 21 installation, update that path or configure Java through Jenkins Global Tool Configuration.
vanakkam csk vs 