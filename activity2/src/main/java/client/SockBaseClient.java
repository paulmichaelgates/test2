package client;

import java.net.*;
import java.util.Scanner;
import java.io.*;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Request.OperationType;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Response.ResponseType;
import buffers.ResponseProtos.Entry;

import  static common.ConsoleLogger.*;

class SockBaseClient {

    /**
     * Constants
     */
    final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    /**
     * Variables
     */
    Socket serverSock = null;
    OutputStream out = null;
    InputStream in = null;

    public static void main (String args[]) throws Exception 
    {
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }
        /**
         * Start the application
         */
        SockBaseClient client = new SockBaseClient( host, port );
    }

    public SockBaseClient
        (
        String host,
        int port
        )
    {
        /**
         * Set up the shut down thread
         */
        Runtime.getRuntime().addShutdownHook( getShutDownThread() );

        // Ask user for username
        System.out.println("Please provide your name for the server.");

        String strToSend = "";

        /**
         * Read in the client's name
         */
         strToSend = readSafe( this.stdin );

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();

        Response response = null;

        try {
            // connect to the server
            serverSock = new Socket( host, port );

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo( out );

            // read from the server
            response = Response.parseDelimitedFrom( in );


        } catch (Exception e) {
            log_err( "Could not connect to server. Check that the server is running\n"
                        + "and that the host and port match. " );
        }

        /**
         * Check for error
         */
        if( response.getResponseType() == ResponseType.ERROR )
            {
            log_err( "Error: " + response.getMessage() );
            System.exit(1);
            }
    
        // print the server response. 
        System.out.println( response.getMessage() );

        /**
         * Main program loop
         */
        boolean quit = false;
        while( quit == false )
            {
            /**
             * Main program loop
             */
            System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");
                
            /**
             * blocking call to read user input
             */
            String menuSelectionStr = readSafe( stdin );

            /**
             * get the user input
             * REQ3: The client shall be able to select from a menu of options
             */
            int menuSelectionEnum = -1;
            while( true )
                {
                try
                    {
                    menuSelectionEnum  = Integer.parseInt( menuSelectionStr );
                    break;
                    }
                catch( NumberFormatException e )
                    {
                    System.out.println("Invalid selection. Please try again.");
                    menuSelectionStr = readSafe( stdin );
                    continue;
                    }

                }


                switch( menuSelectionEnum )
                    {
                    /**
                     * Send a leaderboard request to the server
                     */
                    case ( 1 ):
                        op = Request.newBuilder()
                            .setOperationType( Request.OperationType.LEADER )
                            .build();
        
                        writeRequest( op, serverSock );

                        break;
                    /**
                     * Send a game request to the server
                     */
                    case ( 2 ):
                        op = Request.newBuilder()
                            .setOperationType( Request.OperationType.NEW )
                            .build();
        
                        writeRequest( op, serverSock );
                        break;
                
                    /**
                     * Send a quit request to the server
                     */
                    case ( 3 ):
                        op = Request.newBuilder()
                            .setOperationType( Request.OperationType.QUIT )
                            .build();
        
                        writeRequest( op, serverSock );
                        break;
                    
                    default:
                        System.out.println("Invalid selection. Please try again.");
                        break;
        
                    }
        
                /**
                 * Read the response from the server
                 */
                response = readResponse( serverSock );

                // TODO debug
                //log_success( "Response from server: " + response.toString() );

                /*
                * Handle the response from the server
                */
                handleServerResponse( response );
                }

        /**
         * Close the connection to the server
         */

        safeClose();
    }

    /**
     * handlServerResponse
     * 
     * Desc: The server can send a response with the
     *       following types:
     *      
     *        - GREETING
     *           this is handled prior to this function being called
     *           at this time
     *
     *        - LEADER 
     *          the server is sending us the leaderboard. We should
     *          print it out to the console.
     * 
     *        - PLAY
     *           a game is on going. Responses from the server will be
     *           handled depending on the control flags second, which
     *           indicates that we should guess the second client, and
     *           eval, which indicates whether or not the move we made
     *           was correct.
     *
     *        - BYE
     *          our connection with the server is ending (probably due)
     *          to a quit request.
     *
     * 
     */
    public void handleServerResponse
        (
        Response response
        )
    {
        switch( response.getResponseType() )
            {
            case LEADER:
                printOutLeader( response );
                break;

            case PLAY:
                enterGame( response );
                break;

            case BYE:
                System.out.println( "Goodbye!" );
                getShutDownThread().start();

                /**
                 * Should not return from shut down thread 
                 * but in case we do for some reason shut down
                 * with error
                 */
                System.exit( 0 );
                break;

            default:
                log_err( "Unknown response type" );
                break;
            }
    }

    /**
     * enterGame
     * Desc:    Enter a game with the server
     */
    public void enterGame
        (
        Response response
        )
    {
        /**
         * Print out the game message
         */
        System.out.println( response.getMessage() );
        System.out.println( response.getBoard() );

        String strToSend = "";
        do
            {
            /**
             * Decide what the operation type we should send back is 
             * in addition to what to print out to the user
             */
            Request.OperationType opType = response.getSecond() ? Request.OperationType.TILE2 : Request.OperationType.TILE1;
            String prompt = response.getSecond() ? "Enter the second tile: " : "Enter the first tile: ";
            
            System.out.println( prompt );
            strToSend = readSafe( stdin );
            
            if( strToSend.toLowerCase().equals( "exit" ) )
                {
                break;
                }

            /**
             * Get the response from the server
             */
            response = handleClientGameMove( opType, strToSend );

            /**
             * Handle error messages from the server
             */
            if( response.getResponseType() == Response.ResponseType.ERROR )
                {
                System.out.println( response.getMessage() );
                }

            if( response.getResponseType() == Response.ResponseType.WON )
                {
                break;
                }

            /**
             * If we just sent a tile 2 request then check if the
             * move was correct or not
             */
            if( opType == OperationType.TILE2 )
                {
                if( response.getEval() )
                    {
                    System.out.println( "Match!");
                    }
                else
                    {
                    System.out.println( "No match!");
                    }

                } 

            /**
             * Print out the game information 
             * that the server sent us depending
             * on current state
             */            
            if( opType == Request.OperationType.TILE2 && ( response.getResponseType() != ResponseType.ERROR )  )
                {
                System.out.println( response.getFlippedBoard() );
                /**
                 * wait for client to press any key
                 */
                PressAnyKey();
                System.out.println( response.getBoard() );

                }
            else
                {
                System.out.println( response.getFlippedBoard() );
                }


            } while( true );

            /**
             * If we won, print out the message
             */
            if( response.getResponseType() == Response.ResponseType.WON )
                {
                System.out.println( response.getMessage() );
                }
            else
                {
                System.out.println( "Exiting game" );
                }
    }

    /**
     * handleClientGameMove
     * Desc:    Handle a client's game move
     */
    public Response handleClientGameMove
        (
        Request.OperationType move,
        String                strToSend
        )
    {
        /**
         * Build the request object
         */
        Request op = Request.newBuilder()
            .setOperationType( move )
            .setTile( strToSend )
            .build();

        /**
         * Write the request to the server
         */
        writeRequest( op, this.serverSock );

        /**
         * Wait for the server to reply and pass the message
         * back up
         */
        return readResponse( this.serverSock );
    }


    /**
     * printOutLeader
     * Desc:    Print out the leaderboard
     */
    public static void printOutLeader
        (
        Response response
        )
    {
        //TODO print this better
        System.out.println( "Leaderboard:" );
        for( int i = 0; i < response.getLeaderCount(); i++ )
            {
            Entry lb = response.getLeader(i);
            System.out.println
                (
                "Name: " + lb.getName()  + " "
                 + "Wins: " + lb.getWins() + " "
                 + "Logins: " + lb.getLogins()
                );
            }
    }

    /**
     * writeRequest
     * Desc:    Write a request to the server
     */
    public static void writeRequest
        (
        Request request,
        Socket  out
        )
    {
        try
            {
            // write to the server
            request.writeDelimitedTo( out.getOutputStream() );
            } 
        catch (Exception e) 
            {
            log_err( "Error TXing request  "  + request.toString() );

            e.printStackTrace();
            }
    }

    public static Response readResponse
        (
        Socket  in
        )
    {
        Response response = null;
        try
            {
            // read from the server
            response = Response.parseDelimitedFrom( in.getInputStream() );

            } 
        catch (Exception e) 
            {
            log_err( "Error RXing response" );

            e.printStackTrace();
            }

        return response;
    }

    /**
     * Read safe
     */
    private String readSafe
        (
        BufferedReader in
        )
    {
        try
            {
             return in.readLine();
            }
        catch (Exception e)
            {
            log_err( "IO Error occured" );
            }

        return null;
    }

    private boolean safeClose()
        {
        try
            {
            this.serverSock.close();
            this.out.close();
            this.in.close();
            return true;
            }
        catch (Exception e)
            {
            log_err( "Error closing socket" );
            return false;
            }
            
        }

    private Thread getShutDownThread
        (
        //void
        )
    {
        return new Thread() {
            public void run() {
                System.out.println( "Shutting down. Good Bye Now." );
                safeClose();
            }
        };
    }

    public static void PressAnyKey() 
    {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Press enter to continue...\n");
        try {
          input.read();
        } catch (Exception e) {
          e.printStackTrace();
        }
    }
}


