# План реализации: Dynasty-адаптер (Turun yliopisto) для async-архивирования

Исполнитель: SOL LLM. Источники: `Elsa Dynasty arkisto 20261001.1.docx` (REST API, примеры вызовов),
`ELSA-Dynasty arkisto mäppäystaulukko 25.5.2026.xlsx` (маппинг полей). Оба файла лежат в корне репозитория
и НЕ должны коммититься (они untracked; не добавлять в git).

Правила для исполнителя (из AGENTS.md): не создавать ветки, не коммитить/пушить, не запускать Cypress,
не писать в БД кроме тестовых. Ответы пользователю — на русском.

---

## 0. Ключевое отличие от Helsinki/Tampere

| | Helsinki (Siilo) / Tampere (Louhi) | Turku (Dynasty) |
|---|---|---|
| Транспорт | один ZIP (SÄHKE XML) одним запросом | **5+ REST-вызовов** в строгом порядке |
| Маппинг | во внешней системе | **в Elsa** (поля AD, schemaId, tosClass и т.д.) |
| Идемпотентность | невозможна (задокументированное ограничение) | **обязательна**: Dynasty API не принимает idempotency key, поэтому её надо реализовать на нашей стороне через журнал шагов (step ledger) |

Последовательность для одного `ArkistointiJob`:

1. `POST /oauth token` (client_credentials) — кешируется, не является шагом.
2. `CASE` — `POST {base}/api/v1/cases?tosClass=…` → id дела (целое, напр. `415`).
3. `TASK` — `POST {base}/api/v1/cases/{caseId}/tasks` → GUID фазы (строка в кавычках).
4. `DOCUMENT:<jarjestys>` — `POST {base}/api/v1/documents` (multipart) для главного документа → id документа (`1078`).
5. `DOCUMENT:<jarjestys>` — тот же endpoint для каждого вложения с `parentDocumentSelector` → id (`1079`).
6. `ARCHIVE_CASE` — `POST {base}/api/v1/cases/{caseId}/state/Archived` (пустое тело) → 200.
7. Адаптер возвращает `ArkistointiResult.Success(externalId = caseId)`.

После шага 6 в дело нельзя добавлять документы — порядок жёсткий.

---

## 1. Спецификация Dynasty API (извлечено из docx)

### 1.1 Аутентификация
- `POST {tokenUrl}` (пример: `https://asta-test.utu.fi/dynasty.identityserver/connect/token`),
  `Content-Type: application/x-www-form-urlencoded`, поля:
  `grant_type=client_credentials`, `client_id`, `client_secret`, `scope=external_interface_api`.
- Ответ JSON: `access_token`, `expires_in` (900 = 15 мин), `token_type: Bearer`, `scope`.
- Каждый API-вызов: `Authorization: Bearer <token>`.

### 1.2 Общие заголовки
- `Organization: <значение>` — во ВСЕХ вызовах, одно и то же значение (в тестовой среде `0`), выдаётся отдельно → конфиг/секрет.
- Base URL (тест): `https://asta-test.utu.fi/dynasty.externalinterface.archive.oauth`.

### 1.3 Создание дела (asia)
`POST /api/v1/cases?tosClass=05.06.02.04`, `Content-Type: application/json`, `Accept: text/plain`
```json
{
  "caseModel": {
    "schemaid": "30e1cd27-54f9-44c1-9853-401cbdc871de",
    "title": "EL Valmistumisen yhteenveto Maija Meikäläinen 28.11.2025",
    "publicityClassId": "J",
    "publicityClassName": "Julkinen",
    "manner": "M",
    "AD": {
      "UTUetunimi": "…", "UTUsukunimi": "…", "UTUsyntymaaika": "1979-01-01",
      "UTUtiedekunta": "Lääketieteellinen tiedekunta",
      "UTUkoulutusohjelma": "Erikoislääkärikoulutus", "UTUpaaaine": "Akuuttilääketiede"
    }
  }
}
```
Ответ 200, тело — голое число: `415`. Внимание: ключ `schemaid` в нижнем регистре (в документе — `schemaId`).

### 1.4 Создание фазы (käsittelyvaihe)
`POST /api/v1/cases/{caseId}/tasks`, body `{"title": "Hakeminen"}` → 200, тело `"5ed60e45-3d47-4e2d-a76c-5a22a105a85b"` (JSON-строка в кавычках).

### 1.5 Создание документа (asiakirja)
`POST /api/v1/documents`, `multipart/form-data`, три части (имена как в swagger curl):
- `MetaData` — JSON-текст:
```json
{
  "title": "…", "publicityClassId": "J", "publicityClassName": "Julkinen",
  "personalData": "Sisältää henkilötietoja",
  "documentType": "Valmistumisen yhteenveto, EL, EHL tai YEK",
  "schemaId": "b11af6ae-afc6-42e8-b09b-6b37d3f29397",
  "state": 3,
  "retentionPeriodRule": "Asian lopullinen ratkaisu",
  "retentionPeriodLength": 20,
  "AD": { "UTUetunimi": "…", "...": "…" }
}
```
- `File` — бинарный файл (filename + content type, напр. `application/pdf`).
- `CaseAttachmentOptions` — JSON-текст:
  - главный документ: `{"CaseSelector":{"Id":"415"},"PhaseSelector":{"Id":"<taskGuid>"}}`
  - вложение: `{"caseSelector":{"id":415},"parentDocumentSelector":{"id":1078}}`
