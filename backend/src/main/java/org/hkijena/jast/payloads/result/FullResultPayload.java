package org.hkijena.jast.payloads.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hkijena.jast.model.entities.Result;

import java.util.ArrayList;
import java.util.List;

public class FullResultPayload extends ResultPayload {
    @JsonProperty
    private List<ResultItemPayload> items = new ArrayList<>();

    public FullResultPayload() {

    }

    public FullResultPayload(Result result) {
        super(result);
        this.items = result.getResultItems().stream().map(ResultItemPayload::new).toList();
    }

    public List<ResultItemPayload> getItems() {
        return items;
    }

    public void setItems(List<ResultItemPayload> items) {
        this.items = items;
    }
}
