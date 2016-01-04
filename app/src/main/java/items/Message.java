package items;

public class Message {
    private String chatDate, chatMessage, chatVerified,senderid;


    public Message(String chatDate, String chatMessage,String chatVerified,String senderid) {
        this.chatDate = chatDate;
        this.chatMessage = chatMessage;
        this.chatVerified = chatVerified;
        this.senderid = senderid;
    }

    public String getchatDate() {
        return chatDate;
    }

    public void setchatDate(String chatDate) {
        this.chatDate = chatDate;
    }

    public String getchatMessage() { return chatMessage; }

    public void setchatMessage(String chatMessage) { this.chatMessage = chatMessage; }

    public String getchatVerified() { return chatVerified; }

    public void setchatVerified(String chatVerified) { this.chatVerified = chatVerified; }

    public String getsenderID() { return senderid; }

    public void setsenderID(String senderid) { this.senderid = senderid; }
}