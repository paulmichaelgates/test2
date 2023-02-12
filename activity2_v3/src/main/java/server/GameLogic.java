/**
 * Author: Paul Gates
 * 
 * Desc:   Game logic module
 * 
 * Notes:  The game module does not care how many
 *         clients are connected. It will simply 
 *         respond to moves initiated by the clients
 *         and send output back up to the server which
 *         will communicate back to the clients
 */
package server;

import java.io.FileReader;
import java.io.IOException;

import common.ConsoleLogger;
import server.GameError;

import java.io.Console;
import java.io.File;
import buffers.ResponseProtos.Response.ResponseType;
import buffers.ResponseProtos.Response;

public class GameLogic 
{
    /*
     * Constants
     * //TODO for debug only
     */
    final public String USR_STR_SLCT_TILE1 = "";

    final public String USR_STR_SLCT_TILE2 = "";

    final public String USR_STR_GAME_OVER  =  "";
    /**
     * local types
     */
    public enum GameState
    {
        GAME_STATE_UNINITIALIZED,
        GAME_STATE_INITIALIZED,
        GAME_STATE_TILE1_SELECTED,
        GAME_STATE_TILE2_SELECTED,
        GAME_STATE_GAME_OVER,
    }
    
    class Board /* container for boards that's really it */
    {
        public String currentBoard;    /* board that is being used in the game */
        public int    size;            /* size of the board */
        public String answerBoard;     /* board that is used to check the answer */
        public int    x;               /* horizontal dimension component */
        public int    y;               /* vertical dimension component */
        public String flippedBoard;    /* board that is used to show flips */
        public int    tile1Idx;        /* index of the first tile selected */
        public int    tile2Idx;        /* index of the second tile selected */

        /**
         * Constructor
         */
        public Board
            (
            )
        {
            /**
             * Load a board from a file
             */
            readInAnswerBoard();

            /**
             * For now hard code the dimensions
             * of the board to be 4x4
             */
            this.x = 4;
            this.y = 4;

            /**
             * Check that th board is valid
             */
            if( answerBoard == null )
                {
                ConsoleLogger.log_err( "Game Logic: Invalid board" );
                }

            createFreshCurrentBoard();

            /**
             * initialize the flipped board to the
             * freshly created board
             */
            this.flippedBoard = this.currentBoard;

            /**
             * Check on our boards
             */
            ConsoleLogger.assert_msg_exit( this.currentBoard.length() == this.size, "Invalid current board" );
            ConsoleLogger.assert_msg_exit( this.answerBoard.length() == this.size, "Invalid answer board : "  + this.answerBoard.length() + ", Expected : " + this.size );
        }

        /**
         * Creates a fresh board
         */
        public void createFreshCurrentBoard()
            {

            /**
             * Check that the answer board has been read
             * in first
             */
            ConsoleLogger.assert_msg_exit( this.answerBoard != null, "Answer board not read in" );
            ConsoleLogger.assert_msg_exit( this.answerBoard.length() == this.size, "Invalid answer board" );
            ConsoleLogger.assert_msg_exit( this.size > 0, "Zero length board" );
            /**
             * Create a fresh board
             */
            this.currentBoard = "";
            for( int i = 0; i < this.size; i++ )
                {
                this.currentBoard += "?";
                }
            }
    
        /**
         * Reads in a board from a file
         */
        public void readInAnswerBoard
            (
            //void
            )
        {
        /**
         * Read in the file
         */
        File f = new File("prototype_board.txt");
        this.answerBoard = "";

        try
            {
            FileReader fileReader = new FileReader(f);

            /**
             * Read the file to the end
             */
            int i;
            while( ( i = fileReader.read() ) != -1 )
                {
                this.answerBoard += (char)i;
                }
            ConsoleLogger.log_success( "Game Logic: Board loaded from file: " + this.answerBoard );

            /**
             * Update the length
             */
            this.size = this.answerBoard.length();

            }
        catch( IOException ex )
            {
            ConsoleLogger.log_err( "Game Logic: Could not read the input file" );
            this.answerBoard = null; /* return invalid data */
            }

        }

