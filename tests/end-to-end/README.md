# End-to-end tests

## Setting up the environment variables

Duplicate `local.properties.example` file from `test/resources` and rename the copy to `local.properties`

Setup properties:

| Property                 | Explanation                        |
|--------------------------|------------------------------------|
| MEDIATOR_OOB_URL         | Mediator OOB url invitation        |
| AGENT_URL                | Agent url                          |
| PUBLISHED_DID            | Existing published DID             |
| JWT_SCHEMA_GUID          | Existing JWT schema guid           |
| ANONCRED_DEFINITION_GUID | Existing Anoncred definition guid  |
| APIKEY                   | APIKEY header token authentication |

## Running the end-to-end tests

### Using CLI

Full regression

```bash
./gradlew test
```

Tagged scenario

```bash
./gradlew test -Dtags="@mytag and @anothertag"
```

### Using IntelliJ Integration

