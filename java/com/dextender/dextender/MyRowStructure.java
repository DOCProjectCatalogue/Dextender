package com.dextender.dextender;


public class MyRowStructure {
    public Integer  imgIcon;
    public String   txtTitle;
    public String   txtDate;
    public MyRowStructure() {
        super();
    }


    public void thisRow(Integer inImgIcon, String inTxtDate, String inTxtTitle) {
        this.imgIcon  = inImgIcon;
        this.txtDate  = inTxtDate;
        this.txtTitle = inTxtTitle;
    }
}