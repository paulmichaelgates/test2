package common;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class: Player 
 * Description: Class that represents a Player, I only used it in my Client 
 * to sort the LeaderBoard list
 * You can change this class, decide to use it or not to use it, up to you.
 */

public class Player implements Comparable<Player> 
{
    private int    wins;
    private String name;

    // constructor, getters, setters
    public Player
      (
      String name,
      int wins
      )
    {
      this.wins = wins;
      this.name = name;
    }

    public int getWins
      (
      //void
      )
    {
      return wins;
    }

    public String getName
      (
      //void
      )
    {
      return name;
    }

    // override equals and hashCode
    @Override
    public int compareTo
      (
      Player player
      )
    {
      return ( int )( player.getWins() - this.wins );
    }

    @Override
    public String toString
      (
      //void
      )
    {
      return ("\n" +this.wins + ": " + this.name);
    }

    public void incWins
      (
      //void
      )
    {
      this.wins++;
    }
}