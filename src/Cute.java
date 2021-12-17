import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.io.Text;

public class Cute {
    public static void main(String[] args)throws Exception{
        Configuration conf = new Configuration();
        //获取运行时输入的参数，一般是通过shell脚本文件传进来。
        String [] otherArgs = new GenericOptionsParser(conf,args).getRemainingArgs();

        if(otherArgs.length < 2){
            System.err.println("必须输入读取文件路径和输出路径");
            System.exit(2);
        }
        Job job = new Job();
        job.setJarByClass(Cute.class);
        job.setJobName("Cute app");

        //设置读取文件的路径，都是从HDFS中读取。读取文件路径从脚本文件中传进来
        FileInputFormat.addInputPath(job,new Path(args[0]));

        //设置mapreduce程序的输出路径，MapReduce的结果都是输入到文件中
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        //设置实现了map函数的类
        job.setMapperClass(CuteMap.class);

        //设置实现了reduce函数的类
        job.setReducerClass(CuteReduce.class);

        //设置reduce函数的key值
        job.setOutputKeyClass(Text.class);
        //设置reduce函数的value值
        job.setOutputValueClass(Text.class);

        System.exit(job.waitForCompletion(true) ? 0 :1);
    }
}