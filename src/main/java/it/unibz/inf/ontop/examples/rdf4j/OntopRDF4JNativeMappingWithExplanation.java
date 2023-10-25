package it.unibz.inf.ontop.examples.rdf4j;

/*
 * #%L
 * ontop-quest-owlapi3
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import it.unibz.inf.ontop.injection.OntopSQLOWLAPIConfiguration;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepository;
import it.unibz.inf.ontop.rdf4j.repository.OntopRepositoryConnection;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.algebra.Str;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

public class OntopRDF4JNativeMappingWithExplanation {

    private static final String owlFile = "src/main/resources/example/books/exampleBooks.owl";
    private static final String obdaFile = "src/main/resources/example/books/exampleBooks.obda";
    private static final String propertyFile = "src/main/resources/example/books/exampleBooks.properties";
    private static final String sparqlFile = "src/main/resources/example/books/q1.rq";
    private static final String explanationFile = "src/main/resources/example/books/explanation";

    public static void main(String[] args) {
        try {
            OntopRDF4JNativeMappingWithExplanation example = new OntopRDF4JNativeMappingWithExplanation();
            example.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        String sparqlQuery = Files.readString(Paths.get(sparqlFile));
        System.out.println("The input SPARQL query:");
        System.out.println("=======================");
        System.out.println(sparqlQuery);
        System.out.println();

        System.out.println("Results :");
        System.out.println("=======================");
        OntopSQLOWLAPIConfiguration configuration = OntopSQLOWLAPIConfiguration.defaultBuilder()
                .ontologyFile(owlFile)
                .nativeOntopMappingFile(obdaFile)
                .propertyFile(propertyFile)
                .enableTestMode()
                .build();

        Repository repo = OntopRepository.defaultRepository(configuration);
        repo.init();

        try (
                RepositoryConnection conn = repo.getConnection() ;
                TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery).evaluate()
        ) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                System.out.println(bindingSet);
            }

            // Only for debugging purpose, not for end users: this will redo the query reformulation, which can be expensive
            String sqlQuery = ((OntopRepositoryConnection) conn).reformulateIntoNativeQuery(sparqlQuery);
            System.out.println();
            System.out.println("The reformulated SQL query:");
            System.out.println("=======================");
            System.out.println(sqlQuery);
        }
        repo.shutDown();
        explanationService();
    }

    public void explanationService() throws IOException {
        List<String> explanationLines = readLinesFromFile(explanationFile);
        List<String> sparqlLines = readLinesFromFile(sparqlFile);
        System.out.println("Explanation :");
        System.out.println("=======================");
        for (String line : explanationLines) {
            String[] parts = line.split("\t");

            if (parts.length >= 3) {
                String className1 = parts[0];
                String className2 = parts[1];
                Double similarityDegree = Double.parseDouble(parts[2]);
                String explanation = "It retrived : ["+className1 +","+ className2 +"] "+parts[4];
                String class1Explanation = parts[6];
                String class2Explanation = parts[8];

                boolean containsWord = containsWordInList(sparqlLines, className1,className2);

                if (containsWord && similarityDegree > 0.7) {
                    System.out.println(explanation);
                    System.out.println(class1Explanation);
                    System.out.println(class2Explanation);
                    System.out.println();

                }

            }
        }
    }
    private boolean containsWordInList(List<String> list, String word, String word2) {
        for (String item : list) {
            if (containsWholeWord(item,word2)) {
                return true;
            }
            if (containsWholeWord(item,word2)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> readLinesFromFile(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    private static boolean containsWholeWord(String inputText, String word) {
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
        Matcher matcher = pattern.matcher(inputText);
        return matcher.find();
    }


}
