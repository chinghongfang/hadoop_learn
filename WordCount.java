import java.io.IOException;
import java.util.StringTokenizer;
import java.io.FileWriter;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;

public class WordCount {
    /******* Self defined ********/
    public static final int max_page = 20;
    public static void crawl(String url, Configuration conf, String store_path){
        Connection conn = Jsoup.connect(url);
        // Executing the get request
        // may throw IOException
        Document doc;
        try{
            doc = conn.get();
        }catch (Exception ex){
            System.out.println("Crawl connect fail.");
            return;
        }

        // Retrieving the contents
        String content = doc.body().text();
        try {
            FileSystem fs = (new Path("/user/hadoop")).getFileSystem(new Configuration());
            FSDataOutputStream stream = fs.create(new Path(store_path + "/main_page.txt"));
            byte[] text = content.getBytes();
            stream.write(text);
            fs.close();
        }catch (IOException ex){
            System.out.println("ioex");
            System.out.println(ex.toString());
        }

        // Retrieving all the hrefs
        Elements links = doc.select("a[href]");
        int count = 0;
        // Browse all links, and download them.
        for (Element link : links){
            // control the max web pages
            ++count;
            if (count > max_page) break;

            if (url == link.attr("abs:href")) continue;
            conn = Jsoup.connect(link.attr("abs:href"));
            try {
                // Write content to directory
                FileSystem fs = (new Path("/user/hadoop")).getFileSystem(new Configuration());
                FSDataOutputStream stream = fs.create(new Path(store_path + "/" + link.text()));
                byte[] text = conn.get().body().text().getBytes();
                stream.write(text);
                fs.close();
            }catch (IOException ex){
                System.out.println("ioex");
                System.out.println(ex.toString());
            }
        }
    }
    public static class TokenizerMapper
        extends Mapper<Object, Text, Text, IntWritable>{

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        public void map(Object key, Text value, Context context
                        ) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                context.write(word, one);
            }
        }
    }

    public static class IntSumReducer
        extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
                          ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        // Start getting web content.
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Web get");
        // Store many website in sequence
        crawl(args[0], conf, "WordCount/input");

        // hadoop example code
        conf = new Configuration();
        job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path("/user/hadoop/WordCount/input"));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
