# Druki PPOŻ – czasowe odłączenie monitoringu pożarowego

Gotowa aplikacja przeglądarkowa/PWA na Androida, działająca także bez dostępu do Internetu po pierwszym otwarciu (z GitHub Pages).

## Jak działa

- Po uruchomieniu **obie daty** automatycznie przyjmują **dzisiejszy dzień wg zegara telefonu**. W razie potrzeby można je zmienić.
- **Godziny są puste** – użytkownik ręcznie wybiera początek i koniec w polach godziny.
- W pole „Szukaj” wpisz pierwsze litery obiektu, wybierz podpowiedź, a nazwa, ulica i miejscowość wypełnią się automatycznie. Miejscowość PSP jest ustawiana na miejscowość obiektu, ale **wymaga weryfikacji**.
- Dokument generuje Word (.docx), a opcja PDF otwiera drukowanie → „Zapisz jako PDF”.
- Można poprawiać adresy, dodawać nowe obiekty, eksportować i przywracać bazę JSON. Zmiany przechowuje przeglądarka **tylko na danym urządzeniu**.
- Baza startowa zawiera **73 pozycje** przepisane z dostarczonego pliku Word; niektóre dane wymagają sprawdzenia.

## Na telefonie – bez GitHub

Można użyć pojedynczego pliku `index.html` (równoważnego `Druki_PPOZ_na_telefon.html`) po jego otwarciu w przeglądarce Androida. W tym trybie niektóre przeglądarki mogą blokować zapisywanie danych lub instalację na ekranie głównym – dlatego zalecane jest uruchomienie przez GitHub Pages.

## Instalacja przez GitHub Pages

1. Utwórz **osobne** repozytorium GitHub (np. `Druki-PPOZ`).
2. Wyślij wszystkie pliki i folder `icons/` do głównego katalogu repozytorium.
3. W repozytorium: **Settings → Pages → Build and deployment → Deploy from a branch → main / (root) → Save**.
4. Otwórz wygenerowany adres strony (zwykle `https://TWOJ_LOGIN.github.io/Druki-PPOZ/`) w **Chrome na Androidzie**.
5. W menu Chrome wybierz **Zainstaluj aplikację** lub **Dodaj do ekranu głównego**.
6. Po pierwszym poprawnym otwarciu aplikacja działa offline. Aktualizacje strony mogą wymagać ponownego włączenia internetu lub odświeżenia aplikacji.

> GitHub Pages dla publicznego repozytorium udostępnia kod i osadzone w nim nazwy, adresy oraz numery kontaktowe wszystkim. Nie publikuj niejawnych danych.

## Rozwój

- `data/objects.json`: baza obiektów, format `[{id,name,city,street,source,needsReview}]`.
- `templates/szablon_odlaczenia.docx`: szablon Word z polami `{{OBJECT}}`, `{{STREET}}` itd.
- `src/app.template.html`: kod źródłowy strony i logiki.
- `vendor/jszip.min.js`: biblioteka JSZip używana offline do wypełniania DOCX (licencja MIT).
- `scripts/build.py`: generuje kompletny `index.html` z bazy, szablonu i kodu. **Python 3, bez instalacji pakietów**.

Uruchom `python scripts/build.py` po zmianach. Przy zmianie `index.html` w istniejącej instalacji podnieś numer `CACHE` w `service-worker.js` np. z `v1` na `v2`, by aktualizacja dotarła do użytkowników offline.

## Ważne

Program **nie odłącza systemu** i nie przekazuje zgłoszenia operatorowi. Trzeba potwierdzić procedurę, dane obiektu i kontakt telefoniczny, a wygenerowany druk sprawdzić i zatwierdzić. Wzór odpowiada merytorycznie dostarczonemu dokumentowi, ale nie przenosi automatycznie odręcznego podpisu i pieczątki.

## Testy automatyczne

Test przeglądarkowy (opcjonalny) wymaga `pip install playwright python-docx pypdf` oraz lokalnej przeglądarki Chromium. Uruchom `python tests/test_app_manual.py`. Sprawdza daty, puste godziny, wyszukiwanie, wypełnienie obiektu, walidację, DOCX i jednotronicowy wydruk PDF.