Ответ 200, тело — число (id документа).

### 1.6 Архивирование дела
`POST /api/v1/cases/{caseId}/state/Archived`, пустое тело, `Accept: application/json` → 200.
Переводит дело, фазы и документы в состояние Arkistoitu. Возможные состояния: `Received`, `Archived`.

### 1.7 Env-специфичные значения
| | Тест | Прод |
|---|---|---|
| Case schemaid | `30e1cd27-54f9-44c1-9853-401cbdc871de` | `80cdec85-07fa-455b-9d7d-6c08ac1e2863` |
| Document schemaId | `b11af6ae-afc6-42e8-b09b-6b37d3f29397` | `f9352666-327a-4262-b888-9c4c847590b6` |
| Aineisto | IB13 | IB20 (задаётся правами учётки интеграции, в API не передаётся) |

---

## 2. Маппинг полей (из xlsx)

Обозначения: `FN`=user.firstName, `LN`=user.lastName, `D(x)`=дата в заголовке (формат `d.M.yyyy`, см. открытые вопросы),
ISO = `yyyy-MM-dd` для AD-полей-дат.

Производные значения:
- `lyhenne`: `YEK`, если `erikoisala.id == YEK_ERIKOISALA_ID`; `EHL`, если `erikoisala.tyyppi == HAMMASLAAKETIEDE`; иначе `EL`. (LISE в Elsa нет.)
- `koulutusohjelma`: EL → `Erikoislääkärikoulutus`, EHL → `Erikoishammaslääkärikoulutus`, YEK → `Yleislääketieteen erityiskoulutus`.
- `tiedekunta`: константа `Lääketieteellinen tiedekunta` (в листе 5 опечатка «Lääketieteelinen» — использовать правильное написание).
- `paaaine`: `erikoisala.nimi`.
- `alkamispaiva`: `opintooikeus.opintooikeudenMyontamispaiva`.
- `tutkintoasetus`: из `opintooikeus.asetus.nimi` (значения в БД: `678/1998`, `1343/2002`, `401/2007`, `56/2015`, `55/2020`) → текст через конфиг-карту (см. открытые вопросы), fallback `"Asetus <nimi>"`.
- `laillistamispaiva`: `erikoistuvaLaakari.laillistamispaiva`.
- `valmistumispaiva` / `hyvaksymispaiva`: `approvalDate` из payload (= ELSA hyväksymispäivä).

### 2.1 VALMISTUMINEN (лист 1–3)
**Case** (`tosClass=05.06.02.04`, title `"{lyhenne} Valmistumisen yhteenveto {FN} {LN} {D(approvalDate)}"`,
`publicityClassId=J`, `publicityClassName=Julkinen`, `manner=M`), AD:
`UTUetunimi, UTUsukunimi, UTUsyntymaaika, UTUtiedekunta, UTUkoulutusohjelma, UTUpaaaine`.

**Task**: `Hakeminen`.

**Документы** (порядок = `jarjestysnumero`, проверять ровно `[YHTEENVETO, LIITE, LAILLISTAMISTODISTUS]`):

| RecordType | Роль | title | documentType | retention | AD |
|---|---|---|---|---|---|
| YHTEENVETO | главный (Phase) | `{lyhenne} Valmistumisen yhteenveto {FN} {LN} {D(approval)}` | `Valmistumisen yhteenveto, EL, EHL tai YEK` | 20 | etunimi, sukunimi, syntymaaika, tiedekunta, koulutusohjelma, paaaine, alkamispaiva, valmistumispaiva, tutkintoasetus, laillistamispaiva |
| LIITE | вложение к YHTEENVETO | `Valmistumisen yhteenvedon liitteet {FN} {LN} {D(approval)}` | `Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK` | 10 | etunimi, sukunimi, tiedekunta, koulutusohjelma, paaaine |
| LAILLISTAMISTODISTUS | вложение к YHTEENVETO | `Laillistustodistus {FN} {LN}` | `Valmistumisen yhteenvedon liitteet, EL, EHL tai YEK` | 10 | etunimi, sukunimi, tiedekunta, koulutusohjelma, paaaine, laillistamispaiva |

Общее для всех документов: `publicityClassId=J`, `publicityClassName=Julkinen`, `personalData=Sisältää henkilötietoja`,
`state=3`, `retentionPeriodRule=Asian lopullinen ratkaisu`, `schemaId=<document schema>`.

