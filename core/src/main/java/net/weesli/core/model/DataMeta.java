package net.weesli.core.model;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;

import java.util.List;
@CompiledJson
public class DataMeta {

    @JsonAttribute(name = "id")
    private String id;
    @JsonAttribute(name = "createdAt")
    private String createdAt;
    @JsonAttribute(name = "updatedAt")
    private String updatedAt;
    @JsonAttribute(name = "fields")
    private List<String> fields;

    public DataMeta(){

    }

    public DataMeta(String id, String createdAt, String updatedAt, List<String> fields) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fields = fields;
    }


    public boolean hasField(String field){
        return fields != null && fields.contains(field);
    }

    public void changeFields(List<String> fields){
        this.fields = fields;
    }

    public void changeUpdatedAt(String updatedAt){
        this.updatedAt = updatedAt;
    }
    @Override
    public String toString() {
        return "DataMeta{" +
                "id='" + id + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", fields=" + fields +
                '}';
    }

    public String getId() {
        return id;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
