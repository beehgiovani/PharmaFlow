# PharmaFlow

Protótipo Android para explorar operações de delivery de farmácia em três perfis: cliente, administração e entregador. O projeto usa product flavors para compartilhar a base técnica e separar telas e navegação de cada público.

## Situação do projeto

O PharmaFlow é um projeto anterior ao FarmaDelivery e não está entre os destaques atuais do portfólio. Ele permanece público como referência de arquitetura Android e experimentação de fluxos. Não há garantia de operação comercial, integração com estoque real, pagamento processado ou publicação nas lojas.

## O que o código demonstra

- Kotlin e Jetpack Compose;
- product flavors `client`, `admin` e `motoboy`;
- Hilt para injeção de dependências;
- Firebase Auth, Firestore, Storage e Messaging;
- persistência local com Room;
- Retrofit para integrações HTTP;
- leitura de texto com ML Kit no fluxo administrativo;
- navegação, catálogo, carrinho, pedidos e telas operacionais.

## Ambiente e build

- Android Studio compatível com o projeto;
- Android SDK 36;
- JDK indicado pela versão atual do Android Gradle Plugin;
- configuração Firebase própria para cada uso.

Exemplos de build:

```powershell
.\gradlew.bat assembleClientDebug
.\gradlew.bat assembleAdminDebug
.\gradlew.bat assembleMotoboyDebug
```

Os nomes exatos das tarefas podem variar conforme a combinação atual de flavors e build types; use `.\gradlew.bat tasks` para conferir.

## Limites

O repositório contém código de protótipo e integrações que precisam ser configuradas e auditadas. Dados de clientes, pedidos e pagamentos exigem regras de acesso, proteção de segredos, observância à LGPD e testes antes de qualquer uso real.