### 2.2 KOEJAKSO (лист 4–5)
**Case**: `tosClass=05.06.02.00`, title `"{lyhenne} Koejakson vastuuhenkilön arvio {FN} {LN} {D(approvalDate)}"`
(в xlsx «Koejaksojen…» в одной ячейке и «Koejakson…» в примере — использовать «Koejakson»), J/Julkinen, `manner=M`,
AD как у valmistuminen case.

**Task**: `Arviointi`.

**Документ** ARVIOINTI (главный, Phase): title как у case; `documentType=Koejakson vastuuhenkilön arvio`;
`publicity = Osittain salassa pidettävä` (id — открытый вопрос); `personalData=Sisältää henkilötietoja`;
`state=3`; `retentionPeriodLength=-1` (pysyvä); `retentionPeriodRule=Asiakirjan päivämäärä`;
Salassapitoperuste `JulkL24.1§30, oppilashuolto, opetuksesta vapauttaminen, koesuoritukset, sanallinen arviointi`,
Salassapitoaika `100`, Salassapitoajan laskentaperuste `Asiakirjan päivämäärä` (имена JSON-полей неизвестны — открытый вопрос);
AD: etunimi, sukunimi, syntymaaika, tiedekunta, koulutusohjelma, paaaine, alkamispaiva, **UTUhyvaksymispaiva**, tutkintoasetus.

### 2.3 SOPIMUS
В спецификации Dynasty отсутствует → адаптер НЕ поддерживает (`supportedCaseTypes = {VALMISTUMINEN, KOEJAKSO}`),
конфиг sopimus для Turku не добавлять.

### 2.4 Поля, которые в API не передаются
Tunniste (asiatunnus), Tehtäväluokka (через `tosClass`), Aineisto, Avaustapa (через `manner`), Henkilötietoluonteen peruste,
Omistaja, Avauspäivä/Laadittu, Säilytysajan peruste, Asiakirjatyyppi (Yhteenveto/Liite/Lausunto) — ожидается, что Dynasty
заполнит их через TOS-обогащение (`tosClass`) и schema. Проверить в тестовой среде (см. открытые вопросы).

---

## 3. Изменения payload (snapshot на момент утверждения)

Payload иммутабелен (триггер `prevent_arkistointi_job_id_change`) и воркер не должен читать мутабельные сущности.
Значит, все недостающие поля надо положить в payload при создании job.

### 3.1 `ValmistuminenArkistointiPayload` — добавить (nullable, чтобы не ломать HKI/TRE):
- `specialtyType: ErikoisalaTyyppi?` (`studyRight.erikoisala?.tyyppi`)
- `studyRightStartDate: LocalDate?` (`opintooikeudenMyontamispaiva`, `@JsonFormat(STRING)`)
- `degreeRegulation: String?` (`studyRight.asetus?.nimi`)
- `licensingDate: LocalDate?` (`trainee.laillistamispaiva`)

### 3.2 `KoejaksoArkistointiPayload` — добавить: `specialtyType`, `studyRightStartDate`, `degreeRegulation`.
Порядок позиционных аргументов в `create(...)` изменится — обновить вызовы в `KoejaksoArkistointiService` и тесты.

### 3.3 Совместимость
- `SahkePayloadReader` читает только известные поля → HKI/TRE не затронуты. Проверить тесты, которые сравнивают payload JSON целиком
  (`AsyncArchiveItSupport`, `ArkistointiDocumentAssertions`, `*TransactionIT`, Cypress `assertArchiveProcessed` — только проверить, не запускать Cypress).
- Старые Turku/UEF job'ы без новых полей → Dynasty-ридер выдаёт `DYNASTY_PAYLOAD_INVALID` (permanent). Turku/UEF выключены
  (`kaytossa: false`), поэтому в prod их быть не должно. В README дать SELECT для проверки перед включением:
  `select id, case_type, status from arkistointi_job where university in ('TURUN_YLIOPISTO') and status <> 'LAHETETTY';`

---

## 4. Идемпотентность: журнал шагов (step ledger)

### 4.1 Проблема
`POST /cases` и `POST /documents` не идемпотентны. db-scheduler даёт at-least-once: повтор после таймаута, падения пода,
dead-execution recovery (старая execution может ещё быть в сетевом вызове, когда стартует новая). Без журнала каждый повтор
создаст новое дело/документ.

### 4.2 Новая таблица (Liquibase changeset, новый файл `20261101090000_added_arkistointi_dynasty_step.xml`, подключить в `master.xml` после `20261018090000_archive_document_snapshots.xml`)

`arkistointi_job_dynasty_vaihe` (или англ. `arkistointi_dynasty_step` — выбрать финское имя по стилю репо):
| колонка | тип | примечание |
|---|---|---|
| id | bigint identity (ALWAYS) | PK |
| job_id | uuid not null | FK → arkistointi_job(id) |
| step_key | varchar(50) not null | `CASE`, `TASK`, `DOCUMENT:0`, `DOCUMENT:1`, …, `ARCHIVE_CASE` |
| status | varchar(20) not null | `ALOITETTU` (STARTED) / `VALMIS` (COMPLETED) |
| remote_id | varchar(255) null | id из Dynasty |
| execution_version | bigint not null | версия scheduled_tasks, начавшая шаг |
| started_at | timestamp not null | |
| completed_at | timestamp null | |

