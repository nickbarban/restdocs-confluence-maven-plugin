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

import com.nickbarban.utils.DocumentUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Goal which cleanes generated file.
 *
 * @phase process-sources
 */
@Mojo(name = "clean")
public class CleanMojo extends AbstractMojo {

    /**
     * Location of the result output folder.
     *
     * @parameter property="project.build.directory"
     * @required
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/confluence")
    private File outputDirectory;

    private DocumentUtils documentUtils = new DocumentUtils(getLog());

    public void execute() throws MojoExecutionException {
        documentUtils.clean(outputDirectory);
    }


}
