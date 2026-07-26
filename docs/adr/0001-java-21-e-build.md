# ADR-0001 — Java 21, JDK 25 de compilação e ajustes de build

- **Status:** aceito
- **Data:** 2026-07-18
- **Fase:** 1 (Fundação)

## Contexto

O plano (`inicial.md`) fixa **Java 21 (LTS)** como linguagem-alvo, por causa de
Spring Boot 3, `ProblemDetail`, Virtual Threads e Micronaut 4. A máquina de
desenvolvimento tem **JDK 25 (Temurin LTS)** instalado, além de Maven 3.9.16 e
Docker 29.

## Decisão

1. **Linguagem-alvo Java 21**, compilando com o JDK 25 via
   `<maven.compiler.release>21</maven.compiler.release>`. Garante bytecode 21 e
   impede uso acidental de API mais nova, sem exigir instalar um segundo JDK.

2. Durante o setup surgiram três incompatibilidades do **JDK 23+** que exigiram
   configuração explícita — registradas aqui para não se repetirem nos próximos
   serviços:

   | Sintoma | Causa | Correção |
   |---|---|---|
   | Lombok não gera construtores (`variable not initialized`) | JDK 23+ desliga *annotation processing* implícito | `annotationProcessorPaths` com Lombok no `maven-compiler-plugin` (parent pom) |
   | ArchUnit: `Unsupported class file major version 69` | ASM antigo (ArchUnit 1.3.0) não lê bytecode do Java 25 | ArchUnit **1.4.1** |
   | Testes `*IT` não executavam | Spring Boot parent só gerencia o failsafe, não o vincula | `maven-failsafe-plugin` com execuções `integration-test`+`verify` (parent pom) |

3. **Desvio consciente do plano — ArchUnit sem `@ArchTest`.** O plano mostra
   `@ArchTest` + `@AnalyzeClasses`. Nesse ambiente o *engine* do
   `archunit-junit5` **não é descoberto pelo surefire**: as regras rodavam
   **0 vezes e o build passava** — pior que não ter regra. Adotamos o artefato
   `archunit` (core) com regras como `@Test` do JUnit chamando `.check()`
   explicitamente, que o Jupiter executa de forma confiável. Verificado
   injetando uma violação proposital e confirmando `BUILD FAILURE`.

## Consequências

- Um único JDK (25) compila para o alvo 21. Se algum dia o alvo precisar mudar,
  basta a propriedade `maven.compiler.release`.
- Os três ajustes de build vivem no **parent pom**, então cada novo serviço os
  herda de graça.
- A intenção da regra de arquitetura (domínio isolado de framework/camadas)
  está garantida e testada; apenas a mecânica difere do plano.