Ограничения: `unique(job_id, step_key)`; `check (status in (...))`; `check ((status='VALMIS') = (remote_id is not null or step_key='ARCHIVE_CASE'))`;
индекс по `job_id`. Триггер: запрещать изменение `remote_id` у `VALMIS` и переход `VALMIS → *` (факт о внешнем мире неизменен);
DELETE разрешён только для `ALOITETTU` (сброс маркера при гарантированно неотправленном запросе / ручном разрешении).

JPA-сущность + `@Repository` в `domain/arkistointi` и `repository/arkistointi` по образцу `ArkistointiJobAsiakirja`.

### 4.3 Протокол каждого шага (сервис `DynastyStepLedger`, каждый метод — `@Transactional(REQUIRES_NEW)`)

`begin(execution, jobId, stepKey): StepDecision`
1. Заблокировать `scheduled_tasks` (task_name, task_instance, version, picked=true) `FOR UPDATE`, затем `arkistointi_job` `FOR UPDATE`
   — **тот же порядок блокировок, что и `ArkistointiJobStateService.lockExecution`** (вынести/переиспользовать как общий
   компонент, напр. `ArkistointiExecutionLock`). Нет строки → `StaleArkistointiExecutionException` (не вызывать сеть).
   Статус job ≠ `KASITTELYSSA` → stale.
2. Прочитать строку шага:
   - `VALMIS` → `AlreadyDone(remoteId)` — сетевой вызов пропускается.
   - `ALOITETTU` и `started_at` моложе `inProgressGrace` (конфиг, по умолчанию `2 × callTimeout + 1m`) →
     `InProgress` → адаптер возвращает `RetryableFailure(DYNASTY_STEP_IN_PROGRESS)`.
   - `ALOITETTU` старше grace → `Unconfirmed` → `PermanentFailure(DYNASTY_DELIVERY_UNCONFIRMED)` → job уходит в
     `VAATII_TOIMENPITEITA` (park). Не повторять автоматически — Dynasty мог уже создать объект.
   - нет строки → `INSERT … ALOITETTU, execution_version` (unique-ключ гарантирует атомарность при гонке:
     проигравший получает конфликт → перечитать → `InProgress`) → `Proceed`.

`complete(jobId, stepKey, executionVersion, remoteId)`
- `UPDATE … SET status='VALMIS', remote_id=?, completed_at=now() WHERE job_id=? AND step_key=? AND status='ALOITETTU'`.
  **Без** проверки версии scheduler'а: это факт о внешней системе, его нужно сохранить даже из «устаревшей» execution.
  Блокирует только строку шага — порядок блокировок не нарушается. 0 строк → лог + вернуть `Unconfirmed`.

`abandon(jobId, stepKey, executionVersion)`
- `DELETE … WHERE status='ALOITETTU' AND execution_version=?` — только когда запрос **гарантированно** не обработан Dynasty.

### 4.4 Классификация результатов HTTP для неидемпотентных шагов (CASE, TASK, DOCUMENT)
| Ситуация | Обработан ли Dynasty | Действие |
|---|---|---|
| 2xx + валидный id | да | `complete` → следующий шаг |
| 2xx + невалидный id | да, но id неизвестен | оставить `ALOITETTU`, `PermanentFailure(DYNASTY_RESPONSE_INVALID)` → park |
| `ConnectException`/`UnknownHostException`/TLS handshake (до отправки тела) | нет | `abandon`, `RetryableFailure(DYNASTY_UNAVAILABLE)` |
| 401 | нет | сбросить токен, 1 повтор этого же запроса; повторно 401 → `abandon`, `PermanentFailure(DYNASTY_AUTH_FAILED)` |
| 429, 503 | считаем «нет» (подтвердить у вендора) | `abandon`, `RetryableFailure(DYNASTY_HTTP_UNAVAILABLE)` |
| прочие 4xx (400/403/404/409/413/415/422) | нет | `abandon`, `PermanentFailure(DYNASTY_HTTP_REJECTED)` |
| 500/502/504, read timeout, call timeout, обрыв после отправки | **неизвестно** | оставить `ALOITETTU`, `PermanentFailure(DYNASTY_DELIVERY_UNCONFIRMED)` → park |

`ARCHIVE_CASE` — идемпотентен по смыслу (смена состояния). Для него неизвестный исход → `abandon` + `RetryableFailure`.
Если повторный вызов вернёт 4xx (дело уже Archived), и API позволяет `GET /api/v1/cases/{id}` — проверить состояние и
считать успехом; иначе park.