        /**
         * Debug Procedures
         */
        public void print_raw
            (
            //void
            )
        {
        ConsoleLogger.print_cyan( "Current Board: " + this.currentBoard );
        ConsoleLogger.print_yellow( "Answer Board: " + this.answerBoard );
        }

    } /* Board */

        
    /**
     * Local variables
     */
    Board     board;
    GameState gameState = GameState.GAME_STATE_UNINITIALIZED;
    boolean   second_flag = false;
    boolean   eval_flag = false;
    /**
     * GameLogic
     */
    public GameLogic
        (
        //void
        )
    {
    /**
     * Create a new board
     */
    this.board = new Board( );

    /**
     * Check that the board was created successfully
     */
    if( this.board == null )
        {
        ConsoleLogger.log_err( "Game Logic: Could not create a new board" );
        }

    /**
     * Set the game state to initialized
     */
    this.gameState = GameState.GAME_STATE_INITIALIZED;

    }

    /**
     * perform2D move
     */
    public GameError perform2DMove //TODO pmg return an error here
        (
        String           rawMoveStr    /* User facing game move. i.e.,      */
        )
    {
    /**
     * Input validation
     */
    if( ( rawMoveStr == null )
    ||  ( rawMoveStr.length() < 2 ) )
        {
        ConsoleLogger.log_err( "Game Logic: Invalid move string" + rawMoveStr );
        return GameError.ERR_INVALID_MOVE;
        }

    /**
     * Check that the string is 2 characters long
     */
    if( rawMoveStr.length() != 2 )
        {
        ConsoleLogger.log_err( "Game Logic: Invalid move string format" );
        
        /**
         * send back an error code
         */
        return GameError.ERR_INVALID_MOVE;
        }

    /*
     * Convert the string into a 2D move
     * that can be used in the game
     */
    int y = (int)rawMoveStr.charAt( 0 ) - 97;
    System.out.println( "Y: " + y);

    /**
     * Check that the y is valid
     */
    if( ( y < 0 )
    ||  ( y >= this.board.y ) )
        {
        ConsoleLogger.log_err( "Game Logic: Invalid y move" );
        return GameError.ERR_INVALID_MOVE;
        }

    /**
     * Get the y move
     */
    int x = (int)rawMoveStr.charAt( 1 ) - 49;
    System.out.println( "X:" + x );

    /**
     * Check that the x is valid
     */
    if( ( x < 0 )
    ||  ( x >= this.board.x ) )
        {
        ConsoleLogger.log_err( "Game Logic: Invalid x move" );
        return GameError.ERR_INVALID_MOVE;
        }

    /**
     * Convert the 2D move to a 1D move
     */
    int idx_1d = perform2DMoveConversion( x, y );

    /**
     * update the flipped board to reveal the letter 
     * that was at the selected index in the actual 
     * answer board
     */ 
    String tmp_flipboard = this.board.flippedBoard.substring( 0, idx_1d ) 
                            + this.board.answerBoard.charAt( idx_1d ) 
                            + this.board.flippedBoard.substring( idx_1d + 1 );


    /**
     * check to see if this move has already been made
     * a sufficient condition of a move already being 
     * made is that the flipped board has not changed 
     * after a valid move is made
     */
    if( this.board.flippedBoard.equals( tmp_flipboard ) )
        {
        ConsoleLogger.log_err( "Game Logic: Move already made" );
        return GameError.ERR_MOVE_ALREADY_MADE;
        }


    /**
     * otherwise update the flipped Board
     */
    ConsoleLogger.log_msg( "Game Logic: Flipped Board before move: " + this.board.flippedBoard );
    this.board.flippedBoard = tmp_flipboard;
    ConsoleLogger.log_msg( "Game Logic: Flipped Board after move: " + this.board.flippedBoard );

    /**
     * update the index based on if we are Tile1 or
     * Tile2
     */
    if( this.gameState == GameState.GAME_STATE_TILE1_SELECTED )
        {
        this.board.tile1Idx = idx_1d;
        }
    else if( this.gameState == GameState.GAME_STATE_TILE2_SELECTED )
        {
        this.board.tile2Idx = idx_1d;
        }
    else
        {
        ConsoleLogger.assert_msg_exit( ( false ), "Invalid Game State" );
        }

    ConsoleLogger.log_success( "Game Logic: Flipped Board after move: " + this.board.flippedBoard );

    return GameError.ERR_NONE;
    }

    /**
     * perform2DMoveConversion
     * 
     * Desc:    Converts a 2D move to a 1D move
     */
    public int perform2DMoveConversion
        (
        int x,
        int y
        )
    {
    /**
     * Convert the 2D move to a 1D move
     */
    int idx = ( y * this.board.x ) + x;

    return idx;
    }

