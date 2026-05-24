package net.toload.main.hd.limedb;

public class MemoObj {
    private int id;
    private String content;
    private int pinned;
    private long createdAt;

    public MemoObj() {
    }

    public MemoObj(int id, String content, int pinned, long createdAt) {
        this.id = id;
        this.content = content;
        this.pinned = pinned;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getPinned() {
        return pinned;
    }

    public void setPinned(int pinned) {
        this.pinned = pinned;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
