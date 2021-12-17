import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.io.Text;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.Map;

import java.io.IOException;

// word: {
//     file_name: {
//         "index": [...],
//         "tf": ...,
//         "tf-idf": ...
//     },
//     ...,
//     "idf": ...,
// }

public class CuteReduce extends Reducer<Text,Text,Text,Text> {
    public void reduce(Text key, Iterable<Text> values,Context context)throws IOException,InterruptedException{
//        String file_index = key.toString();
////        int count = 0;
////        for(Text value: values) {
////            count += Integer.parseInt(value.toString());
////        }
////        context.write(new Text(file_index),new Text(String.format("%d", count)));
        JSONObject word_info = new JSONObject();
        for(Text value: values) {
            String word = key.toString();
            JSONObject one_file_info =  JSON.parseObject(value.toString()); //{file_name : ...}
            for(Map.Entry<String, Object> entry:one_file_info.entrySet()) {
                String file_name = entry.getKey();
                JSONObject info = one_file_info.getJSONObject(file_name);
                if(word_info.containsKey(word)) { //word已经存在于word info
                    JSONObject existed_file_info = word_info.getJSONObject(word); // {file_name : ..., ...}
                    if(existed_file_info.containsKey(file_name)) { //已有的和新file_name重复，做合并处理
                        JSONObject existed_info = existed_file_info.getJSONObject(file_name);
                        existed_info.getJSONArray("index").addAll(info.getJSONArray("index")); //更新index
                        existed_info.put("tf", round(existed_info.getFloat("tf") + info.getFloat("tf"))); //更新tf
                    }
                    else{ //已有的和新的file_name不同，加入新的
                        existed_file_info.putAll(one_file_info);
                    }
                }
                else {
                    word_info.put(word, one_file_info);
                }
            }
        }

        for(Map.Entry<String, Object> entry:word_info.entrySet()) {
            String word = entry.getKey();
            JSONObject all_file_info = (JSONObject)entry.getValue();
            float idf = round((float)file_number / (float)all_file_info.size());
            for(Map.Entry<String, Object> _entry: all_file_info.entrySet()) {
                JSONObject one_file_info = (JSONObject) _entry.getValue();
                one_file_info.put("tf-idf", round(one_file_info.getFloat("tf") * idf));
            }
            all_file_info.put("idf", idf);
            context.write(new Text(word),new Text(word_info.get(word).toString()));
        }
    }

    public float round(float x) {
        return Float.parseFloat(String.format("%.3g", x));
    }

    private final int file_number = 64;
//    private final int file_number = 16;
}