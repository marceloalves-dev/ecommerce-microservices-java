# ADR-0002 — Endurecimento da criação e leitura de pedidos

- **Status:** aceito
- **Data:** 2026-07-26
- **Fase:** 1 (Fundação)

## Contexto

O CRUD inicial era suficiente para demonstrar as camadas, mas tinha quatro riscos
antes da introdução de mensageria e múltiplas réplicas:

1. `findAll()` materializava todos os pedidos e seus itens.
2. Retry do `POST` criava outro UUID e duplicava o pedido.
3. SKU repetido e valores maiores que o schema eram rejeitados tarde pelo banco.
4. Eventos concorrentes futuros poderiam sobrescrever o estado da saga.

O request também informava `unitPrice`, permitindo que o chamador escolhesse o
valor cobrado.

## Decisão

1. A listagem usa `Slice`, tamanho máximo 100 e `OrderSummary`. Itens são carregados
   apenas no detalhe por ID.
2. `Idempotency-Key` é obrigatório. A tabela `order_idempotency` possui unicidade
   por cliente e chave. O primeiro request cria uma claim; `SELECT FOR UPDATE`
   serializa concorrentes, e claim e pedido são concluídos na mesma transação.
3. O hash canônico ignora somente a ordem das linhas. Reutilizar a chave com outro
   conteúdo retorna `409`.
4. O preço vem de `PricingPort`. Na Fase 1 o adapter é um catálogo local versionado;
   a API envia apenas SKU e quantidade. A moeda `BRL` é persistida no pedido.
5. A migration V2 adiciona limites e unicidades, `version` para optimistic locking,
   ordem estável das linhas e índices de paginação e timeout. A V1 permanece imutável.
6. O corpo HTTP e a quantidade de itens têm limites explícitos. Valores monetários
   são exatos em duas casas e precisam caber em `NUMERIC(19,2)`.
7. O Actuator usa porta separada; Prometheus está no classpath e a readiness inclui
   PostgreSQL. Virtual Threads ficam habilitadas, sem alterar os limites do pool JDBC.

## Consequências

- Retry seguro não depende de memória local e funciona com várias réplicas.
- A tabela de idempotência permanece enquanto o pedido existir; política de
  arquivamento será definida junto da retenção de eventos na Fase 3.
- O catálogo local não é um serviço de catálogo definitivo. Ele existe para não
  confiar em preço vindo do cliente e será substituído atrás do mesmo port.
- `customerId` ainda vem do request porque autenticação pertence à Fase 6. Antes de
  exposição pública, ele deverá vir do subject do JWT.
- Optimistic locking detecta concorrência; consumers futuros ainda deverão tratar
  eventos repetidos, tardios e conflitantes de forma explícita.
