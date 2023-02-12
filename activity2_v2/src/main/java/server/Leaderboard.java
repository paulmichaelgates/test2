/**
 * Author:  Paul Gates
 * Desc:    Handles leaderboard IO and manipulation
 */
package server;

import java.util.ArrayList;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import buffers.ResponseProtos.Entry;

import static common.ConsoleLogger.*;

import common.Player;

public class Leaderboard 
{
    /**
     * Writes the leaderboard to a file
     * Thread safe
     */
    public synchronized static void updateWins
        (
        String name,
        int wins
        )
    {
        /**
         * Grab a reference to the leaderboard
         */
        ArrayList<Entry> leaderboard = getLeaderboard();

        /**
         * Iterate over leaderboard and see if the
         * name is already in the leaderboard
         */
        for ( int i = 0; i < leaderboard.size(); i++ )
            {
            Entry stat = leaderboard.get( i );

            /**
             * If the name is already in the leaderboard
             * then update the wins
             */
            if ( stat.getName().equals( name ) )
                {
                /**
                 * Update the wins
                 */
                int updatedWins = stat.getWins() + wins;
                stat = Entry.newBuilder()
                    .setName( name )
                    .setWins(  updatedWins )
                    .setLogins( stat.getLogins() ) // ensure previous logins are not lost
                    .build();

                /**
                 * Update the leaderboard
                 */
                leaderboard.set( i, stat );

                /**
                 * Write the leaderboard to a file
                 */
                writeLeaderboard( leaderboard );

                return;
                }
            }

        /**
         * If the name is not in the leaderboard
         * then add it to the leaderboard and set
         * the wins to 1
         */
        Entry stat = Entry.newBuilder()
            .setName( name )
            .setWins( 1 )
            .build();

            
        /**
         * Update the leaderboard
         */
        leaderboard.add( stat );
            

        /**
         * Write the leaderboard to leaderboard.json
         */
        writeLeaderboard( leaderboard );
            
    }

    //TODO PMG this method is awfully similar to updateWins, need to refactor when/if there is time
    /**
     * update logins
     * Desc:    is called when a player logs in. first 
     *          we will try and find the playe that
     *          logged in in the leaderboard. if we
     *          find the player then we will update
     *          the logins. if we do not find the
     *          player then we will add the player
     *          and set the logins to 1
     */
    public synchronized static void updateLogins
        (
        String name
        )
    {
        /**
         * Grab a reference to the leaderboard
         */
        ArrayList<Entry> leaderboard = getLeaderboard();

        /**
         * Iterate over leaderboard and see if the
         * name is already in the leaderboard
         */
        for ( int i = 0; i < leaderboard.size(); i++ )
            {
            Entry stat = leaderboard.get( i );

            /**
             * If the name is already in the leaderboard
             * then update the wins
             */
            if ( stat.getName().equals( name ) )
                {
                /**
                 * Update the wins
                 */
                int updatedLogins = stat.getLogins() + 1;
                stat = Entry.newBuilder()
                    .setName( name )
                    .setLogins(  updatedLogins )
                    .build();

                /**
                 * Update the leaderboard
                 */
                leaderboard.set( i, stat );

                /**
                 * Write the leaderboard to a file
                 */
                writeLeaderboard( leaderboard );

                return;
                }
            }

        /**
         * If the name is not in the leaderboard
         * then add it to the leaderboard and set
         * the wins to 1
         */
        Entry stat = Entry.newBuilder()
            .setName( name )
            .setLogins( 1 )
            .build();

            
        /**
         * Update the leaderboard
         */
        leaderboard.add( stat );
            

        /**
         * Write the leaderboard to leaderboard.json
         */
        writeLeaderboard( leaderboard );
            
    }

