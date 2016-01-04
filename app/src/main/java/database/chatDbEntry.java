package database;


public class chatDbEntry {
    private long id;
    private long CHAT_ID;
    private String CHAT_SENDER_ID;
    private String CHAT_RECIEVER_ID;
    private String CHAT_MESSAGE;
    private String CHAT_READ;
    private String CHAT_DATE;
    private String CHAT_ISSEND;
    private String CHAT_AESKEY;
    private String CHAT_SIGNATURE;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCHAT_ID() {
        return CHAT_ID;
    }

    public void setCHAT_ID(long id) {
        this.CHAT_ID = id;
    }

    public String getCHAT_SENDER_ID() {
        return CHAT_SENDER_ID;
    }

    public void setCHAT_SENDER_ID(String CHAT_SENDER_ID) {
        this.CHAT_SENDER_ID = CHAT_SENDER_ID;
    }

    public String getCHAT_RECIEVER_ID() {
        return CHAT_RECIEVER_ID;
    }

    public void setCHAT_RECIEVER_ID(String CHAT_RECIEVER_ID) {
        this.CHAT_RECIEVER_ID = CHAT_RECIEVER_ID;
    }

    public String getCHAT_MESSAGE() {
        return CHAT_MESSAGE;
    }

    public void setCHAT_MESSAGE(String CHAT_MESSAGE) {
        this.CHAT_MESSAGE = CHAT_MESSAGE;
    }

    public String getCHAT_READ() {
        return CHAT_READ;
    }

    public void setCHAT_READ(String CHAT_READ) {
        this.CHAT_READ = CHAT_READ;
    }

    public String getCHAT_DATE() {
        return CHAT_DATE;
    }

    public void setCHAT_DATE(String CHAT_DATE) {
        this.CHAT_DATE = CHAT_DATE;
    }

    public String getCHAT_ISSEND() {
        return CHAT_ISSEND;
    }

    public void setCHAT_ISSEND(String CHAT_ISSEND) {
        this.CHAT_ISSEND = CHAT_ISSEND;
    }

    public String getCHAT_AESKEY() {
        return CHAT_AESKEY;
    }

    public void setCHAT_AESKEY(String CHAT_AESKEY) {
        this.CHAT_AESKEY = CHAT_AESKEY;
    }

    public String getCHAT_SIGNATURE() {
        return CHAT_SIGNATURE;
    }

    public void setCHAT_SIGNATURE(String CHAT_SIGNATURE) {
        this.CHAT_SIGNATURE = CHAT_SIGNATURE;
    }


}
