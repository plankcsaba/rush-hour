package hu.nye.progtech.rushhour.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hu.nye.progtech.rushhour.model.BoardState;
import hu.nye.progtech.rushhour.model.Orientation;
import hu.nye.progtech.rushhour.model.Position;
import hu.nye.progtech.rushhour.model.Vehicle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileBoardRepositoryTest {

    @TempDir
    Path tempDir;

    private FileBoardRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FileBoardRepository();
    }

    @Test
    void saveToTxtAndLoadFromTxtShouldRoundTripBoardState() throws Exception {
        // Given
        BoardState original = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 1), 2, Orientation.HORIZONTAL),
                        new Vehicle('C', new Position(0, 3), 2, Orientation.VERTICAL)
                ))
                .build();
        Path txtFile = tempDir.resolve("board.txt");

        // When
        repository.saveToTxt(original, txtFile);
        BoardState loaded = repository.loadFromTxt(txtFile);

        // Then
        assertEquals(original.getRows(), loaded.getRows());
        assertEquals(original.getCols(), loaded.getCols());
        assertEquals(original.getExitA(), loaded.getExitA());
        assertEquals(original.getExitB(), loaded.getExitB());
        assertEquals(original.getVehicles(), loaded.getVehicles());
    }

    @Test
    void saveToXmlAndLoadFromXmlShouldRoundTripBoardState() throws Exception {
        // Given
        BoardState original = BoardState.builder()
                .withDimensions(4, 6)
                .withExitA(new Position(1, 5))
                .withExitB(new Position(2, 0))
                .withVehicles(List.of(
                        new Vehicle('A', new Position(1, 2), 2, Orientation.HORIZONTAL),
                        new Vehicle('B', new Position(2, 3), 2, Orientation.HORIZONTAL)
                ))
                .build();
        Path xmlFile = tempDir.resolve("state.xml");

        // When
        repository.saveToXml(original, xmlFile);
        BoardState loaded = repository.loadFromXml(xmlFile);

        // Then
        assertEquals(original.getRows(), loaded.getRows());
        assertEquals(original.getCols(), loaded.getCols());
        assertEquals(original.getExitA(), loaded.getExitA());
        assertEquals(original.getExitB(), loaded.getExitB());
        assertEquals(original.getVehicles(), loaded.getVehicles());
    }

    @Test
    void loadFromTxtShouldThrowExceptionWhenFileIsMalformed() throws IOException {
        // Given
        Path brokenFile = tempDir.resolve("broken.txt");
        Files.writeString(brokenFile, "INVALID_HEADER\n@@@\n");

        // When & Then
        assertThrows(Exception.class, () -> repository.loadFromTxt(brokenFile));
    }

    @Test
    void loadFromXmlShouldThrowExceptionWhenXmlIsMalformed() throws IOException {
        // Given
        Path brokenXml = tempDir.resolve("broken.xml");
        Files.writeString(brokenXml, "<not-valid-xml>");

        // When & Then
        assertThrows(Exception.class, () -> repository.loadFromXml(brokenXml));
    }
}