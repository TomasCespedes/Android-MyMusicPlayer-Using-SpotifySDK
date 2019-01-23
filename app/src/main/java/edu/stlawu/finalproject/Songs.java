package edu.stlawu.finalproject;

public class Songs {
    private String songname;
    private int songduration;
    private String songartist;
    private String songalbum;
    private String songcover;
    private String songuri;

    public Songs(String songuri) {
        this.songuri = songuri;
    }

    public String getSongname() {
        return songname;
    }

    public int getSongduration() {

        return songduration;
    }

    public String getSongartist() {
        return songartist;
    }

    public String getSongalbum() {
        return songalbum;
    }

    public String getSongcover() {
        return songcover;
    }

    public String getSonguri() {
        return songuri;
    }
}
