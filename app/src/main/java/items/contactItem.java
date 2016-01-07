package items;


public class contactItem {

    private long id;
    private String name;
    private String numbermessages;

    public contactItem(long id, String name, String numbermessages) {
        this.id = id;
        this.name = name;
        this.numbermessages = numbermessages;
    }

    public Long getID(){
        return id;
    }

    public void setID(Long id){
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumbermessages() {
        return numbermessages;
    }

    public void setNumbermessages(String numbermessages) {
        this.numbermessages = numbermessages;
    }
}
