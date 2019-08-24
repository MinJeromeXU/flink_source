package org.jerome.bigdata.datastream.datastream.Broadcast.Util;

import java.io.File;

public class FileUtil {
    public static void delFile(String path){
        File file=new File(path);
        if(file.exists()&&file.isFile())
            file.delete();
    }
}
