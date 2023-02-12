/**
 * Author: Paul Gates
 * Desc:   Provides the function definitions
 *         that are used to fufill client requests
 * 
 * Notes:  This module deals with asynchronous requests
 *         from clients
 */

package server;

import static common.ConsoleLogger.*;
import buffers.ResponseProtos.Entry;
import java.util.ArrayList;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response.Builder;
import buffers.ResponseProtos.Response.ResponseType;
import buffers.ResponseProtos.Response;

import server.ClientHandler;
import server.GameLogic;
import server.Leaderboard;

import server.GameLogic;
import server.GameIntf;
import server.MoveType;

import common.Player;

public class ClientIntf 
{
    /**
     * Constants
     */
    final SockBaseServer server;

    /**
     * Variables
     */
    GameLogic           gameLogic;
    ArrayList< Player > activePlayers = new ArrayList<>(); /* game logic doesn't care about  */
                                                          /*   how many players there are   */
                                                          /*   so handle multiple players   */
                                                          /*   here                         */

    /**
     * ClientIntf
     */
    public ClientIntf
        (
        SockBaseServer server
        )
    {
    
    /**
     * Assign reporting server
     */
    this.server = server;
    }

    /**
     * handleViewLeaderboardRequest
     */
    public synchronized Response handleViewLeaderboardRequest
        (
        //void
        )
    {
    /**
     * read in the leaderboard from the
     * leader board module
     */
    ArrayList<Entry> leaderboard = Leaderboard.getLeaderboard();

    /**
     * create a response which includes this leaderboard
     */
    Builder responseBuilder = Response.newBuilder();

    /**
     * add the leaderboard to the response one
     * Entry at a time
     */
    for( Entry entry : leaderboard )
        {
        responseBuilder.addLeader( entry );
        }

    /**
     * Add the response type. The client will expect
     * the same response type as the request type
     */
    responseBuilder.setResponseType( Response.ResponseType.LEADER );

    return responseBuilder.build();

    }

    /**
     * handleQuitRequest
     */
    public synchronized Response handleQuitRequest
        (
        ClientHandler clientHandler
        )
    {
    retireClient( clientHandler );

    /**
     * Since a response which inclues the bye message type
     */
    Builder responseBuilder = Response.newBuilder()
                                            .setResponseType
                                                    ( Response.ResponseType.BYE );

    /**
     * Retire the client
     */
    server.retireClient( clientHandler );

    return responseBuilder.build();
    }


    /**
     * handleGameRequest
     * REQ4
     */
    public synchronized Response handleGameRequest
        (
        ClientHandler   clientHandler,
        Request         rqst
        )
    {
    /**
     * check for player in active players
     */
    if( !activePlayers.contains( clientHandler.getPlayer() ) )
        {
        /**
         * Yay, the player is now in the game
         */
        log_success( clientHandler.getPlayer().getName() + " has joined the game" );
        activePlayers.add( clientHandler.getPlayer() );
        }
    
    /* 
     * Check to see if there is a game currently
     * going on
    */
    if( gameLogic == null )
        {
        /**
         * Create a new game
         */
        log_msg( "A fresh game has started" );
        gameLogic = new GameLogic( );
        }
    
    /**
     * Play the game move
     */
    Response response =  playGameMoveWithRequest( rqst );

    /**
     * Check to see if the game is over
     */
    if( response.getResponseType() == ResponseType.WON )
        {
        /**
         * call game over logic
         */
        gameOver( );
        }

    return response;
    }

    /**
     * playGameMoveWithRequest
     */
    public synchronized Response playGameMoveWithRequest
        (
        Request rqst
        )
    {
    /**
     * Local variables
     */
    GameIntf gameIntf = new GameIntf();

    /**
     * parse the game move from the client request
     */
    gameIntf.move_type = translateOperation( rqst.getOperationType() );
    gameIntf.game_move = rqst.getTile();        /* pass through game move, might 
                                                be empty if starting new game */

    /*
     * handle unkown game move
     */                                       
    if( gameIntf.move_type == MoveType.MV_TYPE_UNK )
        {
        /**
         * Log the error and return
         * the error response
         */
        log_err( "Unknown game move" );

        return Response.newBuilder()
                        .setResponseType( Response.ResponseType.ERROR )
                        .build();

        }

    log_msg( "Server: passing game logic to game module...." );
    return gameLogic.gameStateMachine( gameIntf );

    }

    public MoveType translateOperation
        (
        Request.OperationType operationType
        )
    {
    switch( operationType )
        {
        case NEW:
            return MoveType.MV_TYPE_NEW;
        case TILE1:
            return MoveType.MV_TYPE_TILE1;
        case TILE2:
            return MoveType.MV_TYPE_TILE2;

        /**
         * No other operation types are used in
         * this context
         */
        default:
            return MoveType.MV_TYPE_UNK;
        }
    }

    /**
     * handleClientDead
     */
    public synchronized void handleClientDead
        (
        ClientHandler clientHandler
        )
    {
    /**
     * retire the client
     */
    this.retireClient( clientHandler );

    /*
     * Let the server know to remove
     * this client as well
     */
    server.retireClient( clientHandler );
    }

    /**
     * retireClient
     */
    private synchronized void retireClient
        (
        ClientHandler clientHandler
        )
    {
    /**
     * Remove this player from the list of players
     * playing the game if a game is on going
     */
    if( gameLogic != null )
        {
        removePlayer( clientHandler.getPlayer() );
        }
    
    }


    /**
     * removePlayer
     */
    private synchronized void removePlayer
        (
        Player player
        )
    {
    this.activePlayers.remove( player );
    }

    /**
     * addPlayer
     */
    private synchronized void addPlayer
        (
        Player player
        )
    {
    this.activePlayers.add( player );
    }

    /**
     * gameOver
     */
    private void gameOver
        (
        )
    {
        /**
         * Update all of the players in the game
         * and give them a point. update the leaderboard
         */
        for( Player player : activePlayers )
            {
            player.incWins();
            Leaderboard.updateWins( player.getName(), player.getWins() );
            }

        /**
         * Retire the game and clear all of the players
         * from the active players list
         */
        gameLogic = null;
        activePlayers.clear();
    }


}
