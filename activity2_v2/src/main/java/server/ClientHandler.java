/**
 * Author: Paul Gates
 * 
 * Desc:    Handles low level client I/O
 *          for an individual client
 * 
 *          The main purpose of this module
 *          is to read and write requests
 *          from the client and send them
 *          responses.
 */
 
package server;

import java.net.Socket;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response.Builder;
import buffers.ResponseProtos.Response;

import java.io.InputStream;
import java.io.OutputStream;
import static common.ConsoleLogger.*;
import buffers.ResponseProtos.Entry;
import java.util.ArrayList;

import common.Player;

public class ClientHandler implements Runnable
{

    Socket          socket;
    InputStream     inputStream;  /* read from client */
    OutputStream    outputStream; /* write out to client */
    Player          player;
    ClientIntf      clientIntf;
    boolean         halt = false;

    public ClientHandler
        (
        Socket     socket,      /* means of communication with end user's socket */
        Player     player,      /* player that this handler is responsible for    */
        ClientIntf clientIntf   /* interface with the remainder of the server     */
        )
    {
        /**
         * initialize module variables
         */
        this.player     = player;
        this.socket     = socket;
        this.clientIntf = clientIntf;

        /**
         * set up the input and output streams
         */
        try
        {
        inputStream =  socket.getInputStream();
        outputStream = socket.getOutputStream();
        }
        catch (Exception e)
        {
            System.out.println("Error getting streams");
            System.exit(1);
        }
    }

    @Override
    public void run()
    {
        /**
         * Ensure that this module was created properly
         */
        assert_msg_exit( ( socket != null ), "Socket is null" );
        assert_msg_exit( ( inputStream != null ), "InputStream is null" );
        assert_msg_exit( ( outputStream != null ), "OutputStream is null" );

        /**
         * First greet the new client by writing out a
         * GREET response. 
         */
        Response response = Response.newBuilder()
            .setResponseType( Response.ResponseType.GREETING )
            .setMessage( "Welcome " + player.getName() )
            .build();

        writeResponse( response );

        /**
         * Continue to parse the requests from the client
         */
        while ( !halt )
            {
            /**
             * parse the request 
             * note htat read request will block
             */
            parseRequest( readRequest() );

            }

        /**
         * Remove resources
         * REQ19
         */
        clientIntf.handleClientDead( this );

        /**
         * close the socket
         */
        try
            {
            socket.close();
            }
        catch ( Exception e )
            {
            log_err( "Error closing the socket" );
            }
    }

    /**
     * parseRequest
     * Desc:        Parse the request from the client
     */
    public void parseRequest
        (
        Request request
        )
    {
        /**
         * validate the request
         */
        if( ( request == null                    )
        ||  ( request.getOperationType() == null ) )
            {
            log_err( "The client has sent an invalid request or "
                        + "have disconnected abruptly. kill this client." );

            /**
             * Kill this client
             * REQ19
             */
            this.halt = true;

            return;
            }

        Response response = null;
        
        /**
         * log the request type
         */
        log_msg( "Client Handler: Client is trying to " + request.getOperationType() );

        /**
         * handle client login request
         */
        if( request.getOperationType() == Request.OperationType.NAME )
            {
            /**
             * handle the login request
             */
            response = handleLoginRequest( request.getName() );
            }

        /**
         * handle view leaderboard request
         */
        else if( request.getOperationType() == Request.OperationType.LEADER )
            {
            /**
             * handle the view leaderboard request
             */
            response = clientIntf.handleViewLeaderboardRequest( );
            }
        /**
         * handle play game request
         */
        else if( ( request.getOperationType() == Request.OperationType.NEW   )
              || ( request.getOperationType() == Request.OperationType.TILE1 )
              || ( request.getOperationType() == Request.OperationType.TILE2 ) ) 
            {
            /**
             * handle the play game request
             */
            log_msg( "Client Handler: Client is trying to play game move " + request.getOperationType() );
            response = clientIntf.handleGameRequest( this, request );
            }
        /**
         * handle exit request
         */
        else if( request.getOperationType() == Request.OperationType.QUIT )
            {
            /**
             * handle the exit request
             */
            response = clientIntf.handleQuitRequest( this );
            }
        /**
         * handle unknown request
         */
        else
            {
            /**
             * handle the unknown request
             */
            response = handleUnknownRequest( request );
            }

        /**
         * write the response to the client
         */
        writeResponse( response );
    }

    /**
     * handleLoginRequest
     */
    private Response handleLoginRequest
        (
        String name
        )
    {
        return null;

    }

    /**
     * handleUnknownRequest
     *
     */
    private Response handleUnknownRequest
        (
        Request request
        )
    {
        /**
         * create a response
         */
        Response response = Response.newBuilder()
            .setResponseType( Response.ResponseType.ERROR )
            .setMessage( "Unknown request" )
            .build();

        return response;
    }

    /*
     * readRequest
     * Read a request from the client
     */
    private Request readRequest
        (
        //void
        )
    {
        /**
         * Read the request from the client
         */
        Request request = null;
        try
            {
            request = Request.parseDelimitedFrom( inputStream );
            }
        catch ( Exception e )
            {
            e.printStackTrace();
            }

        return request;
    }

    private boolean writeResponse
        (
        Response response
        )
    {

        /*
         * Check to see if this is an error response, if it is, 
         * go ahead and colorize the message red. 
         * copy over the remaining fields even though for error
         * messages they probably will not be used.
         */
        if( response.getResponseType() == Response.ResponseType.ERROR )
            {
            response = Response.newBuilder( response )
                .setMessage( "\033[31m" + response.getMessage() + "\033[0m" )
                .setBoard( response.getBoard() )
                .setResponseType( response.getResponseType() )
                .setFlippedBoard( response.getFlippedBoard() )
                .setSecond( response.getSecond() )
                .build();
            }

        /**
         * input validation
         */
        if( response == null )
            {
            return false;
            }


        /**
         * log the response
         */
        log_msg( "Client Handler: Sending response to client " + response );

        try
            {
            response.writeDelimitedTo( outputStream );
            }
        catch ( Exception e )
            {
            e.printStackTrace();
            }

        /**
         * return to caller that the response was
         * sent successfully
         */
        return true;
    }

    /**
     * hasHeartBeat
     * 
     * Desc:   Used to report if this client is still alive.
     *         preiodically called in this task
     */
    public boolean hasHeartBeat
        (
        //void
        )
    {
    /**
     * If the socket is closed there is no
     * way to still communicate with the client
     * for the time being
     */
    if( socket.isClosed()  )
        {
        return false;
        }

    /**
     * Otherwise report that everything is ok
     */
    return true;
    }

    /**
     * getPlayer
     */
    public Player getPlayer
        (
        //void
        )
    {
    return player;
    } 

}
