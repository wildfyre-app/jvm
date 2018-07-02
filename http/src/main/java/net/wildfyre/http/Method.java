package net.wildfyre.http;

/**
 * The HTTP method used to connect to the server.
 *
 * This class is NOT part of the public API.
 */
public enum Method {
    /** Change parts of the resource. */
    PATCH,

    /** Get a resource. */
    GET,

    /** Put a new resource. */
    PUT,

    /** Get the list of options from the request. */
    OPTIONS,

    /** Delete a resource. */
    DELETE,

    /** Send a new resource. */
    POST;

    @Override
    public String toString(){
		return name();
	}
}
