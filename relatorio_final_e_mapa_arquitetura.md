# 🏆 Relatório Final de Validação e Mapa Mental de Arquitetura (CityFix)

Este documento atesta a **validação global do projeto** com base no teu planeamento interno (`plano_de_desenvolvimento_e_arquitetura_cityfix.md`) e cria o **Mapa Mental detalhado** para os requisitos do enunciado oficial da cadeira (`AMov.ProjetoKotlin_PT.md`).

---

## 🎯 1. Verificação do Plano de Desenvolvimento (O que falta rever?)

Foi feita uma inspeção a 100% do código do projeto. **Todas as funcionalidades de lógica pesada, hardware e Firebase estão concluídas e interligadas com sucesso.** 

Apenas deves fazer um "Double Check" nos seguintes pontos de pormenor antes de criares o ficheiro ZIP final:
- **Traduções (Idiomas):** Confirma se o ficheiro `res/values/strings.xml` está inteiramente em Inglês (como manda o enunciado) e o `res/values-pt-rPT/strings.xml` em Português. Vê se não há `Text("Hardcoded")` espalhados pelas views (ex: o `Toast` do Registo de Ocorrência que inserimos há pouco está em PT fixo, podes querer extrair para as `strings.xml`).
- **Acessibilidade (`contentDescription`):** Confirma se todos os teus `Icon` e `AsyncImage` têm descrições legíveis ou `null` se forem de decoração.
- **Limpeza do ZIP:** Antes de fechar o projeto e mandar para o Nónio, **não te esqueças** de apagar as pastas `/.gradle`, `/build`, `/app/build` e o ficheiro `local.properties`. 

---

## 🧠 2. Mapa Mental de Arquitetura (Mapeado com o Enunciado)

### 📌 Tópico I: Perfis de Utilizador e Gestão de Permissões
**Requisito do Enunciado:** Três perfis de acesso (Anónimo num raio de 10km, Munícipe Registado validado por NIF único, e Operador Municipal com domínio próprio). Autenticação MFA e imutabilidade do NIF/Nome no perfil.

* **Abordagem de Arquitetura:** Clean Architecture + Repository Pattern. Separação rígida entre a camada de apresentação e a injeção de lógicas de negócio e base de dados.
* **Ficheiros e Ligações:**
  * `domain/location/LocationHandler.kt` $\rightarrow$ Impede navegação do Anónimo calculando o Raio de 10km com a fórmula de *Haversine*.
  * `util/Validators.kt` $\rightarrow$ O `validateUniqueNif()` faz uma query em tempo real ao Firestore para impedir NIFs duplicados. O `validateOperatorEmail` bloqueia registos de Operador sem o domínio municipal.
  * `data/repository/AuthRepository.kt` $\rightarrow$ Gere o fluxo MFA, gravando `mfaCode` temporalmente com um `Timestamp` de 5 minutos, garantindo a Autenticação Duplo Fator (2FA).
  * `ui/screens/auth/ProfileEditScreen.kt` $\rightarrow$ Cumpre a política de dados fixos: *TextFields* do Nome, NIF e Georreferenciação bloqueados (`readOnly = true`, `enabled = false`).

---

### 📌 Tópico II: Ciclo de Vida do Ticket e Regras de Negócio
**Requisito do Enunciado:** Máquina de estados (Aberto, Info, Resolução, Resolvido, Encerrado). Agrupamento de 10m de ocorrências. Prazos estritos de 5 dias para reabrir (com justificação obrigatória). Fecho definitivo passados 5 dias.

