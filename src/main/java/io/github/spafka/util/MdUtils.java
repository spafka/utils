package io.github.spafka.util;

import java.io.*;
import java.util.Base64;

public class MdUtils {


    public static String displayArticle(String path){//本方法完成从md文件中寻找图片本地链接并转码为base64编码后return 新的String内容。
        InputStreamReader isr = null;
        String encoding="UTF-8";
        BufferedReader bufferedReader = null;
        String basePath=null;
        //找到md文件的父目录，更简单的做法是使用：
        /*
                 File dest=new File(path);
                dest.getParentFile();
                来获取
        */
        for(int i=path.length();i>0;i--){
            if(path.substring(i-1,i).equals("/")){
                basePath=path.substring(0,i);
                break;
            }
            if(i==1){
                basePath="";
            }
        }
        File file=new File(path);
        StringBuffer sb = new StringBuffer();
        try {
            if(file.isFile() && file.exists()){ //判断文件是否存在
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file),encoding);//考虑到编码格式
                bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                //the base64 encoder makes photos to base64 code,not the "src=*.png/jpg" patten
                final Base64.Encoder encoder = Base64.getEncoder();
                while((lineTxt = bufferedReader.readLine()) != null){
                    if (lineTxt.contains("<img src=")){//第一种图片格式
                        String imgName=lineTxt.split("\"")[1];
                        String imgType=imgName.split("\\.")[1];
                        String encodedText=generateBase64ForImg(basePath,imgName,encoder);
                        lineTxt=getFullImgHTMLLabel(imgType,encodedText);
                    }else if((lineTxt.contains("![")&&lineTxt.contains("]("))){//第二种图片格式
                        String imgName=lineTxt.split("]\\(")[1];
                        String basePathAlias=basePath;//if image name contains '/',
                        // it means that it doesn't saved in the same directory with the md file.
                        // so we need the absolute path.
                        if (imgName.contains("/")){
                            basePathAlias="";
                        }
                        imgName=imgName.substring(0,imgName.length()-1);
                        String imgType=imgName.split("\\.")[1];
                        String encodedText=generateBase64ForImg(basePathAlias,imgName,encoder);
                        lineTxt=getFullImgHTMLLabel(imgType,encodedText);
                    }
                    sb.append(lineTxt);
                    sb.append("\n");
                }
                read.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    //only called by displayArticle 将base64编码图片组合成md认识的格式
    public static String getFullImgHTMLLabel(String imgType, String encodedText){
        return "<img src=\"data:image/"+imgType+";base64,"+encodedText+"\" />";
    }

    //only called by display...  真正的图片转码函数
    public static String generateBase64ForImg(String basePath, String imgName, Base64.Encoder encoder) throws Exception {
        FileInputStream fis=new FileInputStream(basePath+imgName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bb=new byte[1024];
        int length=0;
        while((length=fis.read(bb))!=-1){
            baos.write(bb,0,length);
        }
        byte[] imgBinary=baos.toByteArray();
        String encodedText = encoder.encodeToString(imgBinary);
        return encodedText;
    }
}
