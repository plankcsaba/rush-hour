package hu.nye.progtech.rushhour.persistence;

import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Osztály a játékállás szöveges (TXT) és XML fájlokba történő mentéséhez és betöltéséhez.
 */
public class FileBoardRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBoardRepository.class);

    /**
     * Betölti a pályát egy szöveges fájlból.
     * Formátum:
     * 1. sor: ROWS COLS
     * 2. sor: EXIT_A_ROW EXIT_A_COL
     * 3. sor: EXIT_B_ROW EXIT_B_COL
     * További sorok: ID ROW COL LENGTH ORIENTATION
     */
    public BoardState loadFromTxt(Path path) throws IOException {
        LOGGER.info("Pálya betöltése TXT fájlból: {}", path);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String[] dims = reader.readLine().trim().split("\\s+");
            int rows = Integer.parseInt(dims[0]);
            int cols = Integer.parseInt(dims[1]);

            String[] exitAParts = reader.readLine().trim().split("\\s+");
            Position exitA = new Position(Integer.parseInt(exitAParts[0]), Integer.parseInt(exitAParts[1]));

            String[] exitBParts = reader.readLine().trim().split("\\s+");
            Position exitB = new Position(Integer.parseInt(exitBParts[0]), Integer.parseInt(exitBParts[1]));

            List<Vehicle> vehicles = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                char id = parts[0].charAt(0);
                int r = Integer.parseInt(parts[1]);
                int c = Integer.parseInt(parts[2]);
                int len = Integer.parseInt(parts[3]);
                Orientation orientation = Orientation.valueOf(parts[4].toUpperCase());
                vehicles.add(new Vehicle(id, new Position(r, c), len, orientation));
            }

            return BoardState.builder()
                    .withDimensions(rows, cols)
                    .withExitA(exitA)
                    .withExitB(exitB)
                    .withVehicles(vehicles)
                    .build();
        }
    }

    /**
     * Kimenti az aktuális pályát szöveges fájlba.
     */
    public void saveToTxt(BoardState state, Path path) throws IOException {
        LOGGER.info("Pálya mentése TXT fájlba: {}", path);
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(state.getRows() + " " + state.getCols());
            writer.newLine();
            writer.write(state.getExitA().getRow() + " " + state.getExitA().getCol());
            writer.newLine();
            writer.write(state.getExitB().getRow() + " " + state.getExitB().getCol());
            writer.newLine();
            for (Vehicle v : state.getVehicles()) {
                writer.write(String.format("%c %d %d %d %s",
                        v.getId(),
                        v.getHeadPosition().getRow(),
                        v.getHeadPosition().getCol(),
                        v.getLength(),
                        v.getOrientation().name()));
                writer.newLine();
            }
        }
    }

    /**
     * Kimenti az aktuális játékállást XML fájlba (plusz pontos követelmény).
     */
    public void saveToXml(BoardState state, Path path) throws Exception {
        LOGGER.info("Pálya mentése XML fájlba: {}", path);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element root = doc.createElement("rushHourBoard");
        root.setAttribute("rows", String.valueOf(state.getRows()));
        root.setAttribute("cols", String.valueOf(state.getCols()));
        doc.appendChild(root);

        Element exitAEl = doc.createElement("exitA");
        exitAEl.setAttribute("row", String.valueOf(state.getExitA().getRow()));
        exitAEl.setAttribute("col", String.valueOf(state.getExitA().getCol()));
        root.appendChild(exitAEl);

        Element exitBEl = doc.createElement("exitB");
        exitBEl.setAttribute("row", String.valueOf(state.getExitB().getRow()));
        exitBEl.setAttribute("col", String.valueOf(state.getExitB().getCol()));
        root.appendChild(exitBEl);

        Element vehiclesEl = doc.createElement("vehicles");
        root.appendChild(vehiclesEl);

        for (Vehicle v : state.getVehicles()) {
            Element vEl = doc.createElement("vehicle");
            vEl.setAttribute("id", String.valueOf(v.getId()));
            vEl.setAttribute("row", String.valueOf(v.getHeadPosition().getRow()));
            vEl.setAttribute("col", String.valueOf(v.getHeadPosition().getCol()));
            vEl.setAttribute("length", String.valueOf(v.getLength()));
            vEl.setAttribute("orientation", v.getOrientation().name());
            vehiclesEl.appendChild(vEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(path.toUri())));
    }

    /**
     * Visszatölt egy játékállást XML fájlból (plusz pontos követelmény).
     */
    public BoardState loadFromXml(Path path) throws Exception {
        LOGGER.info("Pálya betöltése XML fájlból: {}", path);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(path.toUri()));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        int rows = Integer.parseInt(root.getAttribute("rows"));
        int cols = Integer.parseInt(root.getAttribute("cols"));

        Element exitAEl = (Element) root.getElementsByTagName("exitA").item(0);
        Position exitA = new Position(
                Integer.parseInt(exitAEl.getAttribute("row")),
                Integer.parseInt(exitAEl.getAttribute("col"))
        );

        Element exitBEl = (Element) root.getElementsByTagName("exitB").item(0);
        Position exitB = new Position(
                Integer.parseInt(exitBEl.getAttribute("row")),
                Integer.parseInt(exitBEl.getAttribute("col"))
        );

        List<Vehicle> vehicles = new ArrayList<>();
        NodeList vehicleNodes = root.getElementsByTagName("vehicle");
        for (int i = 0; i < vehicleNodes.getLength(); i++) {
            Element vEl = (Element) vehicleNodes.item(i);
            char id = vEl.getAttribute("id").charAt(0);
            int r = Integer.parseInt(vEl.getAttribute("row"));
            int c = Integer.parseInt(vEl.getAttribute("col"));
            int len = Integer.parseInt(vEl.getAttribute("length"));
            Orientation orientation = Orientation.valueOf(vEl.getAttribute("orientation"));
            vehicles.add(new Vehicle(id, new Position(r, c), len, orientation));
        }

        return BoardState.builder()
                .withDimensions(rows, cols)
                .withExitA(exitA)
                .withExitB(exitB)
                .withVehicles(vehicles)
                .build();
    }
}