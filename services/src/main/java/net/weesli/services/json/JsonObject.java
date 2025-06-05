package net.weesli.services.json;

public class JsonObject {

    private Object value;

    public JsonObject(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
    public String getAsString() {
        return (String) value;
    }

    public int getAsInt() {
        try {
            return Integer.parseInt(value.toString());
        }catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    public long getAsLong() {
        try {
            return Long.parseLong(value.toString());
        }catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

}
