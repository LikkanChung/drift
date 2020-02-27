package sleepapp.backend.endpoint;

import com.sun.net.httpserver.HttpHandler;

public abstract class Endpoint implements HttpHandler {

    public abstract String getPath();

}
