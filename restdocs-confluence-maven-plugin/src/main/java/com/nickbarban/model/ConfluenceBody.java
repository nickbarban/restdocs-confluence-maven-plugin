package com.nickbarban.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfluenceBody {
    private ConfluenceStorage storage;

    public ConfluenceStorage getStorage() {
        return storage;
    }

    public void setStorage(ConfluenceStorage storage) {
        this.storage = storage;
    }
}
