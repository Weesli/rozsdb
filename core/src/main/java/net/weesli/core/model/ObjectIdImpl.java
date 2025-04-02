package net.weesli.core.model;

import net.weesli.api.model.ObjectId;

import java.security.SecureRandom;
import java.util.Objects;

public class ObjectIdImpl implements ObjectId {
    private final String objectId;
    public ObjectIdImpl(){
        objectId = generate();
    }

    public ObjectIdImpl(String objectId){
        this.objectId = objectId;
    }

    public static ObjectIdImpl valueOf(String value){
        if(value == null || value.isEmpty()){
            throw new IllegalArgumentException("ObjectId cannot be null or empty");
        }
        return new ObjectIdImpl(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ObjectIdImpl other)) return false;
        return Objects.equals(objectId, other.objectId);
    }

    @Override
    public String toString() {
        return objectId;
    }

    @Override
    public int hashCode() {
        return objectId.hashCode();
    }

    @Override
    public String getObjectId() {
        return objectId;
    }

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private String generate() {
        StringBuilder sb = new StringBuilder(36);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
