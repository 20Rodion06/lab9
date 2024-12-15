package etu.java.lab9;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Program for cinema
 *
 * @author Rodion
 * @version 1.0
 * @since 2024
 */

public class Application {
    JFrame window;
    private JToolBar ButtonsPanel;
    private JButton save, open, add, edit, delete, info, filter;
    private DefaultTableModel model;
    JTable films;
    JComboBox<String> Name;
    JTextField filmName;
    private JPanel filterPanel;
    private JButton generateHTML;
    private JButton generatePDFfromHTML;
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private CountDownLatch loadComplete = new CountDownLatch(1);
    private CountDownLatch editComplete = new CountDownLatch(1);

    public void show() {
        window = new JFrame("Список фильмов");
        ButtonsPanel = new JToolBar();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(800, 400);
        window.setLocationRelativeTo(null);

        save = new JButton(new ImageIcon("./src/Icons/save-20x20.png"));
        open = new JButton(new ImageIcon("./src/Icons/open-20x20.png"));
        add = new JButton(new ImageIcon("./src/Icons/add-20x20.png"));
        edit = new JButton(new ImageIcon("./src/Icons/edit-20x20.jpg"));
        delete = new JButton(new ImageIcon("./src/Icons/trash-20x20.png"));
        info = new JButton(new ImageIcon("./src/Icons/info-20x20.png"));
        generateHTML = new JButton(new ImageIcon("./src/Icons/html-20x20.png"));
        generatePDFfromHTML = new JButton(new ImageIcon("./src/Icons/pdf-20x20.png"));

        ButtonsPanel.add(save);
        ButtonsPanel.add(open);
        ButtonsPanel.add(add);
        ButtonsPanel.add(edit);
        ButtonsPanel.add(delete);
        ButtonsPanel.add(info);
        ButtonsPanel.add(generateHTML);
        ButtonsPanel.add(generatePDFfromHTML);
        window.getContentPane().add(BorderLayout.NORTH, ButtonsPanel);

        String[] columns = {"Фильм", "Жанр", "Сеанс", "Проданные билеты"};
        Object[][] data = {
                {"Форсаж 2", "Боевик", "19:30", "147"},
                {"Стражи галактики 2", "Научная фантастика", "14:30", "182"},
                {"Матрица", "Научная фантастика", "17:00", "156"},
        };
        model = new DefaultTableModel(data, columns);
        films = new JTable(model);

        window.add(BorderLayout.CENTER, new JScrollPane(films));

        Name = new JComboBox<>(new String[]{"Фильм", "Жанр", "Сеанс"});
        filmName = new JTextField("Название фильма");
        filter = new JButton("Поиск");
        filterPanel = new JPanel();
        filterPanel.add(Name);
        filterPanel.add(filmName);
        filterPanel.add(filter);
        window.add(BorderLayout.SOUTH, filterPanel);

        add.addActionListener(new AddButtonListener());
        delete.addActionListener(new DeleteButtonListener());
        filter.addActionListener(new FilterButtonListener());
        open.addActionListener(e -> executor.submit(this::loadDataTask));
        save.addActionListener(e -> executor.submit(this::editDataTask));
        generateHTML.addActionListener(e -> executor.submit(this::generateHTMLTask));
        generatePDFfromHTML.addActionListener(new GeneratePDFReportButtonListener());

        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    saveDataToXml(file);
                }
            }
        });

        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    loadDataFromXml(file);
                }
            }
        });

        window.setVisible(true);
    }

    public void saveDataToXml(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element rootElement = doc.createElement("films");
            doc.appendChild(rootElement);

            for (int i = 0; i < model.getRowCount(); i++) {
                Element film = doc.createElement("film");

                Element name = doc.createElement("name");
                name.appendChild(doc.createTextNode((String) model.getValueAt(i, 0)));
                film.appendChild(name);

                Element genre = doc.createElement("genre");
                genre.appendChild(doc.createTextNode((String) model.getValueAt(i, 1)));
                film.appendChild(genre);

                Element time = doc.createElement("time");
                time.appendChild(doc.createTextNode((String) model.getValueAt(i, 2)));
                film.appendChild(time);

                Element ticketsSold = doc.createElement("ticketsSold");
                ticketsSold.appendChild(doc.createTextNode((String) model.getValueAt(i, 3)));
                film.appendChild(ticketsSold);

                rootElement.appendChild(film);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "да");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);

            JOptionPane.showMessageDialog(window, "Данные успешно сохранены в XML!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(window, "Ошибка сохранения данных в XML: " + e.getMessage());
        }
    }

    public void loadDataFromXml(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            model.setRowCount(0);

            NodeList filmList = doc.getElementsByTagName("film");

            for (int i = 0; i < filmList.getLength(); i++) {
                Node filmNode = filmList.item(i);

                if (filmNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element filmElement = (Element) filmNode;

                    String name = filmElement.getElementsByTagName("name").item(0).getTextContent();
                    String genre = filmElement.getElementsByTagName("genre").item(0).getTextContent();
                    String time = filmElement.getElementsByTagName("time").item(0).getTextContent();
                    String ticketsSold = filmElement.getElementsByTagName("ticketsSold").item(0).getTextContent();

                    model.addRow(new Object[]{name, genre, time, ticketsSold});
                }
            }
            JOptionPane.showMessageDialog(window, "Данные успешно загружены из XML!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(window, "Ошибка загрузки данных из XML: " + e.getMessage());
        }
    }

    public class AddButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String name = filmName.getText();
                if (name.isEmpty()) {
                    throw new EmptyFilmNameExcept("Название фильма не должно быть пустым.");
                }
                model.addRow(new Object[]{name, "Жанр", "Сеанс", "0"});
                JOptionPane.showMessageDialog(window, "Фильм \"" + name + "\" добавлен!");
            } catch (EmptyFilmNameExcept ex) {
                JOptionPane.showMessageDialog(window, ex.getMessage());
            }
        }
    }

    public class DeleteButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                int selectedRow = films.getSelectedRow();
                if (selectedRow == -1) {
                    throw new NoSelectExcept("Выберите фильм, который надо удалить.");
                }
                model.removeRow(selectedRow);
                JOptionPane.showMessageDialog(window, "Фильм удален!");
            } catch (NoSelectExcept ex) {
                JOptionPane.showMessageDialog(window, ex.getMessage());
            }
        }
    }

    public class FilterButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String searchCriteria = filmName.getText().toLowerCase();
            String selectedOption = (String) Name.getSelectedItem();
            StringBuilder result = new StringBuilder("Результаты поиска:\n");
            boolean found = false;

            for (int i = 0; i < model.getRowCount(); i++) {
                String film = (String) model.getValueAt(i, 0);
                String genre = (String) model.getValueAt(i, 1);
                String time = (String) model.getValueAt(i, 2);

                if ((selectedOption.equals("Фильм") && film.toLowerCase().contains(searchCriteria)) ||
                        (selectedOption.equals("Жанр") && genre.toLowerCase().contains(searchCriteria)) ||
                        (selectedOption.equals("Сеанс") && time.toLowerCase().contains(searchCriteria))) {
                    result.append(film).append(", ").append(genre).append(", ").append(time).append("\n");
                    found = true;
                }
            }

            if (found) {
                JOptionPane.showMessageDialog(window, result.toString());
            } else {
                JOptionPane.showMessageDialog(window, "Фильм не найден.");
            }
        }
    }

    public class GenerateHTMLReportButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                generateReport(file);
            }
        }

        void generateReport(File file) {
            try {
                StringBuilder htmlContent = new StringBuilder("""
                    <html>
                    <head>
                        <title>Film Report</title>
                        <style>
                            body { 
                                font-family: Arial, sans-serif;
                                margin: 0;
                                padding: 0;
                            }
                            .header {
                                background-color: #006699;
                                color: white;
                                padding: 20px;
                                overflow: hidden; /* Clear the float */
                            }
                            .header h1 {
                                float: left;
                                margin: 0;
                                font-size: 40px;
                            }
                            .subtitle {
                                float: right;
                                font-size: 14px;
                                opacity: 0.9;
                                margin: 0;
                                line-height: 40px;
                            }
                            .content {
                                padding: 20px;
                            }
                            table {
                                width: 100%;
                                border-collapse: collapse;
                                margin-top: 20px;
                            }
                            th, td {
                                border: 1px solid #ddd;
                                padding: 12px;
                                text-align: left;
                            }
                            th {
                                background-color: #f5f5f5;
                                font-weight: normal;
                            }
                            tr:nth-child(even) {
                                background-color: #f9f9f9;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h1>Отчет</h1>
                            <span class="subtitle">По лабораторной работе</span>
                        </div>
                        <div class="content">
                            <table>
                                <tr>
                                    <th>Фильм</th>
                                    <th>Жанр</th>
                                    <th>Сеанс</th>
                                    <th>Проданные</th>
                                </tr>
                """);

                for (int i = 0; i < model.getRowCount(); i++) {
                    String film = (String) model.getValueAt(i, 0);
                    String genre = (String) model.getValueAt(i, 1);
                    String time = (String) model.getValueAt(i, 2);
                    String ticketsSold = (String) model.getValueAt(i, 3);

                    htmlContent.append("<tr>")
                            .append("<td>").append(film).append("</td>")
                            .append("<td>").append(genre).append("</td>")
                            .append("<td>").append(time).append("</td>")
                            .append("<td>").append(ticketsSold).append("</td>")
                            .append("</tr>");
                }

                htmlContent.append("</table></div></body></html>");
                Files.write(file.toPath(), htmlContent.toString().getBytes());

                JOptionPane.showMessageDialog(window, "Отчет успешно создан!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(window, "Ошибка создания отчета: " + ex.getMessage());
            }
        }
    }

    public class GeneratePDFReportButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser htmlChooser = new JFileChooser();
            htmlChooser.setDialogTitle("Выберите HTML-файл для конвертации");

            if (htmlChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                File htmlFile = htmlChooser.getSelectedFile();

                JFileChooser pdfChooser = new JFileChooser();
                pdfChooser.setDialogTitle("Сохранить PDF как");
                if (pdfChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                    File pdfFile = pdfChooser.getSelectedFile();
                    if (!pdfFile.getName().toLowerCase().endsWith(".pdf")) {
                        pdfFile = new File(pdfFile.getAbsolutePath() + ".pdf");
                    }
                    convertHTMLtoPDF(htmlFile, pdfFile);
                }
            }
        }

        void convertHTMLtoPDF(File htmlFile, File pdfFile) {
            try {
                PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
                PdfDocument pdf = new PdfDocument(writer);

                HtmlConverter.convertToPdf(new FileInputStream(htmlFile), pdf);

                JOptionPane.showMessageDialog(window, "PDF успешно создан!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(window, "Ошибка создания PDF-файла: " + ex.getMessage());
            }
        }
    }

    public void loadDataTask() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                loadDataFromXml(file);
                JOptionPane.showMessageDialog(window, "Данные успешно загружены!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(window, "Ошибка загрузки данных: " + e.getMessage());
        } finally {
            loadComplete.countDown();
        }
    }

    public void editDataTask() {
        try {
            loadComplete.await();
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                saveDataToXml(file);
                JOptionPane.showMessageDialog(window, "Данные успешно сохранены!");
            }
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(window, "Операция прервана: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(window, "Ошибка редактирования данных: " + e.getMessage());
        } finally {
            editComplete.countDown();
        }
    }

    public void generateHTMLTask() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(window) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                new GenerateHTMLReportButtonListener().generateReport(file);
                JOptionPane.showMessageDialog(window, "HTML-отчет успешно создан!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(window, "Ошибка создания отчета: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Application().show();
    }
}