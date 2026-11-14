# Testes do backend

### Executar e encerrar no IntelliJ IDEA

Em **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecione **IntelliJ IDEA** em **Build and run using**. Em seguida, execute a configuração **Application** da classe `com.mapa.MapaApiApplication`. Mantenha as variáveis de ambiente na configuração de execução.

Quando a execução é delegada ao Gradle, o botão **Stop** cancela a tarefa que mantém a API em execução. A mensagem `Build cancelled while executing task` corresponde a esse cancelamento. Ao executar a classe diretamente pelo IntelliJ, o encerramento deixa de cancelar uma tarefa Gradle.

## Suítes de teste

Execute os comandos na pasta do backend, com Java 21.

| Suíte | Fontes | Windows | Linux e GitHub Actions |
|---|---|---|---|
| Unitários | `src/test/java` | `./gradlew.bat test` | `./gradlew test` |
| Integração | `src/integrationTest/java` | `./gradlew.bat integrationTest` | `./gradlew integrationTest` |

Os testes que sobem o Spring, exercitam endpoints HTTP ou usam um banco real em memória ficam na suíte de integração. O perfil de teste usa H2 em memória e fica em `src/integrationTest/resources`.

A tarefa `check` executa as duas suítes e o Checkstyle. No GitHub Actions, as suítes e o Checkstyle rodam em jobs próprios. O Gradle gera os relatórios HTML em `build/reports/tests/test` e `build/reports/tests/integrationTest`.
