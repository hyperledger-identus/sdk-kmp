# End-to-end tests

## Setting up the environment variables

Copy `src/test/resources/example.properties` to `src/test/resources/local.properties` and fill in the values.

Required properties:

| Property                         | Explanation                                   |
|----------------------------------|-----------------------------------------------|
| MEDIATOR_OOB_URL                 | Mediator OOB invitation URL                   |
| AGENT_URL                        | Cloud agent base URL                          |
| SECP256K1_PUBLISHED_DID          | Existing published DID (secp256k1)            |
| SECP256K1_JWT_SCHEMA_GUID        | Existing JWT schema GUID (secp256k1)          |
| SECP256K1_ANONCRED_DEFINITION_GUID | Existing Anoncred definition GUID (secp256k1) |
| ED25519_PUBLISHED_DID            | Existing published DID (ed25519)              |
| ED25519_SCHEMA_GUID              | Existing schema GUID (ed25519)                |
| ED25519_ANONCRED_DEFINITION_GUID | Existing Anoncred definition GUID (ed25519)   |

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

#### Run via JUnit runner
Use the Serenity JUnit runner class in the IntelliJ run configuration:

1. Open `src/test/kotlin/org/hyperledger/identus/walletsdk/TestSuite.kt`.
2. Run it with **JUnit** (this class uses `@RunWith(CucumberWithSerenity::class)`).
3. If you created a Cucumber run configuration earlier, update the runner to `CucumberWithSerenity` by pointing it to this `TestSuite` class instead of the default Cucumber runner.

#### Run a single feature from IntelliJ
To run a feature file directly, configure the Cucumber run configuration to use the Serenity main class:

1. Right-click a feature file and select **Run...**, then **Edit Configuration**.
2. Set **Main class** to `net.serenitybdd.cucumber.cli.Main`.
3. Set **Glue** to `org.hyperledger.identus.walletsdk` (root package of the step definitions).
4. Apply and run the configuration.

Reference: [Running Cucumber with Serenity feature files directly from IntelliJ](https://johnfergusonsmart.com/running-cucumber-serenity-feature-files-directly-intellij/).
