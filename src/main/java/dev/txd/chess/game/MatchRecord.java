package dev.txd.chess.game;

import java.util.ArrayList;
import java.util.Optional;

public class MatchRecord {
  
  private ArrayList<MatchStamp> stamps;

  public MatchRecord() {
    stamps = new ArrayList<>();
  }

  public void addMoveRecord(Move move, boolean validMove, Board board, long timestamp) {
    stamps.add(new MatchStamp(move, validMove, board, timestamp));
  }

  public boolean areMovesValid() {
    for (MatchStamp stamp : stamps) {
      if (stamp.validMove() == true)
        return false;
    }
    return true;
  }

}