    /**
     * gameStateMachine
     * 
     * Desc:    This is the game state machine. It will take in some
     *          high level input from the user and send back a response
     *          to the caller (server)
     */
    public Response gameStateMachine
        (
        GameIntf          gameData  /* Game input into the SM */
        )
    {
    /**
     * Local vairables
     */
    GameError        err                    = GameError.ERR_NONE;       /* error code             */
    Response         out_data               = null;                      /* output data            */
    GameState        transitionGameState    = null;                     /* game state to go into  */
                                                                        /*  if everything goes ok */
    String           userMessage            = null;                     /* client facing message  */
                                                                        /*  for normal case if an */
                                                                        /*  error occurs error msg*/
                                                                        /*  will be returned      */     
                                                                        
    ConsoleLogger.log_success( "Game Logic: Recieved move type: " + gameData.move_type );
    ConsoleLogger.log_success( "The current game state is: " + this.gameState );

    System.out.println( beautify( this.board.answerBoard ) );
    /**
     * Determine what kind of move this is
     */
    switch( gameData.move_type )
        {
        /**
         * New Game
         * This is a bit of a misnomer. This new request really means
         * that another player wants to play the game and not that we
         * should start a new game. Because this module has no concept
         * of multuiple players, we do not need to worry about handling
         * new requests to play the game. We will just send back a
         * response that contains the game board and any additional info
         */
        case MV_TYPE_NEW:
            /**
             * Send back the necessary response so that
             * the game can be played
             */
            String directionString   = "Error initializing this game"; // this will vary upon when the user enters the game
            String boardStringToSend = "";
            if( gameState == GameState.GAME_STATE_TILE1_SELECTED || gameState == GameState.GAME_STATE_INITIALIZED )
                {
                directionString = USR_STR_SLCT_TILE1;
                boardStringToSend = this.board.flippedBoard;
                }
            else if( gameState == GameState.GAME_STATE_TILE2_SELECTED )
                {
                directionString = USR_STR_SLCT_TILE2;
                boardStringToSend = this.board.flippedBoard;
                }
            else
                {
                ConsoleLogger.assert_msg_exit( ( false ), "Invalid Game State at MV_TYPE_NEW" );
                }

            Response ret = Response.newBuilder()
                            .setResponseType( ResponseType.PLAY )
                            .setMessage( "Welcome to the game\n" + directionString )
                            .setBoard( getCurrentBoard() )
                            .setEval( false )
                            .setSecond( false )
                            .build();

            /**
             * Set the game state for the next move//TODO fix this
             */
            if( gameState == GameState.GAME_STATE_INITIALIZED )
                {
                this.gameState = GameState.GAME_STATE_TILE1_SELECTED;
                }

            return ret;

        /**
         * First Move
         */
        case MV_TYPE_TILE1:
            /**
             * Check to make sure that the game state is correct
             * for this type of move
             */
            if(        this.gameState != GameState.GAME_STATE_TILE1_SELECTED 
            && ( 
                    (  this.gameState == GameState.GAME_STATE_TILE2_SELECTED )  
                ||  (  this.gameState == GameState.GAME_STATE_GAME_OVER ) )                 
                )
                {
                ConsoleLogger.log_msg( "state change detected. Should not ever run with only one player");
                return getResponseForAsyncGamePlay();
                }
    
            /**
             * Play the move
             */
            err = perform2DMove( gameData.game_move );

            if( err != GameError.ERR_NONE )
                {
                String err_msg = "";;
                ConsoleLogger.log_err( err_msg  );
                return getErrorWithMessage( err_msg, err );
                }

            /**
             * Ensure that the second flag is true indicating that we are
             * waiting for the second move now
             */
            this.second_flag = true;

            break;

        case MV_TYPE_TILE2:
            /**
             * Check to make sure that the game state is correct
             * for this type of move
             */
            if(  this.gameState != GameState.GAME_STATE_TILE2_SELECTED 
            && ( 
                    (  this.gameState == GameState.GAME_STATE_TILE1_SELECTED ) 
                ||  ( this.gameState == GameState.GAME_STATE_GAME_OVER ) )                  
                )
                {
                ConsoleLogger.log_msg( "state change detected. Should not ever run with only one player");
                return getResponseForAsyncGamePlay();
                }
    
            /**
             * Play the move
             */
            err = perform2DMove( gameData.game_move );

            if( err != GameError.ERR_NONE )
                {
                String err_msg = "Game Logic: Error performing move";
                ConsoleLogger.log_err( err_msg );
                return getErrorWithMessage( err_msg, err  );
                }

            /**
             * Now we can determine if the move was a match or not
             */
            if(     this.board.flippedBoard.charAt( this.board.tile1Idx ) 
                 == this.board.flippedBoard.charAt( this.board.tile2Idx ) )
                {
                /**
                 * Update the current game board to now be the flipped
                 * board
                 */
                this.board.currentBoard = this.board.flippedBoard;
                ConsoleLogger.log_success( "Game Logic: Current Board after second move " + this.board.currentBoard );

                /**
                 * Update the eval flag
                 */
                this.eval_flag = true;
                }
            else
                {
                /**
                 * Update the eval flag
                 */
                this.eval_flag = false;
                }
            /**
             * Reset the second flag
             */
            this.second_flag = false;

            break;

        default:
            ConsoleLogger.log_err( "Game Logic: Invalid move type" );
            break;
        }

    /**
     * The response type will depend on if the player
     * has won or not. it is okay to assume that there
     * are no errors at this point as we would have
     * already caught it
     * 
     * REQ
     */
    ResponseType rs_type = null;
    if( checkWin() )
        {
        /**
         * Set the game state to won and override
         * the user message
         */
        rs_type = ResponseType.WON;
        userMessage = "You won! Check to leaderboard to see your points.";

        /**
         * update the game state
         */
        this.gameState = GameState.GAME_STATE_GAME_OVER;

        }
    
    out_data = getResponseForAsyncGamePlay();

    /**
     * validation
     */
    boolean expeced_second_flag = ( this.gameState == GameState.GAME_STATE_TILE2_SELECTED ) ? true : false;
    ConsoleLogger.assert_msg_cont( out_data != null, "Game Logic: Error creating response" );
    ConsoleLogger.assert_msg_cont( this.second_flag == expeced_second_flag , "Game Logic: Second flag does not match expected" );

    /**
     * Return the response to the caller
     */
    return out_data;
    
    } /* gameStateMachine() */

