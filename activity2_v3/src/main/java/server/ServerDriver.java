/**
 * Author:  Paul Gates
 * 
 * Desc:    Handles the high level operations of
 *          the server I/O. This module does not
 *          deal with socekets or hadnling of
 *          connections directly, but will make
 *          calls to the lower level SocketBaseServer
 *          class
 */
package server;

import server.Leaderboard;
import server.SockBaseServer;
import static common.ConsoleLogger.*;

public class ServerDriver 
{
    /**
     * Application entry point
     */
    public static void main
        (
        String[] args
        )
    {
        /**
         * Get the arguments from the command line
         * User must have specified the port number
         */
        assert_msg_exit( args.length >= 1, "Usage: java ServerDriver <port>" );
        log_err( args[0] );
        /**
         * Initialize the server
         */
        try
            {
            SockBaseServer server = new SockBaseServer( Integer.parseInt( args[0] ) );
            }
        catch(Exception e)
            {
            log_err( "Error initializing server: Check that the port used is "
                    + " open or is a valid port number" );
            e.printStackTrace();
            }

    }
    
}
