package database;

//Klasse die ein Element für die Tabelle User darstellt
public class userDbEntry {

    private String USER_ID;
    private String USER_NAME;
    private String USER_PUBLICKEY;

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String id) {
        this.USER_ID = id;
    }

    public String getUSER_NAME() {
        return USER_NAME;
    }

    public void setUSER_NAME(String name) {
        this.USER_NAME = name;
    }

    public String getUSER_PUBLICKEY() {
        return USER_PUBLICKEY;
    }

    public void setUSER_PUBLICKEY(String publickey) {
        this.USER_PUBLICKEY = publickey;
    }
}
