# Druki PPOŻ — oficjalny formularz NOMA 2

Aplikacja PWA na telefon (Android / Chrome) do przygotowania wniosku o **czasowe odłączenie obiektu od monitoringu pożarowego**.

**Druk w PDF ma pozostać w układzie formularza NOMA 2 z dokumentu przekazanego przez użytkownika.** Dlatego aplikacja **nie tworzy własnego układu dokumentu**: wyświetla oryginalny formularz jako nieruchome tło i uzupełnia wyłącznie pola zmienne. Tło wyczyszczono z przykładowych danych i fotografii podpisu. Wynik należy zawsze porównać wizualnie z wzorem przed przesłaniem.

## Co robi aplikacja

- Wyszukuje obiekt już po pierwszych literach jego nazwy, miejscowości lub ulicy; wypełnia nazwę, ulicę i miasto. Baza początkowa: 73 obiekty z przesłanego Worda. Niektóre adresy są niepełne i wymagają weryfikacji.
- Obie daty automatycznie ustawia na bieżący dzień według telefonu. Godziny rozpoczęcia i zakończenia użytkownik podaje ręcznie. Waliduje kolejność dat i godzin.
- Wypełnia w oryginalnym formularzu NOMA 2: rodzaj prac, obiekt, ulicę, miasto, miejscowość PSP, daty/godziny, drugą osobę kontaktową i telefon, imię i nazwisko podpisującego oraz — jeżeli zapisano — obraz podpisu.
- Odmienia miejscowości w miejscowniku po słowie **„w”** (np. **Gliwice → Gliwicach**, **Katowice → Katowicach**). Zapisany adres pozostaje w formie podstawowej. Obie formy na wniosku (miejscowość obiektu i PSP) można niezależnie poprawić. Gdy nazwa nie jest znana aplikacji, ręczne uzupełnienie odmiany jest obowiązkowe.
- **Przypisuje podpis do osoby**, np. Marcin Kacperski. W sekcji kontaktu wybierz podpisującego, a następnie prześlij PNG/JPG/WebP albo Word DOCX zawierający **dokładnie jeden** obraz. Obraz zostanie zapisany na tym urządzeniu. Następnym razem podpis pojawi się automatycznie po wybraniu tej osoby; działa również, gdy imię i nazwisko wpisane są w odwrotnej kolejności.
- Pozwala usunąć i wymienić podpis osoby. **Obrazy podpisów nigdy nie są wgrywane do repozytorium ani do GitHub Pages.** Trzymane są w `localStorage` tylko w danej przeglądarce pod adresem strony; wyczyszczenie danych strony usuwa je, a instalacja innej przeglądarki nie przejmuje podpisów.
- Pozwala dodawać i poprawiać obiekty oraz eksportować bazę obiektów do JSON. **Eksport bazy nie obejmuje zapisanych podpisów.**
- Tworzy **bezpośrednio jednostronicowy plik PDF A4** za pomocą przycisku „Pobierz PDF · 1 strona A4”, bez otwierania okna drukowania. Generator Worda o innym układzie został wycofany, aby nie pomylić go z formularzem operatora.

## Uruchamianie na Androidzie

1. Otwórz `https://marcinkacp-sketch.github.io/Druki-PPOZ/` w Google Chrome.
2. W menu Chrome wybierz **Zainstaluj aplikację** / **Dodaj do ekranu głównego**.
3. Wpisz godziny, wybierz obiekt i osobę. Wczytaj podpis tej osoby tylko raz, jeśli ma być automatycznie umieszczany w kolejnych drukach.
4. Kliknij **Pobierz PDF**, otwórz pobrany plik i sprawdź jego wygląd przed wysłaniem.

Po pierwszym otwarciu strona może działać offline dzięki Service Worker. Obiekty i podpisy przechowują się lokalnie; bez instalacji odrębnego serwera bazodanowego.

## Kod i budowanie

