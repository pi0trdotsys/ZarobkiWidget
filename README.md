# Zarobki Widget — instrukcja uruchomienia

Widget na ekran główny Androida pokazujący brutto/netto zarobione w bieżącym
miesiącu. Dane liczy Twój arkusz `Godziny_pracy_2026.xlsx` — przeniesiony do
Google Sheets, żeby telefon mógł go odpytać przez proste API (Google Apps
Script). Wpisywanie godzin nadal odbywa się w arkuszu (na telefonie lub
komputerze) — appka na Androida to tylko wyświetlacz widgetu.

## Pobierz gotowe APK

Nie chcesz budować projektu sam? Gotowy plik APK jest dołączony do
[najnowszego wydania (Releases)](https://github.com/pi0trdotsys/ZarobkiWidget/releases/latest) —
pobierz plik `.apk` na telefon i zainstaluj (włącz instalację z
nieznanych źródeł, jeśli system o to poprosi). To build podpisany kluczem
debug — wystarczający do własnego użytku, nie do publikacji w Sklepie Play.

## 1. Przenieś arkusz do Google Sheets

1. Wejdź na [drive.google.com](https://drive.google.com), prześlij plik
   `Godziny_pracy_2026.xlsx`.
2. Kliknij go prawym przyciskiem → **Otwórz za pomocą** → **Arkusze Google**.
   Powstanie edytowalna kopia z tymi samymi zakładkami i formułami.
3. Sprawdź, czy zakładka **Podsumowanie** poprawnie wylicza sumy (wpisz
   testowo kilka godzin w dowolnym miesiącu i zobacz, czy wiersze się
   przeliczają).

## 2. Wdróż Google Apps Script jako mini-API

1. W otwartym arkuszu: **Rozszerzenia → Apps Script**.
2. Usuń domyślną zawartość i wklej całą treść pliku [`appsscript/Code.gs`](appsscript/Code.gs)
   z tego projektu.
3. Zmień wartość `SECRET_TOKEN` na własny, długi, losowy ciąg (to jedyne
   zabezpieczenie tego adresu — nie zostawiaj wartości domyślnej).
4. **Wdróż → Nowe wdrożenie**:
   - Typ: **Aplikacja internetowa** (Web app)
   - Wykonaj jako: **Ja**
   - Kto ma dostęp: **Każdy** (Anyone) — inaczej telefon nie połączy się bez
     logowania Google
5. Zatwierdź uprawnienia (Apps Script musi czytać Twój arkusz) i skopiuj
   wygenerowany **adres URL aplikacji internetowej**.
6. Warto od razu wkleić ten URL + `?token=TWÓJ_TOKEN` w przeglądarce — powinieneś
   zobaczyć JSON w stylu:
   `{"month":"Lipiec","year":2026,"hours":12.5,"gross":475,"net":372.99}`

Uwaga: jeśli kiedykolwiek zmienisz wdrożenie (np. zaktualizujesz kod), zrób
**Zarządzaj wdrożeniami → Edytuj → Nowa wersja**, żeby URL wciąż działał ze
zaktualizowanym kodem.

## 3. Otwórz projekt Androida

1. Otwórz Android Studio → **Open** → wskaż folder `ZarobkiWidget` (ten, w
   którym jest ten plik README).
2. Poczekaj na Gradle sync (pobierze Kotlin/Compose/Glance/WorkManager —
   potrzebny internet przy pierwszym otwarciu).
3. Podłącz telefon (USB debugging) albo użyj emulatora i kliknij **Run**.

## 4. Skonfiguruj aplikację i dodaj widget

1. Po zainstalowaniu otwórz appkę **Zarobki Widget** — wklej **Web App URL**
   oraz **Secret token** z kroku 2, zapisz.
2. Przytrzymaj pusty obszar ekranu głównego → **Widżety** → znajdź
   **Zarobki Widget** → przeciągnij na pulpit.
3. Widget odświeża się automatycznie co 30 minut (WorkManager) oraz od razu
   po dotknięciu go palcem.

## Jak to działa

- Arkusz Google (ten sam, co xlsx) liczy brutto/netto tak samo jak wcześniej
  — zakładka **Podsumowanie** ma już gotowe sumy dla każdego miesiąca.
- `Code.gs` po prostu odczytuje wiersz bieżącego miesiąca z zakładki
  Podsumowanie i zwraca go jako JSON, chroniony tokenem w URL.
- Appka na Androida (Kotlin + Jetpack Glance) co 30 min woła ten adres w tle
  (`RefreshWorker`) i zapisuje wynik w stanie widgetu; dotknięcie widgetu
  wymusza natychmiastowe odświeżenie.
- Zero logiki podatkowej nie jest duplikowane w appce — jedyne źródło prawdy
  to Twój arkusz, dokładnie jak chciałeś.

## Znane ograniczenia

- Adres Web App jest chroniony tylko tokenem w URL, nie prawdziwym
  uwierzytelnianiem — nie udostępniaj go nikomu.
- Web Apps Google czasem "usypiają" i pierwsze odpytanie po dłuższej przerwie
  bywa wolniejsze (kilka sekund) — to normalne.
- Rok w arkuszu (`Ustawienia!B5`) musi być ustawiony na bieżący, inaczej
  etykieta roku w widgecie będzie nieaktualna (dane miesiąca i tak liczone są
  po bieżącej dacie telefonu/serwera Google, nie po tym polu).