* **Abordagem de Arquitetura:** Transações Seguras Firestore (ACID) + WorkManager para Schedulers (Lembretes) + Realtime Listeners.
* **Ficheiros e Ligações:**
  * `data/repository/OcorrenciaRespository.kt` (Linhas de agrupamento) $\rightarrow$ Usa o `firestore.runTransaction` para agregar submissões na mesma coordenada (`< 10m`), somando seguidores e anexos da mesma anomalia.
  * `data/repository/OcorrenciaRepository.kt` (Linhas da Máquina de Estados) $\rightarrow$ A função `reabrirOcorrencia` só prossegue se `estado == resolvido` e compara `resolvidoEm.toDate().time` com `System.currentTimeMillis()`. Se passarem 5 dias, a transação aborta e tranca para `encerrado`.
  * `data/worker/TicketClosingReminderWorker.kt` $\rightarrow$ Quando o Operador muda o ticket para *Resolvido*, agenda-se este *Worker* com `.setInitialDelay(4, TimeUnit.DAYS)` para enviar o alerta ao utilizador quando restarem apenas 24h.

---

### 📌 Tópico III: Sistema de Notificações
**Requisito do Enunciado:** Push (FCM) e locais para: mudança de estado, chat, proximidade da equipa e alertas 24h.

* **Abordagem de Arquitetura:** Android Notification Channels + Firebase Cloud Messaging Service + ViewModel State Observer.
* **Ficheiros e Ligações:**
  * `util/NotificationHelper.kt` $\rightarrow$ Classe *Factory* que cria os Canais de SO e pede a permissão `POST_NOTIFICATIONS` obrigatória do Android 13+.
  * `util/CityFixFirebaseMessagingService.kt` $\rightarrow$ Recetor de notificações Push (background).
  * `ui/viewmodel/TicketViewModel.kt` $\rightarrow$ Mantém uma cache `Map<String, String>` na memória. Sempre que o *SnapshotListener* do Firebase reage, compara a cache. Se o estado for diferente, dispara o alarme sonoro/visual no SO.

---

### 📌 Tópico IV: Persistência de Dados, Multimédia e Cloud (Firebase)
**Requisito do Enunciado:** Offline-first caching, compressão local de imagem/vídeo, envio via `WorkManager` obrigatório para anexos.

* **Abordagem de Arquitetura:** Offline-First Firestore + Background Workers Assíncronos com Constraint de Rede.
* **Ficheiros e Ligações:**
  * `CityFixApp.kt` $\rightarrow$ Ao ligar a app, declara-se `persistentCacheSettings {}` garantindo que toda a DB funciona offline (read/write pendentes).
  * `util/ImageCompressor.kt` $\rightarrow$ Corta os *Bitmaps* para tamanhos otimizados e gera os pequenos ficheiros `_thumb.jpg` e `_img.jpg` dentro do `Context.filesDir` (sem travar a interface principal usando Kotlin Coroutines `Dispatchers.IO`).
  * `data/worker/MediaUploadWorker.kt` $\rightarrow$ Lê as fotos locais e envia-as para o *Firebase Storage* unicamente sob a imposição `.setRequiredNetworkType(NetworkType.CONNECTED)`.

---

### 📌 Tópico V: Sensores de Hardware, Geofence e Acessibilidade (Chat & Mapas)
**Requisito do Enunciado:** Chat nativo, deteção vertical de buracos via acelerómetro e barreira GPS (Geofence) de 20 metros.

* **Abordagem de Arquitetura:** Event-Driven Sensors + Foreground/Background Location APIs.
* **Ficheiros e Ligações:**
  * `data/repository/ChatRepository.kt` $\rightarrow$ Garante a subcoleção `/mensagens` no Firebase. O fluxo temporal simula uma app de conversação e a função de enviar tem o bloqueio de segurança se a ocorrência estiver arquivada (`Encerrado`).
  * `domain/sensor/SensorImpactoManager.kt` $\rightarrow$ Regista o `Sensor.TYPE_ACCELEROMETER`, ouve as variações puras do **Eixo Z** (Saltos Verticais). Integrado ao *Lifecycle* da Compose View para poupar bateria (`ON_START` / `ON_STOP`).
  * `domain/location/GeofenceBroadcastReceiver.kt` $\rightarrow$ Pede a permissão vital `ACCESS_BACKGROUND_LOCATION`. O `GeofencingClient` ouve as aproximações do operador (20m) à obra e lança um Broadcast (Intent) para o sistema que, de forma invisível, altera a `proximidadeEm` no Firebase (alertando de seguida o munícipe).