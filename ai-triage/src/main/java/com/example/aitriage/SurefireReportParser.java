package com.example.aitriage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class SurefireReportParser {

    private SurefireReportParser() {
    }

    public static List<TestFailure> parseDirectory(Path surefireReportsDir) throws IOException {
        List<TestFailure> failures = new ArrayList<>();
        if (!Files.isDirectory(surefireReportsDir)) {
            return failures;
        }

        try (Stream<Path> files = Files.list(surefireReportsDir)) {
            List<Path> xmlFiles = files
                    .filter(p -> p.getFileName().toString().startsWith("TEST-")
                            && p.getFileName().toString().endsWith(".xml"))
                    .sorted(Comparator.naturalOrder())
                    .toList();

            for (Path xmlFile : xmlFiles) {
                failures.addAll(parseFile(xmlFile.toFile()));
            }
        }
        return failures;
    }

    private static List<TestFailure> parseFile(File xmlFile) {
        List<TestFailure> failures = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            NodeList testcases = doc.getElementsByTagName("testcase");
            for (int i = 0; i < testcases.getLength(); i++) {
                Element testcase = (Element) testcases.item(i);
                Element problem = firstChildElement(testcase, "failure");
                if (problem == null) {
                    problem = firstChildElement(testcase, "error");
                }
                if (problem == null) {
                    continue;
                }
                String className = testcase.getAttribute("classname");
                String testName = testcase.getAttribute("name");
                String message = problem.getAttribute("message");
                String stackTrace = problem.getTextContent();
                failures.add(new TestFailure(className, testName, message, stackTrace));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse surefire report: " + xmlFile, e);
        }
        return failures;
    }

    private static Element firstChildElement(Element parent, String tagName) {
        NodeList children = parent.getElementsByTagName(tagName);
        return children.getLength() > 0 ? (Element) children.item(0) : null;
    }
}
