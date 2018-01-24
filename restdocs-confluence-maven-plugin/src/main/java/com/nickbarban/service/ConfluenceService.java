package com.nickbarban.service;

import org.apache.maven.plugin.MojoExecutionException;

public interface ConfluenceService {
    String saveOrUpdate(String anchestorId, String content, String title) throws MojoExecutionException;
}
