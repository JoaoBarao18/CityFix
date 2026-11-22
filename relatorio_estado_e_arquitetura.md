# 📊 Relatório de Estado e Mapeamento Arquitetural - CityFix

Este documento cruza o planeamento original (`plano_de_desenvolvimento_e_arquitetura_cityfix.md`) com os requisitos do enunciado oficial (`AMov.ProjetoKotlin_PT.md`), apresentando o estado atual do projeto e o mapeamento mental da arquitetura implementada.

---

## Parte 1: Verificação do Plano de Desenvolvimento (O que falta fazer)

Analisando o código atual face às *Sprints* planeadas, o estado do projeto é o seguinte:

### ✅ Sprint 1: Fundação, Autenticação e Acesso Anónimo
**Estado:** **Concluído.**
* Repositórios, Firebase, Modelos de dados (`User`, `Ocorrencia`, `Category`, `Message`) criados.
* Validações de campos (`Validators.kt`), Login, Registo (Munícipe pendente / Operador ativo), Edição de Perfil e MFA implementados.
* Lógica de verificação do raio de 10 km (Visitante) e ecrãs do painel público funcional.

### ✅ Sprint 2: Criação de Tickets, Multimédia e Gestão de Categorias
**Estado:** **Concluído.**
* Painel de Gestão de Categorias e Validação de Utilizadores feito.
* Formulário de Criação de Ocorrência dinâmico implementado.
* `ImageCompressor.kt` a tratar da compressão/miniatura e `MediaUploadWorker` a fazer upload para o Firebase em background (com restrição de rede conectada).
* Lógica de agrupamento num raio de 10 metros integrada nas transações do Firestore.

### ⚠️ Sprint 3: Ciclo de Vida, Chat, Geofencing e Acelerómetro
**Estado:** **Parcialmente Concluído. (O QUE FALTA FAZER:)**
* **Acelerómetro (`ImpactSensorManager.kt`):** A lógica de leitura do eixo Z para detetar buracos e lançar a notificação/sugestão de criação de ocorrência tem de ser finalizada e ligada ao ciclo de vida da app (só se a opção estiver ativa no perfil).
* **Geofencing 20m (`GeofenceBroadcastReceiver.kt`):** Falta consolidar o registo da geofence no Android e garantir que, quando o Operador se aproxima a 20m, o campo `proximidadeEm` é efetivamente populado e os munícipes notificados corretamente.
* *(Concluído nesta Sprint: Transição de estados, Prazos de 5 dias de fecho/reabertura, Notificações FCM, ecrãs do Operador e Chat na ocorrência).*

### ❌ Sprint 4: Orientação, Acessibilidade e Entrega
**Estado:** **Por Iniciar. (O QUE FALTA FAZER:)**
* **Internacionalização (Idiomas):** Extrair qualquer texto perdido para o `res/values/strings.xml` e garantir a tradução no `values-pt/strings.xml`.
* **Testes de UI (Portrait/Landscape):** Rodar o emulador e garantir que todos os ecrãs usam `.verticalScroll` para não cortar conteúdo na horizontal.
* **Acessibilidade:** Garantir que todas as imagens/ícones têm `contentDescription` preenchido (ou `null` se forem decorativos).
* **Testes Offline:** Desligar a internet do emulador, criar uma ocorrência e validar o envio assim que a rede volta.
* **Limpeza e ZIP final:** Apagar diretórios `.gradle`, `build` antes da submissão e redigir o Relatório Técnico (PDF).

---

## Parte 2: Mapa Mental da Arquitetura por Tópico (Enunciado)

Abaixo encontra-se a estrutura em "Mapa Mental" descritivo para cada requisito exigido no enunciado (`AMov.ProjetoKotlin_PT.md`), indicando a linha/tópico correspondente no documento original, os ficheiros envolvidos, a arquitetura escolhida e a forma exata como foi implementada no nosso código.

### 1. Perfis de Utilizador e Acesso Georreferenciado (10 km)
* **Requisito do Enunciado:** *I – Perfis de Utilizador e Gestão de Permissões: 1. Munícipe Anónimo - Restrito a utilizadores localizados num raio de 10 Km, com centro na Pr. República.*
* **Arquitetura:** Clean Architecture (Domain Driven) + MVVM.
* **Ficheiros Principais:**
  * `domain/location/LocationHandler.kt` (Lógica de distância)
  * `ui/navigation/AppNavHost.kt` (Controlo de acesso)
  * `data/model/User.kt` (Modelo de dados)
