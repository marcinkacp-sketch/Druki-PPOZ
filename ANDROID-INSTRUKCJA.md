# Druki PPOŻ — aplikacja Android (Atom Serwis)

To samodzielny projekt Android APK oparty na lokalnie spakowanym formularzu NOMA 2. **Nie jest to skrót PWA**: aplikacja używa natywnego WebView, działa offline bez uprawnienia Internet i ma natywny wybór miejsca zapisu PDF.

## Testowa instalacja Android

1. W GitHub otwórz `Actions` → `Android APK • Druki PPOŻ`.
2. Wybierz ostatni zakończony przebieg, potem w `Artifacts` pobierz `Druki-PPOZ-Android-APK-TEST`.
3. Rozpakuj ZIP, otwórz `app-debug.apk` na telefonie z Androidem i potwierdź instalację (może być konieczna zgoda na instalowanie aplikacji z plików).
4. Dla wygenerowania PDF kliknij `Pobierz PDF`, wybierz folder i zatwierdź nazwę pliku.
5. Podpis z Worda trzeba **zaimportować ponownie w aplikacji Android**; aplikacja przeglądarkowa i APK mają niezależną pamięć lokalną. W aplikacji podpis zapamiętuje się do kolejnych wniosków.

## Ważne

- Podpisy i pieczątki nie są częścią aplikacji APK ani repozytorium. Są zapisywane w pamięci aplikacji na urządzeniu użytkownika.
- Dokument PDF jest generowany w aplikacji, offline, jako jedna strona A4 zgodna ze wzorem NOMA 2. **Aplikacja nie wysyła zgłoszenia automatycznie do operatora NOMA 2.**
- Ten pierwszy APK jest wersją diagnostyczną podpisaną kluczem debug GitHub Actions. Kolejne przebudowane APK mogą mieć inny certyfikat, a wtedy Android może wymagać odinstalowania testowej wersji przed instalacją nowej (co usuwa jej lokalne dane). **Do trwałych aktualizacji potrzebny będzie osobny stały klucz podpisywania wersji release w GitHub Secrets**, tak jak w Kilometrówce VAT.
- Repozytorium publiczne zawiera **tylko kod i wzór**, bez prywatnych podpisów użytkowników.

## Aktualizacje bez odinstalowania: stały podpis

Wersja diagnostyczna `app-debug.apk` jest podpisana tymczasowym kluczem wykonawcy GitHub Actions.
**Nie należy jej używać jako wersji docelowej do aktualizacji**. Do stałych aktualizacji konieczny jest podpisany APK release z niezmiennym kluczem i `versionCode` rosnącym przy kolejnych wydaniach.

1. W ustawieniach repozytorium GitHub przejdź do `Settings → Secrets and variables → Actions`.
2. Dodaj cztery sekrety `PPOZ_KEYSTORE_BASE64`, `PPOZ_KEYSTORE_PASSWORD`, `PPOZ_KEY_ALIAS`, `PPOZ_KEY_PASSWORD` z prywatnego pliku przygotowanego dla właściciela. **Nie dodawaj prywatnego klucza, haseł ani pliku z sekretami do publicznego repozytorium.**
3. Na GitHubie uruchom workflow `Android APK - Druki PPOZ` ręcznie (`Run workflow`) albo wykonaj commit na `main`.
4. Po zakończeniu `success` pobierz artefakt `Druki-PPOZ-STALA-PODPISANA-APK` i instaluj go na telefonie. Kolejne wydania podpisane tym samym kluczem i z wyższym `versionCode` mogą aktualizować aplikację bez kasowania danych.
5. Jeśli zainstalowana jest wcześniejsza wersja testowa z innym podpisem, trzeba ją **jednorazowo odinstalować** przed instalacją release. Odinstalowanie usuwa lokalną bazę zmian i podpisy: wcześniej wyeksportuj bazę JSON i zachowaj oryginalne obrazy podpisów.

Klucz `.p12` oraz hasła muszą pozostać w bezpiecznej kopii, bo ich utrata uniemożliwi podpisanie aktualizacji dla istniejących instalacji. Sekrety nie są jeszcze skonfigurowane automatycznie przez tę zmianę w repozytorium.