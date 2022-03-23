package eu.europa.ec.itb.xml.upload;

import eu.europa.ec.itb.validation.commons.web.BaseFileController;
import eu.europa.ec.itb.xml.util.FileManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class FileControllerTest {

    private FileController createFileController() throws Exception {
        var fileController = new FileController();
        var fileManagerField = BaseFileController.class.getDeclaredField("fileManager");
        fileManagerField.setAccessible(true);
        var fileManager = mock(FileManager.class);
        doReturn("ITB-UUID.xml").when(fileManager).getInputFileName(any());
        fileManagerField.set(fileController, fileManager);
        return fileController;
    }

    @Test
    void testGetInputFileName() throws Exception {
        var result = createFileController().getInputFileName("UUID");
        assertEquals("ITB-UUID.xml", result);
    }

}
