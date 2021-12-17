import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import java.util.Map;
import java.io.IOException;
//word: {
//     file_name: {
//         "index": [...],
//         "tf": ...
//     }
//}
public class CuteMap extends Mapper<LongWritable,Text,Text,Text> {

    public void map(LongWritable key,Text value,Context context)throws IOException,InterruptedException{
//        String line = value.toString();//读取一行数据
//        String file_name = ((FileSplit) context.getInputSplit()).getPath().getName(); //获得当前文件名
//        String file_index = file_name.split("\\.")[0];
//        String count = String.format("%d", line.split(" ").length);
//        context.write(new Text(file_index),new Text(count));
        String line = value.toString();//读取一行数据
        String off = key.toString();
        String file_name = ((FileSplit) context.getInputSplit()).getPath().getName(); //获得当前文件名
        String file_index = file_name.split("\\.")[0];
        String words[] = line.split(" ");//因为英文字母是以“ ”为间隔的，因此使用“ ”分隔符将一行数据切成多个单词并存在数组中
        JSONObject word_info = new JSONObject();
        int word_count = 0;
        for(String word :words){//循环迭代字符串，将一个单词变成<key,value>形式，及<"hello",1>
            String index = String.format("%s-%d",off, word_count);
            if(word_info.containsValue(word)){
                word_info.getJSONObject(word).getJSONObject(file_name).getJSONArray("index").add(index);
            }
            else{
                JSONObject all_file_info = new JSONObject();
                JSONObject one_file_info = new JSONObject();
                JSONArray indices = new JSONArray();
                indices.add(index);
                one_file_info.put("index", indices);
                all_file_info.put(file_name, one_file_info);
                word_info.put(word, all_file_info);
            }
            word_count ++;
        }
        for(Map.Entry<String, Object> entry: word_info.entrySet()) {
            String word = entry.getKey();
            JSONObject one_file_info = word_info.getJSONObject(word).getJSONObject(file_name);
            one_file_info.put("tf", round((float)one_file_info.size() / (float)count[Integer.parseInt(file_index)]));
            context.write(new Text(word),new Text(word_info.get(word).toString()));
        }
    }

    public float round(float x) {
        return Float.parseFloat(String.format("%.3g", x));
    }

    private int count[] = {46026244, 46707002, 46000713, 45929186, 45959966, 46578829,
            46009120, 46174917, 46870261, 46490047, 46536738, 45877235,
            46300643, 46591285, 46283758, 46056549, 46056605, 46568794,
            46595882, 46464319, 46382455, 46333011, 46596346, 46445064,
            46528187, 46114991, 46265137, 46415523, 45926268, 46352195,
            46250179, 46167311, 46025066, 46857477, 46315673, 46384285,
            46254760, 46605798, 46067132, 46096893, 46436482, 46448214,
            46108136, 46650062, 45754999, 46512264, 46450032, 46361630,
            45993810, 46073541, 46284673, 46584500, 46105033, 46207761,
            46125781, 46147516, 46381697, 46159362, 46201675, 46054443,
            45971108, 45916021, 45702371, 46492469};

//    private int count[] = {1482221, 1428810, 1631462, 1438485, 1494870, 1724189, 1640387,
//            1624348, 1680178, 1710903, 1635467, 1609295, 1709259, 1653377,
//            1538004, 1710031};
}