* **Implementação:**
  * Utiliza o `LocationServices.getFusedLocationProviderClient` logo no arranque (`AppNavHost`). Verifica a permissão.
  * Usa a fórmula de Haversine (`estaDentroRaio` em `LocationHandler.kt`) para barrar o acesso se a distância do telemóvel à Praça da República for > 10 km. O estado da sessão (Autenticado, Operador, Munícipe) dita o ramo do Navigation Graph (`composable`) a ser renderizado.

### 2. Autenticação, MFA e Registo com Validação Camarária
* **Requisito do Enunciado:** *I – Perfis de Utilizador (Munícipe Registado e Operador Municipal): A. Registo (pendente até validação dos serviços camarários pelo NIF para o Munícipe, ativo logo para o Operador). B. Autenticação com duplo fator (MFA).*
* **Arquitetura:** Repository Pattern (Separação entre UI e Lógica de DB).
* **Ficheiros Principais:**
  * `ui/screens/auth/LoginScreen.kt`, `RegisterScreen.kt` (UI / Stateless)
  * `ui/viewmodel/AuthViewModel.kt` (Stateful / StateFlow)
  * `data/repository/AuthRepository.kt`, `UserRepository.kt` (Lógica Firebase)
* **Implementação:**
  * O `AuthRepository.initiateLogin` não permite entrada direta: gera um código MFA de 6 dígitos, atualiza o documento com um `Timestamp` de expiração (5 min) e dispara notificação local.
  * O `UserRepository.validateMunicipe` é chamado pelo Operador no ecrã `ValidateUsersScreen`, que altera a *flag* `validado = true` para aprovar a conta.

### 3. Máquina de Estados e Ciclo de Vida do Ticket (5 Dias)
* **Requisito do Enunciado:** *II – Ciclo de Vida do Ticket e Regras de Negócio: Transição de estados (Aberto, Pedido de Informações, Em Resolução, Resolvido, Encerrado). Munícipe pode reabrir durante 5 dias. Após 5 dias, é bloqueado e encerrado.*
* **Arquitetura:** Mapeamento de Transações Firestore + Schedulers.
* **Ficheiros Principais:**
  * `data/repository/OcorrenciaRepository.kt`
  * `data/worker/TicketClosingReminderWorker.kt` (WorkManager)
* **Implementação:**
  * Os tickets mudam de estado em `OcorrenciaRepository.atualizarEstadoOperador`. Cada mudança guarda no Firebase o evento no array `historicoEstados`.
  * Ao passar a *Resolvido*, agenda-se o `TicketClosingReminderWorker` com *delay* de 4 dias.
  * A função `reabrirOcorrencia` valida o `System.currentTimeMillis()` vs `resolvidoEm.toDate()`. Se decorrerem mais de 5 dias ($5 \times 24 \times 60 \times 60 \times 1000\text{ ms}$), rejeita a reabertura e tranca para *Encerrado*.

### 4. Multimédia, Compressão Local e Modo Offline (WorkManager)
* **Requisito do Enunciado:** *IV – Persistência de Dados: As imagens devem ser comprimidas localmente antes do envio. O carregamento de ficheiros pesados no Firebase Storage deve ser delegado no WorkManager, condicionado a ligação à rede.*
* **Arquitetura:** Background Processing (Asynchronous) + Local Cache.
* **Ficheiros Principais:**
  * `util/ImageCompressor.kt` (Utilitário de imagem)
  * `data/worker/MediaUploadWorker.kt` (Tarefa de Upload via WorkManager)
  * `ui/components/CapturaMultimedia.kt` (Seleção de Galeria/Câmara)
* **Implementação:**
  * O `ImageCompressor` ajusta as proporções e cria miniaturas quadradas em `context.filesDir`.
  * Em vez de carregar logo a foto, a função regista um `OneTimeWorkRequestBuilder<MediaUploadWorker>` e impõe a restrição `.setRequiredNetworkType(NetworkType.CONNECTED)`. Assim, a foto só sobe e o Firestore é atualizado quando a internet estiver estável e em segundo plano.

