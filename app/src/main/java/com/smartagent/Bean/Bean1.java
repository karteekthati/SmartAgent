package com.smartagent.Bean;

public class Bean1 {
    private String id;
    private String name;
    private String type;
    private String size;
    private String path;




    //constructor initializing values
    public Bean1(String id, String name, String type, String size, String path) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.size = size;
        this.path = path;
    }

    //getters
    public String getid() {
        return id;
    }
    public String getname() {
        return name;
    }
    public String gettype() {
        return type;
    }
    public String getsize(){
        return  size;
    }
    public String getpath(){return  path;}

}
