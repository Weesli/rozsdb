package net.weesli.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter@Setter
public class DataMeta {

    private String id;
    private String createdAt;
    private String updatedAt;
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

}
