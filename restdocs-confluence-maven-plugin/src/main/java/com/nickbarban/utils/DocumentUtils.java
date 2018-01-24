package com.nickbarban.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentUtils {

    private final Log log;

    public DocumentUtils(Log log) {
        this.log = log;
    }

    /**
     * Prepare children files.
     * <p>Prepare content of children files generated by Spring REST Docs<\p>
     *
     * @param childrenDirectoryName  - children directory name.
     * @param generatedDocsDirectory - generated REST docs directory.
     * @throws MojoExecutionException
     */
    public Map<String, String> prepareChildrenFiles(final String childrenDirectoryName,
                                                    final File generatedDocsDirectory) throws MojoExecutionException {

        File childrenDirectory = Arrays.stream(generatedDocsDirectory.listFiles())
                .filter(file -> file.getName().equalsIgnoreCase(childrenDirectoryName))
                .findFirst().orElse(null);

        Map<String, String> childrenContent = new HashMap<>();
        if (childrenDirectory == null) {
            log.debug(String.format("There is not children directory in %s directory", generatedDocsDirectory.getName()));
        } else {
            File sourceDirectory = prepareDirectory(childrenDirectory);
            File[] childrenFiles = sourceDirectory.listFiles();

            if (childrenFiles == null || childrenFiles.length == 0) {
                log.debug(String.format("There are no children in %s directory", generatedDocsDirectory.getName()));
            } else {
                log.debug(String.format("There are %s children files in %s directory: %s",
                        childrenFiles.length,
                        childrenDirectory.getPath(),
                        Stream.of(childrenFiles).map(File::getName).collect(Collectors.joining(",", "[", "]"))));
                final Map<String, Exception> errors = new HashMap<>();
                Arrays.asList(childrenFiles)
                        .forEach(file -> {
                            try {
                                childrenContent.put(file.getName(), readFile(sourceDirectory, file.getName()));
                            } catch (MojoExecutionException e) {
                                errors.put(file.getName(), e);
                            }
                        });

                if (!errors.isEmpty()) {
                    String errorBody = "Exception while read-write file [%s]. Message: %s";
                    String joinedErrorMessage = errors.entrySet().stream()
                            .map(entry -> String.format(errorBody, entry.getKey(), entry.getValue().getMessage()))
                            .collect(Collectors.joining(". "));
                    throw new MojoExecutionException(joinedErrorMessage);
                }
            }
        }
        return childrenContent;
    }

    /**
     * Prepare parent file.
     * <p>Prepare content of parent file generated by Spring REST Docs<\p>
     *
     * @param indexFileName          - parent file name.
     * @param generatedDocsDirectory - generated REST docs directory.
     * @throws MojoExecutionException
     */
    public String prepareParentFile(String indexFileName, File generatedDocsDirectory) throws MojoExecutionException {
        File sourceDirectory = prepareDirectory(generatedDocsDirectory);
        return readFile(sourceDirectory, indexFileName);
    }

    /**
     * Remove directory with all subfolders and files recursively.
     *
     * @param directory - directory to be cleaned.
     * @throws MojoExecutionException
     */
    public void clean(File directory) throws MojoExecutionException {
        try {
            Files.walkFileTree(directory.toPath(), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Objects.requireNonNull(dir);
                    Objects.requireNonNull(attrs);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }
            });
        } catch (IOException e) {
            throw new MojoExecutionException("Error deleting folder " + directory.getName(), e);
        }
    }

    private void processFile(File sourceDirectory, File targetDirectory, String fileName) throws MojoExecutionException {
        String content = readFile(sourceDirectory, fileName);
        writeFile(targetDirectory, fileName, content);
    }

    private String readFile(File directory, String fileName) throws MojoExecutionException {
        log.debug(String.format("Read from file [%s/%s]", directory.getName(), fileName));

        File file = new File(directory, fileName);

        try {
            return Files.lines(Paths.get(file.getPath())).collect(Collectors.joining("\n\r"));
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading file " + fileName, e);
        }
    }

    private void writeFile(File directory, String fileName, String content) throws MojoExecutionException {
        log.debug(String.format("Write to file [%s/%s]", directory.getName(), fileName));

        File targetFile = new File(directory, fileName);
        FileWriter w = null;

        try {
            w = new FileWriter(targetFile);

            w.write(content);
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file " + targetFile, e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private File prepareDirectory(File directory) {

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory;
    }

    /**
     * Convert camelcase file name to sentence with capitalized words.
     * <pre>
     * getPageTitle("cat.html") = "Cat"
     * getPageTitle("cAt.pdf") = "C At"
     * getPageTitle("ab de fg.adoc")   = "Ab  de  fg"
     * getPageTitle("ab   de fg.txt") = "Ab     de   fg"
     * getPageTitle("ab:cd:ef.bat")   = "Ab : cd : ef"
     * getPageTitle("number5.cmd")    = "Number 5"
     * getPageTitle("fooBar.wiki")     = "Foo Bar"
     * getPageTitle("foo200Bar.confluence")  = "Foo 200 Bar"
     * getPageTitle("ASFRules.html5")   = "ASF Rules"
     * getPageTitle("autoGeneratedRestApiDocumentation.xml")   = "Auto Generated Rest Api Documentation"
     * </pre>
     *
     * @param fileName - camelcase file name.
     * @return
     */
    public String getPageTitle(String fileName) {
        StringUtils.isNotEmpty(fileName);
        return FilenameUtils.removeExtension(
                StringUtils.capitalize(
                        StringUtils.join(
                                StringUtils.splitByCharacterTypeCamelCase(fileName),
                                ' '))).trim();
    }
}