### 4.5 Поток адаптера (псевдокод)
```
arkistoi(submission):
  plan = DynastyRequestPlanner.plan(submission)   // ВСЯ валидация и построение DTO до первого сетевого вызова
  // ошибки маппинга/payload → PermanentFailure без сетевых вызовов (иначе останется «осиротевшее» дело)
  caseId  = step("CASE")        { client.createCase(plan.case) }
  taskId  = step("TASK")        { client.createTask(caseId, plan.taskTitle) }
  mainId  = step("DOCUMENT:0")  { client.createDocument(plan.main, file0, Phase(caseId, taskId)) }
  for doc in plan.attachments:
     step("DOCUMENT:${doc.jarjestys}") { client.createDocument(doc, file, Parent(caseId, mainId)) }
  step("ARCHIVE_CASE")          { client.archiveCase(caseId) }
  return Success(externalId = caseId)
```
Каждый `step` = `ledger.begin` → (если `Proceed`) HTTP → `complete`/`abandon` по таблице 4.4.
Перед каждым шагом `begin` заново проверяет версию scheduler'а — устаревшая execution останавливается между шагами.

### 4.6 Передача execution в адаптер
`ArkistointiSubmission` сейчас не содержит execution. Добавить поле `execution: ArkistointiJobExecution? = null`
(последним параметром, с default, чтобы не ломать существующие тесты/адаптеры) и заполнять его в
`ArkistointiJobProcessorImpl.processSubmission`. Не включать в `toString()`. Dynasty-адаптер требует non-null
(иначе `PermanentFailure(DYNASTY_CONFIGURATION_INVALID)`).
`StaleArkistointiExecutionException` из ledger должен пробрасываться адаптером как есть (processor уже его пробрасывает;
он `internal` в том же модуле — ок).

### 4.7 Ручное разрешение (ops)
Новый скрипт `ops/arkistointi/dynasty-resolve-step.sql` (psql, `-v job_id=… -v step_key=… -v remote_id=…`):
- работает только если job в `VAATII_TOIMENPITEITA` (проверка в WHERE, блокировки в порядке scheduled_tasks → job → step);
- режим «подтвердить»: `ALOITETTU → VALMIS` с `remote_id`, найденным оператором в UI Dynasty;
- режим «отбросить»: удалить `ALOITETTU` (оператор убедился, что в Dynasty объект не создан).
После этого — существующий `retry-job.sql`. Задокументировать в README (раздел про uusinta).
Скрипты только добавить в репо; исполнитель НЕ запускает их против реальных БД.

### 4.8 Остаточный риск (задокументировать в README)
Exactly-once невозможна без поддержки со стороны Dynasty. Ledger гарантирует: автоматические повторы никогда не создают
дубликаты; неоднозначный исход → park + ручное решение. Дальнейшее улучшение (фаза B): автоматическая сверка, если Dynasty
предоставит поиск по внешнему ключу (см. открытые вопросы) — интерфейс `DynastyReconciler` с реализацией по умолчанию
«не поддерживается → park».

---

## 5. HTTP-клиент `DynastyClient`

Пакет: `fi.elsapalvelu.elsa.service.arkistointi.dynasty`.
- OkHttp (как Siilo): `connectTimeout` 15s, `callTimeout`/`readTimeout` = `callTimeout` из конфига (default 2m),
  `retryOnConnectionFailure(false)`, `followRedirects(false)`, `followSslRedirects(false)`.
- HTTPS обязателен; HTTP только для литеральных loopback-адресов `127.0.0.1`/`::1` при
  `allow-loopback-http-for-tests=true` (скопировать проверку из `HelsinkiSiiloService.lahetaAsync`).
- Заголовки: `Authorization: Bearer`, `Organization`, `Accept` (`text/plain` для create, `application/json` для state).
- Сериализация: Jackson `ObjectMapper` с явными DTO (`DynastyCaseRequest(caseModel)`, `DynastyCaseModel` с `@JsonProperty("schemaid")`
  и `@JsonProperty("AD") ad: Map<String,String>` — `LinkedHashMap`, без null-значений; `DynastyDocumentMetadata` с
  `@JsonProperty("schemaId")`, `@JsonProperty("AD")`; `DynastyAttachmentOptions`). Даты AD — ISO `yyyy-MM-dd`.
- Multipart: `MultipartBody.Builder().setType(FORM)`, части `MetaData` (text), `CaseAttachmentOptions` (text),
  `File` (`filename`, `contentType` из snapshot, тело — `sisalto` read-only). Имена частей и регистр ключей JSON —
  ровно как в примерах docx; при первом ручном тесте на test-среде подтвердить.
- Парсинг ответа id: trim; если обёрнуто в `"` — снять; case/document id — положительный long; task id — UUID.
  Ограничить чтение тела (напр. 1 KB). Тело ответа никогда не логировать и не класть в ошибки.
- Исключение `DynastyTransferException(code, outcome: NOT_PROCESSED | REJECTED | UNKNOWN, httpStatus?)` по образцу
  `SiiloTransferException`.

