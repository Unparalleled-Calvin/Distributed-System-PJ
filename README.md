## 分布式系统项目报告

##### 姓名:崔晨昊  学号:19307130084

### 项目功能

- hadoop框架下，利用mapreduce实现输入多文件、输出词语的倒排索引以及tf-idf统计
- 利用django框架配置前端展示页面

### 详细实现

#### 架构

- map：接受**一行**文本，处理出每个单词的倒排索引和tf信息

  - ```json
    // map输出value格式
    {
        file_name: {
            "index": [...],
            "tf": ...,
    	}
    }
    ```

- reduce：

  - 第一步，汇总map的信息，将信息合并。由于map的一条输出仅对应于某文件的一行，所以需要对word相同且文件相同的信息做聚合。

  - 第二步，计算idf以及tf-idf

    ```json
    // reduce输出value格式
    {
        file_name: {
            "index": [...],
            "tf": ...,
            "tf-idf": ...
        },
        ...,
        "idf": ...,
    }
    ```

#### 细节与优化

##### Mapreduce

阅读实验文档及要求可知，map、reduce的输入输出是以key-value形式，对于value中需要包含index、tf等信息，那么非常自然地，我想到以hashmap或者json的形式处理数据。这种表示形式简单明晰，处理起来也非常方便。

为了计算tf，需要预处理出每个文件的总词数。我将该预处理结果以数组形式在代码中打表，并以文件名作为下标索引数组。

wiki语料的文件名格式统一为"数字.txt"，为了获得处理文件名，我使用string.split()方法。split中的参数是一个正则表达式。一开始我以"."作为分隔符，后来发现需要用"\\\\."进行处理。

在hadoop框架下，map的分割非常有意思，对一一个大文件来说，hadoop会按照一个预设的参数作为分割大小将文件切分。但是对于本语料来说，文本在文件中是已经按行排布的。所以在hadoop中，map会以行为输入单位，实际上，每次调用map都是处理某个文件中单独的一行。所以利用map计算tf肯定无法得到正确结果——因为每次输出都只是对应于一行的词频。

为了聚合该结果，我在reduce中做了两次遍历循环。第一次遍历以word为key建立json对象，对每一个map的输出进行解码、更新json对象的操作。对map的结果聚合完毕后，reduce之后会计算idf以及tf-idf的结果，完成最后一环。

hadoop的输入输出需要是Writeable的派生类，对于hashmap的支持还不够完备。出于简化代码的考虑，我的代码中数据以字符串形式(Text)传输。为了方便编解码，我使用阿里开发的fastjson包，能够解码json字符串以及创建json对象。json对象内置了一个hashmap，提供了一系列访问数据的方法接口。

另外，实际试验中发现，json字符串为了保留float数据的精度，倾向于保留多位有效数字。为了缩减最终文件的规模，编写round()函数保留float三位有效数字。

```java
public float round(float x) {
    return Float.parseFloat(String.format("%.3g", x));
}
```

最后，hadoop默认只有一个reducer，为了提高并行度，设置多个reducer进行处理。

##### django

后端读入预处理出的文件，并以字典的数据结构存储。通过search函数

该部分我提供了一个搜索框页面，当用户进行输入时，页面会自动发送ajax请求到后端，将以该词开头的相关搜索结果都列举出来作为提示方便用户使用。

对于结果的显示，使用html的table组件进行整个结果的层次化描述。

### 项目不足

在计算tf时，我使用的是整个txt文件的词频，而不是使用某行，即一个文档的数据进行计算，造成了tf、idf的数据不准确。

改进：在计算map时，tf的标准化应该以改行次数作为标准。在reduce中，总文档数不是固定的16，而应该预先进行处理。

### 启动方式

在django的管理界面执行`python manage.py runserver 0.0.0.0:8000`，即可开启服务器。在浏览器访问127.0.0.1即可。

### 前端效果

<img src="https://s2.loli.net/2021/12/16/gdK4x5u7we2hFST.png" alt="image-20211216121721630"  />

<img src="https://s2.loli.net/2021/12/16/O9qnBVKY7uXeFL2.png" alt="image-20211216121922105"  />

<img src="https://s2.loli.net/2021/12/16/u2SWdTPE3MLQ8yY.png" alt="image-20211216121658036"  />

### 代码附录

map函数：

```java
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
```

reduce函数：

```java
public void reduce(Text key, Iterable<Text> values,Context context)throws IOException,InterruptedException{
//        String file_index = key.toString();
//        int count = 0;
//        for(Text value: values) {
//            count += Integer.parseInt(value.toString());
//        }
//        context.write(new Text(file_index),new Text(String.format("%d", count)));
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
```

view.py:

```py
from django.shortcuts import render, HttpResponse
from django.http import HttpResponseRedirect
from django.http import JsonResponse

# Create your views here.

import json
word_info = {}
all_words = []
with open(r"C:\Users\admin\Desktop\Distributed\mini_result.txt", encoding="utf8") as f:
    for line in f:
        word, info = line.split("\t")
        all_words.append(word)
        info = json.loads(info)
        word_info[word] = info
all_words.sort()

def display(request):
    response = render(request, "display.html")
    return response

def complete(request):
    def like(pattern, max_display=10):
        cnt = 0
        words = []
        for word in all_words:
            if word.startswith(pattern):
                words.append(word)
                cnt += 1
                if cnt == max_display:
                    break
        return words
    pattern = request.POST["pattern"]
    words = like(pattern)
    return JsonResponse({"words" : words})

def search(request):
    word = request.POST["word"]
    ret_info = {}
    temp_info = {}
    for key, value in word_info[word].items():
        if key != "idf":
            temp_info[key] = value
    ret_info["idf"] = word_info[word]["idf"]
    ret_info["indices"] = [temp_info]
    return JsonResponse({"word_info" : ret_info})
```

