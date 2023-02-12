/**
 * Author:  Paul Gates
 * 
 * Desc:    Just some static procedures for
 *          nicely printing data types for
 *          this project
 */
package common;

import static common.ConsoleLogger.*;

/* import data types to print */
import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;

public class DataTypeLoggerUtil 
{
    public static  void log_request
        (
        Request request
        )
    {
        log_msg("Request:"); //todo
        log_msg("  Operation Type: " + request.getOperationType().toString());
        log_msg("  Player Name: " +    request.getName());
    }

    public static void log_response
        (
        Response response
        )
    {
        log_msg("Response:");
        log_msg( "Response Type: " + response.getResponseType().toString() );
    }
}
