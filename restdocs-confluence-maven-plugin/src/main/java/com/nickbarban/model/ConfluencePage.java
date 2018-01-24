package com.nickbarban.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluencePage {

    public static final String PAGE = "page";

    private String id;

    private String type;

    private String status;

    private String title;

    private ConfluenceSpace space;

    private ConfluenceBody body;

    private List<ConfluenceAncestor> ancestors;

    private ConfluencePageVersion version;

    public ConfluenceSpace getSpace() {
        return space;
    }

    public void setSpace(ConfluenceSpace space) {
        this.space = space;
    }

    public ConfluenceBody getBody() {
        return body;
    }

    public void setBody(ConfluenceBody body) {
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ConfluenceAncestor> getAncestors() {
        return ancestors;
    }

    public void setAncestors(List<ConfluenceAncestor> ancestors) {
        this.ancestors = ancestors;
    }

    public void setVersion(ConfluencePageVersion version) {
        this.version = version;
    }

    public ConfluencePageVersion getVersion() {
        return version;
    }
}
