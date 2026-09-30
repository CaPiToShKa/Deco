# Konfiguracja połączenia aplikacji z Salesforce Marketing Cloud

Instrukcja krok po kroku: od projektu Firebase, przez aplikację MobilePush i pakiet API, po Data Extensions i Journey Builder. Kolejność ma znaczenie — każdy krok daje wartości potrzebne w następnym.

> Stan na 30.09.2026. Nazwy menu w SFMC i Firebase zmieniają się między wydaniami — jeśli coś wygląda inaczej, szukaj tej samej funkcji pod podobną nazwą. Miejsca, których nie dało się potwierdzić w dokumentacji, oznaczono **[sprawdź]**.

## 0. Jak to działa (w skrócie)

```
Aplikacja Android ──HTTPS + token──▶ BFF (moduł :bff) ──OAuth S2S──▶ SFMC REST API
      │                                    │                           ├─ Data Extensions (upsert)
      │                                    │                           └─ Journey Builder (API Event)
      └── SFMC MobilePush SDK 11 ◀── FCM ◀── MobilePush ◀── Journey Builder (aktywność Push)
```

- **Aplikacja nigdy nie woła API SFMC bezpośrednio.** Sekret pakietu API żyje tylko w BFF. Salesforce wprost zabrania umieszczania client secret w aplikacji mobilnej.
- **Push idzie drugą drogą:** SDK MobilePush w aplikacji rejestruje urządzenie w SFMC z `profileId = ContactKey` członka. Journey Builder wysyła push do tego kontaktu przez FCM.
- **ContactKey musi być wszędzie ten sam:** w SDK (ustawiany przy logowaniu), w wierszach Data Extensions (zapisywanych przez BFF) i w Contact Builder. W demo to `DEMO-0001`; w produkcji — identyfikator członka, którego DECO PROteste już używa jako Subscriber Key.

## 1. Warunki wstępne

1. **Licencja MobilePush** w koncie Marketing Cloud Engagement. Bez niej krok 3 jest niedostępny. Z raportu: w publicznym cenniku wiadomości mobilne są w pakietach od Corporate+; **potwierdź z opiekunem konta Salesforce**, czy obecny kontrakt je obejmuje.
2. Uprawnienia administratora SFMC (Setup) i dostęp do konsoli Firebase (konto Google firmy).
3. Android Studio (aktualne) i JDK 17 — do zbudowania aplikacji.

## 2. Firebase (FCM)

