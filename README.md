Rush Hour NxM - Java Command Line Game

A Programozási technológiák (BPI1116L / BMI1303L) kurzus féléves beadandó projektje. Egy parancssoros (command line), kétszemélyes, stratégiai csúszó-blokk táblajáték (Rush Hour NxM), amely Java 21-ben, Maven build tool használatával készült

🛠️ Felhasznált Technológiák és Elvárások:

  - Nyelv és Keretrendszer: Java 21, Maven

  - Architektúra: Rétegzett csomagszerkezet (model, service, persistence, ui, exception) az alacsony kötés és a magas kohézió jegyében.

  - Design Patternek és OO Alapelvek:

    - Builder Minta a BoardState biztonságos felépítéséhez.

    - Immutability (Módosíthatatlanság): Immutábilis Value Objectek (Position, Vehicle, BoardState) final mezőkkel és védett másolatokkal.

    - Object metódusok felülírása: Egyedi equals, hashCode és toString implementációk.

  - Funkcionális Programozás: Java Stream API (filter, map, flatMap, findFirst, collect) az ütközésvizsgálathoz és járműkezeléshez.

  - Kivételkezelés: Saját ellenőrzött kivétel (InvalidMoveException) szabálytalan lépésekhez.

  - Adatbázis & Perzisztencia: Beágyazott H2 adatbázis tiszta JDBC (PreparedStatement, Connection) használatával a győzelmek mentésére és a High Score táblázathoz.

  - Fájlkezelés: TXT alapú pályabetöltés/mentés, valamint XML alapú játékállás-mentés és visszatöltés.

  - Naplózás: Logback (SLF4J) alapú naplózás fájlba (rushhour.log).

  - Minőségbiztosítás & Tesztelés:

    - JUnit 5 és Mockito egységtesztek (>80% JaCoCo kódlefedettség az üzleti logikán).

    - Maven Checkstyle plugin a Clean Code elnevezési konvenciók ellenőrzésére. 

🚀 Build és Futtatás:
  A projekt építéséhez, a Checkstyle ellenőrzéshez, az egységtesztek futtatásához és a JaCoCo lefedettségi jelentés generálásához futtasd az alábbi parancsot a projekt gyökerében:
    
    mvn clean install


A minden függőséget tartalmazó önálló (fat) JAR fájl futtatása:

    java -jar target/rush-hour-1.0-SNAPSHOT-jar-with-dependencies.jar

🎮 Játékmenet és Parancsok:
Indulás után a program bekéri a neved, majd betölti a pályát (ha létezik `board.txt` a gyökérben, abból, különben a beégetett alapértelmezett 6x12-es pályáról).

📄 Pályafájl formátum (`board.txt`)

A koordináták **0-alapúak** (a sorindex `0`-tól `rows - 1`-ig, az oszlopindex `0`-tól `cols - 1`-ig tart; a `Position` fejpozíciója a jármű bal felső / kezdő cellája):

1. **1. sor:** `<sorok_száma> <oszlopok_száma>` (pl. `6 12`)
2. **2. sor:** `<exitA_sor> <exitA_oszlop>` — a humán játékos (`A`) célkijárata a tábla szélén (pl. `2 11`)
3. **3. sor:** `<exitB_sor> <exitB_oszlop>` — a gépi ellenfél (`B`) célkijárata a tábla szélén (pl. `3 0`)
4. **4. sor:** `<járművek_száma>` (pl. `8`)
5. **Következő sorok:** `<azonosító> <kezdő_sor> <kezdő_oszlop> <hossz> <HORIZONTAL|VERTICAL>`

Példa (`src/main/resources/board.txt`):

```text
6 12
2 11
3 0
8
A 2 1 2 HORIZONTAL
B 3 9 2 HORIZONTAL
C 1 4 3 VERTICAL
D 2 7 3 VERTICAL
E 0 2 2 HORIZONTAL
F 4 3 2 VERTICAL
G 5 5 3 HORIZONTAL
H 0 8 2 VERTICAL

Elérhető parancsok a konzolon:
* move <jármű_id> <up|down|left|right> <lépésszám> — Jármű csúsztatása (pl. move A right 2 vagy move C down 1)
* save [fájlnév.txt] — Pálya mentése szöveges fájlba
* load [fájlnév.txt] — Pálya betöltése szöveges fájlból
* savexml [fájlnév.xml] — Játékállás mentése XML fájlba
* loadxml [fájlnév.xml] — Játékállás visszatöltése XML fájlból
* highscore — High Score táblázat megjelenítése a H2 adatbázisból
* help — Parancsok listázása
* exit — Kilépés a játékból
    
