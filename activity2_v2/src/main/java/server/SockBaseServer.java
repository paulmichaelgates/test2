/**
 * Author: Paul Gates
 * 
 * Desc:    Low level server I/O (Threaded)
 */
package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.lang.Thread;
import static common.ConsoleLogger.*;
import static common.DataTypeLoggerUtil.*;
import common.Player;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;

// TODO pmg threadify this
/**
 * Class: SockBaseServer
 * Description: Server tasks.
 */
public class SockBaseServer {

    /**
     * Local variables
     */
    ClientIntf   clientIntf;
    ServerSocket server;

    ArrayList< ClientHandler > clientFarm = new ArrayList< ClientHandler >();
    
    /**
     * SockBaseServer
     */
    public SockBaseServer
        (
        int port
        )
    {
        /**
         * initialize the client interface
         */
        clientIntf = new ClientIntf( this );

        /**
         * Create a new server socket and client socket.
         * Wait for the client to connect and then 
         * create a thread that we will use from here on
         * to communicate with this client
         */
        try
            {
            server = new ServerSocket( port );
            }
        catch ( Exception e )
            {
            log_err( "Error creating the server socket" );
            return; /* kills this task */
            }
        
        log_success( "Server Started..." );

        /**
         * Run the main server loop
         */
        serverMain();
    }

    /**
     * Main server loop
     */
    public void serverMain
        (
        //void
        )
    {
    /**
     * Ensure that the server was initialized
     * before starting the main loop
     */
    if ( server == null )
        {
        log_err( "Server not initialized" );
        return;
        }

    /**
     * Loop forever accepting clients
     */
    while ( true ) 
        {
        log_msg("Accepting a Request...");

        /**
         * Accepting the client
         */
        Socket sock;
        
        try
            {
            sock = server.accept();
            setUpNewClient( sock );
            }
        catch ( Exception e )
            {
            log_err( "Error accepting the client" );
            continue;
            }
        
        /**
         * Check on all of the existing clients
         */
        periodicClientHealthCheck( );
        }
    }

    /**
     * setUpNewClient
     * 
     * Description: Sets up a new client
     */
    public void setUpNewClient
        (
        Socket sock
        )
    {
    /**
     * Input handling
     */
    if( sock == null )
        {
        log_err( "Socket is null" );
        return;
        }
    
    System.out.println("Client Connected...");

    Request request  = null;

    /**
     * Read in the player name
     */
    try
        {
        request = Request.parseDelimitedFrom( sock.getInputStream() );
        }
    catch ( Exception e )
        {
        log_err( "Error reading the request from the client" );
        }

    /**
     * Check to see if the client sent a valid request
     */
    if( request == null )
        {
        log_err( "Client sent an invalid request" );

        /**
         * send back an error response
         */
        Response response = Response.newBuilder()
            .setResponseType( Response.ResponseType.ERROR )
            .setMessage( "Invalid Request: nullptr" )
            .build();
        
        writeOutToClient( sock, response );

        /**
         * nothing else for us to do
         */
        return;
        }
    
    /**
     * trim the name of the client
     */
    if( request != null )
        {
        request = request.toBuilder()
            .setName( request.getName().trim() )
            .build();
        }

    /**
     * Log the request recieved from the client
     */
    log_request( request );

    /**
     * Check to see if the client's name is valid
     */
    if( !nameIsValid( request.getName() ) )
        {
        /**
         * Send the client an error response
         * letting them know that a person 
         * with this name is already logged in
         */
        Response response = Response.newBuilder()
            .setResponseType( Response.ResponseType.ERROR )
            .setMessage( "\u001B[36mA client with the name  " + request.getName() +  " is already logged in. "
                        + "Or, you have chosen a name that has an illegal symbol. Please use A-Za-z "
                        + "and ensure name is not already logged in on the server. Thank you.\u001B[36m" )
            .build();

    
        /**
         * Send the response to the client
         */
        writeOutToClient( sock, response );

        /**
         * Nothing else for us to do since client is invalid
         */
        return;
        }

    /**
     * update the login status and get the
     * players stats to create the player
     * object
     * (thread safe)
     */
    Leaderboard.updateLogins( request.getName() );
    Player player = Leaderboard.getPlayer( request.getName() );

    /**
     * If the player has player before, log them
     */

    /**
     * create the thread. Ensure that the common client interface
     * object is passed to the client.
     */
    ClientHandler client = new ClientHandler( sock, player, clientIntf );
    Thread t = new Thread( client );

    /**
     * Add the client to the client farm
     */
    clientFarm.add( client );

    t.start();
    }

    /**
     * retireClient
     * 
     * Desc:  reporting procedure for when a client is done
     */
    public void retireClient
        (
        ClientHandler client
        )
    {
    /**
     * Remove the client from the client farm
     */
    clientFarm.remove( client );

    /**
     * let the other clients know that this client is gone
     */
    broadcast( client.getPlayer().getName() + " has left the game" );

    }

    /**
     * periodicClientHealthCheck
     *
     * Desc:  checks to see if all the clients are still alive
     */
    //TODO pmg  do this if there is time
    public void periodicClientHealthCheck
        (
        //void
        )
    {
    /**
     * Check all the clients in the farm
     * to see if they have a heartbeat
     */
    // for( ClientHandler client : clientFarm )
    //     {
    //     if( client.hasHeartBeat() == false )
    //         {
    //         retireClient( client );
    //         }
    //     }
    }

    /**
     * broadcast
     * 
     * Desc:  broadcasts a message to all clients
     */
    public void broadcast
        (
        String msg
        )
    {
    //TODO pmg  do this if there is time, not a requirement and will only
    //TODO pmg  show up on my end unless sent as an error and even that is not a
    //TODO pmg  garauntee
    
    }

    /**
     * nameIsValid
     * 
     * Desc:  check the client farm for existing user
     */
    public boolean nameIsValid
        (
        String name
        )
    {
    /**
     * Check all the clients in the farm
     * to see if there is a match for this name
     */
    for( ClientHandler client : clientFarm )
        {
        if( client.getPlayer().getName().equals( name ) )
            {
            /**
             * The name already exists in the client farm
             */
            return false;
            }
        }

    /**
     *  Check for names with symbols
     * !@#$%^&*()_+{}|:"<>?[]\;',./
     */
    if( name.matches( ".*[!@#$%^&*()_+{}|:\"<>?\\[\\]\\\\;',./].*" ) )
        {
        return false;
        }

    /**
     * The name is valid
     */
    return true;
    }

    private void writeOutToClient
        (
        Socket sock,
        Response response
        )
    {
    /**
     * Input handling
     */

    if( sock == null )
        {
        log_err( "Socket is null" );
        return;
        }
    
    try
        {
        response.writeDelimitedTo( sock.getOutputStream() );
        }
    catch ( Exception e )
        {
        log_err( "Could not write to client at port " + sock.getPort() );
        }
    }
}