package io.guthub.spafka.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static io.github.spafka.util.MdUtils.displayArticle;

public class Mds {


    public static void main(String[] args) {
        String newMd=displayArticle("spf.md");
        String newPath="spf.md".split(".md")[0]+"_Upload.md";//args的第一个参数作为目标md文件的路径
        try{
            File file =new File(newPath);
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWritter = new FileWriter(file.getName());
            fileWritter.write(newMd);
            fileWritter.close();
            System.out.println("finish");
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
