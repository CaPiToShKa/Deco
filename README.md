# DECO PROteste Sócio — aplikacja Android (demo) + BFF dla Salesforce Marketing Cloud

Klikalne demo aplikacji członkowskiej dla DECO PROteste, napisane natywnie w **Kotlin + Jetpack Compose**, z backendem **BFF** (Backend-for-Frontend) w Kotlinie (Ktor), który synchronizuje dane z **Salesforce Marketing Cloud**. Architektura jest zgodna z rekomendacją z raportu: aplikacja nigdy nie łączy się z SFMC bezpośrednio, push idzie przez **SFMC MobilePush SDK 11.1.0**.

Wszystkie dane w demo są fikcyjne: członek, ceny, partnerzy, kody i artykuły. Logo to zaślepka.

## Co jest w aplikacji

| Funkcja (MVP) | Jak działa w demo |
|---|---|
| 1. Lista wiadomości i artykułów | Kanał JSON/RSS z konfiguracji (np. Sitecore headless albo `/v1/news` w BFF). Bez adresu pokazuje oznaczone artykuły przykładowe. |
| 2. Status subskrypcji | Aktywna / wygasa / wygasła, data odnowienia, plan, metoda płatności; przycisk odnowienia prowadzi na stronę. |
| 3. Usługi dodatkowe | Aktywacja z przyciskiem „z obowiązkiem zapłaty”. Przez 14 dni działa przycisk „Retrate-se do contrato aqui” (dyrektywa 2023/2673) z potwierdzeniem, a później zwykłe anulowanie z końcem okresu i jego cofnięcie. |
| 4. Profil i zgody | Edycja danych z walidacją (kod pocztowy PT, telefon, e-mail). Onboarding zgód: wszystko domyślnie wyłączone, „odrzuć wszystkie” równie łatwe jak zapis. Historia zgód z wersją tekstu. |
| 5. Nagrody i kupony | Punkty, poziomy, wymiana punktów na kupony, kampanie z jednorazowymi kodami (idempotentne pobranie), kod QR i kopiowanie kodu. |
| 6. Push | SFMC MobilePush SDK: `profileId = ContactKey` przy logowaniu, wyłączenie przy wylogowaniu, uprawnienie Androida 13+ pytane dopiero po wyjaśnieniu, osobne kanały serwisowy i marketingowy. |

RODO i Google Play: ekran „Moje dane” z eksportem JSON (art. 15/20), usuwanie konta w aplikacji, brak kopii zapasowej danych osobowych, HTTPS-only w wersji release.

Język interfejsu: **pt-PT** (domyślny) i **angielski**. Przełącznik jest w Perfil → Idioma; na Androidzie 13+ język można też zmienić w ustawieniach systemu. Motyw jasny i ciemny, czerwień zbliżona do marki.

**Ukryte menu demo:** Perfil → stuknij 5× numer wersji na dole. Pozwala przełączać stan subskrypcji (aktywna / wygasa / wygasła), symulować awarię sieci, pokazać pusty stan kuponów, wysłać testowy push i zresetować dane. Pokazuje też stan SFMC SDK.

**Konto demo:** `demo@exemplo.pt` / `demo1234` (przycisk „Usar conta de demonstração” wypełnia pola).

## Struktura

```
core/domain   czysty Kotlin: modele i reguły biznesowe (14 dni odstąpienia, statusy, kupony, zgody); gotowe pod KMP/iOS
core/api      kontrakt HTTP aplikacja ↔ BFF (DTO + trasy), wspólny dla obu stron
core/data     repozytoria: backend demo w pamięci (te same reguły) i klient BFF (Ktor), newsy JSON/RSS
app           aplikacja Android (Compose, Koin, Navigation, DataStore, SFMC MobilePush SDK, ZXing)
bff           serwer Ktor: logowanie, reguły, synchronizacja z SFMC (Data Extensions + Journey Builder)
docs/         KONFIGURACJA_SFMC.md — instrukcja połączenia z SFMC krok po kroku
tools/        strings_source.py — jedno źródło tekstów pt-PT/EN (generuje strings.xml)
```

Wersje (z oficjalnych przykładów Google z 22–29.09.2026): AGP 9.3.1, Gradle 9.5.0, Kotlin 2.4.20, Compose BOM 2026.09.00, Ktor 3.6.0, Koin 4.2.2; compileSdk 37, targetSdk 36 (wymóg Google Play od 31.08.2026), minSdk 26 (wymóg SFMC SDK 11).

## Uruchomienie

