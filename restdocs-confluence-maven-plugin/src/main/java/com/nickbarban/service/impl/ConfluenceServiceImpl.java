package com.nickbarban.service.impl;

import com.nickbarban.model.ConfluencePage;
import com.nickbarban.model.ConfluencePageFactory;
import com.nickbarban.rest.ConfluenceClient;
import com.nickbarban.service.ConfluenceService;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.util.Optional;

public class ConfluenceServiceImpl implements ConfluenceService {

    private final Log log;

    private ConfluenceClient confluenceClient;

    private final String space;

    public ConfluenceServiceImpl(String userName, String password, HttpUrl endpoint, Log log, String space) {
        this.log = log;
        confluenceClient = new ConfluenceClient(userName, password, endpoint, log);
        this.space = space;
    }

    @Override
    public String saveOrUpdate(final String ancestorId, final String content, final String title) throws MojoExecutionException {
        Optional<ConfluencePage> pageOptional = Optional.ofNullable(confluenceClient.getPageByTitleAndSpace(title, space));

        if (pageOptional.isPresent()) {
            ConfluencePage page = confluenceClient.getPage(pageOptional.get().getId());
            if (pageContentIsChanged(page, content)) {
                updatePageContent(page, content);
                return confluenceClient.updatePage(page);
            } else {
                return page.getId();
            }
        } else {
            ConfluencePage page = ConfluencePageFactory.createStoragePage(title, content, space, ancestorId);
            return confluenceClient.createPage(page);
        }
    }

    private void updatePageContent(ConfluencePage page, String content) {
        page.getBody().getStorage().setValue(content);
        page.getVersion().increment();
    }

    private boolean pageContentIsChanged(ConfluencePage page, String content) {
        return !StringUtils.equals(page.getBody().getStorage().getValue(), content);
    }
}
