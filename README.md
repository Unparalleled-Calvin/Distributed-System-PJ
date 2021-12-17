## 分布式系统项目报告

### 项目功能

- hadoop框架下，利用mapreduce实现输入多文件、输出词语的倒排索引以及tf-idf统计
- 利用django框架配置前端展示页面

### 详细实现

#### 架构

- map：接受**一行**文本，处理出每个单词的倒排索引和tf信息

  - ```
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

    ```
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

- map读入的是某文件一行的数据，所以要计算某词的tf，需要将改词在同一文件中不同行计算出的tf累加(reduce第一步中完成)。

- 为了计算tf，需要预处理出每个文件的总词数。我将该预处理结果以数组形式写在代码中，并以文件名作为下标索引数组。

- hadoop的输入输出需要是Writeable的派生类，出于简化代码的考虑，以字符串形式(Text)传输。

- 为了方便编解码，使用fastjson包，能够解码json字符串以及创建json对象。

- 实际试验中发现，json字符串为了保留float数据的精度，倾向于保留多位有效数字。为了缩减最终文件的规模，编写round()函数保留float三位有效数字。

  ```java
  public float round(float x) {
      return Float.parseFloat(String.format("%.3g", x));
  }
  ```

#### 前端效果

<img src="https://s2.loli.net/2021/12/16/gdK4x5u7we2hFST.png" alt="image-20211216121721630"  />

<img src="https://s2.loli.net/2021/12/16/O9qnBVKY7uXeFL2.png" alt="image-20211216121922105"  />

<img src="https://s2.loli.net/2021/12/16/u2SWdTPE3MLQ8yY.png" alt="image-20211216121658036"  />

