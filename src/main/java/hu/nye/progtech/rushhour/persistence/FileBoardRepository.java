package hu.nye.progtech.rushhour.persistence;

import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A pálya- és játékállás-perzisztenciát (TXT és XML) valamint a bemeneti pályák validációját
 * megvalósító osztály.
 */
public class FileBoardRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBoardRepository.class);
    private static final String CLASSPATH_BOARD_RESOURCE = "/board.txt";

    /**
     * Betölti a classpath-on található alapértelmezett {@code /board.txt} pályát, ha létezik.
     *
     * @return a beolvasott és validált {@link BoardState}, ha elérhető és érvényes
     */
    public Optional<BoardState> loadDefaultFromClasspath() {
        InputStream stream = FileBoardRepository.class.getResourceAsStream(CLASSPATH_BOARD_RESOURCE);
        if (stream == null) {
            LOGGER.info("Nem található {} a classpath-on.", CLASSPATH_BOARD_RESOURCE);
            return Optional.empty();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            BoardState parsed = parseTxtFromReader(reader);
            validateBoardState(parsed);
            LOGGER.info("Pálya sikeresen betöltve a classpath-ról: {}", CLASSPATH_BOARD_RESOURCE);
            return Optional.of(parsed);
        } catch (Exception e) {
            LOGGER.warn("Érvénytelen vagy hibás a classpath-os {} pálya.", CLASSPATH_BOARD_RESOURCE, e);
            return Optional.empty();
        }
    }

    /**
     * Elmenti a megadott {@link BoardState} állapotot TXT fájlba.
     *
     * @param state a mentendő pályaállapot
     * @param path  a célfájl elérési útja
     * @throws IOException írási hiba esetén
     */
    public void saveToTxt(BoardState state, Path path) throws IOException {
        validateBoardState(state);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(state.getRows() + " " + state.getCols());
            writer.newLine();
            writer.write(state.getExitA().getRow() + " " + state.getExitA().getCol());
            writer.newLine();
            writer.write(state.getExitB().getRow() + " " + state.getExitB().getCol());
            writer.newLine();
            writer.write(String.valueOf(state.getVehicles().size()));
            writer.newLine();
            for (Vehicle vehicle : state.getVehicles()) {
                writer.write(vehicle.getId()
                        + " " + vehicle.getHeadPosition().getRow()
                        + " " + vehicle.getHeadPosition().getCol()
                        + " " + vehicle.getLength()
                        + " " + vehicle.getOrientation().name());
                writer.newLine();
            }
        }
    }

    /**
     * Beolvassa és validálja a pályát a megadott TXT fájlból.
     *
     * @param path a forrásfájl elérési útja
     * @return a validált {@link BoardState}
     * @throws IOException              olvasási hiba esetén
     * @throws IllegalArgumentException ha a fájl formátuma vagy tartalma érvénytelen
     */
    public BoardState loadFromTxt(Path path) throws IOException {
        LOGGER.info("Pálya betöltése TXT fájlból: {}", path);
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            BoardState state = parseTxtFromReader(reader);
            validateBoardState(state);
            return state;
        }
    }

    /**
     * Elmenti a megadott {@link BoardState} állapotot XML fájlba.
     *
     * @param state a mentendő játékállapot
     * @param path  a cél XML fájl elérési útja
     * @throws IOException írási hiba esetén
     */
    public void saveToXml(BoardState state, Path path) throws IOException {
        validateBoardState(state);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<board rows=\"").append(state.getRows())
                .append("\" cols=\"").append(state.getCols()).append("\">\n");
        sb.append("  <exitA row=\"").append(state.getExitA().getRow())
                .append("\" col=\"").append(state.getExitA().getCol()).append("\"/>\n");
        sb.append("  <exitB row=\"").append(state.getExitB().getRow())
                .append("\" col=\"").append(state.getExitB().getCol()).append("\"/>\n");
        sb.append("  <vehicles>\n");
        for (Vehicle v : state.getVehicles()) {
            sb.append("    <vehicle id=\"").append(v.getId())
                    .append("\" row=\"").append(v.getHeadPosition().getRow())
                    .append("\" col=\"").append(v.getHeadPosition().getCol())
                    .append("\" length=\"").append(v.getLength())
                    .append("\" orientation=\"").append(v.getOrientation().name()).append("\"/>\n");
        }
        sb.append("  </vehicles>\n");
        sb.append("</board>\n");
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Beolvassa és validálja a játékállást XML fájlból.
     *
     * @param path a forrás XML fájl elérési útja
     * @return a validált {@link BoardState}
     * @throws Exception ha a fájl nem olvasható, érvénytelen XML vagy hibás a pályaállapot
     */
    public BoardState loadFromXml(Path path) throws Exception {
        String xml = Files.readString(path, StandardCharsets.UTF_8);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        Element root = doc.getDocumentElement();
        int rows = Integer.parseInt(root.getAttribute("rows"));
        int cols = Integer.parseInt(root.getAttribute("cols"));

        Element exitAEl = (Element) root.getElementsByTagName("exitA").item(0);
        Element exitBEl = (Element) root.getElementsByTagName("exitB").item(0);
        if (exitAEl == null || exitBEl == null) {
            throw new IllegalArgumentException("Hiányzó exitA vagy exitB elem az XML fájlban.");
        }

        Position exitA = new Position(
                Integer.parseInt(exitAEl.getAttribute("row")),
                Integer.parseInt(exitAEl.getAttribute("col"))
        );
        Position exitB = new Position(
                Integer.parseInt(exitBEl.getAttribute("row")),
                Integer.parseInt(exitBEl.getAttribute("col"))
        );

        NodeList vehicleNodes = root.getElementsByTagName("vehicle");
        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < vehicleNodes.getLength(); i++) {
            Element vEl = (Element) vehicleNodes.item(i);
            String idStr = vEl.getAttribute("id");
            if (idStr.isEmpty()) {
                throw new IllegalArgumentException("Hiányzó jármű azonosító az XML fájlban.");
            }
            char id = idStr.charAt(0);
            int row = Integer.parseInt(vEl.getAttribute("row"));
            int col = Integer.parseInt(vEl.getAttribute("col"));
            int length = Integer.parseInt(vEl.getAttribute("length"));
            Orientation orientation = Orientation.valueOf(vEl.getAttribute("orientation").toUpperCase());
            vehicles.add(new Vehicle(id, new Position(row, col), length, orientation));
        }

        BoardState state = BoardState.builder()
                .withDimensions(rows, cols)
                .withExitA(exitA)
                .withExitB(exitB)
                .withVehicles(vehicles)
                .build();

        validateBoardState(state);
        return state;
    }

    /**
     * Ellenőrzi, hogy a {@link BoardState} minden kényszert kielégít-e:
     * pozitív méretek, határon belüli és nem átfedő járművek, valamint a tábla szélén lévő kijáratok.
     *
     * @param state a vizsgálandó pályaállapot
     * @throws IllegalArgumentException ha bármelyik feltétel sérül
     */
    public void validateBoardState(BoardState state) {
        if (state == null) {
            throw new IllegalArgumentException("A pályaállapot nem lehet null.");
        }
        int rows = state.getRows();
        int cols = state.getCols();
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("A pálya méreteinek pozitívnak kell lenniük: " + rows + "x" + cols);
        }

        validateExit(state.getExitA(), rows, cols, "exitA");
        validateExit(state.getExitB(), rows, cols, "exitB");
        if (state.getExitA().equals(state.getExitB())) {
            throw new IllegalArgumentException("Az exitA és exitB kijáratok nem eshetnek egyazon mezőre.");
        }

        Set<Position> occupiedPositions = new HashSet<>();
        Set<Character> seenIds = new HashSet<>();

        for (Vehicle vehicle : state.getVehicles()) {
            char upperId = Character.toUpperCase(vehicle.getId());
            if (!seenIds.add(upperId)) {
                throw new IllegalArgumentException("Duplikált jármű azonosító a pályán: " + vehicle.getId());
            }
            if (vehicle.getLength() <= 0) {
                throw new IllegalArgumentException("A jármű hosszának pozitívnak kell lennie: " + vehicle.getId());
            }

            List<Position> cells = vehicle.getOccupiedPositions();
            for (Position pos : cells) {
                if (!isWithinBounds(pos, rows, cols)) {
                    throw new IllegalArgumentException("A(z) " + vehicle.getId()
                            + " jármű a táblán kívüli mezőre esik: " + pos);
                }
                if (!occupiedPositions.add(pos)) {
                    throw new IllegalArgumentException("Ütköző / átfedő járművek a(z) " + pos + " mezőn.");
                }
            }
        }
    }

    private BoardState parseTxtFromReader(BufferedReader reader) throws IOException {
        String dimsLine = nextNonBlankLine(reader);
        if (dimsLine == null) {
            throw new IllegalArgumentException("Hiányzik a pálya méretsora.");
        }
        String[] dimsParts = dimsLine.trim().split("\\s+");
        if (dimsParts.length != 2) {
            throw new IllegalArgumentException("Érvénytelen pálya méretsor: " + dimsLine);
        }
        int rows = Integer.parseInt(dimsParts[0]);
        int cols = Integer.parseInt(dimsParts[1]);

        Position exitA = parsePositionLine(nextNonBlankLine(reader), "exitA");
        Position exitB = parsePositionLine(nextNonBlankLine(reader), "exitB");

        String countLine = nextNonBlankLine(reader);
        if (countLine == null) {
            throw new IllegalArgumentException("Hiányzik a járművek darabszáma.");
        }
        int vehicleCount = Integer.parseInt(countLine.trim());
        if (vehicleCount < 0) {
            throw new IllegalArgumentException("A járművek darabszáma nem lehet negatív.");
        }

        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < vehicleCount; i++) {
            String line = nextNonBlankLine(reader);
            if (line == null) {
                throw new IllegalArgumentException("Kevesebb járműsor található a várt " + vehicleCount + " darabnál.");
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 5 || parts[0].length() != 1) {
                throw new IllegalArgumentException("Érvénytelen járműsor: " + line);
            }
            char id = parts[0].charAt(0);
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            int length = Integer.parseInt(parts[3]);
            Orientation orientation = Orientation.valueOf(parts[4].toUpperCase());
            vehicles.add(new Vehicle(id, new Position(row, col), length, orientation));
        }

        return BoardState.builder()
                .withDimensions(rows, cols)
                .withExitA(exitA)
                .withExitB(exitB)
                .withVehicles(vehicles)
                .build();
    }

    private String nextNonBlankLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                return trimmed;
            }
        }
        return null;
    }

    private Position parsePositionLine(String line, String label) {
        if (line == null) {
            throw new IllegalArgumentException("Hiányzó " + label + " koordinátasor.");
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Érvénytelen " + label + " koordinátasor: " + line);
        }
        return new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private void validateExit(Position exit, int rows, int cols, String label) {
        if (exit == null || isOutsideBounds(exit, rows, cols)) {
            throw new IllegalArgumentException("A(z) " + label + " kijárat a táblán kívül van: " + exit);
        }
        boolean onEdge = exit.getRow() == 0
                || exit.getRow() == rows - 1
                || exit.getCol() == 0
                || exit.getCol() == cols - 1;
        if (!onEdge) {
            throw new IllegalArgumentException("A(z) " + label + " kijáratnak a tábla szélén kell lennie: " + exit);
        }
    }

    private boolean isWithinBounds(Position pos, int rows, int cols) {
        return !isOutsideBounds(pos, rows, cols);
    }

    private boolean isOutsideBounds(Position pos, int rows, int cols) {
        return pos.getRow() < 0 || pos.getRow() >= rows
                || pos.getCol() < 0 || pos.getCol() >= cols;
    }
}