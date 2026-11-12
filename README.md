# Testes do backend

Execute os comandos na pasta do backend, com Java 21.

| Suíte | Fontes | Windows | Linux e GitHub Actions |
|---|---|---|---|
| Unitários | `src/test/java` | `./gradlew.bat test` | `./gradlew test` |
| Integração | `src/integrationTest/java` | `./gradlew.bat integrationTest` | `./gradlew integrationTest` |

Os testes que sobem o Spring, exercitam endpoints HTTP ou usam um banco real em memória ficam na suíte de integração. O perfil de teste usa H2 em memória e fica em `src/integrationTest/resources`.

A tarefa `check` executa as duas suítes e o Checkstyle. No GitHub Actions, as suítes e o Checkstyle rodam em jobs próprios. O Gradle gera os relatórios HTML em `build/reports/tests/test` e `build/reports/tests/integrationTest`.