    /**
     * getPlayer
     * 
     * Desc:    using the nonvol leaderboard, find an
     *          entry with the provided name and Return
     *          a player object
     */
    public synchronized static Player getPlayer
        (
        String name
        )
    {
        /**
         * Grab a reference to the leaderboard
         */
        ArrayList<Entry> leaderboard = getLeaderboard();

        /**
         * Iterate over leaderboard and see if the
         * name is already in the leaderboard
         */
        for ( int i = 0; i < leaderboard.size(); i++ )
            {
            Entry stat = leaderboard.get( i );

            /**
             * If the name is already in the leaderboard
             * then update the wins
             */
            if ( stat.getName().equals( name ) )
                {
                /**
                 * Create a new player object
                 */
                Player player = new Player( name, stat.getWins() );
                return player;
                }
            }

        /**
         * If the name is not in the leaderboard
         * then application is in an undefined
         * state
         * 
         * will not return from this call
         */
        assert_msg_exit( false , "No entry was found for current player." );

        return null;
    }


    /**
     * Writes the leaderboard to a file
     * Thread safe.
     */
    public synchronized static void writeLeaderboard
        (
        ArrayList<Entry> leaderboard
        )
    {
        /**
         * Create a new JSON object
         */
        JSONObject leaderboardData = new JSONObject();

        /**
         * Create a new JSON array
         */
        JSONArray playersArray = new JSONArray();

        /**
         * Iterate over all the players that are in the leaderboard
         * and add them to the players array
         */
        for ( int i = 0; i < leaderboard.size(); i++ )
            {
            Entry stat = leaderboard.get( i );

            /**
             * Create a new JSON object
             */
            JSONObject statData = new JSONObject();

            /**
             * Add the name and wins to the JSON object
             */
            try
                {
                statData.put( "name", stat.getName() );
                statData.put( "wins", stat.getWins() );
                statData.put( "logins", stat.getLogins() );
                }
            catch (JSONException e)
                {
                log_err( "Error adding name and wins to JSON object" );
                e.printStackTrace();
                }

            /**
             * Add the JSON object to the JSON array
             */
            playersArray.put( statData );

            }

        /**
         * Add the JSON array to the JSON object
         */
        try
            {
            leaderboardData.put( "stats", playersArray );
            }
        catch (JSONException e)
            {
            log_err( "Error adding players array to JSON object" );
            e.printStackTrace();
            }



        /**
         * Write the JSON object to a file
         */
        try
            {
            File f = new File("leaderboard.json");
            FileWriter writer = new FileWriter( f.getCanonicalPath() );
    
            writer.write( leaderboardData.toString() );

            writer.close();
            }
        catch( IOException e )
            {
            log_err( "Error writing leaderboard to leaderboard.json" );
            e.printStackTrace();
            }

    }


    /**
     * Reads in the leaderboard from a file
     * 
     */
    public static ArrayList<Entry> getLeaderboard
        (
        //void
        )
    {
        /**
         * Local variables
         */
        ArrayList<Entry> stats = new ArrayList<Entry>();
        String ouString           = "";

        try
            {
            /**
             * Read in the file
             */
            File f = new File("leaderboard.json");

            FileReader reader = new FileReader( new File( f.getCanonicalPath() ) );
    
            int i;

            while ( ( i = reader.read() ) != -1)
                {
                ouString += (char) i;
                }
            }
        catch (IOException e)
            {
            log_err( "Error reading in leaderboard.json returning empty list of entries" );
            
            e.printStackTrace();
            
            return null;
            }


        JSONObject leaderboardData = new JSONObject( ouString );

        JSONArray playersArray = leaderboardData.getJSONArray("stats");

        /**
         * Iterate over all the players that are in the leaderboard
         * and add them to the players array
         */
        for ( int i = 0; i < playersArray.length(); i++ )
            {
            JSONObject statData = playersArray.getJSONObject( i ); 

            /**
             * Create a new Entry
             */
            Entry stat = Entry.newBuilder()
                .setName( statData.getString("name") )
                .setWins( statData.getInt("wins") )
                .setLogins( statData.getInt("logins") )
                .build();

            stats.add( stat );
            }

        return stats;
    }

}
