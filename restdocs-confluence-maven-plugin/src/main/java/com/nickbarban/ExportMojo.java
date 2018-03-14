package com.nickbarban;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
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
 */

import com.nickbarban.service.ConfluenceService;
import com.nickbarban.service.impl.ConfluenceServiceImpl;
import com.nickbarban.utils.DocumentUtils;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Goal which exports restdocs files.
 *
 * @phase process-sources
 */
@Mojo(name = "report")
public class ExportMojo extends AbstractMojo {

    /**
     * Location of the resources folder.
     *
     * @parameter property="basedir"
     */
    @Parameter(property = "generatedDocsDirectory", defaultValue = "${project.build.directory}/generated-docs")
    private File generatedDocsDirectory;

    /**
     * Location of the resources folder.
     *
     * @parameter property="children"
     */
    @Parameter(property = "childrenDirectoryName", defaultValue = "children")
    private String childrenDirectoryName;

    /**
     * Name of the generated base html file.
     *
     * @parameter property="index.html"
     */
    @Parameter(property = "indexFile", defaultValue = "index.html")
    private String indexFileName;

    /**
     * Confluence anchestor id
     *
     * @required
     */
    @Parameter(property = "anchestorId")
    private String anchestorId;

    /**
     * Confluence credential username.
     *
     * @required
     */
    @Parameter(property = "userName")
    private String userName;

    /**
     * Confluence credential password.
     *
     * @required
     */
    @Parameter(property = "password")
    private String password;

    /**
     * Confluence base url port.
     *
     * @parameter property="8080"
     */
    @Parameter(property = "port", defaultValue = "8080")
    private Integer port;

    /**
     * Confluence base url.
     *
     * @required
     */
    @Parameter(property = "confluenceBaseUrl")
    private String confluenceBaseUrl;

    /**
     * Confluence base url scheme.
     *
     * @parameter property="http"
     */
    @Parameter(property = "protocol", defaultValue = "http")
    private String protocol;

    /**
     * Confluence base url host.
     */
    @Parameter(property = "host")
    private String host;

    /**
     * Confluence space key.
     *
     * @required
     */
    @Parameter(property = "space")
    private String space;

    private DocumentUtils documentUtils;

    private ConfluenceService confluenceClient;

    private HttpUrl initEndpoint() {

        if (StringUtils.isNotEmpty(confluenceBaseUrl)) {
            getLog().info(String.format("Init endpoint: %s", confluenceBaseUrl));
            return HttpUrl.parse(confluenceBaseUrl);
        } else {
            getLog().info(String.format("Init endpoint: scheme: %s, port: %s, host: %s, confluenceBaseUrl: %s",
                    protocol, port, host, confluenceBaseUrl));
            return new HttpUrl.Builder()
                    .scheme(protocol)
                    .host(host)
                    .port(port).build();
        }
    }

    public void execute() throws MojoExecutionException {
        documentUtils = new DocumentUtils(getLog());
        logAllProperties();

        HttpUrl endpoint = initEndpoint();
        confluenceClient = new ConfluenceServiceImpl(userName, password, endpoint, getLog(), space);

        if (generatedDocsDirectory == null) {
            getLog().debug(String.format("There is not directory %s", generatedDocsDirectory.getName()));
        } else if (generatedDocsDirectory.listFiles() == null) {
            getLog().debug(String.format("Directory %s is empty", generatedDocsDirectory.getName()));
        } else {
            String parentPageContent = documentUtils.prepareParentFile(indexFileName, generatedDocsDirectory);
            String parentPageTitle = documentUtils.getPageTitle(indexFileName);
            final String parentId = confluenceClient.saveOrUpdate(anchestorId, parentPageContent, parentPageTitle);
            Map<String, String> childrenContent = documentUtils.prepareChildrenFiles(childrenDirectoryName, generatedDocsDirectory);
            Map<String, Throwable> errors = new HashMap<>();
            childrenContent.entrySet().forEach(child -> {
                String childPageTitle = documentUtils.getPageTitle(child.getKey());

                try {
                    confluenceClient.saveOrUpdate(parentId, child.getValue(), childPageTitle);
                } catch (MojoExecutionException e) {
                    errors.put(childPageTitle, e);
                }
            });

            if (!errors.isEmpty()) {
                String messages = errors.entrySet().stream()
                        .map(e -> e.getKey() + "::" + e.getValue())
                        .collect(Collectors.joining("\r\n", "[msg-start]", "[msg-end]"));
                throw new MojoExecutionException(String.format("Errors while saveOrUpdate children of parent %s. Messages:\r\n%s",
                        parentId, messages));
            }
        }
    }

    private void logAllProperties() {
        getLog().debug(String.format("confluenceBaseUrl=%s", this.confluenceBaseUrl));
        getLog().debug(String.format("host=%s", this.host));
        getLog().debug(String.format("protocol=%s", this.protocol));
        getLog().debug(String.format("port=%s", this.port));
        getLog().debug(String.format("anchestorId=%s", this.anchestorId));
        getLog().debug(String.format("childrenDirectoryName=%s", this.childrenDirectoryName));
        getLog().debug(String.format("generatedDocsDirectory=%s", this.generatedDocsDirectory));
        getLog().debug(String.format("indexFileName=%s", this.indexFileName));
        getLog().debug(String.format("password=%s", this.password));
        getLog().debug(String.format("userName=%s", this.userName));
        getLog().debug(String.format("space=%s", this.space));
    }
}