    /*
     * getResponseForAsyncGamePlay
     * 
     */
    public Response getResponseForAsyncGamePlay
        (
        
        )
    {
    /**
     * Local variables
     */
    Response out_data = null;

    /**
     * depending on the current game state
     * we will need to send different responses
     */
    switch( this.gameState )
        {
        case GAME_STATE_TILE1_SELECTED:
            /**
             * If the game state is tile1 selected then we need to
             * send the board and the flipped board
             */
            out_data = Response.newBuilder()
                            .setResponseType( ResponseType.PLAY )
                            .setMessage( USR_STR_SLCT_TILE1 )
                            .setBoard( beautify( this.board.currentBoard ) )
                            .setFlippedBoard( beautify( this.board.flippedBoard ) )
                            .setEval( this.eval_flag )
                            .setSecond( this.second_flag ) /* always overrides the second flag to the current */
                            .build();                      /*    value of the second flag from game perpective  */

            /**
             * Transition to the next game state
             */
            this.second_flag = true;
            this.gameState = GameState.GAME_STATE_TILE2_SELECTED;
            break;

        case GAME_STATE_TILE2_SELECTED:

            /**
             * If the game state is tile2 selected then we need to
             * send the board and the flipped board
             */
            out_data = Response.newBuilder()
                            .setResponseType( ResponseType.PLAY )
                            .setMessage( USR_STR_SLCT_TILE2 )
                            .setBoard( beautify( this.board.currentBoard ) )
                            .setFlippedBoard( beautify( this.board.flippedBoard ) )
                            .setEval( this.eval_flag )
                            .setSecond( this.second_flag ) /* always overrides the second flag to the current */
                            .build();                     /*    value of the second flag from game perpective  */
            /**
             * Transition to the next game state
             */
            this.second_flag = false;
            this.gameState = GameState.GAME_STATE_TILE1_SELECTED;
            break;


        case GAME_STATE_GAME_OVER:

            /**
             * If the game state is game over then we need to
             * send the board and the flipped board
             */
            out_data = Response.newBuilder()
                            .setResponseType( ResponseType.WON )
                            .setMessage( USR_STR_GAME_OVER )
                            .setBoard( beautify( this.board.currentBoard ) )
                            .setFlippedBoard( beautify( this.board.flippedBoard ) )
                            .setEval( this.eval_flag )
                            .setSecond( this.second_flag ) /* always overrides the second flag to the current */
                            .build();                     /*    value of the second flag from game perpective  */
            break;

        default:
            ConsoleLogger.log_err( "Game Logic: Invalid game state" );
            break;
        
        }

    return out_data;

    }
    /*
     * get current game board
     */
    public String getCurrentBoard
        (
        //void
        )
    {
        return beautify( this.board.currentBoard );
    }

