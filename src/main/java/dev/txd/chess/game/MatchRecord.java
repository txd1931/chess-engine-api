package dev.txd.chess.game;

import java.util.ArrayList;

public class MatchRecord {
  private record MoveData(Move move, boolean cheatsActivated, long timestamp) {}
  private ArrayList<MoveData> moves;
  public MatchRecord() {
    moves = new ArrayList<>();
  }

  public void registerMove(Tile from, Tile to, boolean cheatsActivated, long timestamp) {
    moves.add(new MoveData(new Move(from, to, cheatsActivated), cheatsActivated, timestamp));
  }

  public boolean normalMatch() {
    for (MoveData moveData : moves) {
      if (moveData.cheatsActivated == true)
        return false;
    }
    return true;
  }

}