**Android Studio:** File → Open → ten folder → poczekaj na synchronizację Gradle → uruchom konfigurację `app` na emulatorze lub telefonie. Nic nie trzeba konfigurować: domyślnie działa tryb demo bez sieci.

**Z linii poleceń** (JDK 17 i Android SDK):

```bash
./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
./gradlew test                      # testy domeny, danych, BFF i ViewModeli
./gradlew :bff:run                  # BFF na http://localhost:8080 (tryb mock SFMC)
```

**APK z GitHub Actions:** wypchnij repozytorium na GitHub. Workflow `.github/workflows/build.yml` uruchomi testy, zbuduje APK debug i załączy go jako artefakt `deco-socio-debug-apk` w zakładce Actions. Opcjonalne sekrety repozytorium:
- `GOOGLE_SERVICES_JSON` — cała zawartość `google-services.json`, z aplikacjami `com.example.decosocio` i `com.example.decosocio.debug`
- `SFMC_APP_ID`, `SFMC_ACCESS_TOKEN`, `SFMC_SERVER_URL`, `SFMC_MID`, `SFMC_SENDER_ID`

Opcjonalne zmienne repozytorium: `BACKEND_MODE`, `BFF_BASE_URL`, `NEWS_FEED_URL`.

## Konfiguracja

Skopiuj `local.properties.example` do `local.properties`. Te same klucze działają też jako `-Pklucz=…` i zmienne środowiskowe.

| Klucz | Znaczenie |
|---|---|
| `BACKEND_MODE` | `demo` (domyślnie, wszystko w pamięci telefonu) albo `bff` |
| `BFF_BASE_URL` | adres BFF, np. `http://10.0.2.2:8080` z emulatora |
| `NEWS_FEED_URL` | kanał artykułów: tablica JSON (`id, title, summary, body, category, publishedOn, url`) lub RSS 2.0 |
| `RENEW_URL`, `PRIVACY_POLICY_URL` | strony odnowienia i polityki prywatności |
| `SFMC_APP_ID`, `SFMC_ACCESS_TOKEN`, `SFMC_SERVER_URL`, `SFMC_MID`, `SFMC_SENDER_ID` | aplikacja MobilePush w SFMC; wymaga też `app/google-services.json` |

Pełna instrukcja połączenia z SFMC (Firebase, MobilePush, Installed Package, Data Extensions, Journey Builder, test end-to-end): **[docs/KONFIGURACJA_SFMC.md](docs/KONFIGURACJA_SFMC.md)**.

## Branding

- Logo: podmień `app/src/main/res/drawable/brand_logo.xml` (ekran logowania) i `ic_launcher_foreground.xml` (ikona) na oficjalne pliki.
- Kolory: `app/src/main/java/com/example/decosocio/ui/theme/Theme.kt`.
- Nazwa i teksty: `tools/strings_source.py`, potem `python3 tools/strings_source.py`.
- Pakiet `com.example.decosocio` to neutralny pakiet demo. Przed publikacją zmień `namespace` i `applicationId` w `app/build.gradle.kts`.

## Stan weryfikacji

Projekt powstał w środowisku bez dostępu do repozytoriów Google i Maven, więc **pełny build Gradle nie został tu uruchomiony**. Pierwszy prawdziwy build nastąpi w Android Studio albo w GitHub Actions. Sprawdzono:

- `core/domain` skompilowany kompilatorem Kotlin z `-Werror`; 27 testów reguł przechodzi.
- Backend demo, parser RSS i wszystkie ViewModele skompilowane; 24 testy przechodzą (wersje bibliotek coroutines/serialization/lifecycle zastąpione wiernymi odpowiednikami).
- `PushManager` skompilowany z prawdziwymi klasami **SFMC SDK 11.1.0**.
- Dwa niezależne przeglądy porównały każde użyte API z kodem źródłowym bibliotek w tych wersjach (Compose/Material3/Navigation/Koin/AGP 9 oraz Ktor 3.6/serialization/Gradle/CI). Type-check całego modułu `app` i modułów Ktor na tych odpowiednikach nie wykazał błędów.

Jeśli pierwszy build jednak coś zgłosi, najpewniej będą to drobiazgi, np. duplikat pliku `META-INF` przy pakowaniu (dopisz ścieżkę do `packaging.resources.excludes` w `app/build.gradle.kts`).

## Przed produkcją

Logowanie OIDC przez istniejące konto członka zamiast konta demo, integracja z billingiem/CRM, baza BFF z trwałą kolejką synchronizacji, hosting w UE, klasyfikacja usług pod Google Play Billing (treści cyfrowe), ocena DPIA i umowy powierzenia. Szczegóły w sekcji 10 instrukcji SFMC i w raporcie.
