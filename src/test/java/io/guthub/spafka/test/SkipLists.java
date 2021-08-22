package io.guthub.spafka.test;

import io.github.spafka.ds.SkipList;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentSkipListMap;

public class SkipLists {

    @Test
    public void _1(){
        SkipList<String> stringSkipList = new SkipList<>();

        stringSkipList.insert("1");
        stringSkipList.insert("2");
        stringSkipList.insert("3");
        stringSkipList.insert("11");
        System.out.println();
    }

    @Test
    public void _2(){


        ConcurrentSkipListMap concurrentSkipListMap=new ConcurrentSkipListMap();
        concurrentSkipListMap.put(1,"2");
        concurrentSkipListMap.put(-1,"2");
        concurrentSkipListMap.put(1,"3");

        System.out.println();
    }


    @Test
    public void _3() throws IOException {

        long count = Files.lines(new File("~/Desktop/authentication-integration 2.log").toPath()).filter(x -> x.contains("/service-api/auth/v1/user?companyId=1000&userName=sch")).count();

        System.out.println(count);
    }



}