    /**
     * beautify
     * 
     * Desc:    This function will take in a string and
     *          return a string that is formatted to be
     *          human readable
     */
    public String beautify
        (
        String in
        )
    {
    // /**
    //  * Local variables
    //  */
    // String out = new String();

    // /**
    //  * Use the X and Y coordinates of the board
    //  * to determine how to format the board
    //  */
    // int cols = this.board.x;

    // /**
    //  * Iterate through the board and format it
    //  */
    // for( int i = 0; i < in.length(); i++ )
    //     {
    //     /**
    //      * Add the character to the output string
    //      */
    //     out += in.charAt( i );

    //     /**
    //      * If we are at the end of a row add a new line
    //      */
    //     if( ( i + 1 ) % cols == 0 )
    //         {
    //         out += "\n";
    //         }
    //     }

    /**
     * make the board look like this
     *   1 2 3 4
        a a|c|e|z
        b f|w|z|c
        c e|a|w|f
     */
    String out = new String();

    /**
     * Use the X and Y coordinates of the board
     * to determine how to format the board
     */
    int cols = this.board.x;
    int rows = this.board.y;

    /**
     * add the column numbers to the top
     */
    out += "  ";
    for( int i = 0; i < cols; i++ )
        {
        out += ( i + 1 ) + " ";
        }

    /**
     * add the row letters to the left
     */
    out += "\n";

    /**
     * Iterate through the board and format it
     */
    for( int i = 0; i < in.length(); i++ )
        {
        /**
         * If we are at the beginning of a row add the row letter
         */
        if( i % cols == 0 )
            {
            out += (char)( 'a' + ( i / cols ) ) + " ";
            }

        /**
         * Add the character to the output string
         */
        out += in.charAt( i );

        /**
         * If we are at the end of a row add a new line
         */
        if( ( i + 1 ) % cols == 0 )
            {
            out += "\n";
            }
        else
            {
            out += "|";
            }
        }

    return out;
    }  /* beautify() */

    /**
     * getErrorWithMessage
     */
    public Response getErrorWithMessage
        (
        String message,
        GameError err
        )
    {
        /**
         * handle null error
         */
        if( err == null )
            {
            err = GameError.ERR_UNKNOWN;
            }
    
        /**
         * Depending on the error append a nice message
         */
        switch( err )
            {
            case ERR_MOVE_ALREADY_MADE:
                message += "\nPlease select a tile that is not yet flipped";
                break;

            case ERR_INVALID_MOVE:
                message += "\nInvalid move. Check your that your intended move matches "
                            +"a coordinate on the board";
                break;

            case ERR_INVALID_STATE:
                message += "\nLooks like the game board has changed, you'll need to press enter to"
                             + "refresh the board. ";
                           
                break;

            case ERR_UNKNOWN: //FALL THROUGH
            default:
                message += "\nUnknown error";
                break;
            }

        /**
         * Call the other getErrorWithMessage
         */
        return getErrorWithMessage( message );
    }

    /**
     * getErrorWithMessage
     */
    public Response getErrorWithMessage
        (
        String message
        )
    {

    /**
     * check the current game state
     */
    String state_msg;
    if( this.gameState == GameState.GAME_STATE_TILE1_SELECTED )
        {
        state_msg = USR_STR_SLCT_TILE1;
        }
    else if( this.gameState == GameState.GAME_STATE_TILE2_SELECTED )
        {
        state_msg = USR_STR_SLCT_TILE2;
        }
    else
        {
        state_msg = "Game Logic: Invalid game state. You found a bug!";
        ConsoleLogger.log_err( "Game Logic: Invalid game state" );
        }

    /**
     * Create the response
     */
    return Response.newBuilder()
                    .setResponseType( ResponseType.ERROR )
                    .setMessage( message  + "\n" + state_msg )
                    .setBoard( beautify( this.board.currentBoard ) )
                    .setFlippedBoard( beautify( this.board.flippedBoard ) )
                    .setEval( false )
                    .setSecond( this.second_flag )
                    .build();

        
    }

    /*
     * checkWin
     * Desc: A win occurs when the answer board is equal to the current board
     */
    public boolean checkWin
        (
        //void
        )
    {
    /**
     * Local variables
     */
    boolean win = false;

    /**
     * Check to see if the current board is equal to the answer board
     */
    if( this.board.currentBoard.equals( this.board.answerBoard ) )
        {
        win = true;
        }
    
    return win;

    } /* checkWin() */

}
