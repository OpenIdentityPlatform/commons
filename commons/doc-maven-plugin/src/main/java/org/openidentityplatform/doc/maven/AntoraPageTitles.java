/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.doc.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.Section;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Validates the document titles Antora uses for generated pages. */
final class AntoraPageTitles {
    private AntoraPageTitles() {
    }

    static void validate(Path modulesDirectory) throws IOException, MojoFailureException {
        List<Path> pages;
        try (Stream<Path> paths = Files.walk(modulesDirectory)) {
            pages = paths.filter(Files::isRegularFile)
                    .map(modulesDirectory::relativize)
                    .filter(path -> path.getNameCount() >= 3 && "pages".equals(path.getName(1).toString()))
                    .filter(path -> path.getFileName().toString().endsWith(".adoc"))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (pages.isEmpty()) {
            throw new MojoFailureException("No generated Antora pages in " + modulesDirectory);
        }

        // Parsing the full page can fall back to the first section heading.
        Options siteHeader = Options.builder().safe(SafeMode.SAFE)
                .attributes(Map.of("env-site", "")).parseHeaderOnly(true).build();
        List<String> untitledPages = new ArrayList<>();
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.javaExtensionRegistry().includeProcessor(
                    new RootPartialIncludeProcessor(modulesDirectory.resolve("ROOT/partials")));
            for (Path page : pages) {
                Path input = modulesDirectory.resolve(page);
                Document document = asciidoctor.loadFile(input.toFile(), siteHeader);
                if (!hasDocumentTitle(document)) {
                    untitledPages.add(page.toString());
                }
            }
        }
        if (!untitledPages.isEmpty()) {
            throw new MojoFailureException("Generated Antora pages without document titles:\n"
                    + String.join("\n", untitledPages));
        }
    }

    private static boolean hasDocumentTitle(Document document) {
        String title = document.getDoctitle();
        // A :title: attribute can supply a resolved title even when the header is missing.
        return title != null && !title.isBlank() && document.findBy(Map.of()).stream()
                .filter(Section.class::isInstance)
                .map(Section.class::cast)
                .anyMatch(section -> "header".equals(section.getSectionName()));
    }

    private static final class RootPartialIncludeProcessor extends IncludeProcessor {
        private static final String PREFIX = "ROOT:partial$";
        private final Path partialsDirectory;

        private RootPartialIncludeProcessor(Path partialsDirectory) {
            this.partialsDirectory = partialsDirectory.toAbsolutePath().normalize();
        }

        @Override
        public boolean handles(String target) {
            return target.startsWith(PREFIX);
        }

        @Override
        public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
            Path partial = partialsDirectory.resolve(target.substring(PREFIX.length())).normalize();
            if (!partial.startsWith(partialsDirectory)) {
                throw new IllegalArgumentException("Invalid Antora ROOT partial: " + target);
            }
            String path = partial.toString();
            try {
                reader.pushInclude(Files.readString(partial), path, path, 1, attributes);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read Antora partial " + partial, e);
            }
        }
    }
}
