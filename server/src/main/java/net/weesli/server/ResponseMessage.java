package net.weesli.server;

public record ResponseMessage(String status, String message) {

    public static ResponseMessage success(String message) {
        return new ResponseMessage("success", message);
    }

    public static ResponseMessage error(String message) {
        return new ResponseMessage("error", message);
    }


    @Override
    public String toString() {
        return String.format("{\"status\":\"%s\", \"message\":\"%s\"}", status, message);
    }
}
