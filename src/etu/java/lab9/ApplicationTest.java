package etu.java.lab9;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationTest {

    private Application app;
    private DefaultTableModel model;

    @Before
    public void setUp() {
        app = new Application();
        app.show();
        model = (DefaultTableModel) app.films.getModel();
    }

    @Test
    public void testAddButtonListener_ValidInput() {
        app.filmName.setText("Test Film");
        ActionEvent mockEvent = Mockito.mock(ActionEvent.class);
        Application.AddButtonListener addButtonListener = app.new AddButtonListener();

        addButtonListener.actionPerformed(mockEvent);

        assertEquals(4, model.getRowCount());
        assertEquals("Test Film", model.getValueAt(3, 0));
    }

    @Test
    public void testDeleteButtonListener_ValidSelection() {
        app.films.setRowSelectionInterval(0, 0);
        ActionEvent mockEvent = Mockito.mock(ActionEvent.class);
        Application.DeleteButtonListener deleteButtonListener = app.new DeleteButtonListener();

        deleteButtonListener.actionPerformed(mockEvent);

        assertEquals(2, model.getRowCount());
        assertEquals("Стражи галактики 2", model.getValueAt(0, 0));
    }

    @Test
    public void testFilterButtonListener_SuccessfulFilmSearch() {
        app.Name.setSelectedItem("Фильм");
        app.filmName.setText("Матрица");
        ActionEvent mockEvent = Mockito.mock(ActionEvent.class);
        Application.FilterButtonListener filterButtonListener = app.new FilterButtonListener();

        filterButtonListener.actionPerformed(mockEvent);

        assertTrue(app.filmName.getText().contains("Матрица"));
    }

    @Test
    public void testSaveAndLoadXml() throws Exception {
        File tempFile = File.createTempFile("test", ".xml");
        tempFile.deleteOnExit();

        app.saveDataToXml(tempFile);
        model.setRowCount(0);
        app.loadDataFromXml(tempFile);

        assertEquals(3, model.getRowCount());
        assertEquals("Форсаж 2", model.getValueAt(0, 0));
    }

    @Test
    public void testGenerateHTMLReport() throws Exception {
        File tempHtmlFile = File.createTempFile("test-report", ".html");
        tempHtmlFile.deleteOnExit();

        app.new GenerateHTMLReportButtonListener().generateReport(tempHtmlFile);

        assertTrue(tempHtmlFile.exists());
        String content = Files.readString(tempHtmlFile.toPath());
        assertTrue(content.contains("Форсаж 2"));
        assertTrue(content.contains("Стражи галактики 2"));
    }

    @Test
    public void testConvertHTMLtoPDF() throws Exception {
        File tempHtmlFile = File.createTempFile("test-report", ".html");
        File tempPdfFile = File.createTempFile("test-report", ".pdf");
        tempHtmlFile.deleteOnExit();
        tempPdfFile.deleteOnExit();

        app.new GenerateHTMLReportButtonListener().generateReport(tempHtmlFile);
        app.new GeneratePDFReportButtonListener().convertHTMLtoPDF(tempHtmlFile, tempPdfFile);

        assertTrue(tempPdfFile.exists());
        assertTrue(tempPdfFile.length() > 0);
    }

    @After
    public void tearDown() {
        if (app.window != null) {
            app.window.dispose();
        }
    }
}