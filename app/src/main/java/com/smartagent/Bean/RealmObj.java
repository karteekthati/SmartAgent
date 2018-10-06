package com.smartagent.Bean;

import io.realm.RealmObject;

public class RealmObj extends RealmObject {
    // realmObj.setKey("id", String.valueOf(t.getId()));
    //  realmObj.setKey("name", String.valueOf(t.getName()));
    //  realmObj.setKey("type",String.valueOf(t.getType()) );
    //  realmObj.setKey("size", String.valueOf(t.getSizeInBytes()));
    //  realmObj.setKey("path", String.valueOf(t.getCdnPath()));

    public String id;
    public String name;
    public String type;
    public String size;
    public String path;


    // getting Key
    public String getKey(String key) {
        if (key.equals("path")) {
            return this.path;

        }else if (key.equals("id")) {
            return this.id;

        }else if(key.equalsIgnoreCase("type")){
            return this.type;
        }else if (key.equals("name")) {
            return this.name;

        }else if (key.equals("size")) {
            return this.size;
        }

        return "";
    }


    // setting Key
    public void setKey(String key, String value) {

        if (key.equals("path")) {
            this.path = value;

        }else if (key.equals("id")) {
            this.id= value;

        }
        else if(key.equalsIgnoreCase("type")){
            this.type = value;
        }

        else if (key.equals("name")) {
            this.name= value;

        }else if (key.equals("size")) {
            this.size= value;

        }

    }
}




