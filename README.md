# 網頁字頻計算
目標：使用hadoop MapReduce計算網頁字頻，網頁數量達某種程度。  
方法：先將要計算的網頁搜集到hdfs儲存，再使用FileInputFormat.addInputPath()設定輸入資料夾。交給hadoop預設去分配MapReduce工作。<br/>
To do：<br/>
1. 不下載網頁內容，直接計算各網頁字頻。
2. 改token parsing，如：不計算標點符號

## 結果
編譯
```shell=
/usr/local/hadoop/bin/hadoop com.sun.tools.javac.Main WordCount.java
jar cf wc.jar WordCount*.class
```
執行前檢查是否有WordCount/input資料夾。若沒有
```shell=
bin/hdfs dfs -mkdir WordCount
bin/hdfs dfs -mkdir WordCount/input
```
執行（目前預設只取21個網頁）
```shell=
bin/hadoop jar wc.jar WordCount https://hadoop.apache.org /user/hadoop/wordcount/output
```
結果
```shell=
bin/hadoop fs -cat wordcount/output/part-r-00000
```

## 建置
看`how_to_build.md`  

## 程式碼說明
### 抓網頁
先使用jsoup抓取某網站內的所有連結，再抓取連結網頁的內容。抓取後暫存入input directory(暫定抓上限20個網頁)，再計算這些網頁內容的字頻。
#### 使用jsoup
1. Download jsoup-x.x.x.jar
2. Add `/usr/local/hadoop/jsoupp-x.x.x.jar` to `HADOOP_CLASSPATH`

### 計算字頻
（取自hadoop document example code）

## 可改進的一些想法
1. 不儲存網頁內容，直接分散計算網頁字頻<br/>
先把要計算字頻的**網址**們存成一個一個的檔案，存在HDFS；改寫map method，由原本"計算檔案內字頻"，改成"取得檔案內連結->取得網頁內容->計算字頻"。如此一來，就可以省儲存空間，如果網頁們大小超過HDFS block size。

2. 計算單字、不計標點符號<br/>
可參考[Example: WordCount v2.0](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html#Example:_WordCount_v2.0)
使用distributedCache放置read-only資料（這裡是skip\_pattern\_file），在setup method讀取pattern，然後在map method挑出所有skip pattern替換掉。

## Error

* (Solved) When excecuting `$bin/hadoop jar wc.jar WordCount https://hadoop.apache.org /user/hadoop/wordcount/output`
Executing `bin/hdfs dfs -put etc/hadoop/*.xml input` also causes the same problem
`
File /user/hadoop/input/main\_page.txt could only be wirtten to 0 of the 1 minReplication nodes. There is 0 datanode(s) running and 0 node(s) are excluded in this operation
`
解決方法：是因為容量不足導致。個人推測因虛擬機是採用動態配置空間，所以hadoop誤以爲空間不足，導致無法儲存檔案。
所以我在`/usr/local/hadoop/tmp`多塞一些檔案，讓VirtualBox配置
空間，之後再刪除，便可完整執行。

* (Solved) HADOOP\_MAPRED\_HOME
檢查`/usr/local/hadoop/etc/hadoop/mapred-siet.xml`的`<configuration>`裡面有沒有設定`HADOOP_MAPRED_HOME`
```
    <property>
        <name>yarn.app.mapreduce.am.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
    <property>
        <name>mapreduce.map.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
    <property>
        <name>mapreduce.reduce.env</name>
        <value>HADOOP_MAPRED_HOME=/usr/local/hadoop</value>
    </property>
```

## Reference

[MapReduce Tutorial](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html)  
[Apache Hadoop Main 3.3.0 API](https://hadoop.apache.org/docs/r3.3.0/api/index.html)  
[How to read the contents of a webpage into a string in java?](https://www.tutorialspoint.com/how-to-read-the-contents-of-a-webpage-into-a-string-in-java)  
