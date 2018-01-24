package com.nickbarban.model;

import java.util.Collections;

public class ConfluencePageFactory {

    public static ConfluencePage createStoragePage(String title, String content, String space, String ancestorId) {
        ConfluencePage page = new ConfluencePage();
        page.setType(ConfluencePage.PAGE);
        page.setTitle(title);
        page.setBody(createStorageBody(content));
        page.setSpace(createConfluenceSpace(space));
        page.setAncestors(Collections.singletonList(createAncestor(ancestorId)));
        page.setVersion(createVersion(1));
        return page;
    }

    private static ConfluencePageVersion createVersion(int number) {
        ConfluencePageVersion version = new ConfluencePageVersion();
        version.setNumber(number);
        return version;
    }

    private static ConfluenceAncestor createAncestor(String ancestorId) {
        ConfluenceAncestor ancestor = new ConfluenceAncestor();
        ancestor.setId(ancestorId);
        return ancestor;
    }

    private static ConfluenceSpace createConfluenceSpace(String space) {
        ConfluenceSpace confluenceSpace = new ConfluenceSpace();
        confluenceSpace.setKey(space);
        return confluenceSpace;
    }

    private static ConfluenceBody createStorageBody(String content) {
        ConfluenceStorage storage = new ConfluenceStorage();
        storage.setValue(content);
        storage.setRepresentation(ConfluenceStorage.STORAGE);
        ConfluenceBody body = new ConfluenceBody();
        body.setStorage(storage);
        return body;
    }
}