### 5. Deteção de Impactos (Acelerómetro)
* **Requisito do Enunciado:** *I – Perfis de Utilizador: F. Deteção de Impactos: Deteção de acelerações bruscas no eixo vertical sugerindo buracos no pavimento, emitindo sugestão imediata para abrir ocorrência.*
* **Arquitetura:** Event-Driven / Hardware Listeners.
* **Ficheiros Principais:**
  * `domain/sensor/SensorImpactoManager.kt`
  * `ui/screens/municipe/SugestaoImpacto.kt` (Popup / Snackbar)
* **Implementação:**
  * A classe ouve o `Sensor.TYPE_ACCELEROMETER` (Eixo Z/Vertical).
  * A UI `SugestaoImpacto` reage a picos anómalos de força gravítica e, verificando que o munícipe tem a flag `detecaoImpactos` a `true` no perfil, exibe um botão pop-up para ir direto à criação de ocorrência (Em curso na Sprint 3).

### 6. Geofencing (20 Metros) de Aviso de Aproximação
* **Requisito do Enunciado:** *I – Perfis de Utilizador: 2. Operador Municipal - Obs: Sempre que um Operador se aproximar a menos de 20 mt de raio de uma ocorrência ativa, os autores são alertados.*
* **Arquitetura:** Geofencing API + Broadcast Receiver.
* **Ficheiros Principais:**
  * `domain/location/GeofenceBroadcastReceiver.kt`
  * `data/repository/PainelOperacionalRepository.kt`
  * `ui/components/AvisoProximidade.kt`
* **Implementação:**
  * Quando a ocorrência passa a *Em Resolução*, regista-se uma geocerca no `GeofencingClient`.
  * Se o Operador municipal der trigger a `GEOFENCE_TRANSITION_ENTER` (raio de 20m), o `GeofenceBroadcastReceiver` corre em background e atualiza o `proximidadeEm = Timestamp.now()` na base de dados.
  * O ecrã do Munícipe (`AvisoProximidade.kt`) deteta esse Timestamp != null no Flow e exibe o alerta verde instantaneamente.

### 7. Sistema de Chat / Troca de Mensagens na Ocorrência
* **Requisito do Enunciado:** *I – Perfis de Utilizador: E. Gestão de ocorrências - Detalhe de uma ocorrência, com acesso a troca de mensagens com os serviços camarários.*
* **Arquitetura:** Realtime Database (Subcoleções Firestore).
* **Ficheiros Principais:**
  * `data/repository/ChatRepository.kt`
  * `ui/components/TicketChatSection.kt`
  * `data/model/Message.kt`
* **Implementação:**
  * As mensagens são documentos armazenados em `ocorrencias/{id}/mensagens`.
  * O `ChatRepository.getMensagensFlow` usa um `addSnapshotListener` ordenado pelo `enviadoEm` (Timestamp). O `TicketChatSection` atualiza-se em tempo real, num formato tipo WhatsApp. O envio é bloqueado se o estado da ocorrência for *Encerrado*.

### 8. Notificações Push (FCM) e Locais
* **Requisito do Enunciado:** *III – Sistema de Notificações: Suporte a notificações locais e remotas (Mudança de estado, mensagens de pedido, alerta 24 horas para fecho).*
* **Arquitetura:** Cloud Messaging Service + NotificationManagerCompat.
* **Ficheiros Principais:**
  * `util/NotificationHelper.kt` (Fábrica de Notificações Locais)
  * `util/CityFixFirebaseMessagingService.kt` (Recetor de Push)
  * `ui/viewmodel/TicketViewModel.kt`
* **Implementação:**
  * O `NotificationHelper` tem 3 canais de notificação (`Auth`, `Tickets`, `Alerts`) criados com `NotificationManagerCompat`.
  * O `TicketViewModel` compara o novo estado da ocorrência recebido do Firebase com o estado em cache; se detetar mudança de estado, invoca `showTicketStatusNotification`.
  * A aplicação regista o token `FCM` para envio centralizado de push, configurado no Android 13+ com pedido explicito da permissão `POST_NOTIFICATIONS`.
