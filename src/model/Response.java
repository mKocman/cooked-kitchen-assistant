package model;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private Status status;
    private Object payload; // to send data back (like a list of ingredients)
    private String message; // error or success message

    public Response(Status status, Object payload, String message) {
        this.status = status;
        this.payload = payload;
        this.message = message;
    }

    public Status getStatus() { return status; }
    public Object getPayload() { return payload; }
    public String getMessage() { return message; }
}