- `src/app.template.html` — formularz oraz mechanizm uzupełniania oficjalnego druku i lokalnego podpisu.
- `templates/noma2_blank.png` — tło powstałe z przekazanego PDF, z którego usunięto przykładowe dane i podpis.
- `data/objects.json` — dane obiektów.
- `vendor/jszip.min.js` — biblioteka JSZip, wykorzystywana do lokalnego wyciągnięcia obrazu podpisu z pliku `.docx` (MIT).
- `scripts/build.py` — generuje `index.html` z bazy, kodu i JSZip. Python 3 (bez zewnętrznych zależności).
- `manifest.webmanifest` i `service-worker.js` — instalacja i pamięć offline.
- `tests/test_smoke.py` — testy kontrolne `python -m unittest discover -s tests`.

Po zmianach: `python scripts/build.py`, sprawdź wydruk i zwiększ wersję `CACHE` w `service-worker.js` przy zmianach zasobów offline.

## Zastrzeżenia

Sam wydruk **nie powoduje odłączenia monitoringu i nie wysyła powiadomienia do NOMA 2**. Przed użyciem trzeba sprawdzić aktualność danych, właściwą jednostkę PSP, okres odłączenia i procedury operatora, a także potwierdzić, czy NOMA 2 dopuszcza taką formę podpisu w przesyłanym wniosku. Umieszczenie obrazu podpisu nie nadaje samo z siebie dokumentowi statusu kwalifikowanego podpisu elektronicznego.

**Uwaga:** repozytorium GitHub jest publiczne. Nie umieszczaj pliku z rzeczywistym podpisem, skanu dokumentu z podpisem ani wypełnionych druków w kodzie repozytorium.

## Bezpośredni eksport PDF

Przycisk **Pobierz PDF · 1 strona A4** generuje jeden arkusz PDF (595 × 841 pkt) bez okna drukowania i bez serwera. Dane są rysowane na bazie nieruchomego tła oryginalnego formularza NOMA 2. Podpis jest pobierany z pamięci bieżącej przeglądarki i nie jest publikowany w repozytorium. PDF zawiera rasteryzowany obraz całej strony, dlatego tekst nie jest selekcjonowalny.

Jeżeli aplikacja była już instalowana, zamknij ją i otwórz ponownie, aby pobrać nowszą wersję PWA (cache `druki-ppoz-v4`).


## Aplikacja Android (APK, niezależna od PWA)

Zbudowany projekt Android znajduje się w `app/`. APK uruchamia lokalny formularz NOMA 2 offline, z brandingiem **ATOM SERWIS**, czerwono-granatową szatą graficzną i wozem strażackim. Zapisywanie PDF korzysta z systemowego okna zapisu Android. Podpisy są zapisywane lokalnie, nie trafiają do publicznego repozytorium ani APK.

Zobacz instrukcję [ANDROID-INSTRUKCJA.md](ANDROID-INSTRUKCJA.md) i zakładkę GitHub Actions `Android APK • Druki PPOŻ`. Testowy APK diagnostyczny może wymagać odinstalowania przy przyszłej przebudowie, bo ma tymczasowy klucz debug. Do trwałych aktualizacji niezbędny jest stabilny klucz wydania.


## Android 1.1.0: zakładki i etapy

Ekran Start pokazuje osobne kafelki: **Nowe zgłoszenie**, **Moje obiekty**, **Osoby i podpisy**, **Dokumenty PDF** i **Ustawienia**. Zgłoszenie ma 4 kroki: Obiekt i zakres prac → Termin odłączenia → Podpis → Podgląd / PDF. Niebiesko-czerwona szata ATOM SERWIS występuje tylko w interfejsie, nie na oficjalnym druku NOMA 2.

Historia PDF to lista nazw i dat lokalnych wygenerowań — rzeczywiste PDF zapisuje się w wybranym przez użytkownika folderze Androida. Brak automatycznej wysyłki do operatora. Baza 73 obiektów i podpisy pozostają lokalne.

**Podpisywanie aktualizacji:** pierwszy diagnostyczny APK był podpisany tymczasowym kluczem debug. Został przygotowany osobny stały klucz wydania, ale aktywacja zautomatyzowanych podpisanych wydań wymaga ustawienia sekretów GitHub. Do czasu tej konfiguracji workflow publikuje tylko testowy APK. Patrz `ANDROID-INSTRUKCJA.md`. Nigdy nie dodawaj plików klucza ani danych podpisu do publicznego repozytorium.