### 5.1 Токен `DynastyTokenProvider`
- Потокобезопасный кеш (`ReentrantLock`/`@Synchronized`), обновлять, если до истечения < 60s; `expires_in` из ответа.
- `invalidate()` при 401 от API.
- Ошибки token endpoint: IO/5xx → `NOT_PROCESSED` retryable; 400/401 (`invalid_client`) → permanent `DYNASTY_AUTH_FAILED`.
- Секреты не логировать; `toString()` без секретов.

---

## 6. Конфигурация

### 6.1 `ApplicationProperties.Arkistointi`
Добавить класс `Dynasty` (переиспользуемый для Turku и в будущем UEF) и поле `val dynasty = Dynasty()` в `Turku`
(и в `Uef` — пусто, не используется до получения маппинга UEF):
```kotlin
class Dynasty {
    var baseUrl: String? = null
    var tokenUrl: String? = null
    var clientId: String? = null
    var clientSecret: String? = null
    var scope: String = "external_interface_api"
    var organization: String? = null
    var caseSchemaId: String? = null
    var documentSchemaId: String? = null
    var callTimeout: Duration = Duration.ofMinutes(2)
    var inProgressGrace: Duration = Duration.ofMinutes(5)
    var allowLoopbackHttpForTests: Boolean = false
    var tutkintoasetukset: Map<String, String> = emptyMap() // asetus.nimi -> текст для UTUtutkintoasetus
}
```
Валидация при старте воркера: расширить `ArkistointiAdapterRegistry.validateWorkerConfiguration` или сделать
`DynastyConfigurationValidator`, вызываемый адаптером → при отсутствии обязательных значений `DYNASTY_CONFIGURATION_INVALID` (permanent).
Предпочтительно fail-fast при старте воркера, если `isAsyncEnabled(TURUN_YLIOPISTO, *)`.

### 6.2 `application.yml` (`application.arkistointi.turku`)
```yaml
turku:
  kaytossa: false
  dynasty:
    base-url:
    token-url:
    client-id:
    client-secret:
    organization:
    case-schema-id:
    document-schema-id:
    call-timeout: 2m
    in-progress-grace: 5m
    allow-loopback-http-for-tests: false
    tutkintoasetukset:
      "56/2015": "Asetus 56/2015, muutettu asetuksilla 55/2020 ja 1240/2022"
      # остальные — после ответа заказчика
  metadata:
    cases:
      koejakso:
        async-enabled: false
        documents:
          arviointi:
            retention-period: "-1"
      valmistuminen:
        async-enabled: false
        documents:
          yhteenveto:
            retention-period: "20"
          liite:
            retention-period: "10"
          laillistamistodistus:
            retention-period: "10"
```
Убрать комментарии «Dynasty-specific metadata will be configured with its adapter». `retention-period` теперь читается
маппером (единственный источник). UEF не трогать (маппинг UEF не предоставлен; ключи AD `UTU*` специфичны для Turku).
Секреты — только через env (`APPLICATION_ARKISTOINTI_TURKU_DYNASTY_CLIENT_SECRET` и т.д.), в репо не коммитить.
Тестовые/прод schema ID задокументировать в README (они не секретны).

---

## 7. Новые классы (пакет `service/arkistointi/dynasty`)

| Класс | Ответственность |
|---|---|
| `DynastyPayloadReader` | Строгое чтение payload VALMISTUMINEN/KOEJAKSO (как `SahkePayloadReader`: явные проверки типов, без coercion). Обязательные: firstName, lastName, approvalDate, specialtyName, specialtyType/yek, documents. Отсутствие → `DYNASTY_PAYLOAD_INVALID`. |
| `DynastyMetadataMapper` (interface) + `TurkuDynastyMetadataMapper` | Чистая функция payload + список документов → `DynastyRequestPlan` (case, taskTitle, tosClass, main doc, attachments). Константы маппинга из §2. Без I/O. |
| `DynastyRequestPlan` | Иммутабельный план; `toString()` без персональных данных. |
| `DynastyTokenProvider` | §5.1 |
| `DynastyClient` | §5 |
| `DynastyStepLedger` | §4.3 |
| `DynastyTransferException` | коды ошибок §8 |
| `DynastyArkistointiAdapter` (abstract/generic) + `TurkuDynastyArkistointiAdapter` (`@Component @Profile("!e2e-archive")`, `university = TURUN_YLIOPISTO`, `supportedCaseTypes = {VALMISTUMINEN, KOEJAKSO}`) | §4.5 |

Опционально вынести `lockExecution` из `ArkistointiJobStateService` в общий компонент `ArkistointiExecutionLock`
(не меняя поведение; `ArkistointiJobStateService` начинает его использовать).

---

## 8. Коды ошибок (без персональных данных, имён файлов, тел ответов, токенов)

