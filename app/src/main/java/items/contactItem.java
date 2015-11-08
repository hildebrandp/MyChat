package items;


public class contactItem {

    private long id;
    private String name;
    private String date;

    public contactItem(long id, String name, String date) {
        this.id = id;
        this.name = name;
        this.date = date;
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

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}
