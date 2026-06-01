package vgu.pe2026.ttt.basis;
import vgu.pe2026.ttt.basis.Board;

public abstract class Player {

    protected int mark;
    protected String name;

    public Player(int mark, String name) {
        this.mark = mark;
        this.name = name;
    }

    public int getMark() { return mark; }
    public String getName() { return name; }

    public abstract int chooseCell(Board board);
}