| Код | Retryable | Когда |
|---|---|---|
| `DYNASTY_CONFIGURATION_INVALID` | нет | нет URL/секретов/schema/organization, не-HTTPS, нет execution |
| `DYNASTY_PAYLOAD_INVALID` | нет | payload не читается/не хватает полей/неожиданный набор документов |
| `DYNASTY_AUTH_FAILED` | нет | token endpoint 400/401 или API 401 после обновления токена |
| `DYNASTY_UNAVAILABLE` | да | соединение не установлено |
| `DYNASTY_HTTP_UNAVAILABLE` | да | 429/503 |
| `DYNASTY_HTTP_REJECTED` | нет | прочие 4xx |
| `DYNASTY_DELIVERY_UNCONFIRMED` | нет (park) | неоднозначный исход или старый `ALOITETTU` |
| `DYNASTY_RESPONSE_INVALID` | нет (park) | 2xx, но id не парсится |
| `DYNASTY_STEP_IN_PROGRESS` | да | шаг ведёт другая (возможно, живая) execution |

Описание: `"Dynasty-toimitus epaonnistui [step=CASE, status=500, outcome=UNKNOWN]"` — step_key допустим (не ПДн).
Использовать `ArkistointiError.create`.

---

## 9. Регистрация адаптеров / тестовые профили

- `TurkuDynastyArkistointiAdapter` — `@Profile("!e2e-archive")`, как `SiiloAsyncArkistointiAdapter`.
- `E2eArchiveConfiguration`: добавить `turkuE2eAdapter()` (стаб), чтобы в e2e-профиле не было пустоты, если
  когда-либо включат Turku; обновить README-абзац о Cypress.
- **Конфликт в IT**: `DynastyGraduationApprovalIT` и `DynastyKoejaksoApprovalIT` регистрируют свой `turku()` адаптер →
  с реальным бином будет `"Multiple archive job adapters configured for the same university"`. Решение:
  убрать тестовый Turku-стаб и прогонять Turku через **реальный** адаптер против `MockWebServer` (Dynasty-стаб);
  UEF-стаб оставить. Это и будет основной сквозной IT.

---

## 10. Тесты (исполнитель пишет и запускает только unit/IT; Cypress не запускать)

Unit:
1. `TurkuDynastyMetadataMapperTest` — EL/EHL/YEK, valmistuminen и koejakso: tosClass, title (формат даты), AD-ключи
   ровно по §2 (наборы для main/LIITE/LAILLISTAMISTODISTUS различаются), retention, documentType, publicity, schema IDs из конфига,
   отсутствие null в AD, неизвестный asetus → fallback.
2. `DynastyPayloadReaderTest` — невалидные типы/отсутствующие поля → `DYNASTY_PAYLOAD_INVALID`.
3. `DynastyClientTest` (MockWebServer) — точные заголовки (`Authorization`, `Organization`, `Accept`), query `tosClass`,
   JSON-тело (`schemaid` vs `schemaId`, `AD`), multipart-части `MetaData`/`File`/`CaseAttachmentOptions`,
   парсинг `415`, `"guid"`, `1078`; 401 → refresh + 1 повтор; классификация §4.4; нет redirect; HTTPS-проверка;
   тело ответа/токен не попадают в исключения.
4. `DynastyTokenProviderTest` — кеш, refresh за 60s до истечения, конкурентный доступ, invalidate.
5. `DynastyArkistointiAdapterTest` (ledger замокан или H2 не используется — см. ниже) — порядок шагов, пропуск `VALMIS`-шагов,
   ошибки маппинга не вызывают сеть.

IT (Testcontainers PostgreSQL, как существующие `*IT`):
6. `DynastyStepLedgerIT` — unique-гонка двух `begin`, stale version → `StaleArkistointiExecutionException`,
   `complete` из устаревшей execution сохраняет remote_id, триггеры (VALMIS неизменяем, DELETE только ALOITETTU),
   порядок блокировок (без дедлоков при параллельных `begin` и `ArkistointiJobStateService.fail`).
7. `TurkuDynastyAsyncApprovalIT` (или расширение `DynastyGraduationApprovalIT`/`DynastyKoejaksoApprovalIT`) — HTTP-утверждение →
   job → db-scheduler → реальный адаптер → MockWebServer. Сценарии:
   - happy path valmistuminen (5 запросов + token, вложения ссылаются на id главного, итог `LAHETETTY`, `external_id = caseId`,
     строка scheduled_tasks удалена);
   - happy path koejakso (hyväksytty и hylätty);
   - **идемпотентность**: 500 на `DOCUMENT:1` → park; ручное разрешение (эмулировать SQL «отбросить») + `retry-job.sql` →
     повтор не вызывает `POST /cases` и `/tasks` повторно (assert количества запросов на MockWebServer);
   - 503 на `CASE` → retryable, затем успех, ровно одно дело создано;
   - сбой после `complete(CASE)` (эмулировать исключение до TASK) → повтор продолжает с TASK;
   - 2xx с мусором → park, `ALOITETTU` сохранён;
   - 401 → обновление токена.