1. W [konsoli Firebase](https://console.firebase.google.com/) utwórz projekt (np. `deco-socio`). Analytics nie jest potrzebne.
2. Dodaj aplikację Android z nazwą pakietu **`com.example.decosocio`**. Dodaj też drugą z `com.example.decosocio.debug` — wersja debug ma ten przyrostek.
   Przy docelowej publikacji zmień pakiet na docelowy (np. `pt.deco.proteste.socio`) w `app/build.gradle.kts` (`namespace`, `applicationId`) i w Firebase.
3. Pobierz **`google-services.json`** i skopiuj do `app/google-services.json`. Plik jest w `.gitignore`; do CI dodaj go jako sekret (patrz README).
4. Zanotuj **Sender ID** (Project settings → Cloud Messaging) → to będzie `SFMC_SENDER_ID`.
5. Utwórz **klucz konta usługi** dla FCM HTTP v1 (Project settings → Service accounts → Generate new private key). Plik JSON przekażesz do SFMC w kroku 3. Nie commituj go.

Bez `google-services.json` aplikacja buduje się i działa normalnie, tylko push jest wyłączony (menu demo pokazuje `sfmc.configured = false`).

## 3. Aplikacja MobilePush w SFMC

1. SFMC → **Setup → Platform Tools → Apps → MobilePush → New App**.
2. Nazwa (np. „DECO Sócio Android”), platforma **Android**.
3. W sekcji Android wgraj **plik JSON konta usługi Firebase** (FCM HTTP v1). **[sprawdź]** dokładną etykietę pola w swoim wydaniu SFMC.
4. Po zapisaniu skopiuj z karty aplikacji:

| Pole w SFMC | Klucz w `local.properties` |
|---|---|
| App ID | `SFMC_APP_ID` |
| Access Token | `SFMC_ACCESS_TOKEN` |
| App Endpoint (URL serwera) | `SFMC_SERVER_URL` |
| MID jednostki biznesowej | `SFMC_MID` |
| Sender ID z Firebase | `SFMC_SENDER_ID` |

„Access Token” aplikacji MobilePush to identyfikator tej konkretnej aplikacji mobilnej, nie sekret API — SDK musi go mieć na urządzeniu. Mimo to nie commituj go do repozytorium: trzymaj w `local.properties` albo w sekretach CI.

5. Skopiuj `local.properties.example` do `local.properties` i uzupełnij wartości. Zbuduj i uruchom aplikację.

**Test SDK:** zaloguj się kontem demo, zezwól na powiadomienia, stuknij 5× wersję w zakładce Perfil → menu demo. Powinno być `sfmc.configured = true`, `sfmc.sdkReady = true`, `contactKey = DEMO-0001`. Potem w SFMC: **MobilePush → Create Message → Outbound**, odbiorca: kontakt `DEMO-0001` → wyślij. Push powinien przyjść na telefon.

## 4. Pakiet API dla BFF (Installed Package)

1. SFMC → **Setup → Apps → Installed Packages → New**, nazwa np. „DECO Sócio BFF”.
2. **Add Component → API Integration → Server-to-Server**.
3. Uprawnienia (najmniejszy potrzebny zakres):
   - **Data → Data Extensions: Read, Write**
   - **Automation → Journeys: Read, Execute** (do wywoływania zdarzeń wejścia API Event) **[sprawdź]** nazwę uprawnienia w swoim wydaniu
4. Skopiuj **Client ID**, **Client Secret** i **Authentication Base URI**. Z URI `https://<SUBDOMENA>.auth.marketingcloudapis.com/` weź samą subdomenę → `SFMC_SUBDOMAIN`.
5. Jeśli pakiet jest na poziomie całej organizacji, a dane mają trafić do konkretnej jednostki biznesowej, podaj jej MID jako `SFMC_ACCOUNT_ID`.

Sekretów nie wklejaj do czatów ani commitów. W produkcji trzymaj je w menedżerze sekretów chmury (Azure Key Vault, AWS Secrets Manager, GCP Secret Manager).

BFF sam pobiera token (`POST /v2/token`, ważny ok. 20 minut), trzyma go w pamięci i odświeża minutę przed wygaśnięciem albo po odpowiedzi 401. Pobieranie tokenu przy każdym wywołaniu zużywa limity API — dlatego jest cache.

## 5. Data Extensions

Utwórz w **Email Studio → Subscribers → Data Extensions** albo w **Contact Builder** cztery DE. Nazwa i **External Key** muszą się zgadzać z konfiguracją BFF (domyślne poniżej). Kolumny oznaczone 🔑 to klucz główny.

**`DECO_App_Profile`** (sendable: `ContactKey` → Subscriber Key; pole Email do wysyłek e-mail)

| Pole | Typ | Uwagi |
|---|---|---|
| 🔑 ContactKey | Text(254) | = Subscriber Key |
| MemberNumber | Text(50) | |
| FirstName | Text(100) | |
| LastName | Text(100) | |
| Email | EmailAddress | |
| Phone | Text(30) | |
| PostalCode | Text(10) | format PT 0000-000 |
| City | Text(100) | |
| PreferredLanguage | Text(10) | pt-PT / en |
| UpdatedAt | Text(30) | ISO-8601, np. 2026-09-30T12:00:00Z |

**`DECO_App_Consents`**

| Pole | Typ |
|---|---|
| 🔑 ContactKey | Text(254) |
| Newsletter | Boolean |
| MarketingPush | Boolean |
| PartnerOffers | Boolean |
| Personalisation | Boolean |
| TextVersion | Text(20) |
| UpdatedAt | Text(30) |

**`DECO_App_AddOns`**

| Pole | Typ |
|---|---|
| 🔑 ContactKey | Text(254) |
| 🔑 AddOnId | Text(50) |
| Name | Text(200) |
| Status | Text(30) — ACTIVE / CANCELLATION_REQUESTED / INACTIVE |
| ActivatedOn | Text(10) — yyyy-MM-dd |
| WithdrawalDeadline | Text(10) |
| EndsOn | Text(10) |
| LastAction | Text(30) — ACTIVATED / WITHDRAWN / CANCELLATION_REQUESTED / CANCELLATION_UNDONE |
| UpdatedAt | Text(30) |

**`DECO_App_Coupons`**

| Pole | Typ |
|---|---|
| 🔑 ContactKey | Text(254) |
| 🔑 CouponId | Text(50) |
| CampaignId | Text(50) |
| RewardId | Text(50) |
| Code | Text(50) |
| IssuedOn | Text(10) |
| ValidUntil | Text(10) |

Daty są zapisywane jako tekst ISO-8601, żeby uniknąć problemów z formatem regionalnym. Jeśli wolisz typ Date, sprawdź najpierw na jednym wierszu, czy import przechodzi.

**Ważne:** BFF tylko *zapisuje* do DE (upsert przez `/hub/v1/dataevents/key:{klucz}/rowset`). Nie czyta z nich na żądanie aplikacji. SFMC nie ma udokumentowanego API do odczytu wierszy w czasie rzeczywistym, a limity API są wspólne z wysyłkami kampanii. Źródłem prawdy o subskrypcji jest system billingowy/CRM, a BFF trzyma własną kopię.

## 6. Journey Builder: zdarzenia z aplikacji

BFF może wywoływać trzy zdarzenia wejścia (API Event). Dla każdego:

1. **Journey Builder → Entry Sources → API Event → New**.
2. Wybierz lub utwórz DE zdarzenia z polami wysyłanymi przez BFF (plus `ContactKey`):
   - zgoda zmieniona: `Newsletter, MarketingPush, PartnerOffers, Personalisation, TextVersion, UpdatedAt`
   - usługa zmieniona: `AddOnId, Name, Status, ActivatedOn, WithdrawalDeadline, EndsOn, LastAction, UpdatedAt`
   - prośba o usunięcie konta: `RequestId, CompletesBy`
3. Skopiuj **Event Definition Key** (zwykle `APIEvent-…`) do zmiennej BFF:
   `SFMC_EVENT_CONSENT_CHANGED`, `SFMC_EVENT_ADDON_CHANGED`, `SFMC_EVENT_DELETION_REQUESTED`.
   Puste = BFF nie wysyła danego zdarzenia (zapis do DE i tak działa).

Proponowane journeys:

| Journey | Wejście | Co robi | Dlaczego |
|---|---|---|---|
| Potwierdzenie odstąpienia | API Event „usługa zmieniona”, filtr `LastAction = WITHDRAWN` | E-mail z potwierdzeniem odstąpienia (data, kwota zwrotu, numer) | Dyrektywa 2023/2673 wymaga potwierdzenia na trwałym nośniku |
| Aktywacja usługi | ten sam event, `LastAction = ACTIVATED` | E-mail z warunkami i informacją o 14 dniach na odstąpienie | Obowiązek informacyjny przy umowach na odległość |
| Obsługa usunięcia konta | API Event „usunięcie konta” | Zadanie dla zespołu/DPO + e-mail z potwierdzeniem | Art. 17 RODO, wymóg Google Play |
| Wygasająca subskrypcja | DE z billingu (Automation Studio) | Push serwisowy + e-mail na 30 i 7 dni przed końcem | Push serwisowy (umowa), bez treści promocyjnych |

## 7. Push z Journey Builder do aplikacji

- Dodaj w journey aktywność **Push Notification** (MobilePush) i wybierz aplikację z kroku 3.
- **Push marketingowy:** przed aktywnością dodaj filtr (Decision Split) na `DECO_App_Consents.MarketingPush = true`. Aplikacja dodatkowo ustawia w SDK atrybut `MarketingPushConsent`, ale to BFF/DE jest źródłem prawdy o zgodzie.
- **Push serwisowy** (renowacja, potwierdzenia) wysyłaj osobnymi journeys, tylko z treścią dotyczącą umowy członka.
- Aplikacja włącza push dopiero po zalogowaniu i nadaniu uprawnienia Androida 13+, a przy wylogowaniu go wyłącza. Na współdzielonym telefonie nie przyjdą więc wiadomości innej osoby.

## 8. Uruchomienie BFF

Lokalnie, w trybie mock (bez SFMC):

```bash
./gradlew :bff:run
# sprawdź: curl http://localhost:8080/health
# wywołania, które poszłyby do SFMC: curl http://localhost:8080/debug/sfmc-calls
```

Z prawdziwym SFMC (zmienne z kroków 4–6; szablon w `bff/.env.example`):

```bash
export SFMC_MODE=live SFMC_SUBDOMAIN=... SFMC_CLIENT_ID=... SFMC_CLIENT_SECRET=...
export SFMC_EVENT_CONSENT_CHANGED=APIEvent-...   # opcjonalnie
./gradlew :bff:run
```

Aplikacja w trybie BFF — w `local.properties`:

```
BACKEND_MODE=bff
BFF_BASE_URL=http://10.0.2.2:8080   # emulator → komputer; na telefonie użyj IP komputera w tej samej sieci
```

Wersja debug pozwala na HTTP do lokalnego serwera; wersja release przyjmuje tylko HTTPS.

Kontener: `docker build -f bff/Dockerfile -t deco-socio-bff .`, potem `docker run -p 8080:8080 --env-file bff/.env deco-socio-bff`. Hostuj w regionie UE.

**Sprawdzenie end-to-end:** w aplikacji zmień zgodę na push marketingowy → w SFMC otwórz `DECO_App_Consents`, wiersz `DEMO-0001` → `MarketingPush = True`. Jeśli wiersza nie ma, logi BFF pokażą odpowiedź SFMC (np. 400 przy niezgodnych nazwach pól).

## 9. Najczęstsze problemy

| Objaw | Przyczyna | Rozwiązanie |
|---|---|---|
| `sfmc.configured = false` | brak `google-services.json` albo pustych kluczy SFMC | krok 2–3; przebuduj aplikację |
| `sdkReady = false`, błąd inicjalizacji | zły App ID / Access Token / App Endpoint | skopiuj wartości ponownie z karty MobilePush |
| Push nie przychodzi | brak uprawnienia powiadomień, inny ContactKey, brak klucza FCM w SFMC | menu demo → sprawdź `push.requested` i `contactKey`; krok 3 pkt 3 |
| BFF: `SFMC token request failed: 401` | zły Client ID/Secret albo subdomena | krok 4 |
| BFF: `rowset failed: 400` | nazwy pól DE ≠ nazwy wysyłane przez BFF | porównaj z tabelami w kroku 5 |
| BFF: `interaction/v1/events failed: 400` | pola zdarzenia nie istnieją w DE zdarzenia | krok 6 pkt 2 |

## 10. Przed produkcją

- Logowanie przez istniejące konto członka (OIDC Authorization Code + PKCE) zamiast konta demo. BFF weryfikuje wtedy JWT dostawcy tożsamości (Ktor `jwt` + JWKS).
- Własna baza BFF + trwała kolejka synchronizacji do SFMC (outbox) zamiast pamięci procesu.
- Integracja z billingiem/CRM jako źródłem prawdy o subskrypcji i usługach.
- Hosting BFF w UE, sekrety w menedżerze sekretów, limity zapytań na użytkownika, logi audytowe zmian zgód i profilu.
- Umowy powierzenia (DPA) i ocena transferów (TIA) dla Salesforce i Google/FCM; formularz Data safety w Google Play; publiczny link do usuwania konta.
- Klasyfikacja każdej płatnej usługi pod kątem Google Play Billing (treści cyfrowe → Play Billing).
