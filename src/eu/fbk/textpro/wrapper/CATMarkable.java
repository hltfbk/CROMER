package eu.fbk.textpro.wrapper;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 02/04/14
 * Time: 10.01
 */
public class CATMarkable {
    private String naming="";
    private String mid="";
    private String classname="";
    private String tokenIDs="";
    private String ent_type="";
    private String value="";

    public CATMarkable(String mid, String classname, String naming) {
        this.mid = mid;
        this.classname = classname;
        this.naming = naming;
    }

    public String getNaming() {
        return naming;
    }

    public String getMid() {
        return mid;
    }

    public String getClassname() {
        return classname;
    }

    public String getTokenIDs() {
        return tokenIDs;
    }

    public String getEnt_type() {
        return ent_type;
    }

    public String getValue() {
        return value;
    }

    public void setNaming(String naming) {
        this.naming = naming;
    }

    public void setTokenIDs(String tokenIDs) {
        this.tokenIDs = tokenIDs;
    }

    public void setEnt_type(String ent_type) {
        this.ent_type = ent_type;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