8. Существующие тесты, которые надо прогнать после изменений payload/submission:
   `SahkePayloadReaderTest`, `AsyncSahkePackageBuilderTest`, `SiiloAsyncArkistointiAdapterTest`, `LouhiAsyncArkistointiAdapterTest`,
   `HelsinkiAsyncApprovalIT`, `TampereAsyncApprovalIT`, `DynastyGraduationApprovalIT`, `DynastyKoejaksoApprovalIT`,
   `ValmistuminenArkistointiTransactionIT`, `KoejaksoArkistointiTransactionIT`, `KoejaksoArkistointiServiceTest`, тесты в
   `service/arkistointi/job/**`. Команды (задачи из `build.gradle`): `./gradlew test --tests '…'` для unit, `./gradlew integrationTest --tests '…'` для `*IT`.
   `StaleArkistointiExecutionException` — `internal` в `job/processing/ArkistointiJobExecution.kt`, доступен из того же модуля.

---

## 11. Документация
- README: новый раздел «Turun Dynasty-arkistointi taustakäsittelyssä» (по образцу раздела Siilo): env-переменные,
  test/prod schema ID, последовательность вызовов, step ledger, классификация ошибок, ручное разрешение
  (`dynasty-resolve-step.sql` → `retry-job.sql`), остаточный риск, проверка отсутствия старых Turku-job перед включением.
- Обновить абзацы «Turun ja Itä-Suomen valmistumisasiakirjat» и «…koejakson arvio» (адаптер Turku реализован; UEF — нет).
- CHANGELOG не трогать (генерируется из PR).

---

## 12. Порядок реализации (рекомендуемый)
1. Liquibase + сущность + репозиторий step ledger; вынести `ArkistointiExecutionLock`; `ArkistointiSubmission.execution`.
2. Расширение payload (§3) + обновление создания job + тесты payload.
3. `ApplicationProperties.Dynasty` + `application.yml`.
4. `DynastyPayloadReader` + `TurkuDynastyMetadataMapper` + unit-тесты.
5. `DynastyTokenProvider` + `DynastyClient` + unit-тесты (MockWebServer).
6. `DynastyStepLedger` + IT.
7. `TurkuDynastyArkistointiAdapter`, `E2eArchiveConfiguration`, правка Dynasty-IT.
8. Сквозной IT, ops-скрипт, README.
9. Прогон затронутых тестов (§10.8). Не коммитить.

---

## 13. Открытые вопросы (задать заказчику/Innofactor; до ответа — значения по умолчанию в скобках)
1. Есть ли в API поиск дела/документа по внешнему ключу или idempotency-key? Можно ли добавить AD-поле (напр. `UTUelsatunniste`)
   с UUID job'а для автоматической сверки? (по умолчанию: нет → park при неоднозначном исходе)
2. Есть ли `GET /api/v1/cases/{id}` (состояние, список документов)? (по умолчанию: не используется)
3. Какие HTTP-статусы гарантируют, что объект НЕ создан (429/503?) (по умолчанию: 429/503 = не создан).
4. `publicityClassId` для «Osittain salassa pidettävä» (в docx оба «S» — опечатка?) и JSON-имена полей
   Salassapitoperuste / Salassapitoaika / Salassapitoajan laskentaperuste для koejakso.
5. `UTUmedammaterikoisalat` — есть в примере docx, нет в xlsx. Передавать? (по умолчанию: нет)
6. Формат даты в заголовке: `28.11.2025` → `dd.MM.yyyy` или `d.M.yyyy`? (по умолчанию `d.M.yyyy`)
7. Текст UTUtutkintoasetus для каждого `asetus.nimi` (678/1998, 1343/2002, 401/2007, 56/2015, 55/2020).
8. YEK в koejakso: какое сокращение в заголовке (в xlsx только EL/EHL)? (по умолчанию `YEK`)
9. Поля, не передаваемые в API (Henkilötietoluonteen peruste, Omistaja, Säilytysajan peruste, Asiakirjatyyppi Yhteenveto/Liite/Lausunto,
   Saapuva/Lähtevä/Sisäinen — в UI-скриншоте документ получился «Saapuva»): заполняются ли автоматически через tosClass/schema,
   или нужны доп. поля в MetaData?
10. Нужен ли `PhaseSelector` у вложений (в примере только `caseSelector` + `parentDocumentSelector`)? (по умолчанию: нет)
11. Лимиты размера файла и допустимые MIME (Laillistamistodistus может быть PNG).
12. Значение `Organization` и учётные данные для прод; тот же ли token endpoint для прод.
13. UEF (Itä-Suomen yliopisto): тот же Dynasty-инстанс/маппинг или отдельный? Документ озаглавлен «UTU/Oulu» — касается ли Оулу?
14. Пустые/отсутствующие значения (birthDate, alkamispäivä, asetus, laillistamispäivä): пропускать AD-ключ или блокировать отправку?
    (по умолчанию: пропускать необязательные; имена/approvalDate/specialty — обязательны)
